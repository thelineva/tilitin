package kirjanpito.reports;

public interface PrintCanvas {
	/**
	 * Palauttaa sivun leveyden.
	 * 
	 * @return sivun leveys
	 */
	public int getPageWidth();
	
	/**
	 * Palauttaa sivun korkeuden.
	 * 
	 * @return sivun korkeus
	 */
	public int getPageHeight();
	
	/**
	 * Palauttaa tulostusalueen X-koordinaatin.
	 * 
	 * @return tulostusalueen X-koordinaatti
	 */
	public int getImageableX();
	
	/**
	 * Palauttaa tulostusalueen Y-koordinaatin.
	 * 
	 * @return tulostusalueen Y-koordinaatti
	 */
	public int getImageableY();
	
	/**
	 * Palauttaa tulostusalueen leveyden.
	 * 
	 * @return tulostusalueen leveys
	 */
	public int getImageableWidth();
	
	/**
	 * Palauttaa tulostusalueen korkeuden.
	 * 
	 * @return tulostusalueen korkeus
	 */
	public int getImageableHeight();
	
	/**
	 * Kutsuttava lopuksi.
	 */
	public void close();
	
	/**
	 * Asettaa tekstityyliksi otsikkotyylin.
	 */
	public void setHeadingStyle();
	
	/**
	 * Asettaa nykyiseksi tekstityyliksi normaalin leipätekstityylin.
	 */
	public void setNormalStyle();
	
	/**
	 * Asettaa nykyiseksi tekstityyliksi pienemmän leipätekstityylin.
	 */
	public void setSmallStyle();
	
	/**
	 * Asettaa nykyiseksi tekstityyliksi lihavoidun leipätekstityylin.
	 */
	public void setBoldStyle();
	
	/**
	 * Asettaa nykyiseksi tekstityyliksi kursivoidun leipätekstityylin.
	 */
	public void setItalicStyle();
	
	/**
	 * Tulostaa merkkijonon <code>s</code>.
	 * 
	 * @param s tulostettava merkkijono
	 */
	public void drawText(int x, int y, String s);
	
	/**
	 * Tulostaa merkkijonon <code>s</code> keskitettynä.
	 * 
	 * @param s tulostettava merkkijono
	 */
	public void drawTextCenter(int x, int y, String s);
	
	/**
	 * Tulostaa merkkijonon <code>s</code> oikealle tasattuna.
	 * 
	 * @param s tulostettava merkkijono
	 */
	public void drawTextRight(int x, int y, String s);
	
	/**
	 * Tulostaa vaakaviivan, jonka leveys on <code>width</code>.
	 * 
	 * @param lineWidth viivan leveys
	 */
	public void drawLine(int x1, int y1, int x2, int y2, float lineWidth);
	
	/**
	 * Palauttaa merkkijonon <code>s</code> leveyden.
	 * 
	 * @param s merkkijono
	 * @return merkkijonon leveys
	 */
	public int stringWidth(String s);
}
