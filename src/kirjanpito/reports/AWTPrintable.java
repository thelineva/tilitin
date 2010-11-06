package kirjanpito.reports;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

public class AWTPrintable implements Printable {
	private Print print;
	private AWTCanvas canvas;
	
	public AWTPrintable(Print print, AWTCanvas canvas) {
		this.print = print;
		this.canvas = canvas;
	}

	public int print(Graphics g, PageFormat pf, int pageIndex) {
		if (canvas == null) {
			canvas = new AWTCanvas(pf);
			print.setCanvas(canvas);
		}
		
		if (pageIndex < print.getPageCount()) {
			canvas.setGraphics((Graphics2D)g);
			print.printPage(pageIndex);
			return Printable.PAGE_EXISTS;
		}
		else {
			return Printable.NO_SUCH_PAGE;
		}
	}
}
