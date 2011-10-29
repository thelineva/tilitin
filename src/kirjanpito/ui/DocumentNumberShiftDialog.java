package kirjanpito.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import kirjanpito.db.DataAccessException;
import kirjanpito.db.DataSource;
import kirjanpito.db.Document;
import kirjanpito.db.Session;
import kirjanpito.util.Registry;

public class DocumentNumberShiftDialog extends JDialog {
	private Registry registry;
	private JSpinner startNumberSpinner;
	private JSpinner endNumberSpinner;
	private JSpinner shiftSpinner;
	private JTextPane textPane;
	private JButton okButton;
	private JButton cancelButton;
	private HashSet<Integer> numberSet;
	private List<Document> documents;
	private int result;
	
	private static Logger logger = Logger.getLogger(Kirjanpito.LOGGER_NAME);
	private static final long serialVersionUID = 1L;
	
	public DocumentNumberShiftDialog(Frame owner, Registry registry) {
		super(owner, "Muuta tositenumeroita", true);
		this.registry = registry;
		this.result = JOptionPane.CANCEL_OPTION;
	}
	
	public int getResult() {
		return result;
	}
	
	public void setStartNumber(int startNumber) {
		startNumberSpinner.setValue(startNumber);
		updateTextPane();
	}
	
	public void fetchDocuments(int start, int end) throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		int lastNumber = start;
		
		try {
			sess = dataSource.openSession();
			documents = dataSource.getDocumentDAO(sess).getByPeriodId(
					registry.getPeriod().getId(), 1);
			numberSet = new HashSet<Integer>();
			
			for (Document document : documents) {
				numberSet.add(document.getNumber());
				
				if (document.getNumber() < end) {
					lastNumber = Math.max(lastNumber, document.getNumber());
				}
			}
		}
		finally {
			if (sess != null) sess.close();
		}
		
		startNumberSpinner.setValue(start);
		endNumberSpinner.setValue(lastNumber);
		shiftSpinner.setValue(1);
	}
	
	/**
	 * Luo ikkunan komponentit.
	 */
	public void create() {
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(400, 280));
		setLocationRelativeTo(getOwner());
		createContentPanel();
		createButtonPanel();
		pack();
	}
	
	private void createContentPanel() {
		GridBagConstraints c = new GridBagConstraints();
		startNumberSpinner = new JSpinner();
		startNumberSpinner.addChangeListener(changeListener);
		endNumberSpinner = new JSpinner();
		endNumberSpinner.addChangeListener(changeListener);
		shiftSpinner = new JSpinner();
		shiftSpinner.addChangeListener(changeListener);
		textPane = new JTextPane();
		
		JPanel panel = new JPanel(new GridBagLayout());
		JLabel label = new JLabel("Alkaa");
		label.setDisplayedMnemonic('A');
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(4, 12, 4, 12);
		c.ipadx = 10;
		panel.add(label, c);
		
		label = new JLabel("Päättyy");
		label.setDisplayedMnemonic('P');
		c.gridy = 1;
		panel.add(label, c);
		
		label = new JLabel("Muutos");
		label.setDisplayedMnemonic('M');
		c.gridx = 2;
		c.gridy = 0;
		c.gridheight = 2;
		panel.add(label, c);
		
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 1;
		c.ipadx = 50;
		panel.add(startNumberSpinner, c);
		
		c.gridy = 1;
		panel.add(endNumberSpinner, c);
		
		c.gridx = 3;
		c.gridy = 0;
		c.gridheight = 2;
		panel.add(shiftSpinner, c);
		
		c.gridx = 0;
		c.gridy = 2;
		c.gridheight = 1;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		panel.add(new JScrollPane(textPane), c);
		
		add(panel, BorderLayout.CENTER);
	}
	
	private void createButtonPanel() {
		GridBagConstraints c = new GridBagConstraints();
		JPanel panel = new JPanel(new GridBagLayout());
		
		okButton = new JButton("OK");
		okButton.setPreferredSize(new Dimension(100, 30));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				accept();
			}
		});
		
		cancelButton = new JButton("Peruuta");
		cancelButton.setPreferredSize(new Dimension(100, 30));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		c.gridx = 1;
		c.anchor = GridBagConstraints.EAST;
		c.insets = new Insets(5, 10, 10, 5);
		c.weightx = 1.0;
		panel.add(okButton, c);
		
		c.gridx = 2;
		c.insets = new Insets(5, 5, 10, 10);
		c.weightx = 0.0;
		panel.add(cancelButton, c);
		add(panel, BorderLayout.SOUTH);
		rootPane.setDefaultButton(okButton);
	}
	
	private void accept() {
		try {
			shiftNumbers();
		}
		catch (DataAccessException e) {
			String message = "Tositenumeroiden muuttaminen epäonnistui";
			logger.log(Level.SEVERE, message, e);
			SwingUtils.showDataAccessErrorMessage(this, e, message);
			return;
		}
		
		result = JOptionPane.OK_OPTION;
		dispose();
	}
	
	private void shiftNumbers() throws DataAccessException {
		DataSource dataSource = registry.getDataSource();
		Session sess = null;
		int shift = (Integer)shiftSpinner.getValue();
		int start = (Integer)startNumberSpinner.getValue();
		int end = (Integer)endNumberSpinner.getValue();
		
		try {
			sess = dataSource.openSession();
			dataSource.getDocumentDAO(sess).shiftNumbers(
					registry.getPeriod().getId(), start, end, shift);
			sess.commit();
		}
		catch (DataAccessException e) {
			if (sess != null) sess.rollback();
			throw e;
		}
		finally {
			if (sess != null) sess.close();
		}
	}
	
	private void updateTextPane() {
		StyledDocument doc = textPane.getStyledDocument();
		textPane.setText("");
		addStylesToDocument();
		int shift = (Integer)shiftSpinner.getValue();
		
		if (shift == 0) {
			okButton.setEnabled(false);
			return;
		}
		
		int start = (Integer)startNumberSpinner.getValue();
		int end = (Integer)endNumberSpinner.getValue();
		int newStart = start + shift;
		int newEnd = end + shift;
		
		if (shift < 0) {
			newEnd = Math.min(newEnd, start - 1);
		}
		else {
			newStart = Math.max(newStart, end + 1);
		}
		
		boolean conflicts = false;
		
		for (Document document : documents) {
			if (document.getNumber() < start || document.getNumber() > end) {
				continue;
			}
			
			int newNumber = document.getNumber() + shift;
			String s = String.format("(%d → %d)", document.getNumber(), newNumber);
			String style = "regular";
			
			if (newNumber < 1 || (newNumber >= newStart && newNumber <= newEnd && numberSet.contains(newNumber))) {
				style = "error";
				conflicts = true;
			}
			
			try {
				if (doc.getLength() > 0) {
					doc.insertString(doc.getLength(), " ", doc.getStyle("regular"));
				}
				
				doc.insertString(doc.getLength(), s, doc.getStyle(style));
			}
			catch (BadLocationException e) {
			    e.printStackTrace();
			}
		}
		
		okButton.setEnabled(!conflicts);
	}
	
	private void addStylesToDocument() {
		StyledDocument doc = textPane.getStyledDocument();
		
		Style def = StyleContext.getDefaultStyleContext().
        getStyle(StyleContext.DEFAULT_STYLE);

		Style regular = doc.addStyle("regular", def);
		
		Style error = doc.addStyle("error", regular);
		StyleConstants.setBackground(error, Color.red);
		StyleConstants.setForeground(error, Color.white);
	}
	
	private ChangeListener changeListener = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			updateTextPane();
		}
	};
}