package kirjanpito.reports;

import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import kirjanpito.db.Settings;

/**
 * Tuloste
 *
 * @author Tommi Helineva
 */
public abstract class Print implements DrawCommandVarProvider {
	protected DateFormat dateFormat;
	protected DateFormat dateFormatTable;
	protected int x;
	protected int y;
	protected Insets margins;
	private Date now;
	private PrintCanvas canvas;
	private Settings settings;
	private String printId;
	private int pageIndex;
	private DrawCommandParser headerParser;
	private DrawCommandParser footerParser;

	private static final int DEFAULT_MARGIN_WIDTH =
		(int)(1.5 * 0.3937 * 72); // 1,5 cm

	public Print() {
		dateFormat = new SimpleDateFormat("d.M.yyyy");
		dateFormatTable = new SimpleDateFormat("dd.MM.yyyy");
		now = new Date();
		margins = new Insets(DEFAULT_MARGIN_WIDTH,
				DEFAULT_MARGIN_WIDTH,
				DEFAULT_MARGIN_WIDTH,
				DEFAULT_MARGIN_WIDTH);

		headerParser = new DrawCommandParser();
		headerParser.setVariableProvider(this);
		footerParser = new DrawCommandParser();
		footerParser.setVariableProvider(this);
	}

	/**
	 * Palauttaa tulosteen nimen.
	 * Ylikirjoitetaan aliluokassa.
	 *
	 * @return tulosteen nimi
	 */
	public abstract String getTitle();

	/**
	 * Palauttaa sivujen lukumäärän.
	 * Ylikirjoitetaan aliluokassa.
	 *
	 * @return sivujen lukumäärä
	 */
	public abstract int getPageCount();

	public PrintCanvas getCanvas() {
		return canvas;
	}

	public void setCanvas(PrintCanvas canvas) {
		this.canvas = canvas;

		margins = new Insets(Math.max(canvas.getImageableY(), DEFAULT_MARGIN_WIDTH),
				Math.max(canvas.getImageableX(), DEFAULT_MARGIN_WIDTH),
				Math.max(canvas.getImageableY(), DEFAULT_MARGIN_WIDTH),
				Math.max(canvas.getImageableX(), DEFAULT_MARGIN_WIDTH));

		headerParser.setMargins(margins);
		headerParser.setCanvas(canvas);
		footerParser.setMargins(margins);
		footerParser.setCanvas(canvas);
		initialize();
	}

	public Settings getSettings() {
		return settings;
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
	}

	/**
	 * Tulostaa sivun.
	 */
	public boolean printPage(int pageIndex) {
		this.pageIndex = pageIndex;

		if (pageIndex >= getPageCount())
			return false;

		x = margins.left;
		y = margins.top;
		printHeader();

		x = margins.left;
		y = margins.top + getHeaderHeight();
		printContent();

		x = margins.left;
		y = getPageHeight() - margins.bottom - getFooterHeight();
		printFooter();

		canvas.close();
		return true;
	}

	public String getVariableValue(String name) {
		if (name.equals("n")) {
			return settings.getName();
		}
		else if (name.equals("y")) {
			return settings.getBusinessId();
		}
		else if (name.equals("d")) {
			return dateFormat.format(now);
		}
		else if (name.equals("p")) {
			return Integer.toString(pageIndex + 1);
		}
		else if (name.equals("r")) {
			return Integer.toString(getPageCount());
		}

		return "";
	}

	protected String getPrintId() {
		return printId;
	}

	protected void setPrintId(String printId) {
		this.printId = printId;
	}

	/**
	 * Kutsutaan ennen tulostuksen aloittamista.
	 */
	protected void initialize() {
		String header = settings.getProperty(printId + "/header", "");

		try {
			if (header.isEmpty()) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						Print.class.getResourceAsStream(String.format("header-%s.txt", printId)),
						Charset.forName("UTF-8")));

				headerParser.parse(reader);
				reader.close();
			}
			else {
				headerParser.parse(header);
			}
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		String footer = settings.getProperty(printId + "/footer", "");

		try {
			if (!footer.isEmpty()) {
				footerParser.parse(footer);
			}
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Tulostaa sivun ylätunnisteen.
	 */
	protected void printHeader() {
		headerParser.draw();
	}

	/**
	 * Palauttaa ylätunnisteen korkeuden.
	 *
	 * @return ylätunnisteen korkeus
	 */
	protected int getHeaderHeight() {
		return headerParser.getHeight();
	}

	/**
	 * Tulostaa sivun alatunnisteen. Ylikirjoitetaan aliluokassa.
	 */
	protected void printFooter() {
		footerParser.draw();
	}

	/**
	 * Palauttaa alatunnisteen korkeuden. Ylikirjoitetaan aliluokassa.
	 *
	 * @return alatunnisteen korkeus
	 */
	protected int getFooterHeight() {
		return footerParser.getHeight();
	}

	/**
	 * Tulostaa sivun sisällön. Ylikirjoitetaan aliluokassa.
	 */
	protected void printContent() {
	}

	/**
	 * Palauttaa marginaalien leveydet.
	 *
	 * @return marginaalien leveydet
	 */
	protected Insets getMargins() {
		return margins;
	}

	/**
	 * Asettaa marginaalien leveydet.
	 *
	 * @param margins marginaalien leveydet
	 */
	protected void setMargins(Insets margins) {
		this.margins = margins;
		headerParser.setMargins(margins);
		footerParser.setMargins(margins);
	}

	/**
	 * Palauttaa sisältöalueen korkeuden.
	 *
	 * @return korkeus
	 */
	protected int getContentHeight() {
		return getPageHeight() - margins.top - margins.bottom -
			getHeaderHeight() - getFooterHeight();
	}

	/**
	 * Palauttaa sisältöalueen leveyden.
	 *
	 * @return leveys
	 */
	protected int getContentWidth() {
		return getPageWidth() - margins.left - margins.right;
	}

	/**
	 * Asettaa x- ja y-koordinaatit.
	 *
	 * @param x
	 * @param y
	 */
	protected void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Palauttaa x-koordinaatin.
	 *
	 * @return x
	 */
	protected int getX() {
		return x;
	}

	/**
	 * Asettaa x-koornaatin.
	 *
	 * @param x x
	 */
	protected void setX(int x) {
		this.x = x;
	}

	/**
	 * Palauttaa y-koordinaatin.
	 *
	 * @return y
	 */
	protected int getY() {
		return y;
	}

	/**
	 * Asettaa y-koordinaatin.
	 *
	 * @param y y
	 */
	protected void setY(int y) {
		this.y = y;
	}

	/**
	 * Palauttaa tulostettavan sivun numeron. Ensimmäisen sivun
	 * numero on 0.
	 *
	 * @return sivunumero
	 */
	protected int getPageIndex() {
		return pageIndex;
	}

	/**
	 * Palauttaa sivun leveyden.
	 *
	 * @return sivun leveys
	 */
	protected int getPageWidth() {
		return canvas.getPageWidth();
	}

	/**
	 * Palauttaa sivun korkeuden.
	 *
	 * @return sivun korkeus
	 */
	protected int getPageHeight() {
		return canvas.getPageHeight();
	}

	/**
	 * Tulostaa merkkijonon <code>s</code>.
	 *
	 * @param s tulostettava merkkijono
	 */
	protected void drawText(String s) {
		canvas.drawText(x, y, s);
	}

	/**
	 * Tulostaa merkkijonon <code>s</code> keskitettynä.
	 *
	 * @param s tulostettava merkkijono
	 */
	protected void drawTextCenter(String s) {
		canvas.drawTextCenter(x, y, s);
	}

	/**
	 * Tulostaa merkkijonon <code>s</code> oikealle tasattuna.
	 *
	 * @param s tulostettava merkkijono
	 */
	protected void drawTextRight(String s) {
		canvas.drawTextRight(x, y, s);
	}

	/**
	 * Tulostaa vaakaviivan, jonka leveys on <code>width</code>.
	 *
	 * @param lineWidth viivan leveys
	 */
	protected void drawHorizontalLine(float lineWidth) {
		canvas.drawLine(margins.left, y, getPageWidth() - margins.right, y, lineWidth);
	}

	/**
	 * Asettaa tekstityyliksi otsikkotyylin.
	 */
	protected void setHeadingStyle() {
		canvas.setHeadingStyle();
	}

	/**
	 * Asettaa nykyiseksi tekstityyliksi normaalin leipätekstityylin.
	 */
	protected void setNormalStyle() {
		canvas.setNormalStyle();
	}

	/**
	 * Asettaa nykyiseksi tekstityyliksi pienemmän leipätekstityylin.
	 */
	protected void setSmallStyle() {
		canvas.setSmallStyle();
	}

	/**
	 * Asettaa nykyiseksi tekstityyliksi lihavoidun leipätekstityylin.
	 */
	protected void setBoldStyle() {
		canvas.setBoldStyle();
	}

	/**
	 * Asettaa nykyiseksi tekstityyliksi kursivoidun leipätekstityylin.
	 */
	protected void setItalicStyle() {
		canvas.setItalicStyle();
	}

	/**
	 * Palauttaa merkkijonon <code>s</code> leveyden.
	 *
	 * @param s merkkijono
	 * @return merkkijonon leveys
	 */
	protected int stringWidth(String s) {
		return canvas.stringWidth(s);
	}

	protected String cutString(String s, int w) {
		while (s.length() > 0 && stringWidth(s) > w) {
			s = s.substring(0, s.length() - 1);
		}

		return s;
	}
}
