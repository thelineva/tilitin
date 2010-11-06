package kirjanpito.reports;

import java.io.IOException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

public class PDFCanvas implements PrintCanvas {
	private PdfContentByte cb;
	private float pageWidth, pageHeight;
	private float lineHeight;
	private boolean textMode;
	private BaseFont currentFont;
	private float currentFontSize;
	private BaseFont normalFont;
	private BaseFont boldFont;
	private BaseFont italicFont;
	
	public PDFCanvas(Document document, PdfWriter writer)
		throws IOException, DocumentException
	{
		this.cb = writer.getDirectContent();
		this.pageWidth = document.getPageSize().getWidth();
		this.pageHeight = document.getPageSize().getHeight();
		this.normalFont = BaseFont.createFont(BaseFont.HELVETICA,
				BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		this.boldFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD,
				BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		this.italicFont = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE,
				BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
	}
	
	public int getPageWidth() {
		return (int)pageWidth;
	}
	
	public int getPageHeight() {
		return (int)pageHeight;
	}
	
	public int getImageableHeight() {
		return (int)pageHeight;
	}

	public int getImageableWidth() {
		return (int)pageWidth;
	}

	public int getImageableX() {
		return 0;
	}

	public int getImageableY() {
		return 0;
	}
	
	public void close() {
		endText();
	}
	
	public void setHeadingStyle() {
		beginText();
		cb.setFontAndSize(normalFont, 14f);
		calculateLineHeight(normalFont, 14f);
	}
	
	public void setNormalStyle() {
		beginText();
		cb.setFontAndSize(normalFont, 10f);
		calculateLineHeight(normalFont, 10f);
	}
	
	public void setSmallStyle() {
		beginText();
		cb.setFontAndSize(normalFont, 9f);
		calculateLineHeight(normalFont, 9f);
	}
	
	public void setBoldStyle() {
		beginText();
		cb.setFontAndSize(boldFont, 10);
		calculateLineHeight(boldFont, 10);
	}
	
	public void setItalicStyle() {
		beginText();
		cb.setFontAndSize(italicFont, 10);
		calculateLineHeight(italicFont, 10);
	}
	
	public void drawText(int x, int y, String s) {
		beginText();
		cb.setTextMatrix(x, pageHeight - lineHeight - y);
		cb.showText(s);
	}
	
	public void drawTextCenter(int x, int y, String s) {
		beginText();
		cb.showTextAligned(PdfContentByte.ALIGN_CENTER,
				s, x, pageHeight - lineHeight - y, 0);
	}
	
	public void drawTextRight(int x, int y, String s) {
		beginText();
		cb.showTextAligned(PdfContentByte.ALIGN_RIGHT,
				s, x, pageHeight - lineHeight - y, 0);
	}
	
	public void drawLine(int x1, int y1, int x2, int y2, float lineWidth) {
		endText();
		cb.setLineWidth(lineWidth);
		cb.moveTo(x1, pageHeight - y1);
		cb.lineTo(x2, pageHeight - y2);
		cb.stroke();
	}
	
	public int stringWidth(String s) {
		return (int)currentFont.getWidthPoint(s, currentFontSize);
	}
	
	private void calculateLineHeight(BaseFont font, float size) {
		currentFont = font;
		currentFontSize = size;
		lineHeight = font.getAscentPoint("M", size) -
			font.getDescentPoint("M", size) - 5;
	}
	
	private void beginText() {
		if (!textMode) {
			cb.beginText();
			textMode = true;
		}
	}
	
	private void endText() {
		if (textMode) {
			cb.endText();
			textMode = false;
		}
	}
}
