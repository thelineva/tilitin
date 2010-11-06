package kirjanpito.util;

public interface RegistryListener {
	/**
	 * Kutsutaan, kun asetuksia on muutettu.
	 */
	public void settingsChanged();
	
	/**
	 * Kutsutaan, kun nykyinen tilikausi on vaihdettu.
	 */
	public void periodChanged();
	
	/**
	 * Kutsutaan, kun tilikarttaa on muokattu.
	 */
	public void chartOfAccountsChanged();
	
	/**
	 * Kutsutaan, kun vientimalleja on muokattu.
	 */
	public void entryTemplatesChanged();
	
	/**
	 * Kutsutaan, kun tositelajeja on muokattu.
	 */
	public void documentTypesChanged();
}
