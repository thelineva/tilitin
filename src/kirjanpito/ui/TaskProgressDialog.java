package kirjanpito.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

/**
 * Ikkuna, joka näyttää <code>SwingWorker</code>in edistymisen.
 * 
 * @author Tommi Helineva
 */
public class TaskProgressDialog extends JDialog {
	private SwingWorker<?, ?> worker;
	private JProgressBar progressBar;
	private JButton cancelButton;

	private static final long serialVersionUID = 1L;
	
	public TaskProgressDialog(Window owner, String title,
			SwingWorker<?, ?> worker)
	{
		super(owner, title);
		this.worker = worker;
	}

	/**
	 * Luo ikkunan komponentit.
	 * 
	 * @param title ikkunan otsikkoteksti
	 */
	public void create() {
		GridBagConstraints c;
		setLayout(new GridBagLayout());
		setSize(new Dimension(400, 80));
		setLocationRelativeTo(null);
		
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);

		c = new GridBagConstraints();
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(4, 6, 4, 6);
		add(progressBar, c);
		
		cancelButton = new JButton("Peruuta");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				worker.cancel(true);
			}
		});
		
		c.weightx = 0.0;
		add(cancelButton, c);
		
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("progress")) {
					/* Päivitetään edistysmistilanne. */
					progressBar.setValue(worker.getProgress());
				}
				else if (e.getPropertyName().equals("state")) {
					/* Suljetaan ikkuna, kun työ on valmis. */
					if (worker.getState() == StateValue.DONE) {
						dispose();
					}
				}
			}
		});
	}
}
