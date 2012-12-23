package kirjanpito.ui;

import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker.StateValue;
import javax.swing.table.TableColumn;

import kirjanpito.db.Account;
import kirjanpito.models.VATChangeModel;
import kirjanpito.models.VATChangeTableModel;
import kirjanpito.models.VATChangeWorker;
import kirjanpito.ui.resources.Resources;
import kirjanpito.util.Registry;

public class VATChangeDialog extends JDialog implements AccountSelectionListener {
	private Registry registry;
	private VATChangeModel model;
	private VATChangeTableModel ruleTableModel;
	private JTable ruleTable;
	private AccountCellEditor accountCellEditor;
	private JButton addRuleButton;
	private JButton removeRuleButton;
	private AccountSelectionDialog accountSelectionDialog;

	private static final long serialVersionUID = 1L;

	public VATChangeDialog(Frame owner, Registry registry) {
		super(owner, "ALV-kantojen muutokset", true);
		this.registry = registry;
		this.model = new VATChangeModel(registry);
	}

	public void create() {
		JScrollPane scrollPane = new JScrollPane();
		ruleTableModel = new VATChangeTableModel(model);
		ruleTable = new JTable(ruleTableModel);
		ruleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ruleTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		accountCellEditor = new AccountCellEditor(registry, ruleTableModel, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showAccountSelectionDialog();
			}
		});

		TableColumn column = ruleTable.getColumnModel().getColumn(0);
		column.setCellEditor(accountCellEditor);
		column.setCellRenderer(new AccountCellRenderer(registry, ruleTableModel));
		column.setPreferredWidth(80);

		DecimalFormat formatter = new DecimalFormat();
		formatter.setMinimumFractionDigits(0);
		formatter.setMaximumFractionDigits(2);
		CurrencyCellEditor percentCellEditor = new CurrencyCellEditor();
		CurrencyCellRenderer percentCellRenderer = new CurrencyCellRenderer(formatter);

		column = ruleTable.getColumnModel().getColumn(1);
		column.setCellEditor(percentCellEditor);
		column.setCellRenderer(percentCellRenderer);
		column.setPreferredWidth(30);

		column = ruleTable.getColumnModel().getColumn(2);
		column.setCellEditor(percentCellEditor);
		column.setCellRenderer(percentCellRenderer);
		column.setPreferredWidth(30);

		column = ruleTable.getColumnModel().getColumn(3);
		column.setPreferredWidth(550);

		addRuleButton = new JButton(new ImageIcon(Resources.load("list-add-16x16.png")));
		addRuleButton.setToolTipText("Lisää muutos");
		addRuleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.addRule();
				ruleTableModel.fireTableDataChanged();
			}
		});

		removeRuleButton = new JButton(new ImageIcon(Resources.load("list-remove-16x16.png")));
		removeRuleButton.setToolTipText("Poista muutos");
		removeRuleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int index = ruleTable.getSelectedRow();

				if (index >= 0) {
					model.removeRule(index);
					ruleTableModel.fireTableRowsDeleted(index, index);
				}
			}
		});

		JSeparator separator = new JSeparator();

		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				ruleTable.setRowHeight(getFontMetrics(ruleTable.getFont()).getHeight() + 6);
			}

			public void windowClosing(WindowEvent e) {
				close();
			}
		});

		scrollPane.setViewportView(ruleTable);

		JButton doChangesButton = new JButton("Tee muutokset");
		doChangesButton.setMnemonic('m');
		doChangesButton.setMargin(new Insets(3, 10, 3, 10));
		doChangesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doChanges();
			}
		});

		JButton closeButton = new JButton("Sulje");
		closeButton.setMnemonic('S');
		closeButton.setMargin(new Insets(3, 10, 3, 10));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});

		GroupLayout layout = new GroupLayout(getContentPane());
		setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(separator, javax.swing.GroupLayout.DEFAULT_SIZE, 850, Short.MAX_VALUE)
								.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
										.addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(addRuleButton)
												.addComponent(removeRuleButton)))
												.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
														.addComponent(closeButton)
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addComponent(doChangesButton)))
														.addContainerGap())
		);
		layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addComponent(addRuleButton)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(removeRuleButton))
										.addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(separator, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(doChangesButton)
												.addComponent(closeButton))
												.addContainerGap())
		);

		pack();
		setLocationRelativeTo(getOwner());
		model.addDefaultRules();
	}

	public void close() {
		if (accountSelectionDialog != null) {
			accountSelectionDialog.dispose();
		}

		dispose();
	}

	public void doChanges() {
		stopEditing();
		model.sortRules();
		ruleTableModel.fireTableDataChanged();
		final VATChangeDialog parent = this;
		final VATChangeWorker worker = new VATChangeWorker(registry, model);
		TaskProgressDialog dialog = new TaskProgressDialog(this, getTitle(), worker);
		dialog.create();
		dialog.setVisible(true);
		worker.execute();
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (!"state".equals(evt.getPropertyName()) ||
						evt.getNewValue() != StateValue.DONE ||
						worker.isCancelled()) {
					return;
				}

				int changes;

				try {
					changes = worker.get();
				}
				catch (ExecutionException e) {
					SwingUtils.showInformationMessage(parent, "Virhe: " + e.getCause().getMessage());
					return;
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}

				if (changes == 0) {
					SwingUtils.showInformationMessage(parent, "Tilikarttaan ei tehty muutoksia.");
				}
				else {
					SwingUtils.showInformationMessage(parent,
							String.format("Tilikarttaan lisättiin %d uutta tiliä.", changes));
				}

				close();
			}
		});
	}

	public void save() {
		stopEditing();

		try {
			model.save();
		}
		catch (IOException e) {
			SwingUtils.showErrorMessage(this, "Tiedostoon kirjoittaminen epäonnistui");
		}
	}

	public void showAccountSelectionDialog() {
		if (accountSelectionDialog == null) {
			accountSelectionDialog = new AccountSelectionDialog(
					this, registry);

			accountSelectionDialog.setListener(this);
			accountSelectionDialog.create();
		}

		if (ruleTable.isEditing())
			ruleTable.getCellEditor().cancelCellEditing();

		accountSelectionDialog.setSearchPhrase(
				accountCellEditor.getTextField().getText());
		accountSelectionDialog.setVisible(true);
	}

	public void accountSelected() {
		Account account = accountSelectionDialog.getSelectedAccount();
		int index = ruleTable.getSelectedRow();
		model.setAccountId(index, account.getId());
		ruleTableModel.fireTableDataChanged();
		accountSelectionDialog.setVisible(false);
	}

	private void stopEditing() {
		if (ruleTable.isEditing())
			ruleTable.getCellEditor().stopCellEditing();
	}
}
