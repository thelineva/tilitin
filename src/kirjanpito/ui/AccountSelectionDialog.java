package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import kirjanpito.db.Account;
import kirjanpito.models.COATableModel;
import kirjanpito.util.ChartOfAccounts;
import kirjanpito.util.Registry;
import kirjanpito.util.RegistryAdapter;

/**
 * Tilinvalintaikkuna.
 * 
 * @author Tommi Helineva
 */
public class AccountSelectionDialog extends JDialog {
	private AccountSelectionListener listener;
	private Registry registry;
	private JTable accountTable;
	private JTextField searchTextField;
	private JButton okButton;
	private COATableCellRenderer cellRenderer;
	private COATableModel tableModel;
	private boolean firstFocus;
	
	private static final long serialVersionUID = 1L;
	
	public AccountSelectionDialog(Window owner, Registry registry) {
		super(owner, "Tilin valinta", Dialog.ModalityType.APPLICATION_MODAL);
		this.registry = registry;
	}

	/**
	 * Palauttaa kuuntelijan.
	 * 
	 * @return kuuntelija
	 */
	public AccountSelectionListener getListener() {
		return listener;
	}

	/**
	 * Asettaa kuuntelijan.
	 * 
	 * @param listener kuuntelija
	 */
	public void setListener(AccountSelectionListener listener) {
		this.listener = listener;
	}

	/**
	 * Palauttaa valitun tilin.
	 * 
	 * @return tili
	 */
	public Account getSelectedAccount() {
		int index = accountTable.getSelectedRow();
		return registry.getChartOfAccounts().getAccount(index);
	}
	
	/**
	 * Asettaa valitun rivin.
	 * 
	 * @param index rivinumero
	 */
	public void setSelectedRow(int index) {
		accountTable.getSelectionModel().setSelectionInterval(
				index, index);
		
		/* Vieritetään taulukkoa niin, että valittu rivi on näkyvissä. */
		Rectangle rect = accountTable.getCellRect(index, 0, true);
		accountTable.scrollRectToVisible(rect);
	}
	
	/**
	 * Asettaa hakusanan.
	 * 
	 * @param q hakusana
	 */
	public void setSearchPhrase(String q) {
		firstFocus = true;
		searchTextField.setText(q);
		searchTextField.requestFocusInWindow();
	}
	
	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				accountTable.setRowHeight(getFontMetrics(accountTable.getFont()).getHeight() + 4);
			}
		});
		
		createSearchPanel();
		createTable();
		createButtonPanel();
		
		rootPane.setDefaultButton(okButton);
		rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
					KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
		rootPane.getActionMap().put("cancel", cancelButtonListener);

		pack();
		setLocationRelativeTo(null);
		
		registry.addListener(new RegistryAdapter() {
			public void chartOfAccountsChanged() {
				/* Päivitetään taulukko, kun tilikartan sisältö muuttuu. */
				tableModel.fireTableDataChanged();
			}
		});
	}

	private void createSearchPanel() {
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
		
		searchTextField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (firstFocus) {
					/* Kun Mac L&F on käytössä, tekstikentän sisältö maalataan automaattisesti
					 * kun kohdistus siirtyy tekstikenttään. Poistetaan maalaus, jotta sisältöä ei korvata,
					 * kun käyttäjä jatkaa hakusanan kirjoittamista.*/ 
					searchTextField.setCaretPosition(searchTextField.getText().length());
					firstFocus = false;
				}
			}
		});
		
		searchTextField.getDocument().addDocumentListener(
				searchTextFieldListener);
		panel.add(new JLabel("Haku"), BorderLayout.LINE_START);
		panel.add(searchTextField, BorderLayout.CENTER);
		add(panel, BorderLayout.NORTH);
	}
	
	/**
	 * Luo tilikarttataulukon.
	 * 
	 * @param container taulukon säiliö
	 */
	private void createTable() {
		TableColumn column;
		
		tableModel = new COATableModel();
		tableModel.setChartOfAccounts(registry.getChartOfAccounts());
		
		accountTable = new JTable(tableModel);
		accountTable.setFillsViewportHeight(true);
		accountTable.setPreferredScrollableViewportSize(
				new Dimension(500, 200));
		accountTable.getSelectionModel().addListSelectionListener(
				selectionListener);
		accountTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 2 && okButton.isEnabled()) {
					fireAccountSelected();
				}
			}
		});
		
		/* Muutetaan enter-näppäimen toiminta. */
		accountTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "accept");
		
		accountTable.getActionMap().put("accept", okButtonListener);
		
		cellRenderer = new COATableCellRenderer();
		cellRenderer.setChartOfAccounts(registry.getChartOfAccounts());
		
		column = accountTable.getColumnModel().getColumn(0);
		column.setPreferredWidth(80);
		
		column = accountTable.getColumnModel().getColumn(1);
		column.setPreferredWidth(420);
		column.setCellRenderer(cellRenderer);
		
		add(new JScrollPane(accountTable,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
	}
	
	private void createButtonPanel() {
		GridBagConstraints c;
		
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(panel, BorderLayout.SOUTH);
		
		okButton = new JButton("OK");
		okButton.setMnemonic('O');
		okButton.setPreferredSize(new Dimension(100, 30));
		okButton.addActionListener(okButtonListener);
		
		JButton cancelButton = new JButton("Peruuta");
		cancelButton.setMnemonic('P');
		cancelButton.setPreferredSize(new Dimension(100, 30));
		cancelButton.addActionListener(cancelButtonListener);
		
		c = new GridBagConstraints();
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(0, 0, 0, 5);
		panel.add(okButton, c);
		
		c = new GridBagConstraints();
		panel.add(cancelButton, c);
	}
	
	private void search() {
		int index = registry.getChartOfAccounts().search(
				searchTextField.getText());
		
		if (index >= 0) {
			setSelectedRow(index);
		}
	}
	
	protected void fireAccountSelected() {
		if (listener != null)
			listener.accountSelected();
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
	
	private ListSelectionListener selectionListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			int index = accountTable.getSelectedRow();
			
			/* OK-painiketta voi painaa, jos taulukosta on valittu tili. */
			okButton.setEnabled(index >= 0 &&
				registry.getChartOfAccounts().getType(index) == ChartOfAccounts.TYPE_ACCOUNT);
		}
	};
	
	private AbstractAction okButtonListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			fireAccountSelected();
		}
	};
	
	private AbstractAction cancelButtonListener = new AbstractAction() {
		private static final long serialVersionUID = 1L;
		
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
	};
}
