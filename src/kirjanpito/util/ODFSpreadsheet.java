package kirjanpito.util;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import kirjanpito.ui.Kirjanpito;


public class ODFSpreadsheet extends ODFDocument {
	private DecimalFormat numberFormatter;
	private SimpleDateFormat dateFormatter1;
	private SimpleDateFormat dateFormatter2;
	private boolean contentElementOpen;
	private boolean tableElementOpen;
	private boolean rowElementOpen;
	private int indentLevels;
	private boolean indentStyleBold;
	private boolean indentStyleItalic;
	private int colSpan;
	private ArrayList<ODFSpreadsheetColumn> columns;

	public ODFSpreadsheet() {
		setGenerator(Kirjanpito.APP_NAME + "/" + Kirjanpito.APP_VERSION);
		numberFormatter = new DecimalFormat();
		numberFormatter = new DecimalFormat();
		numberFormatter.setMinimumFractionDigits(2);
		numberFormatter.setMaximumFractionDigits(2);
		dateFormatter1 = new SimpleDateFormat("yyyy-MM-dd");
		dateFormatter2 = new SimpleDateFormat("dd.MM.yyyy");
		indentLevels = 0;
		columns = new ArrayList<ODFSpreadsheetColumn>();
	}

	public int getColSpan() {
		return colSpan;
	}

	public void setColSpan(int colSpan) {
		this.colSpan = colSpan;
	}

	public void defineColumn(String name, String width) {
		columns.add(new ODFSpreadsheetColumn(name, width));
	}

	public void addIndentLevels(int levels, boolean bold, boolean italic) {
		this.indentLevels = levels;
		this.indentStyleBold = bold;
		this.indentStyleItalic = bold;
	}

	public void addTable(String name) {
		startDocumentContent();
		endTable();
		XMLWriter w = getContentWriter();
		w.startElement("table:table");
		w.writeAttribute("table:name", name);
		tableElementOpen = true;
	}

	private void endTable() {
		if (tableElementOpen) {
			endRow();
			XMLWriter w = getContentWriter();
			w.endElement("table:table");
			tableElementOpen = false;
		}
	}

	public void addColumn(String style, String defaultCellStyle) {
		XMLWriter w = getContentWriter();
		w.startElement("table:table-column");
		w.writeAttribute("table:style-name", style);
		w.writeAttribute("table:default-cell-style-name", defaultCellStyle);
		w.endElement();
	}

	public void addRow() {
		endRow();
		XMLWriter w = getContentWriter();
		w.startElement("table:table-row");
		w.writeAttribute("table:style-name", "ro1");
		rowElementOpen = true;
	}

	private void endRow() {
		if (rowElementOpen) {
			XMLWriter w = getContentWriter();
			w.endElement("table:table-row");
			rowElementOpen = false;
		}
	}

	public void writeTextCell(String value) {
		writeTextCell(value, "Default");
	}

	public void writeTextCell(String value, String style) {
		XMLWriter w = getContentWriter();
		w.startElement("table:table-cell");
		w.writeAttribute("table:style-name", style);
		w.writeAttribute("office:value-type", "string");
		
		if (colSpan > 1) {
			w.writeAttribute("table:number-columns-spanned", Integer.toString(colSpan));
		}

		w.writeTextElement("text:p", value);
		w.endElement("table:table-cell");
	}

	public void writeFloatCell(BigDecimal value, String style) {
		XMLWriter w = getContentWriter();
		w.startElement("table:table-cell");
		w.writeAttribute("table:style-name", style);
		w.writeAttribute("office:value-type", "float");
		w.writeAttribute("office:value", value.toPlainString());

		if (colSpan > 1) {
			w.writeAttribute("table:number-columns-spanned", Integer.toString(colSpan));
		}

		w.writeTextElement("text:p", numberFormatter.format(value));
		w.endElement("table:table-cell");
	}

	public void writeFloatCell(int value, String style) {
		XMLWriter w = getContentWriter();
		w.startElement("table:table-cell");
		w.writeAttribute("table:style-name", style);
		w.writeAttribute("office:value-type", "float");
		w.writeAttribute("office:value", Integer.toString(value));

		if (colSpan > 1) {
			w.writeAttribute("table:number-columns-spanned", Integer.toString(colSpan));
		}

		w.writeTextElement("text:p", Integer.toString(value));
		w.endElement("table:table-cell");
	}

	public void writeDateCell(Date date, String style) {
		XMLWriter w = getContentWriter();
		w.startElement("table:table-cell");
		w.writeAttribute("table:style-name", style);
		w.writeAttribute("office:value-type", "date");
		w.writeAttribute("office:date-value", dateFormatter1.format(date));

		if (colSpan > 1) {
			w.writeAttribute("table:number-columns-spanned", Integer.toString(colSpan));
		}

		w.writeTextElement("text:p", dateFormatter2.format(date));
		w.endElement("table:table-cell");
	}

	public void writeEmptyCell() {
		writeTextCell("", "Default");
	}

	private void writeStyles() {
		XMLWriter w = getStyleWriter();
		w.startElement("office:document-styles");
		w.writeAttribute("xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0");
		w.writeAttribute("xmlns:style", "urn:oasis:names:tc:opendocument:xmlns:style:1.0");
		w.writeAttribute("xmlns:text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0");
		w.writeAttribute("xmlns:table", "urn:oasis:names:tc:opendocument:xmlns:table:1.0");
		w.writeAttribute("xmlns:fo", "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0");
		w.writeAttribute("xmlns:svg", "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0");
		w.writeAttribute("xmlns:number", "urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0");

		w.startElement("office:font-face-decls");
		writeFontFace("Arial", "Arial", "swiss", "variable");
		w.endElement("office:font-face-decls");

		w.startElement("office:styles");
		w.startElement("style:style");
		w.writeAttribute("style:name", "Default");
		w.writeAttribute("style:family", "table-cell");
		w.startElement("style:text-properties");
		w.writeAttribute("style:font-name-complex", "Arial");
		w.endElement();
		w.endElement("style:style");
		w.endElement("office:styles");

		w.endElement("office:document-styles");
	}

	private void writeFontFace(String name, String fontFamily, String fontFamilyGeneric, String fontPitch) {
		XMLWriter w = getStyleWriter();
		w.startElement("style:font-face");
		w.writeAttribute("style:name", name);
		w.writeAttribute("svg:font-family", fontFamily);
		w.writeAttribute("style:font-family-generic", fontFamilyGeneric);
		w.writeAttribute("style:font-pitch", fontPitch);
		w.endElement();
	}

	private void startDocumentContent() {
		if (contentElementOpen) {
			return;
		}

		XMLWriter w = getContentWriter();
		w.startElement("office:document-content");
		w.writeAttribute("xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0");
		w.writeAttribute("xmlns:style", "urn:oasis:names:tc:opendocument:xmlns:style:1.0");
		w.writeAttribute("xmlns:text", "urn:oasis:names:tc:opendocument:xmlns:text:1.0");
		w.writeAttribute("xmlns:table", "urn:oasis:names:tc:opendocument:xmlns:table:1.0");
		w.writeAttribute("xmlns:fo", "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0");
		w.writeAttribute("xmlns:svg", "urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0");
		w.writeAttribute("xmlns:number", "urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0");

		w.startElement("office:automatic-styles");

		for (ODFSpreadsheetColumn column : columns) {
			w.startElement("style:style");
			w.writeAttribute("style:name", column.name);
			w.writeAttribute("style:family", "table-column");
			w.startElement("style:table-column-properties");
			w.writeAttribute("fo:break-before", "auto");
			w.writeAttribute("style:column-width", column.width);
			w.endElement();
			w.endElement("style:style");
		}

		w.startElement("style:style");
		w.writeAttribute("style:name", "ro1");
		w.writeAttribute("style:family", "table-row");
		w.startElement("style:table-row-properties");
		w.writeAttribute("style:row-height", "0.55cm");
		w.writeAttribute("fo:break-before", "auto");
		w.writeAttribute("style:use-optimal-row-height", "true");
		w.endElement();
		w.endElement("style:style");

		w.startElement("number:number-style");
		w.writeAttribute("style:name", "N0");
		w.startElement("number:number");
		w.writeAttribute("number:decimal-places", "0");
		w.writeAttribute("number:min-integer-digits", "1");
		w.endElement();
		w.endElement("number:number-style");

		w.startElement("number:number-style");
		w.writeAttribute("style:name", "N2");
		w.startElement("number:number");
		w.writeAttribute("number:decimal-places", "2");
		w.writeAttribute("number:min-integer-digits", "1");
		w.writeAttribute("number:grouping", "true");
		w.endElement();
		w.endElement("number:number-style");

		w.startElement("number:date-style");
		w.writeAttribute("style:name", "D1");
		w.writeAttribute("number:automatic-order", "true");
		w.startElement("number:day");
		w.writeAttribute("number:style", "long");
		w.endElement();
		w.writeTextElement("number:text", ".");
		w.startElement("number:month");
		w.writeAttribute("number:style", "long");
		w.endElement();
		w.writeTextElement("number:text", ".");
		w.startElement("number:year");
		w.writeAttribute("number:style", "long");
		w.endElement();
		w.endElement("number:date-style");

		w.startElement("style:style");
		w.writeAttribute("style:name", "boldBorderBottom");
		w.writeAttribute("style:family", "table-cell");
		w.writeAttribute("style:parent-style-name", "Default");
		w.startElement("style:table-cell-properties");
		w.writeAttribute("fo:border-bottom", "0.26pt solid #000000");
		w.writeAttribute("fo:border-left", "none");
		w.writeAttribute("fo:border-right", "none");
		w.writeAttribute("fo:border-top", "none");
		w.endElement();
		w.startElement("style:text-properties");
		w.writeAttribute("fo:font-weight", "bold");
		w.writeAttribute("style:font-weight-complex", "bold");
		w.endElement();
		w.endElement("style:style");

		w.startElement("style:style");
		w.writeAttribute("style:name", "boldAlignRightBorderBottom");
		w.writeAttribute("style:family", "table-cell");
		w.writeAttribute("style:parent-style-name", "Default");
		w.startElement("style:table-cell-properties");
		w.writeAttribute("fo:border-bottom", "0.26pt solid #000000");
		w.writeAttribute("fo:border-left", "none");
		w.writeAttribute("fo:border-right", "none");
		w.writeAttribute("fo:border-top", "none");
		w.writeAttribute("fo:padding-right", "0.2cm");
		w.endElement();
		w.startElement("style:text-properties");
		w.writeAttribute("fo:font-weight", "bold");
		w.writeAttribute("style:font-weight-complex", "bold");
		w.endElement();
		w.startElement("style:paragraph-properties");
		w.writeAttribute("fo:text-align", "end");
		w.writeAttribute("fo:margin-left", "0cm");
		w.endElement();
		w.endElement("style:style");

		w.startElement("style:style");
		w.writeAttribute("style:name", "bold");
		w.writeAttribute("style:family", "table-cell");
		w.writeAttribute("style:parent-style-name", "Default");
		w.startElement("style:text-properties");
		w.writeAttribute("fo:font-weight", "bold");
		w.writeAttribute("style:font-weight-complex", "bold");
		w.endElement();
		w.endElement("style:style");

		w.startElement("style:style");
		w.writeAttribute("style:name", "boldAlignRight");
		w.writeAttribute("style:family", "table-cell");
		w.writeAttribute("style:parent-style-name", "Default");
		w.startElement("style:table-cell-properties");
		w.writeAttribute("style:text-align-source", "fix");
		w.writeAttribute("style:repeat-content", "false");
		w.writeAttribute("fo:padding-right", "0.2cm");
		w.endElement();
		w.startElement("style:text-properties");
		w.writeAttribute("fo:font-weight", "bold");
		w.writeAttribute("style:font-weight-complex", "bold");
		w.endElement();
		w.startElement("style:paragraph-properties");
		w.writeAttribute("fo:text-align", "end");
		w.writeAttribute("fo:margin-left", "0cm");
		w.endElement();
		w.endElement("style:style");

		BigDecimal indent = new BigDecimal("0.00");

		for (int i = 0; i < indentLevels; i++) {
			w.startElement("style:style");
			w.writeAttribute("style:name", "indent" + i);
			w.writeAttribute("style:family", "table-cell");
			w.writeAttribute("style:parent-style-name", "Default");
			w.startElement("style:paragraph-properties");
			w.writeAttribute("fo:text-align", "start");
			w.writeAttribute("fo:margin-left", indent.toPlainString() + "cm");
			w.endElement();
			w.endElement("style:style");
			
			if (indentStyleBold) {
				w.startElement("style:style");
				w.writeAttribute("style:name", "indent" + i + "Bold");
				w.writeAttribute("style:family", "table-cell");
				w.writeAttribute("style:parent-style-name", "Default");
				w.startElement("style:paragraph-properties");
				w.writeAttribute("fo:text-align", "start");
				w.writeAttribute("fo:margin-left", indent.toPlainString() + "cm");
				w.endElement();
				w.startElement("style:text-properties");
				w.writeAttribute("fo:font-weight", "bold");
				w.writeAttribute("style:font-weight-complex", "bold");
				w.endElement();
				w.endElement("style:style");
			}

			if (indentStyleItalic) {
				w.startElement("style:style");
				w.writeAttribute("style:name", "indent" + i + "Italic");
				w.writeAttribute("style:family", "table-cell");
				w.writeAttribute("style:parent-style-name", "Default");
				w.startElement("style:paragraph-properties");
				w.writeAttribute("fo:text-align", "start");
				w.writeAttribute("fo:margin-left", indent.toPlainString() + "cm");
				w.endElement();
				w.startElement("style:text-properties");
				w.writeAttribute("fo:font-style", "italic");
				w.writeAttribute("style:font-style-complex", "italic");
				w.endElement();
				w.endElement("style:style");
			}

			indent = indent.add(new BigDecimal("0.35"));
		}

		writeNumberStyle("num0", "N0", "end", false);
		writeNumberStyle("num0AlignLeft", "N0", "start", false);
		writeNumberStyle("num2", "N2", "end", false);
		writeNumberStyle("num2Bold", "N2", "end", true);
		writeNumberStyle("date", "D1", "end", false);
		writeNumberStyle("dateAlignLeft", "D1", "start", false);

		w.endElement("office:automatic-styles");

		w.startElement("office:body");
		w.startElement("office:spreadsheet");
		contentElementOpen = true;
	}

	private void writeNumberStyle(String name, String style, String align, boolean bold) {
		XMLWriter w = getContentWriter();
		w.startElement("style:style");
		w.writeAttribute("style:name", name);
		w.writeAttribute("style:family", "table-cell");
		w.writeAttribute("style:parent-style-name", "Default");
		w.writeAttribute("style:data-style-name", style);
		w.startElement("style:table-cell-properties");
		w.writeAttribute("style:text-align-source", "fix");
		w.writeAttribute("style:repeat-content", "false");
		w.writeAttribute("fo:padding-right", "0.2cm");
		w.endElement();
		w.startElement("style:paragraph-properties");
		w.writeAttribute("fo:text-align", align);
		w.writeAttribute("fo:margin-left", "0cm");
		w.endElement();

		if (bold) {
			w.startElement("style:text-properties");
			w.writeAttribute("fo:font-weight", "bold");
			w.writeAttribute("style:font-weight-complex", "bold");
			w.endElement();
		}

		w.endElement("style:style");
	}

	private void endDocumentContent() {
		if (contentElementOpen) {
			endTable();
			XMLWriter w = getContentWriter();
			w.endElement("office:spreadsheet");
			w.endElement("office:body");
			w.endElement("office:document-content");
			contentElementOpen = false;
		}
	}

	@Override
	public void save(File file) throws IOException {
		endDocumentContent();
		writeStyles();
		super.save(file);
	}

	private static class ODFSpreadsheetColumn {
		public String name;
		public String width;

		public ODFSpreadsheetColumn(String name, String width) {
			this.name = name;
			this.width = width;
		}
	}
}
