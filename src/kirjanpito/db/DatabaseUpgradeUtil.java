package kirjanpito.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kirjanpito.models.DataSourceInitializationModel;

public class DatabaseUpgradeUtil {
	public static void executeQueries(Connection conn, InputStream stream) throws IOException, SQLException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream,
				Charset.forName("UTF-8")));

		StringBuffer buf = new StringBuffer();
		String line;

		while ((line = reader.readLine()) != null) {
			buf.append(line.trim());
		}

		String[] queries = buf.toString().split(";");
		Statement stmt = conn.createStatement();

		for (String query : queries) {
			stmt.execute(query);
		}

		conn.commit();
		stmt.close();
	}

	public static void upgrade3to4(Connection conn, Statement stmt) throws SQLException {
		ResultSet rs = stmt.executeQuery("SELECT name FROM account WHERE number = '9000'");
		boolean updateCOA = false;

		if (rs.next()) {
			if (rs.getString(1).equals("Osinkotuotot konserniyrityksiltä")) {
				updateCOA = true;
			}
		}

		rs.close();

		if (updateCOA) {
			rs = stmt.executeQuery("SELECT id FROM account WHERE number = '29391'");
			int alvMyynnistaId = -1;

			if (rs.next()) {
				alvMyynnistaId = rs.getInt(1);
			}

			/* Tulotili -> menotili */
			stmt.executeUpdate("UPDATE account SET type=4 WHERE number IN ('3500', '3510', '3520', '3550', '3560', '3570', '3580', '3590')");

			/* Menotili -> tulotili */
			stmt.executeUpdate("UPDATE account SET type=3 WHERE number IN ('4230', '4240', '4260', '4270', '4280', '4340', '4350', '4360')");
			stmt.executeUpdate("UPDATE account SET type=3 WHERE number IN ('5470', '5480', '5770', '5780', '5960', '5970', '5990', '7060')");
			stmt.executeUpdate("UPDATE account SET type=3 WHERE number IN ('7100', '7440', '7510', '8610', '8990', '9360', '9410')");
			stmt.executeUpdate("UPDATE account SET type=3 WHERE number >= '9000' AND number < '9300'");
			stmt.executeUpdate("UPDATE account SET type=3 WHERE number >= '9700' AND number < '9740'");

			if (alvMyynnistaId > 0) {
				stmt.executeUpdate("UPDATE account SET vat_code=4, vat_account1_id=" + alvMyynnistaId + " WHERE number IN ('9700', '9710')");
			}
		}

		stmt.executeUpdate("UPDATE settings SET version=4");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 4 onnistui");
	}

	public static void upgrade4to5(Connection conn, Statement stmt) throws SQLException {
		/* Lukitus pois kaikista tilikausista. */
		stmt.executeUpdate("UPDATE period SET locked=0");

		/* Edellisten tilikausien voitto -tilin tyypiksi Account.TYPE_PROFIT. */
		stmt.executeUpdate("UPDATE account SET type=5 WHERE name LIKE 'Edellisten tilikausien voitto%'");

		stmt.executeUpdate("UPDATE settings SET version=5");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 5 onnistui");
	}

	public static void upgrade5to6(Connection conn, Statement stmt) throws IOException, SQLException {
		boolean coaAmmatinharjoittaja = false;
		boolean coaYksityistalous = false;

		/* Korjataan tuloslaskelman virheet. */
		ResultSet rs = stmt.executeQuery("SELECT data FROM report_structure WHERE id = 'income-statement'");

		if (rs.next()) {
			String content = rs.getString(1);
			rs.close();
			coaAmmatinharjoittaja = content.indexOf("Muut välittömät verot") >= 0;

			if (coaAmmatinharjoittaja) {
				content = content.replace("TP0;9980;9980;Muut välittömät verot",
						"TP0;9980;9990;Muut välittömät verot");
				PreparedStatement upd = conn.prepareStatement(
						"UPDATE report_structure SET data=? WHERE id = 'income-statement'");
				upd.setString(1, content);
				upd.executeUpdate();
				upd.close();
			}
			else {
				coaYksityistalous = content.indexOf("Asuminen") >= 0;
			}
		}
		else {
			rs.close();
		}

		/* Korjataan taseen virheet. */
		rs = stmt.executeQuery("SELECT data FROM report_structure WHERE id = 'balance-sheet'");

		if (rs.next()) {
			String content = rs.getString(1);
			rs.close();

			content = content.replace("TP2;2050;2100;Muut rahastot",
				"GP2;2050;2070;2120;2140;2070;2080;2140;2150;Muut rahastot");
			content = content.replace("TP3;2060;2070;Yhtiöjärjestyksen tai sääntöjen mukaiset rahastot",
				"TP3;2060;2070;2130;2140;Yhtiöjärjestyksen tai sääntöjen mukaiset rahastot");
			content = content.replace("TP3;2070;2100;2140;2150;Muut rahastot",
				"TP3;2070;2080;2140;2150;Muut rahastot\n" +
				"TB2;2050;2070;2120;2140;2070;2080;2140;2150;Muut rahastot yhteensä");

			PreparedStatement upd = conn.prepareStatement(
					"UPDATE report_structure SET data=? WHERE id = 'balance-sheet'");
			upd.setString(1, content);
			upd.executeUpdate();
			upd.close();
		}
		else {
			rs.close();
		}

		String incomeStatementDetailed = "";
		String balanceSheetDetailed = "";

		if (coaAmmatinharjoittaja || coaYksityistalous) {
			File dir = new DataSourceInitializationModel().getArchiveDirectory();
			File file;

			if (coaYksityistalous) {
				file = new File(dir, "yksityistalous.jar");
			}
			else {
				file = new File(dir, "ammatinharjoittaja-2010-07.jar");
			}

			JarFile jarFile = new JarFile(file);
			incomeStatementDetailed = readTextFile(jarFile, "income-statement-detailed.txt");
			balanceSheetDetailed = readTextFile(jarFile, "balance-sheet-detailed.txt");
		}

		/* Lisätään tulosteet. */
		PreparedStatement stmt2 = conn.prepareStatement(
				"INSERT INTO report_structure (id, data) VALUES (?, ?)");

		stmt2.setString(1, "income-statement-detailed");
		stmt2.setString(2, incomeStatementDetailed);
		stmt2.executeUpdate();

		stmt2.setString(1, "balance-sheet-detailed");
		stmt2.setString(2, balanceSheetDetailed);
		stmt2.executeUpdate();
		stmt2.close();

		stmt.executeUpdate("UPDATE settings SET version=6");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 6 onnistui");
	}

	public static void upgrade6to7(Connection conn, Statement stmt) throws SQLException {
		/* Korjataan yhdistyksen tuloslaskelman virhe. */
		ResultSet rs = stmt.executeQuery("SELECT data FROM report_structure WHERE id = 'income-statement'");

		if (rs.next()) {
			String content = rs.getString(1);
			rs.close();

			if (content.indexOf("Yleisavustukset") >= 0) {
				content = content.replace("HB0;7500;8000;Yleisavustukset",
						"SB0;7500;8000;Yleisavustukset");
				PreparedStatement upd = conn.prepareStatement(
						"UPDATE report_structure SET data=? WHERE id = 'income-statement'");
				upd.setString(1, content);
				upd.executeUpdate();
				upd.close();
			}
		}
		else {
			rs.close();
		}

		stmt.executeUpdate("UPDATE settings SET version=7");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 7 onnistui");
	}

	public static void upgrade7to8(Connection conn, Statement stmt) throws SQLException {
		/* Päivitetään ammatinharjoittajan tase. */
		ResultSet rs = stmt.executeQuery("SELECT data FROM report_structure WHERE id = 'balance-sheet'");

		if (rs.next()) {
			String content = rs.getString(1);
			rs.close();

			if (content.indexOf("Muut rahastot") >= 0) {
				Pattern pattern = Pattern.compile("TP2;[\\d;]+;Osake-, osuus- tai muu vastaava pääoma");
				Matcher matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"TP2;2000;2020;Osakepääoma\n" +
							"TP2;2100;2110;Osuuspääoma\n" +
							"TP2;2150;2160;2180;2190;2340;2360;Pääomapanokset\n" +
							"TP2;2200;2210;2360;2370;Peruspääoma");
				}

				content = content.replace("TP2;2030;2040;2110;2120;2160;2170;2190;2200;2210;2220;Arvonkorotusrahasto",
					"TP2;2030;2040;2110;2120;Arvonkorotusrahasto");

				content = content.replace("GP2;2050;2070;2120;2140;2070;2080;2140;2150;Muut rahastot",
					"GP2;2050;2100;Muut rahastot");

				content = content.replace("TP3;2050;2060;2120;2130;Vararahasto",
					"TP3;2050;2060;Vararahasto");

				content = content.replace("TP3;2060;2070;2130;2140;Yhtiöjärjestyksen tai sääntöjen mukaiset rahastot",
					"TP3;2060;2070;Yhtiöjärjestyksen tai sääntöjen mukaiset rahastot");

				content = content.replace("TP3;2070;2080;2140;2150;Muut rahastot",
					"TP3;2070;2100;Muut rahastot");

				content = content.replace("TP2;2050;2070;2120;2140;2070;2080;2140;2150;Muut rahastot yhteensä",
					"TP2;2050;2100;Muut rahastot yhteensä");

				pattern = Pattern.compile("TP2;[\\d;]+;Edellisten tilikausien voitto \\(tappio\\)");
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"TP2;2250;2330;Edellisten tilikausien voitto (tappio)\n" +
							"TP2;2330;2340;Pääomavajaus");
				}

				content = content.replace("TB1;2000;2400;3000;9999;Oma pääoma yhteensä",
					"TP2;2380;2390;Pääomalainat\n" +
					"TB1;2000;2400;3000;9999;Oma pääoma yhteensä");

				PreparedStatement upd = conn.prepareStatement(
						"UPDATE report_structure SET data=? WHERE id = 'balance-sheet'");
				upd.setString(1, content);
				upd.executeUpdate();
				upd.close();
			}

		}
		else {
			rs.close();
		}

		rs = stmt.executeQuery("SELECT data FROM report_structure WHERE id = 'balance-sheet-detailed'");

		if (rs.next()) {
			String content = rs.getString(1);
			rs.close();

			if (content.indexOf("Muut rahastot") >= 0) {
				Pattern pattern = Pattern.compile("GB2;2000;2020;Osake-, osuus- tai muu vastaava pääoma.+Osake-, osuus- tai muu vastaava pääoma yhteensä", Pattern.DOTALL);
				Matcher matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"GB2;2000;2020;Osakepääoma\n" +
							"DP3;2000;2020;Osakepääoma\n" +
							"TB2;2000;2020;Osakepääoma yhteensä\n\n" +
							"GB2;2100;2110;Osuuspääoma\n" +
							"DP3;2100;2110;Osuuspääoma\n" +
							"TB2;2100;2110;Osuuspääoma yhteensä\n\n" +
							"GB2;2150;2160;2180;2190;2340;2360;Pääomapanokset\n" +
							"DP3;2150;2160;2180;2190;2340;2360;Pääomapanokset\n" +
							"TB2;2150;2160;2180;2190;2340;2360;Pääomapanokset yhteensä\n\n" +
							"GB2;2200;2210;2360;2370;Peruspääoma\n" +
							"DP3;2200;2210;2360;2370;Peruspääoma\n" +
							"TB2;2200;2210;2360;2370;Peruspääoma yhteensä");
				}

				pattern = Pattern.compile("GB2;2030;2040;2110;2120;2160;2170;2190;2200;2210;2220;Arvonkorotusrahasto.+Arvonkorotusrahasto yhteensä", Pattern.DOTALL);
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"GB2;2030;2040;2110;2120;Arvonkorotusrahasto\n" +
							"DP3;2030;2040;2110;2120;Arvonkorotusrahasto\n" +
							"TB2;2030;2040;2110;2120;Arvonkorotusrahasto yhteensä");
				}

				pattern = Pattern.compile("GB3;2050;2060;2120;2130;Vararahasto.+Vararahasto yhteensä", Pattern.DOTALL);
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"GB3;2050;2060;2120;2130;Vararahasto\n" +
							"DP4;2050;2060;2120;2130;Vararahasto\n" +
							"TB3;2050;2060;2120;2130;Vararahasto yhteensä");
				}

				pattern = Pattern.compile("GB3;2060;2070;2130;2140;Yhtiöjärjestyksen tai sääntöjen mukaiset rahastot.+Sääntöjen mukaiset rahastot yhteensä", Pattern.DOTALL);
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"GB3;2060;2070;Yhtiöjärjestyksen tai sääntöjen mukaiset rahastot\n" +
							"DP4;2060;2070;Yhtiöjärjestyksen tai sääntöjen mukaiset rahastot\n" +
							"TB3;2060;2070;Sääntöjen mukaiset rahastot yhteensä");
				}

				pattern = Pattern.compile("GB3;2070;2080;2140;2150;Muut rahastot.+2070;2080;2140;2150;Muut rahastot yhteensä", Pattern.DOTALL);
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"GB3;2070;2100;Muut rahastot\n" +
							"DP4;2070;2100;Muut rahastot\n" +
							"TB3;2070;2100;Muut rahastot yhteensä");
				}

				pattern = Pattern.compile("TB2;[\\d;]+;Edellisten tilikausien voitto \\(tappio\\)");
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"\nGB2;2250;2330;Edellisten tilikausien voitto (tappio)\n" +
							"DP3;2250;2330;Edellisten tilikausien voitto (tappio)\n" +
							"TB2;2250;2330;Edellisten tilikausien voitto (tappio) yhteensä\n\n" +
							"GB2;2330;2340;Pääomavajaus\n" +
							"DP3;2330;2340;Pääomavajaus\n" +
							"TB2;2330;2340;Pääomavajaus yhteensä\n");
				}

				PreparedStatement upd = conn.prepareStatement(
						"UPDATE report_structure SET data=? WHERE id = 'balance-sheet-detailed'");
				upd.setString(1, content);
				upd.executeUpdate();
				upd.close();
			}
		}
		else {
			rs.close();
		}

		/* Päivitetään yhdistyksen tuloslaskelma. */
		rs = stmt.executeQuery("SELECT data FROM report_structure WHERE id = 'income-statement'");

		if (rs.next()) {
			String content = rs.getString(1);
			rs.close();

			if (content.indexOf("Varainhankinta") >= 0) {
				content = content.replace("SP1;5000;5500;Tuotot", "SP1;5000;5050;Tuotot");
				content = content.replace("SP1;5500;6000;Kulut", "SP1;5050;6000;Kulut");
				content = content.replace("SP1;6000;6500;Tuotot", "SP1;6000;6100;Tuotot");
				content = content.replace("SP1;6500;7000;Kulut", "SP1;6100;7000;Kulut");

				PreparedStatement upd = conn.prepareStatement(
					"UPDATE report_structure SET data=? WHERE id = 'income-statement'");
				upd.setString(1, content);
				upd.executeUpdate();
				upd.close();
			}
		}

		rs = stmt.executeQuery("SELECT data FROM report_structure WHERE id = 'income-statement-detailed'");

		if (rs.next()) {
			String content = rs.getString(1);
			rs.close();

			if (content.indexOf("Varainhankinta") >= 0) {
				Pattern pattern = Pattern.compile("HB1;5000;5500;Varainhankinnan tuotot.+SB1;5000;5500;Varainhankinnan tuotot yhteensä", Pattern.DOTALL);
				Matcher matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"HB1;5000;5050;Varainhankinnan tuotot\n" +
							"DP2;5000;5050;Varainhankinnan tuotot\n" +
							"SB1;5000;5050;Varainhankinnan tuotot yhteensä");
				}

				pattern = Pattern.compile("HB1;5500;6000;Varainhankinnan kulut.+SB1;5500;6000;Varainhankinnan kulut yhteensä", Pattern.DOTALL);
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"HB1;5050;6000;Varainhankinnan kulut\n" +
							"DP2;5050;6000;Varainhankinnan kulut\n" +
							"SB1;5050;6000;Varainhankinnan kulut yhteensä");
				}

				pattern = Pattern.compile("HB1;6000;6500;Sijoitus- ja rahoitustoiminnan tuotot.+SB1;6000;6500;Sijoitus- ja rahoitustoiminnan tuotot yhteensä", Pattern.DOTALL);
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"HB1;6000;6100;Sijoitus- ja rahoitustoiminnan tuotot\n" +
							"DP2;6000;6100;Sijoitus- ja rahoitustoiminnan tuotot\n" +
							"SB1;6000;6100;Sijoitus- ja rahoitustoiminnan tuotot yhteensä");
				}

				pattern = Pattern.compile("HB1;6500;7000;Sijoitus- ja rahoitustoiminnan kulut.+SB1;6500;7000;Sijoitus- ja rahoitustoiminnan kulut yhteensä", Pattern.DOTALL);
				matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"HB1;6100;7000;Sijoitus- ja rahoitustoiminnan kulut\n" +
							"DP2;6100;7000;Sijoitus- ja rahoitustoiminnan kulut\n" +
							"SB1;6100;7000;Sijoitus- ja rahoitustoiminnan kulut yhteensä");
				}

				PreparedStatement upd = conn.prepareStatement(
					"UPDATE report_structure SET data=? WHERE id = 'income-statement-detailed'");
				upd.setString(1, content);
				upd.executeUpdate();
				upd.close();
			}
		}

		/* Edellisten tilikausien ylijäämät -tili yhdistyksen tilikartassa */
		stmt.executeUpdate("UPDATE account SET number='2251', type=5 WHERE name = 'Edellisten tilikausien ylijäämät'");

		stmt.executeUpdate("UPDATE settings SET version=8");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 8 onnistui");
	}

	public static void upgrade8to9(Connection conn, Statement stmt) throws SQLException {
		stmt.executeUpdate("ALTER TABLE settings ADD properties text NOT NULL DEFAULT ''");
		stmt.executeUpdate("UPDATE settings SET version=9");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 9 onnistui");
	}

	public static void upgrade9to10(Connection conn, Statement stmt) throws SQLException {
		/* Päivitetään ammatinharjoittajan tuloslaskelma. */
		String[] reportIds = {"income-statement", "income-statement-detailed"};

		for (String reportId : reportIds) {
			ResultSet rs = stmt.executeQuery(String.format("SELECT data FROM report_structure WHERE id = '%s'", reportId));

			if (rs.next()) {
				String content = rs.getString(1);

				if (content.indexOf("0;3000;3650;LIIKEVAIHTO") >= 0) {
					content = content.replace("0;3000;3650;LIIKEVAIHTO", "0;3000;3600;LIIKEVAIHTO");
					PreparedStatement upd = conn.prepareStatement(String.format(
						"UPDATE report_structure SET data=? WHERE id = '%s'", reportId));
					upd.setString(1, content);
					upd.executeUpdate();
					upd.close();
				}
			}
		}

		stmt.executeUpdate("UPDATE settings SET version=10");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 10 onnistui");
	}

	public static void upgrade10to11(Connection conn, Statement stmt) throws SQLException {
		stmt.executeUpdate("ALTER TABLE account ADD flags integer NOT NULL DEFAULT 0");
		stmt.executeUpdate("UPDATE settings SET version=11");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 11 onnistui");
	}

	public static void upgrade11to12(Connection conn, Statement stmt) throws SQLException {
		/* Päivitetään ammatinharjoittajan tase. */
		ResultSet rs = stmt.executeQuery("SELECT data FROM report_structure WHERE id = 'income-statement'");
		boolean containsYlijaama = false;

		if (rs.next()) {
			String content = rs.getString(1);
			containsYlijaama = content.contains("Tilikauden ylijäämä");
			rs.close();
		}

		String[] reportIds = {"balance-sheet", "balance-sheet-detailed"};

		for (String reportId : reportIds) {
			rs = stmt.executeQuery(String.format("SELECT data FROM report_structure WHERE id = '%s'", reportId));

			if (rs.next()) {
				String content = rs.getString(1);
				Pattern pattern = Pattern.compile("GB2;2250;2330;Edellisten tilikausien voitto \\(tappio\\).+DP3;2250;2330;Edellisten tilikausien voitto \\(tappio\\).+TB2;2250;2330;Edellisten tilikausien voitto \\(tappio\\) yhteensä", Pattern.DOTALL);
				Matcher matcher = pattern.matcher(content);

				if (matcher.find()) {
					content = content.replace(matcher.group(),
							"TB2;2250;2330;Edellisten tilikausien voitto (tappio)");
				}

				content = content.replace("GB2;3000;9999;Tilikauden voitto", "TB2;3000;9999;Tilikauden voitto");
				content = content.replace("HB0;2000;9999;VASTATTAVAA", "--\nHB0;2000;9999;VASTATTAVAA");

				if (containsYlijaama) {
					content = content.replace("Tilikauden voitto (tappio)", "Tilikauden ylijäämä (alijäämä)");
					content = content.replace("Edellisten tilikausien voitto (tappio)", "Edellisten tilikausien ylijäämä (alijäämä)");
				}

				PreparedStatement upd = conn.prepareStatement(String.format(
						"UPDATE report_structure SET data=? WHERE id = '%s'", reportId));
				upd.setString(1, content);
				upd.executeUpdate();
				upd.close();
			}

			rs.close();
		}

		stmt.executeUpdate("UPDATE settings SET version=12");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 12 onnistui");
	}

	public static void upgrade12to13(Connection conn, Statement stmt) throws SQLException {
		stmt.executeUpdate("ALTER TABLE entry ADD flags integer NOT NULL DEFAULT 0");
		stmt.executeUpdate("UPDATE settings SET version=13");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 13 onnistui");
	}

	public static void upgrade13to14(Connection conn, Statement stmt, boolean sqlite) throws SQLException {
		stmt.executeUpdate("ALTER TABLE account ADD vat_percentage numeric(10, 2) NOT NULL DEFAULT 0");
		String[] rates = { "0", "22", "17", "8", "12", "9", "13", "23" };

		for (int i = 0; i < rates.length; i++) {
			String query = String.format("UPDATE account SET vat_percentage = %s WHERE vat_rate = %s",
					sqlite ? "'" + rates[i] + "'" : rates[i], i);
			stmt.executeUpdate(query);
		}

		if (sqlite) {
			stmt.executeUpdate("ALTER TABLE account RENAME TO account_old");
			stmt.executeUpdate("CREATE TABLE account (\n" +
				"\tid integer PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
				"\tnumber varchar(10) NOT NULL,\n" +
				"\tname varchar(100) NOT NULL,\n" +
				"\ttype integer NOT NULL,\n" +
				"\tvat_code integer NOT NULL,\n" +
				"\tvat_percentage numeric(10, 2) NOT NULL,\n" +
				"\tvat_account1_id integer,\n" +
				"\tvat_account2_id integer,\n" +
				"\tflags integer NOT NULL,\n" +
				"\tFOREIGN KEY (vat_account1_id) REFERENCES account (id),\n" +
				"\tFOREIGN KEY (vat_account2_id) REFERENCES account (id)\n)");
			stmt.executeUpdate("INSERT INTO account SELECT id, number, name, type, vat_code, " +
				"vat_percentage, vat_account1_id, vat_account2_id, flags FROM account_old");
			stmt.executeUpdate("DROP TABLE account_old");
		}
		else {
			stmt.executeUpdate("ALTER TABLE account DROP vat_rate");
		}

		stmt.executeUpdate("UPDATE settings SET version=14");
		conn.commit();

		Logger logger = Logger.getLogger("kirjanpito.db");
		logger.info("Tietokannan päivittäminen versioon 14 onnistui");
	}

	private static String readTextFile(JarFile jarFile, String name) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				jarFile.getInputStream(jarFile.getEntry(name)),
				Charset.forName("UTF-8")));

		StringBuilder sb = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			sb.append(line).append('\n');
		}

		return sb.toString();
	}
}
