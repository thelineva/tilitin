package kirjanpito.reports;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;

import kirjanpito.util.AppSettings;

public class AWTCanvas implements PrintCanvas {
	private Graphics2D g;
	private PageFormat pageFormat;
	private Font normalFont;
	private Font smallFont;
	private Font boldFont;
	private Font italicFont;
	private Font headingFont;
	
	public AWTCanvas(PageFormat pageFormat) {
		this.pageFormat = pageFormat;
		AppSettings settings = AppSettings.getInstance();
		String fontFamily = settings.getString("font", Font.SANS_SERIF);
		normalFont = new Font(fontFamily, Font.PLAIN, 10);
		smallFont = new Font(fontFamily, Font.PLAIN, 9);
		boldFont = new Font(fontFamily, Font.BOLD, 10);
		italicFont = new Font(fontFamily, Font.ITALIC, 10);
		headingFont = new Font(fontFamily, Font.PLAIN, 14);
	}
	
	void setGraphics(Graphics2D g) {
		this.g = g;
		this.g.setColor(Color.BLACK);
	}
	
	public int getPageWidth() {
		return (int)pageFormat.getWidth();
	}
	
	public int getPageHeight() {
		return (int)pageFormat.getHeight();
	}
	
	public int getImageableHeight() {
		return (int)pageFormat.getImageableHeight();
	}

	public int getImageableWidth() {
		return (int)pageFormat.getImageableWidth();
	}

	public int getImageableX() {
		return (int)pageFormat.getImageableX();
	}

	public int getImageableY() {
		return (int)pageFormat.getImageableY();
	}
	
	public void close() {
	}

	public void setHeadingStyle() {
		g.setFont(headingFont);
	}
	
	public void setNormalStyle() {
		g.setFont(normalFont);
	}
	
	public void setSmallStyle() {
		g.setFont(smallFont);
	}
	
	public void setBoldStyle() {
		g.setFont(boldFont);
	}
	
	public void setItalicStyle() {
		g.setFont(italicFont);
	}
	
	public void drawText(int x, int y, String s) {
		g.drawString(s, x, y);
	}
	
	public void drawTextCenter(int x, int y, String s) {
		int tx = x - (int)(g.getFont().getStringBounds(s,
				g.getFontRenderContext()).getWidth() / 2.0);
		
		g.drawString(s, tx, y);
	}
	
	public void drawTextRight(int x, int y, String s) {
		int tx = x - (int)(g.getFont().getStringBounds(s,
				g.getFontRenderContext()).getWidth());
		
		g.drawString(s, tx, y);
	}
	
	public void drawLine(int x1, int y1, int x2, int y2, float lineWidth) {
		g.setStroke(new BasicStroke(lineWidth));
		g.drawLine(x1, y1, x2, y2);
	}
	
	public int stringWidth(String s) {
		return g.getFontMetrics().stringWidth(s);
	}
}
