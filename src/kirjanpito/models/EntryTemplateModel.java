package kirjanpito.models;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.EntryTemplate;
import kirjanpito.db.EntryTemplateDAO;
import kirjanpito.db.Session;
import kirjanpito.util.Registry;

public class EntryTemplateModel {
	private Registry registry;
	private List<EntryTemplate> entryTemplates;
	private HashSet<EntryTemplate> changedTemplates;
	private HashSet<EntryTemplate> deletedTemplates;

	public EntryTemplateModel(Registry registry) {
		this.registry = registry;
		entryTemplates = registry.getEntryTemplates();
		changedTemplates = new HashSet<EntryTemplate>();
		deletedTemplates = new HashSet<EntryTemplate>();
	}

	/**
	 * Palauttaa <code>true</code>, jos vientimalleihin on
	 * tehty muutoksia.
	 *
	 * @return <code>true</code>, jos vientimalleihin on
	 * tehty muutoksia
	 */
	public boolean isChanged() {
		return !changedTemplates.isEmpty() || !deletedTemplates.isEmpty();
	}

	/**
	 * Tallentaa vientimalleihin tehdyt muutokset
	 * tietokantaan.
	 *
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		EntryTemplateDAO dao;

		try {
			sess = dataSource.openSession();
			dao = dataSource.getEntryTemplateDAO(sess);

			for (EntryTemplate template : deletedTemplates) {
				dao.delete(template.getId());
			}

			for (EntryTemplate template : changedTemplates) {
				dao.save(template);
			}

			sess.commit();
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}

		changedTemplates.clear();
		deletedTemplates.clear();
		registry.fireEntryTemplatesChanged();
	}

	/**
	 * Hylkää käyttäjän tekemät muutokset ja hakee
	 * tietokannasta vientimallien tiedot uudelleen.
	 *
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void discardChanges() throws DataAccessException {
		registry.fetchEntryTemplates();
	}

	/**
	 * Palauttaa vientimallien lukumäärän
	 *
	 * @return vientimallien lukumäärä
	 */
	public int getEntryTemplateCount() {
		return entryTemplates.size();
	}

	/**
	 * Palauttaa rivillä <code>index</code> olevan vientimallin.
	 *
	 * @param index rivinumero
	 * @return vientimalli
	 */
	public EntryTemplate getEntryTemplate(int index) {
		return entryTemplates.get(index);
	}

	/**
	 * Lisää uuden vientimallin.
	 *
	 * @return uuden vientimallin rivinumero
	 */
	public int addEntryTemplate() {
		int number = 1;

		if (entryTemplates.size() > 0) {
			number = entryTemplates.get(
					entryTemplates.size() - 1).getNumber() + 1;
		}

		EntryTemplate template = new EntryTemplate();
		template.setNumber(number);
		template.setName("");
		template.setAccountId(-1);
		template.setDebit(true);
		template.setAmount(BigDecimal.ZERO);
		template.setDescription("");
		entryTemplates.add(template);
		changedTemplates.add(template);
		return entryTemplates.size() - 1;
	}

	/**
	 * Poistaa vientimallin riviltä <code>index</code>.
	 *
	 * @param index rivinumero
	 */
	public void removeEntryTemplate(int index) {
		EntryTemplate template = entryTemplates.get(index);
		changedTemplates.remove(template);

		if (template.getId() >= 0)
			deletedTemplates.add(template);

		int rowNumber = 0;

		/* Päivitetään järjestysnumerot. */
		for (EntryTemplate t : entryTemplates) {
			if (t.getNumber() == template.getNumber() && t != template) {
				t.setRowNumber(rowNumber);
				changedTemplates.add(t);
				rowNumber++;
			}
		}

		changedTemplates.remove(template);
		entryTemplates.remove(index);
	}

	/**
	 * Päivittää rivillä <code>index</code> olevan vientimallin
	 * numeroksi <code>number</code>.
	 *
	 * @param index rivinumero
	 * @param number vientimallin numero
	 */
	public void updateNumber(int index, int number) {
		EntryTemplate template = entryTemplates.get(index);
		int oldNumber = template.getNumber();

		if (template.getNumber() == number)
			return;

		int rowNumber = 0;

		/* Päivitetään järjestysnumerot. */
		for (EntryTemplate t : entryTemplates) {
			if (t.getNumber() == oldNumber && t != template) {
				t.setRowNumber(rowNumber);
				changedTemplates.add(t);
				rowNumber++;
			}
		}

		String name = template.getName();
		rowNumber = -1;

		for (EntryTemplate t : entryTemplates) {
			if (t.getNumber() == number) {
				name = t.getName();
				rowNumber = Math.max(t.getRowNumber(), rowNumber);
			}
		}

		template.setNumber(number);
		template.setName(name);
		template.setRowNumber(rowNumber + 1);
		changedTemplates.add(template);
		Collections.sort(entryTemplates);
	}

	/**
	 * Päivittää rivillä <code>index</code> olevan vientimallin
	 * nimeksi <code>name</code>.
	 *
	 * @param index rivinumero
	 * @param number vientimallin nimi
	 */
	public void updateName(int index, String name) {
		int number = entryTemplates.get(index).getNumber();

		for (EntryTemplate template : entryTemplates) {
			if (template.getNumber() == number) {
				template.setName(name);
				changedTemplates.add(template);
			}
		}
	}

	/**
	 * Päivittää rivillä <code>index</code> olevan vientimallin
	 * tiliksi <code>accountId</code>.
	 *
	 * @param index rivinumero
	 * @param accountId tilin tunniste
	 */
	public void updateAccountId(int index, int accountId) {
		EntryTemplate template = entryTemplates.get(index);
		template.setAccountId(accountId);
		changedTemplates.add(template);
	}

	/**
	 * Päivittää rivillä <code>index</code> olevan vientimallin
	 * rahamääräksi <code>amount</code>.
	 *
	 * @param index rivinumero
	 * @param debit <code>true</code> jos debet; <code>false</code> jos kredit
	 * @param amount rahamäärä
	 */
	public void updateAmount(int index, boolean debit, BigDecimal amount) {
		EntryTemplate template = entryTemplates.get(index);
		template.setDebit(debit);
		template.setAmount(amount);
		changedTemplates.add(template);
	}

	/**
	 * Päivittää rivillä <code>index</code> olevan vientimallin
	 * selitteeksi <code>description</code>.
	 *
	 * @param index rivinumero
	 * @param description selite
	 */
	public void updateDescription(int index, String description) {
		EntryTemplate template = entryTemplates.get(index);
		template.setDescription(description);
		changedTemplates.add(template);
	}
}
