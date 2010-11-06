package kirjanpito.reports;

import java.awt.Insets;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DrawCommandParser {
	private ArrayList<DrawCommand> commands;
	private Insets margins;
	private PrintCanvas canvas;
	private DrawCommandVarProvider variableProvider;
	private boolean fullPage;
	private int height;
	private String lastFont;
	
	private Pattern COORD_PATTERN = Pattern.compile("([\\+-])([\\d\\.]+)(%)?([\\+-])([\\d\\.]+)(%)?");
	
	public DrawCommandParser() {
		commands = new ArrayList<DrawCommand>();
	}
	
	public Insets getMargins() {
		return margins;
	}

	public void setMargins(Insets margins) {
		this.margins = margins;
	}
	
	public PrintCanvas getCanvas() {
		return canvas;
	}

	public void setCanvas(PrintCanvas canvas) {
		this.canvas = canvas;
	}

	public DrawCommandVarProvider getVariableProvider() {
		return variableProvider;
	}

	public void setVariableProvider(DrawCommandVarProvider provider) {
		this.variableProvider = provider;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void parse(BufferedReader reader) throws IOException, ParseException {
		String line;
		int lineNumber = 0;
		commands.clear();
		
		while ((line = reader.readLine()) != null) {
			lineNumber++;
			parseLine(line, lineNumber);
		}
	}

	public void parse(String source) throws ParseException {
		String[] lines = source.split("\n");
		int lineNumber = 0;
		commands.clear();
		
		for (String line : lines) {
			lineNumber++;
			parseLine(line, lineNumber);
		}
	}
	
	private void parseLine(String line, int lineNumber) throws ParseException {
		int pos = line.indexOf(' ');
		int len = line.length();
		String s;
		
		if (pos < 0) {
			s = line.trim();
			pos = len;
		}
		else {
			s = line.substring(0, pos);
			pos++;
		}

		if (s.isEmpty() || s.startsWith("#")) {
			return;
		}
		
		DrawCommand command = createCommand(s);
		int paramIndex = 0;
		
		if (command == null) {
			throw new ParseException("Tuntematon komento: " + s, lineNumber);
		}
		
		while (pos < len) {
			StringBuilder buf = new StringBuilder();
			int valueOffset = 0;
			boolean quotes = false;
			boolean escaped = false;
			
			for (int i = pos; i < len; i++) {
				char c = line.charAt(i);
				pos = i;
				
				if (!quotes && c == '=') {
					valueOffset = buf.length();
				}
				else if (!quotes && c == ' ') {
					break;
				}
				else if (!quotes && c == '#') {
					pos = len;
					break;
				}
				else if (!escaped && c == '"') {
					quotes = !quotes;
				}
				else if (quotes && c == '\\') {
					escaped = true;
				}
				else {
					buf.append(c);
					escaped = false;
				}
			}
			
			try {
				if (valueOffset == 0) {
					command.parseParameter(paramIndex++, buf.toString());
				}
				else {
					command.parseParameter(buf.substring(0, valueOffset), buf.substring(valueOffset));
				}
			}
			catch (ParseException e) {
				throw new ParseException(e.getMessage(), lineNumber);
			}
			
			pos++;
		}
		
		try {
			command.parametersParsed(paramIndex);
		}
		catch (ParseException e) {
			throw new ParseException(e.getMessage(), lineNumber);
		}
		
		if (command.isDrawable()) {
			commands.add(command);
		}
	}
	
	private DrawCommand createCommand(String s) {
		if (s.equals("text")) {
			return new DrawTextCommand();
		}
		else if (s.equals("line")) {
			return new DrawLineCommand();
		}
		else if (s.equals("set")) {
			return new SetCommand();
		}
		
		return null;
	}
	
	public void draw() {
		reset();
		
		for (DrawCommand command : commands) {
			command.draw(canvas);
		}
	}
	
	private void reset() {
		lastFont = null;
	}
	
	private int millimetersToDevicePixels(double mm) {
		return (int)Math.round(mm * 0.0393700787 * 72);
	}
	
	private float millimetersToDevicePixelsF(float mm) {
		return mm * 0.0393700787f * 72f;
	}
	
//	private double devicePixelsToMillimeters(double dp) {
//		return dp / 72.0 * 25.4;
//	}
	
	private Point parsePoint(String s) throws ParseException {
		Matcher matcher = COORD_PATTERN.matcher(s);
		
		if (matcher.matches()) {
			try {
				Point point = new Point();
				
				if (canvas == null) {
					return point;
				}
				
				double dx = Double.parseDouble(matcher.group(2));
				double dy = Double.parseDouble(matcher.group(5));
				boolean fromRight = (matcher.group(1).equals("-"));
				boolean fromBottom = (matcher.group(4).equals("-"));
				boolean propX = (matcher.group(3) != null);
				boolean propY = (matcher.group(6) != null);
				
				if (fullPage) {
					int x = propX ? (int)Math.round(dx / 100.0 * canvas.getPageWidth())
							: millimetersToDevicePixels(dx);
					
					int y = propY ? (int)Math.round(dy / 100.0 * canvas.getPageHeight())
							: millimetersToDevicePixels(dy);
					
					point.x = fromRight ? canvas.getPageWidth() - x : x;
					point.y = fromBottom ? canvas.getPageHeight() - y : y;
				}
				else {
					int x = propX ? (int)Math.round(dx / 100.0 * (canvas.getPageWidth() - margins.left - margins.right))
							: millimetersToDevicePixels(dx);
					
					int y = propY ? (int)Math.round(dy / 100.0 * (canvas.getPageHeight() - margins.top - margins.bottom))
							: millimetersToDevicePixels(dy);
					
					point.x = fromRight ? canvas.getPageWidth() - margins.right - x : x + margins.left;
					point.y = fromBottom ? canvas.getPageHeight() - margins.bottom - y : y + margins.top;
				}
				
				return point;
			}
			catch (NumberFormatException e) {
			}
		}
		
		throw new ParseException("Virheellinen piste: " + s, 0);
	}
	
	private void parseFont(String s) throws ParseException {
		if (!s.equals("normal") &&
				!s.equals("bold") &&
				!s.equals("italic") &&
				!s.equals("small") &&
				!s.equals("heading")) {
			
			throw new ParseException("Virheellinen fontti: " + s, 0);
		}
	}
	
	private void setFont(String s) {
		if (s.equals(lastFont)) {
			return;
		}
		
		if (s.equals("normal")) {
			canvas.setNormalStyle();
		}
		else if (s.equals("bold")) {
			canvas.setBoldStyle();
		}
		else if (s.equals("italic")) {
			canvas.setItalicStyle();
		}
		else if (s.equals("small")) {
			canvas.setSmallStyle();
		}
		else if (s.equals("heading")) {
			canvas.setHeadingStyle();
		}
		
		lastFont = s;
	}
	
	private String replaceVariables(String s) {
		StringBuilder sb = new StringBuilder(s);
		int pos = sb.indexOf("$");
		
		while (pos >= 0) {
			if (pos == 0 || sb.charAt(pos - 1) != '\\') {
				String name = "";
				int endPos = pos + 1;
				
				if (pos + 1 < sb.length()) {
					name = Character.toString(sb.charAt(pos + 1));
					endPos = pos + 2;
				}
				
				sb.replace(pos, endPos, variableProvider.getVariableValue(name));
			}
			else {
				pos++;
			}
			
			pos = sb.indexOf("$", pos);
		}
		
		return sb.toString();
	}
	
	private interface DrawCommand {
		public void parseParameter(int index, String param) throws ParseException;
		public void parseParameter(String key, String value) throws ParseException;
		public void parametersParsed(int count) throws ParseException;
		public boolean isDrawable();
		public void draw(PrintCanvas canvas);
	}
	
	private class DrawTextCommand implements DrawCommand {
		private Point point;
		private String text;
		private String font;
		private int alignment;
		
		public DrawTextCommand() {
			text = "";
			font = "normal";
			point = new Point();
		}
		
		public void parseParameter(int index, String param) throws ParseException {
			if (index == 0) {
				point = parsePoint(param);
			}
			else if (index == 1) {
				text = param;
			}
			else {
				text += " " + param;
			}
		}
		
		public void parametersParsed(int count) throws ParseException {
			if (count < 2) {
				throw new ParseException("text-komennolle on annettava vähintään kaksi parametria", 0);
			}
		}
		
		public void parseParameter(String key, String value) throws ParseException {
			if (key.equals("align")) {
				if (value.equals("left")) {
					alignment = 0;
				}
				else if (value.equals("center")) {
					alignment = 1;
				}
				else if (value.equals("right")) {
					alignment = 2;
				}
				else {
					throw new ParseException("Virheellinen tekstin tasaus: " + value, 0);
				}
			}
			else if (key.equals("font")) {
				parseFont(value);
				font = value;
			}
			else {
				throw new ParseException("Virheellinen attribuutti: " + key, 0);
			}
		}
		
		public void draw(PrintCanvas canvas) {
			String s = replaceVariables(text);
			setFont(font);
			
			if (alignment == 0) {
				canvas.drawText(point.x, point.y, s);
			}
			else if (alignment == 1) {
				canvas.drawTextCenter(point.x, point.y, s);
			}
			else {
				canvas.drawTextRight(point.x, point.y, s);
			}
		}
		
		public boolean isDrawable() {
			return true;
		}
	}
	
	private class DrawLineCommand implements DrawCommand {
		private Point point1;
		private Point point2;
		private float lineWidth;
		
		public void parseParameter(int index, String param) throws ParseException {
			if (index == 0) {
				point1 = parsePoint(param);
			}
			else if (index == 1) {
				point2 = parsePoint(param);
			}
		}
		
		public void parseParameter(String key, String value) throws ParseException {
			if (key.equals("width")) {
				try {
					lineWidth = millimetersToDevicePixelsF(Float.parseFloat(value));
				}
				catch (NumberFormatException e) {
					throw new ParseException("Virheellinen viivan leveys: " + value, 0);
				}
			}
			else {
				throw new ParseException("Virheellinen attribuutti: " + key, 0);
			}
		}
		
		public void parametersParsed(int count) throws ParseException {
			if (count != 2) {
				throw new ParseException("line-komennolle on annettava kaksi parametria", 0);
			}
		}
		
		public void draw(PrintCanvas canvas) {
			canvas.drawLine(point1.x, point1.y, point2.x, point2.y, lineWidth);
		}
		
		public boolean isDrawable() {
			return true;
		}
	}
	
	private class SetCommand implements DrawCommand {
		public void parseParameter(int index, String param) throws ParseException {
		}
		
		public void parseParameter(String key, String value) throws ParseException {
			if (key.equals("height")) {
				try {
					height = millimetersToDevicePixels(Double.parseDouble(value));
				}
				catch (NumberFormatException e) {
					throw new ParseException("Virheellinen korkeus: " + value, 0);
				}
			}
			else if (key.equals("fullPage")) {
				if (value.equals("true")) {
					fullPage = true;
				}
				else if (value.equals("false")) {
					fullPage = false;
				}
				else {
					throw new ParseException("Virheellinen fullPage-asetuksen arvo: " + value, 0);
				}
			}
			else {
				throw new ParseException("Virheellinen attribuutti: " + key, 0);
			}
		}
		
		public void parametersParsed(int count) throws ParseException {
			if (count > 0) {
				throw new ParseException("Ylimääräisiä parametreja", 0);
			}
		}
		
		public void draw(PrintCanvas canvas) {
		}
		
		public boolean isDrawable() {
			return false;
		}
	}
}
