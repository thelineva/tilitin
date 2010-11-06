package kirjanpito.ui.resources;

import java.io.InputStream;
import java.net.URL;

/**
 * <code>Resources</code>-luokan avulla voidaan lukea
 * JAR-pakettiin tallennettuja resurssitiedostoja,
 * esimerkiksi käyttöliittymässä tarvittavia kuvakkeita.
 * 
 * @author tommi
 */
public class Resources {
	private Resources() {}
	
	/**
	 * Palauttaa resurssitiedoston <code>filename</code>
	 * URL-osoitteen.
	 * 
	 * @param filename tiedoston nimi
	 * @return URL-osoite
	 */
	public static URL load(String filename) {
		return Resources.class.getResource(filename);
	}
	
	/**
	 * Palauttaa resurssitiedoston <code>filename</code>
	 * tiedostovirran.
	 * 
	 * @param filename tiedoston nimi
	 * @return tiedostovirta
	 */
	public static InputStream loadAsStream(String filename) {
		return Resources.class.getResourceAsStream(filename);
	}
}
