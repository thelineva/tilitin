package kirjanpito.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.DocumentType;
import kirjanpito.db.DocumentTypeDAO;
import kirjanpito.db.Session;
import kirjanpito.util.Registry;

public class DocumentTypeModel {
	private Registry registry;
	private List<DocumentType> documentTypes;
	private HashSet<DocumentType> changedDocumentTypes;
	private HashSet<DocumentType> deletedDocumentTypes;
	
	public DocumentTypeModel(Registry registry) {
		this.registry = registry;
		this.documentTypes = registry.getDocumentTypes();
		changedDocumentTypes = new HashSet<DocumentType>();
		deletedDocumentTypes = new HashSet<DocumentType>();
	}
	
	/**
	 * Palauttaa <code>true</code>, jos tositelajeihin on
	 * tehty muutoksia.
	 * 
	 * @return <code>true</code>, jos tositelajeihin on
	 * tehty muutoksia
	 */
	public boolean isChanged() {
		return !changedDocumentTypes.isEmpty() || !deletedDocumentTypes.isEmpty();
	}
	
	/**
	 * Tallentaa tositelajeihin tehdyt muutokset
	 * tietokantaan.
	 * 
	 * @throws DataAccessException jos tallentaminen epäonnistuu
	 */
	public void save() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		DocumentTypeDAO dao;
		
		try {
			sess = dataSource.openSession();
			dao = dataSource.getDocumentTypeDAO(sess);
			
			for (DocumentType type : deletedDocumentTypes) {
				dao.delete(type.getId());
			}
			
			for (DocumentType type : changedDocumentTypes) {
				dao.save(type);
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
		
		changedDocumentTypes.clear();
		deletedDocumentTypes.clear();
		registry.fireDocumentTypesChanged();
	}
	
	/**
	 * Hylkää käyttäjän tekemät muutokset ja hakee
	 * tietokannasta tositelajien tiedot uudelleen.
	 * 
	 * @throws DataAccessException jos tietojen hakeminen epäonnistuu
	 */
	public void discardChanges() throws DataAccessException {
		registry.fetchDocumentTypes();
	}
	
	/**
	 * Palauttaa tositelajien lukumäärän.
	 * 
	 * @return tositelajien lukumäärä
	 */
	public int getDocumentTypeCount() {
		return documentTypes.size();
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan tositelajin.
	 * 
	 * @param index rivinumero
	 * @return tositelaji
	 */
	public DocumentType getDocumentType(int index) {
		return documentTypes.get(index);
	}
	
	/**
	 * Lisää uuden tositelajin.
	 * 
	 * @return uuden tositelajin rivinumero
	 */
	public int addDocumentType() {
		int number = 1;
		int numberStart = 1;
		int numberEnd = 9999;
		
		if (documentTypes.size() > 0) {
			DocumentType last = documentTypes.get(
					documentTypes.size() - 1);
			number = last.getNumber() + 1;
			numberStart = last.getNumberEnd() + 1;
			numberEnd = numberStart + (last.getNumberEnd() - last.getNumberStart());
			if ((numberEnd % 100) == 98) numberEnd++;
		}
		
		DocumentType type = new DocumentType();
		type.setNumber(number);
		type.setName("");
		type.setNumberStart(numberStart);
		type.setNumberEnd(numberEnd);
		changedDocumentTypes.add(type);
		documentTypes.add(type);
		return documentTypes.size() - 1;
	}
	
	/**
	 * Poistaa tositelajin riviltä <code>index</code>
	 * 
	 * @param index rivinumero
	 */
	public void removeDocumentType(int index) {
		DocumentType type = documentTypes.get(index);
		changedDocumentTypes.remove(type);
		
		if (type.getId() > 0)
			deletedDocumentTypes.add(type);
		
		documentTypes.remove(index);
	}
	
	/**
	 * Päivittää rivillä <code>index</code> olevan tositelajin
	 * numeroksi <code>number</code>.
	 * 
	 * @param index rivinumero
	 * @param number tositelajin numero
	 */
	public void updateNumber(int index, int number) {
		DocumentType type = documentTypes.get(index);
		type.setNumber(number);
		changedDocumentTypes.add(type);
		Collections.sort(documentTypes);
	}
	
	/**
	 * Päivittää rivillä <code>index</code> olevan tositelajin
	 * nimeksi <code>name</code>.
	 * 
	 * @param index rivinumero
	 * @param name tositelajin nimi
	 */
	public void updateName(int index, String name) {
		DocumentType type = documentTypes.get(index);
		type.setName(name);
		changedDocumentTypes.add(type);
	}
	
	/**
	 * Päivittää rivillä <code>index</code> olevan tositelajin
	 * numerovälin aluksi <code>numberStart</code>.
	 * 
	 * @param index rivinumero
	 * @param numberStart tositenumerovälin alku
	 */
	public void updateNumberStart(int index, int numberStart) {
		DocumentType type = documentTypes.get(index);
		type.setNumberStart(numberStart);
		changedDocumentTypes.add(type);
	}
	
	/**
	 * Päivittää rivillä <code>index</code> olevan tositelajin
	 * numerovälin lopuksi <code>numberStart</code>.
	 * 
	 * @param index rivinumero
	 * @param numberStart tositenumerovälin loppu
	 */
	public void updateNumberEnd(int index, int numberEnd) {
		DocumentType type = documentTypes.get(index);
		type.setNumberEnd(numberEnd);
		changedDocumentTypes.add(type);
	}
}
