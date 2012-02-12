package kirjanpito.models;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

import kirjanpito.reports.AWTPrintable;
import kirjanpito.reports.PDFCanvas;
import kirjanpito.reports.Print;
import kirjanpito.reports.PrintCanvas;
import kirjanpito.reports.PrintModel;
import kirjanpito.ui.Kirjanpito;
import kirjanpito.util.AppSettings;
import kirjanpito.util.CSVWriter;
import kirjanpito.util.ODFSpreadsheet;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Malli tulosteiden esikatseluikkunalle.
 * 
 * @author Tommi Helineva
 */
public class PrintPreviewModel {
	private PrintService printService;
	private PrintRequestAttributeSet aset;
	private Print print;
	private PrintModel printModel;
	private int pageIndex;

	/**
	 * Palauttaa tulosteen.
	 * 
	 * @return tuloste
	 */
	public Print getPrint() {
		return print;
	}

	/**
	 * Asettaa tulosteen.
	 * 
	 * @param print tuloste
	 */
	public void setPrint(Print print) {
		this.aset = null;
		this.pageIndex = 0;
		this.print = print;
	}
	
	/**
	 * Palauttaa tulosteen mallin.
	 * 
	 * @return tulosteen malli
	 */
	public PrintModel getPrintModel() {
		return printModel;
	}

	/**
	 * Asettaa tulosteen mallin.
	 * 
	 * @param printModel tulosteen malli
	 */
	public void setPrintModel(PrintModel printModel) {
		this.printModel = printModel;
	}

	/**
	 * Palauttaa sivunumeron.
	 * 
	 * @return sivunumero
	 */
	public int getPageIndex() {
		return pageIndex;
	}

	/**
	 * Asettaa sivunumeron.
	 * 
	 * @param pageIndex sivunumero
	 */
	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
	
	/**
	 * Palauttaa sivujen lukumäärän.
	 * 
	 * @return sivujen lukumäärä
	 */
	public int getPageCount() {
		return (print == null) ? 0 : print.getPageCount();
	}

	/**
	 * Palauttaa tulostimen, jolla tuloste tulostetaan.
	 * 
	 * @return tulostin
	 */
	public PrintService getPrintService() {
		if (printService != null)
			return printService;
		
		AppSettings settings = AppSettings.getInstance();
		String printerName = settings.getString("printer");
		PrintService service = null;
		PrintService[] services;
		
		/* Jos tulostinta ei ole vielä valittu, käytetään
		 * oletustulostinta. */
		if (printerName == null) {
			service = PrintServiceLookup.lookupDefaultPrintService();
			
			if (service == null) {
				services = PrintServiceLookup.lookupPrintServices(null, null);
				
				if (services.length > 0)
					service = services[0];
			}
		}
		else {
			/* Etsitään tulostin nimen perusteella. */
			services = PrintServiceLookup.lookupPrintServices(null, null);
			
			for (PrintService serv : services) {
				if (serv.getName().equals(printerName)) {
					service = serv;
					break;
				}
			}
			
			if (service == null && services.length > 0)
				service = services[0];
		}
		
		return service;
	}
	
	/**
	 * Asettaa tulostimen, jolla tuloste tulostetaan.
	 * 
	 * @param service tulostin
	 */
	public void setPrintService(PrintService service) {
		this.printService = service;
		AppSettings settings = AppSettings.getInstance();
		settings.set("printer", service.getName());
	}
	
	/**
	 * Palauttaa <code>PrintRequestAttributeSet</code>in, jota
	 * käytetään tulostettaessa.
	 * 
	 * @return PrintRequestAttributeSet-olio
	 */
	public PrintRequestAttributeSet getAttributeSet() {
		if (aset == null) {
			aset = new HashPrintRequestAttributeSet();
			aset.add(new JobName(print.getTitle(), null));
			aset.add(MediaSizeName.ISO_A4);
			aset.add(new MediaPrintableArea(5, 5, 200, 287, MediaPrintableArea.MM));
			
			AppSettings settings = AppSettings.getInstance();
			String orientation = settings.getString("paper.orientation", "");
			
			if (orientation.equalsIgnoreCase("landscape")) {
				aset.add(OrientationRequested.LANDSCAPE);
			}
			else if (orientation.equalsIgnoreCase("reverse-landscape")) {
				aset.add(OrientationRequested.REVERSE_LANDSCAPE);
			}
			else if (orientation.equalsIgnoreCase("reverse-portrait")) {
				aset.add(OrientationRequested.REVERSE_PORTRAIT);
			}
			else {
				aset.add(OrientationRequested.PORTRAIT);
			}
		}
		
		return aset;
	}
	
	public void saveAttributeSet() {
		AppSettings settings = AppSettings.getInstance();
		Attribute orientation = aset.get(OrientationRequested.class);
		
		if (OrientationRequested.LANDSCAPE.equals(orientation)) {
			settings.set("paper.orientation", "landscape");
		}
		else if (OrientationRequested.REVERSE_LANDSCAPE.equals(orientation)) {
			settings.set("paper.orientation", "reverse-landscape");
		}
		else if (OrientationRequested.REVERSE_PORTRAIT.equals(orientation)) {
			settings.set("paper.orientation", "reverse-portrait");
		}
		else {
			settings.set("paper.orientation", "portrait");
		}
	}
	
	/**
	 * Luo sivun asetukset.
	 */
	public PageFormat createPageFormat() {
		Paper a4Paper = new Paper();
		double paperWidth, paperHeight;
		AppSettings settings = AppSettings.getInstance();
		String orientation = settings.getString("paper.orientation", "");
		
		if (orientation.equalsIgnoreCase("landscape") ||
				orientation.equalsIgnoreCase("reverse-landscape")) {
			paperHeight =  8.26;
			paperWidth  = 11.69;
		}
		else {
			paperWidth  =  8.26;
			paperHeight = 11.69;
		}
		
		a4Paper.setSize(paperWidth * 72.0, paperHeight * 72.0);

		double leftMargin   = 0.25;
		double rightMargin  = 0.25;
		double topMargin    = 0.25;
		double bottomMargin = 0.25;

		a4Paper.setImageableArea(leftMargin * 72.0, topMargin * 72.0,
				(paperWidth  - leftMargin - rightMargin) * 72.0,
				(paperHeight - topMargin - bottomMargin) * 72.0);
		
		PageFormat pageFormat = new PageFormat();
		pageFormat.setPaper(a4Paper);
		return pageFormat;
	}
	
	/**
	 * Tulostaa tulosteen.
	 * 
	 * @throws PrintException jos tulostaminen epäonnistuu
	 */
	public void print() throws PrintException {
		PrintService printService = getPrintService();
		
		if (printService == null) {
			throw new PrintException("Tulostinta ei löytynyt");
		}
		
		PrintCanvas oldCanvas = print.getCanvas();
		DocPrintJob pj = printService.createPrintJob();

		Doc doc = new SimpleDoc(new AWTPrintable(print, null),
				DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
		
		try {
			pj.print(doc, getAttributeSet());
		}
		catch (PrintException e) {
			e.printStackTrace();
		}
		
		print.setCanvas(oldCanvas);
	}
	
	/**
	 * Tallentaa tulosteen PDF-tiedostoon.
	 * 
	 * @param file PDF-tiedosto
	 */
	public void writePDF(File file) throws IOException {
		PrintCanvas oldCanvas = print.getCanvas();
		AppSettings settings = AppSettings.getInstance();
		String orientation = settings.getString("paper.orientation", "");
		
		try {
			OutputStream output = new FileOutputStream(file);
			Document document;

			if (orientation.equalsIgnoreCase("landscape") ||
					orientation.equalsIgnoreCase("reverse-landscape")) {
				document = new Document(PageSize.A4.rotate());
			}
			else {
				document = new Document();
			}
			
			PdfWriter writer = PdfWriter.getInstance(document, output);
			document.open();
			document.addTitle(print.getTitle());
			document.addCreator(Kirjanpito.APP_NAME + " " +
					Kirjanpito.APP_VERSION);
			document.addCreationDate();

			print.setCanvas(new PDFCanvas(document, writer));
			int pageCount = print.getPageCount();
			int pageIndex = 0;
			print.printPage(pageIndex);
			pageIndex++;
			
			while (pageIndex < pageCount) {
				document.newPage();
				print.printPage(pageIndex);
				pageIndex++;
			}
			
			document.close();
			output.close();
		}
		catch (DocumentException e) {
			e.printStackTrace();
		}
		finally {
			print.setCanvas(oldCanvas);
		}
	}
	
	public void writeCSV(File file, char delimiter) throws IOException {
		FileWriter writer = new FileWriter(file);
		CSVWriter csv = new CSVWriter(writer);
		csv.setDelimiter(delimiter);
		printModel.writeCSV(csv);
		writer.close();
	}

	public void writeODS(File file) throws IOException {
		ODFSpreadsheet spreadsheet = new ODFSpreadsheet();
		printModel.writeODS(spreadsheet);
		spreadsheet.save(file);
	}
}
