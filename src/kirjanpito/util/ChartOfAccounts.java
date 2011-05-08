package kirjanpito.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import kirjanpito.db.Account;
import kirjanpito.db.COAHeading;

public class ChartOfAccounts {
	private COAItem[] items;
	
	/**
	 * Tili
	 */
	public static final int TYPE_ACCOUNT = 0;
	
	/**
	 * Otsikko
	 */
	public static final int TYPE_HEADING = 1;
	
	/**
	 * Asettaa tilikartan sisällön.
	 * 
	 * @param accounts tilikartan tilit
	 * @param headings tilikartan otsikot
	 */
	public void set(List<Account> accounts, List<COAHeading> headings) {
		Account account;
		COAHeading heading;
		int ai = 0;
		int hi = 0;
		
		items = new COAItem[accounts.size() + headings.size()];
		
		for (int i = 0; i < items.length; i++) {
			/* Jos kaikki tilit jo lisätty, lisätään loput otsikot. */
			if (ai >= accounts.size()) {
				items[i] = new COAItem(headings.get(hi));
				hi++;
			}
			/* Jos kaikki otsikot jo lisätty, lisätään loput tilit. */
			else if (hi >= headings.size()) {
				items[i] = new COAItem(accounts.get(ai));
				ai++;
			}
			else {
				account = accounts.get(ai);
				heading = headings.get(hi);
				
				if (heading.getNumber().compareTo(account.getNumber()) <= 0) {
					/* Otsikko on ennen tiliä. */
					items[i] = new COAItem(heading);
					hi++;
				}
				else {
					/* Tili on ennen otsikkoa. */
					items[i] = new COAItem(account);
					ai++;
				}
			}
		}
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan tilin.
	 * 
	 * @param index rivinumero
	 * @return tili
	 */
	public Account getAccount(int index) {
		return items[index].account;
	}
	
	/**
	 * Palauttaa rivillä <code>index</code> olevan otsikon.
	 * 
	 * @param index rivinumero
	 * @return otsikko
	 */
	public COAHeading getHeading(int index) {
		return items[index].heading;
	}
	
	/**
	 * Palauttaa rivin <code>index</code> tyypin,
	 * TYPE_ACCOUNT tai TYPE_HEADING.
	 * 
	 * @param index rivinumero
	 * @return rivin tyyppi
	 */
	public int getType(int index) {
		return (items[index].heading == null) ? TYPE_ACCOUNT : TYPE_HEADING;
	}
	
	/**
	 * Palauttaa rivien lukumäärän.
	 * 
	 * @return rivien lukumäärä
	 */
	public int getSize() {
		return (items == null) ? 0 : items.length;
	}
	
	/**
	 * Etsii tilikartasta tilin <code>account</code> ja
	 * palauttaa tilin rivinumeron.
	 * 
	 * @param account haettava tili
	 * @return rivinumero tai -1, jos tiliä ei löydy
	 */
	public int indexOfAccount(Account account) {
		int index = 0;
		
		for (COAItem item : items) {
			if (item.account == account) {
				return index;
			}
			
			index++;
		}
		
		return -1;
	}
	
	/**
	 * Etsii tilikartasta otsikon <code>otsikon</code> ja
	 * palauttaa otsikon rivinumeron.
	 * 
	 * @param heading haettava otsikko
	 * @return rivinumero tai -1, jos tiliä ei löydy
	 */
	public int indexOfHeading(COAHeading heading) {
		int index = 0;
		
		for (COAItem item : items) {
			if (item.heading == heading) {
				return index;
			}
			
			index++;
		}
		
		return -1;
	}
	
	/**
	 * Etsii tilikartan tilien nimistä ja otsikkoteksteistä
	 * merkkijonoa <code>q</code>. Metodi palauttaa
	 * ensimmäisen löytyneen tilin tai otsikon rivinumeron.
	 * 
	 * @param q hakusana
	 * @return rivinumero
	 */
	public int search(String q) {
		int index = 0;
		int match1 = -1;
		int match2 = -1;
		int match3 = -1;
		int len = q.length();
		
		for (COAItem item : items) {
			if (item.account != null) {
				if (item.account.getNumber().equalsIgnoreCase(q) ||
						item.account.getName().equalsIgnoreCase(q)) {
					match1 = index;
					break;
				}
				else if (item.account.getNumber().regionMatches(true, 0, q, 0, len) ||
						item.account.getName().regionMatches(true, 0, q, 0, len)) {
					
					if (match2 < 0) {
						match2 = index;
					}
				}
			}
			else if (item.heading.getText().regionMatches(true, 0, q, 0, len)) {
				if (match3 < 0) {
					match3 = index;
				}
			}
			
			index++;
		}
		
		if (match2 < 0) {
			match2 = match3;
		}
		
		if (match1 < 0) {
			match1 = match2;
		}
		
		return match1;
	}
	
	public void filterNonFavouriteAccounts() {
		LinkedList<COAItem> headingList = new LinkedList<COAItem>();
		ArrayList<COAItem> tmp = new ArrayList<COAItem>();
		
		for (COAItem item : items) {
			if (item.account != null) {
				if ((item.account.getFlags() & 0x01) == 0) {
					continue;
				}
				
				while (!headingList.isEmpty()) {
					tmp.add(headingList.removeFirst());
				}
				
				tmp.add(item);
			}
			else {
				while (!headingList.isEmpty() && headingList.peekLast().heading.getLevel() >= item.heading.getLevel()) {
					headingList.removeLast();
				}
				
				headingList.add(item);
			}
		}
		
		items = new COAItem[tmp.size()];
		tmp.toArray(items);
	}
	
	public void filterNonUsedAccounts(AccountBalances balances) {
		LinkedList<COAItem> headingList = new LinkedList<COAItem>();
		ArrayList<COAItem> tmp = new ArrayList<COAItem>();

		for (COAItem item : items) {
			if (item.account != null) {
				if (balances.getBalance(item.account.getId()) == null)
					continue;

				while (!headingList.isEmpty()) {
					tmp.add(headingList.removeFirst());
				}

				tmp.add(item);
			}
			else {
				while (!headingList.isEmpty() && headingList.peekLast().heading.getLevel() >= item.heading.getLevel()) {
					headingList.removeLast();
				}

				headingList.add(item);
			}
		}

		items = new COAItem[tmp.size()];
		tmp.toArray(items);
	}

	private static class COAItem {
		private Account account;
		private COAHeading heading;
		
		public COAItem(Account account) {
			this.account = account;
		}
		
		public COAItem(COAHeading heading) {
			this.heading = heading;
		}
	}
}
