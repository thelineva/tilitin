package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import kirjanpito.db.Account;
import kirjanpito.models.COATableModel;
import kirjanpito.util.ChartOfAccounts;
import kirjanpito.util.Registry;

/**
 * Tiliotetulosteen asetusikkuna.
 * 
 * @author Tommi Helineva
 */
public class AccountStatementOptionsDialog extends PrintOptionsDialog {
	private Registry registry;
	private JTextField searchTextField;
	private JTable accountTable;
	private COATableCellRenderer cellRenderer;
	private COATableModel tableModel;
	private Account account;
	
	private static final long serialVersionUID = 1L;
	
	public AccountStatementOptionsDialog(Frame owner, Registry registry) {
		super(owner, "Tiliote");
		this.registry = registry;
	}
	
	public Account getSelectedAccount() {
		return account;
	}
	
	protected int addExtraOptions(JPanel panel) {
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				accountTable.setRowHeight(getFontMetrics(accountTable.getFont()).getHeight() + 4);
				searchTextField.requestFocusInWindow();
			}
		});
		
		okButton.setEnabled(false);
		JPanel container = new JPanel();
		container.setLayout(new BorderLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(8, 8, 8, 8);
		c.gridwidth = 3;
		c.gridy = 0;
		panel.add(container, c);
		createTable(container);
		createSearchPanel(container);
		return 1;
	}
	
	/**
	 * Luo tilikarttataulukon.
	 * 
	 * @param container säiliö, johon taulukko lisätään
	 */
	private void createTable(JPanel panel) {
		TableColumn column;
		
		tableModel = new COATableModel();
		tableModel.setChartOfAccounts(registry.getChartOfAccounts());
		
		accountTable = new JTable(tableModel);
		accountTable.setFillsViewportHeight(true);
		accountTable.setPreferredScrollableViewportSize(
				new Dimension(400, 200));
		
		accountTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					accept();
				}
			}
		});
		
		/* OK-painiketta voi klikata, jos tilikartasta on valittu tili. */
		accountTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				ChartOfAccounts coa = registry.getChartOfAccounts();
				int index = accountTable.getSelectedRow();
				
				if (coa.getType(index) == ChartOfAccounts.TYPE_ACCOUNT) {
					account = coa.getAccount(index);
					okButton.setEnabled(true);
				}
				else {
					okButton.setEnabled(false);
				}
			}
		});
		
		cellRenderer = new COATableCellRenderer();
		cellRenderer.setChartOfAccounts(registry.getChartOfAccounts());
		
		column = accountTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(80);
		
		column = accountTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(420);
		column.setCellRenderer(cellRenderer);
		
		panel.add(new JScrollPane(accountTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
				BorderLayout.CENTER);
	}
	
	/**
	 * Luo paneelin, joka sisältää hakusanakentän.
	 * 
	 * @param container säiliö, johon paneeli lisätään
	 */
	private void createSearchPanel(JPanel container) {
		JPanel panel = new JPanel();
		BorderLayout layout = new BorderLayout();
		layout.setHgap(8);
		panel.setLayout(layout);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 8, 4));
		searchTextField = new JTextField();
		searchTextField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				/* Valitaan taulukosta seuraava rivi, kun hakukentässä
				 * painetaan alanuolinäppäintä. */
				if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					int index = accountTable.getSelectedRow();
					
					if (index < accountTable.getRowCount() - 1)
						setSelectedRow(index + 1);
				}
				/* Valitaan taulukosta edellinen rivi, kun hakukentässä
				 * painetaan ylänuolinäppäintä. */
				else if (e.getKeyCode() == KeyEvent.VK_UP) {
					int index = accountTable.getSelectedRow();
					
					if (index > 0)
						setSelectedRow(index - 1);
				}
			}
		});
		
		searchTextField.getDocument().addDocumentListener(
				searchTextFieldListener);
		JLabel label = new JLabel("Haku");
		label.setDisplayedMnemonic('H');
		label.setLabelFor(searchTextField);
		panel.add(label, BorderLayout.LINE_START);
		panel.add(searchTextField, BorderLayout.CENTER);
		container.add(panel, BorderLayout.NORTH);
	}
	
	/**
	 * Etsii tilien nimistä ja otsikkoteksteistä merkkijonoa
	 * <code>q</code>. Ensimmäinen löytynyt rivi valitaan
	 * taulukosta.
	 * 
	 * @param q hakusana
	 */
	public void search() {
		int index = registry.getChartOfAccounts().search(
				searchTextField.getText());
		
		if (index >= 0)
			setSelectedRow(index);
	}
	
	private void setSelectedRow(int position) {
		accountTable.getSelectionModel().setSelectionInterval(
				position, position);
		
		/* Vieritetään taulukkoa niin, että valittu rivi on näkyvissä. */
		Rectangle rect = accountTable.getCellRect(position, 0, true);
		accountTable.scrollRectToVisible(rect);
	}
	
	private DocumentListener searchTextFieldListener = new DocumentListener() {
		public void changedUpdate(DocumentEvent e) { }

		public void insertUpdate(DocumentEvent e) {
			search();
		}

		public void removeUpdate(DocumentEvent e) {
			search();
		}
	};
}
