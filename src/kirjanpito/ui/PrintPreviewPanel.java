package kirjanpito.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.swing.JPanel;

/**
 * Tulosteiden esikatselupaneeli.
 * 
 * @author Tommi Helineva
 */
public class PrintPreviewPanel extends JPanel {
	private PageFormat pageFormat;
	private Printable printable;
	private int pageIndex;
	private double scale;
	
	private static final long serialVersionUID = 1L;
	
	public PrintPreviewPanel() {
		scale = -1;
	}
	
	/**
	 * Palauttaa sivun asetukset.
	 * 
	 * @return sivun asetukset
	 */
	public PageFormat getPageFormat() {
		return pageFormat;
	}

	/**
	 * Asettaa sivun asetukset.
	 * 
	 * @param pageFormat sivun asetukset
	 */
	public void setPageFormat(PageFormat pageFormat) {
		this.pageFormat = pageFormat;
	}

	/**
	 * Palauttaa tulosteen.
	 * 
	 * @return tuloste
	 */
	public Printable getPrintable() {
		return printable;
	}
	
	/**
	 * Asettaa tulosteen.
	 * 
	 * @param printable tuloste
	 */
	public void setPrintable(Printable printable) {
		this.printable = printable;
	}
	
	/**
	 * Palauttaa näytettävän sivun numeron. Ensimmäisen
	 * sivun numero on 0.
	 * 
	 * @return sivunumero
	 */
	public int getPageIndex() {
		return pageIndex;
	}

	/**
	 * Asettaa näytettävän sivun numeron. Ensimmäisen
	 * sivun numero on 0.
	 * 
	 * @param pageIndex sivunumero
	 */
	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
		
		if (printable != null && pageFormat != null)
			repaint();
	}

	/**
	 * Palauttaa mittakaavan.
	 * 
	 * @return mittakaava
	 */
	public double getScale() {
		return scale;
	}

	/**
	 * Asettaa mittakaavan. Jos <code>scale &lt; 1</code>,
	 * sivu näytetään pienennettynä; jos <code>scale &gt 1</code>,
	 * sivu näytetään suurennettuna; jos <code>scale == 1</code>,
	 * sivu näytetään alkuperäisessä koossa; jos <code>scale
	 * == -1</code>, tuloste piirretään niin suurena kuin
	 * paneeliin mahtuu.
	 * 
	 * @param scale mittakaava
	 */
	public void setScale(double scale) {
		this.scale = scale;
		
		if (printable != null && pageFormat != null) {
			revalidate();
			repaint();
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (pageFormat == null || printable == null)
			return;
		
		double scale = this.scale;
		double width = getWidth();
		double height = getHeight();
		double pageWidth = pageFormat.getWidth();
		double pageHeight = pageFormat.getHeight();
		
		/* Jos mittakaavaksi on asetettu luku, joka on pienempi kuin 0,
		 * lasketaan mittakaava niin, että sivu näkyy kokonaan paneelissa.
		 */
		if (scale < 0) {
			scale = Math.min((width - 20) / pageWidth, (height - 20) / pageHeight);
		}
		
		/* Lasketaan sivun mitat oikeassa mittakaavassa. */
		Rectangle rect = new Rectangle(
				(int)((width - pageWidth * scale) / 2.0),
				(int)((height - pageHeight * scale) / 2.0),
				(int)(pageWidth * scale), (int)(pageHeight * scale));
		
		rect.x = Math.max(0, rect.x);
		rect.y = Math.max(0, rect.y);

		Rectangle dirtyBounds = g.getClipBounds();
		
		/* Lasketaan mitat alueelle, joka on piirrettävä uudelleen. */
		Rectangle dirtyPaperBounds = dirtyBounds.intersection(rect);
		
		if (dirtyPaperBounds.width <= 0 || dirtyPaperBounds.height <= 0)
			return;

		/* Piirretään sivun varjo. */
		g.setColor(Color.BLACK);
		g.fillRect(rect.x + 5, rect.y + 5, rect.width, rect.height);
		
		/* Piirretään sivun reunat. */
		g.drawRect(rect.x - 1, rect.y - 1, rect.width + 1, rect.height + 1);
		
		AffineTransform transform = new AffineTransform();
		Image image = null;

		image = createImage(dirtyPaperBounds.width, dirtyPaperBounds.height);
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, dirtyPaperBounds.width, dirtyPaperBounds.height);
		g2.setColor(Color.BLACK);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		/* Kaikille piirtokäskyille tehdään muunnos, jotta
		 * ne piirretään oikeaan kohtaan kuvassa ja oikeassa
		 * mittakaavassa. */
		transform.translate(rect.x - dirtyPaperBounds.x, rect.y - dirtyPaperBounds.y);
		transform.scale(scale, scale);
		g2.setTransform(transform);
		
		/* Piirretään sivun sisältö kuvaan. */
		try {
			printable.print(g2, pageFormat, pageIndex);
		}
		catch (PrinterException e) {
			e.printStackTrace();
		}
		
		/* Piirretään kuva näytölle. */
		g.drawImage(image, dirtyPaperBounds.x, dirtyPaperBounds.y,
				dirtyPaperBounds.width, dirtyPaperBounds.height, this);
	}

	/**
	 * Palauttaa paneelin koon.
	 * 
	 * @return paneelin koko
	 */
	public Dimension getPreferredSize() {
		if (pageFormat == null) {
			return super.getPreferredSize();
		}
		else {
			double pageWidth = pageFormat.getWidth();
			double pageHeight = pageFormat.getHeight();
			
			return new Dimension((int)(pageWidth * scale + 15),
					(int)(pageHeight * scale + 15));
		}
	}
}
