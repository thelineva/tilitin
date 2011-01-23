package kirjanpito.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import kirjanpito.ui.resources.Resources;
import kirjanpito.util.ChartOfAccounts;

/**
 * <code>TableCellRenderer</code>in toteuttava luokka, joka näyttää
 * tilikartan rivin.
 * 
 * @author Tommi Helineva
 */
public class COATableCellRenderer extends DefaultTableCellRenderer {
	private ChartOfAccounts coa;
	private boolean indentEnabled;
	private boolean highlightFavouriteAccounts;
	private Color favouriteColor;
	private BufferedImage favouriteImage;
	private boolean imageVisible;
	
	private static final long serialVersionUID = 1L;
	
	public COATableCellRenderer() {
		this.indentEnabled = true;
		this.highlightFavouriteAccounts = true;
		this.favouriteColor = new Color(245, 208, 169);
		
		try {
			this.favouriteImage = ImageIO.read(Resources.load("favourite-16x16.png"));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Palauttaa tilikartan, jonka rivit näytetään.
	 * 
	 * @return tilikartta
	 */
	public ChartOfAccounts getChartOfAccounts() {
		return coa;
	}

	/**
	 * Asettaa tilikartan, jonka rivit näytetään.
	 * 
	 * @param coa tilikartta
	 */
	public void setChartOfAccounts(ChartOfAccounts coa) {
		this.coa = coa;
	}

	public boolean isIndentEnabled() {
		return indentEnabled;
	}

	public void setIndentEnabled(boolean indentEnabled) {
		this.indentEnabled = indentEnabled;
	}

	public boolean isHighlightFavouriteAccounts() {
		return highlightFavouriteAccounts;
	}

	public void setHighlightFavouriteAccounts(boolean highlightFavouriteAccounts) {
		this.highlightFavouriteAccounts = highlightFavouriteAccounts;
	}

	public Component getTableCellRendererComponent(JTable table, Object value, 
			boolean isSelected, boolean hasFocus, int row, int column)
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		row = table.convertRowIndexToModel(row);
		setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
		Font font = getFont();
		int level;

		if (coa.getType(row) == ChartOfAccounts.TYPE_HEADING) {
			imageVisible = false;
			setForeground((coa.getHeading(row).getLevel() == 0) ? Color.RED : Color.BLACK);
			setFont(font.deriveFont(Font.BOLD));
			level = coa.getHeading(row).getLevel() * 2;
		}
		else {
			imageVisible = highlightFavouriteAccounts && (coa.getAccount(row).getFlags() & 0x01) != 0;
			
			if (!isSelected && imageVisible) {
				setBackground(favouriteColor);
			}
			
			setForeground(Color.BLACK);
			setFont(font.deriveFont(Font.PLAIN));
			level = 12;
		}
		
		if (!indentEnabled) {
			level = 0;
		}
		
		/* Sisennetään tekstiä. */
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < level; i++) sb.append(' ');
		
		if (value != null)
			sb.append(value.toString());
		
		setText(sb.toString());
		return this;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if (imageVisible) {
			g.drawImage(favouriteImage, getWidth() - 25, (getHeight() - 16) / 2, null);
		}
	}
}
