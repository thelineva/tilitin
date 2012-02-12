package kirjanpito.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ODFDocument {
	private XMLWriter styleWriter;
	private XMLWriter contentWriter;
	private String generator;
	private String title;
	private String mimeType;

	public ODFDocument() {
		styleWriter = new XMLWriter();
		contentWriter = new XMLWriter();
		mimeType = "application/vnd.oasis.opendocument.spreadsheet";
	}

	public String getGenerator() {
		return generator;
	}

	public void setGenerator(String generator) {
		this.generator = generator;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public XMLWriter getStyleWriter() {
		return styleWriter;
	}
	
	public XMLWriter getContentWriter() {
		return contentWriter;
	}
	
	public void save(File file) throws IOException {
		FileOutputStream stream = new FileOutputStream(file);
		ZipOutputStream zip = new ZipOutputStream(stream);
		writeManifest(zip);
		writeMimeType(zip);
		writeMeta(zip);

		ZipEntry entry = new ZipEntry("styles.xml");
		zip.putNextEntry(entry);
		writeXml(zip, styleWriter);
		zip.closeEntry();

		entry = new ZipEntry("content.xml");
		zip.putNextEntry(entry);
		writeXml(zip, contentWriter);
		zip.closeEntry();

		zip.close();
	}
	
	private void writeManifest(ZipOutputStream zip) throws IOException {
		XMLWriter writer = new XMLWriter();
		writer.startElement("manifest:manifest");
		writer.writeAttribute("xmlns:manifest", "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0");
		writer.writeAttribute("manifest:version", "1.2");
		
		writer.startElement("manifest:file-entry");
		writer.writeAttribute("manifest:media-type", "application/vnd.oasis.opendocument.spreadsheet");
		writer.writeAttribute("manifest:version", "1.2");
		writer.writeAttribute("manifest:full-path", "/");
		writer.endElement();
		
		writer.startElement("manifest:file-entry");
		writer.writeAttribute("manifest:media-type", "text/xml");
		writer.writeAttribute("manifest:full-path", "meta.xml");
		writer.endElement();
		
		writer.startElement("manifest:file-entry");
		writer.writeAttribute("manifest:media-type", "text/xml");
		writer.writeAttribute("manifest:full-path", "content.xml");
		writer.endElement();
		
		writer.startElement("manifest:file-entry");
		writer.writeAttribute("manifest:media-type", "text/xml");
		writer.writeAttribute("manifest:full-path", "styles.xml");
		writer.endElement();

		writer.endElement("manifest:manifest");
		
		ZipEntry entry = new ZipEntry("META-INF/manifest.xml");
		zip.putNextEntry(entry);
		writeXml(zip, writer);
		zip.closeEntry();
	}
	
	private void writeMimeType(ZipOutputStream zip) throws IOException {
		ZipEntry entry = new ZipEntry("mimetype");
		zip.putNextEntry(entry);
		zip.write(mimeType.getBytes());
		zip.closeEntry();
	}
	
	private void writeMeta(ZipOutputStream zip) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		XMLWriter writer = new XMLWriter();
		writer.startElement("office:document-meta");
		writer.writeAttribute("xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0");
		writer.writeAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
		writer.writeAttribute("xmlns:meta", "urn:oasis:names:tc:opendocument:xmlns:meta:1.0");
		writer.writeAttribute("office:version", "1.2");
		writer.startElement("office:meta");
		writer.writeTextElement("meta:creation-date", dateFormat.format(new Date()));

		if (generator != null) {
			writer.writeTextElement("meta:generator", generator);
		}

		if (title != null) {
			writer.writeTextElement("dc:title", title);
		}

		writer.endElement("office:meta");
		writer.endElement("office:document-meta");
		
		ZipEntry entry = new ZipEntry("meta.xml");
		zip.putNextEntry(entry);
		writeXml(zip, writer);
		zip.closeEntry();
	}
	
	private void writeXml(ZipOutputStream zip, XMLWriter writer) throws IOException {
		zip.write(writer.toString().getBytes("UTF-8"));
	}
}
