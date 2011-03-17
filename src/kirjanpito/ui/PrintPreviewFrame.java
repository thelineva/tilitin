package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import kirjanpito.models.PrintPreviewModel;
import kirjanpito.reports.AWTCanvas;
import kirjanpito.reports.AWTPrintable;
import kirjanpito.reports.Print;
import kirjanpito.util.AppSettings;

/**
 * Tulosteiden esikatseluikkuna.
 * 
 * @author Tommi Helineva
 */
public class PrintPreviewFrame extends JFrame {
	private PrintPreviewModel model;
	private PrintPreviewPanel previewPanel;
	private JLabel pageLabel;
	private JComboBox zoomComboBox;
	private int px, py, dx, dy;
	
	private static final double[] ZOOM_LEVELS = {
		-1, 0.50, 0.70, 0.85, 1.00, 1.25, 1.50, 1.75, 2.00, 3.00, 4.00
	};
	
	private static final String[] ZOOM_TEXTS = {
		"Paras sovitus", "50 %", "70 %", "85 %", "100 %", "125 %",
		"150 %", "175 %", "200 %", "300 %", "400 %"
	};
	
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME); 
	private static final long serialVersionUID = 1L;
	
	public PrintPreviewFrame(Window owner, PrintPreviewModel model) {
		super("Tulosteen esikatselu");
		this.model = model;
	}
	
	public PrintPreviewModel getModel() {
		return model;
	}

	public void close() {
		AppSettings settings = AppSettings.getInstance();
		settings.set("print-preview-window.width", getWidth());
		settings.set("print-preview-window.height", getHeight());
		dispose();
	}
	
	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}
		});
		
		setLayout(new BorderLayout());
		createToolBar();
		createPrintPreviewPanel();
		
		/* Suljetaan ikkuna, kun Escape-näppäintä painetaan. */
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
		rootPane.getActionMap().put("close", closeListener);
		
		/* Siirrytään edelliselle sivulle, kun Page Up -näppäintä painetaan. */
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), "prev");
		rootPane.getActionMap().put("prev", prevPageListener);
		
		/* Siirrytään edelliselle sivulle, kun Page Down -näppäintä painetaan. */
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), "next");
		rootPane.getActionMap().put("next", nextPageListener);
		
		AppSettings settings = AppSettings.getInstance();
		int width = settings.getInt("print-preview-window.width", 0);
		int height = settings.getInt("print-preview-window.height", 0);
		
		if (width > 0 && height > 0) {
			setSize(width, height);
		}
		else {
			pack();
		}
		
		setLocationRelativeTo(null);
		updatePrint();
	}
	
	/**
	 * Luo ikkunan työkalurivin.
	 */
	private void createToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		
		/* Luodaan työkalurivin painikkeet. */
		JButton button = SwingUtils.createToolButton("close-22x22.png",
				"Sulje", closeListener, true);
		button.setMnemonic('S');
		toolBar.add(button);
		
		button = SwingUtils.createToolButton("print-22x22.png",
				"Tulosta", printListener, true);
		button.setMnemonic('u');
		toolBar.add(button);
		
		button = SwingUtils.createToolButton("save-22x22.png",
				"Tallenna", saveListener, true);
		button.setMnemonic('T');
		toolBar.add(button);
		
		button = SwingUtils.createToolButton("preferences-22x22.png",
				"Asetukset", settingsListener, false);
		button.setMnemonic('A');
		toolBar.add(button);
		
		toolBar.addSeparator();
		
		toolBar.add(SwingUtils.createToolButton("go-previous-22x22.png",
				"Edellinen", prevPageListener, false));
		
		toolBar.add(SwingUtils.createToolButton("go-next-22x22.png",
				"Seuraava", nextPageListener, false));
		
		toolBar.addSeparator();
		
		pageLabel = new JLabel();
		toolBar.add(pageLabel);
		toolBar.add(Box.createRigidArea(new Dimension(15, 1)));
		
		/* Luodaan pudotusvalikko, josta zoomaustaso voidaan valita. */
		zoomComboBox = new JComboBox(ZOOM_TEXTS);
		zoomComboBox.setMaximumSize(
				new Dimension(120, Integer.MAX_VALUE)); // Leveimmillään 120px
		zoomComboBox.setSelectedIndex(4); // 100%
		zoomComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				previewPanel.setScale(ZOOM_LEVELS[zoomComboBox.getSelectedIndex()]);
			}
		});
		
		toolBar.add(zoomComboBox);
		add(toolBar, BorderLayout.PAGE_START);
	}
	
	/**
	 * Luo esikatselupaneelin.
	 */
	private void createPrintPreviewPanel() {
		previewPanel = new PrintPreviewPanel();
		previewPanel.setScale(1.00);
		previewPanel.setPreferredSize(new Dimension(710, 500));
		
		JScrollPane scrollPane = new JScrollPane(previewPanel,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		final JViewport viewport = scrollPane.getViewport();
		
		/* Zoomataan, kun hiiren rullaa vieritään Ctrl-näppäin pohjassa */
		previewPanel.addMouseWheelListener(new MouseAdapter() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if ((e.getModifiers() & KeyEvent.CTRL_MASK) > 0) {
					int index = zoomComboBox.getSelectedIndex();
					
					if (e.getWheelRotation() > 0 && index > 0) {
						zoomComboBox.setSelectedIndex(
								zoomComboBox.getSelectedIndex() - 1);
					}
					else if (e.getWheelRotation() < 0 &&
							index < ZOOM_LEVELS.length - 1) {
						zoomComboBox.setSelectedIndex(index + 1);
					}
				}
				else {
					Point pos = viewport.getViewPosition();
					Dimension size = viewport.getViewSize();
					
					/* Vieritetään sivua vaakatasossa, jos käytetään
					 * shift-näppäintä. */
					if ((e.getModifiers() & KeyEvent.SHIFT_MASK) > 0) {
						pos.x = Math.max(0, Math.min(
								pos.x + e.getWheelRotation() * 50,
								size.width - viewport.getWidth()));
					}
					else {
						/* Siirrytään edellisen sivun alareunaan, jos sivun
						 * yläreunassa rullataan ylöspäin. */
						if (pos.y == 0 && e.getWheelRotation() < 0) {
							if (gotoPreviousPage()) {
								pos.y = size.height - viewport.getHeight();
							}
						}
						/* Siirrytään seuraavan sivun yläreunaan, jos sivun
						 * alareunassa rullataan alaspäin. */
						else if (pos.y >= size.height - viewport.getHeight()
								&& e.getWheelRotation() > 0) {
							
							if (gotoNextPage()) {
								pos.y = 0;
							}
						}
						else {
							pos.y = Math.max(0, Math.min(
									pos.y + e.getWheelRotation() * 50,
									size.height - viewport.getHeight()));
						}
					}
					
					viewport.setViewPosition(pos);
				}
			}
		});
		
		previewPanel.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				px = e.getX();
				py = e.getY();
				dx = dy = 0;
			}

			public void mouseReleased(MouseEvent e) {
				if (dx != 0 || dy != 0) {
					Point pos = viewport.getViewPosition();
					Dimension size = viewport.getViewSize();
					pos.x = Math.max(0, Math.min(pos.x + dx,
							size.width - viewport.getWidth()));
					pos.y = Math.max(0, Math.min(pos.y + dy,
							size.height - viewport.getHeight()));
					viewport.setViewPosition(pos);
				}
			}
		});
		
		previewPanel.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				dx = px - e.getX();
				dy = py - e.getY();
			}
		});
		
		add(scrollPane, BorderLayout.CENTER);
	}
	
	/**
	 * Näyttää tulostusasetukset.
	 */
	public void showSettings() {
		PrintService service = model.getPrintService();
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		service = ServiceUI.printDialog(null, 50, 50,
				services, service, null, model.getAttributeSet());
		
		if (service != null) {
			model.setPrintService(service);
			model.saveAttributeSet();
			model.setPageIndex(0);
			previewPanel.revalidate();
			updatePrint();
		}
	}
	
	/**
	 * Tulostaa tulosteen.
	 */
	public void print() {
		try {
			model.print();
		}
		catch (PrintException e) {
			logger.log(Level.SEVERE, "Tulostaminen epäonnistui", e);
			SwingUtils.showErrorMessage(this,
					"Tulostaminen epäonnistui. " + e.getMessage());
		}
	}
	
	/**
	 * Tallentaa tulosteen PDF-tiedostoon.
	 */
	public void save() {
		AppSettings settings = AppSettings.getInstance();
		String path = settings.getString("pdf-directory", ".");
		JFileChooser fc = new JFileChooser(path);
		fc.setFileFilter(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".pdf");
			}

			public String getDescription() {
				return "PDF-tiedostot";
			}
		});
		
		FileFilter csvCommaFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".csv");
			}

			public String getDescription() {
				return "CSV-tiedostot, erottimena pilkku";
			}
		};
		
		FileFilter csvSemicolonFilter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().endsWith(".csv");
			}

			public String getDescription() {
				return "CSV-tiedostot, erottimena puolipiste";
			}
		};
		
		fc.addChoosableFileFilter(csvCommaFilter);
		fc.addChoosableFileFilter(csvSemicolonFilter);
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			settings.set("pdf-directory",
					file.getParentFile().getAbsolutePath());
			
			try {
				if (file.getName().endsWith(".csv") ||
						fc.getFileFilter() == csvCommaFilter ||
						fc.getFileFilter() == csvSemicolonFilter) {
					
					if (!file.getName().endsWith(".csv")) {
						file = new File(file.getAbsolutePath() + ".csv");
					}
					
					char delimiter = ';';
					
					if (fc.getFileFilter() == csvCommaFilter) {
						delimiter = ',';
					}
					
					model.writeCSV(file, delimiter);
				}
				else {
					if (!file.getName().endsWith(".pdf")) {
						file = new File(file.getAbsolutePath() + ".pdf");
					}
					
					model.writePDF(file);
				}
			}
			catch (IOException e) {
				logger.log(Level.SEVERE, "Tallentaminen epäonnistui", e);
				SwingUtils.showErrorMessage(this,
						"Tallentaminen epäonnistui. " + e.getMessage());
			}
		}
	}
	
	/**
	 * Siirtyy edelliselle sivulle.
	 */
	public boolean gotoPreviousPage() {
		if (model.getPageIndex() > 0) {
			model.setPageIndex(model.getPageIndex() - 1);
			updatePage();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Siirtyy seuraavalle sivulle.
	 */
	public boolean gotoNextPage() {
		int numPages = model.getPageCount();
		
		if (model.getPageIndex() < numPages - 1) {
			model.setPageIndex(model.getPageIndex() + 1);
			updatePage();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Päivittää ikkunan otsikkorivin ja esikatselupaneelin.
	 */
	public void updatePrint() {
		Print print = model.getPrint();
		String title;
		
		if (print == null) {
			previewPanel.setPrintable(null);
			title = "Tulosteen esikatselu";
		}
		else {
			PageFormat pageFormat = model.createPageFormat();
			AWTCanvas canvas = new AWTCanvas(pageFormat);
			print.setCanvas(canvas);
			
			previewPanel.setPageFormat(pageFormat);
			previewPanel.setPrintable(new AWTPrintable(print, canvas));
			previewPanel.repaint();
			title = String.format("%s - Tulosteen esikatselu",
					print.getTitle());
		}
		
		setTitle(title);
		updatePage();
	}
	
	/**
	 * Päivittää sivunumeron ja sivujen lukumäärän.
	 */
	private void updatePage() {
		pageLabel.setText(String.format("Sivu %d / %d",
				model.getPageIndex() + 1, model.getPageCount()));
		
		previewPanel.setPageIndex(model.getPageIndex());
	}
	
	/* Kuuntelija Sulje-painikkeelle */
	private AbstractAction closeListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			close();
		}
	};
	
	/* Kuuntelija Tulosta-painikkeelle */
	private ActionListener printListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			print();
		}
	};
	
	/* Kuuntelija Tallenna-painikkeelle */
	private ActionListener saveListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			save();
		}
	};
	
	/* Kuuntelija Asetukset-painikkeelle */
	private ActionListener settingsListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showSettings();
		}
	};
	
	/* Kuuntelija Edellinen sivu -painikkeelle */
	private AbstractAction prevPageListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent e) {
			gotoPreviousPage();
		}
	};
	
	/* Kuuntelija Seuraava sivu -painikkeelle */
	private AbstractAction nextPageListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent e) {
			gotoNextPage();
		}
	};
}
