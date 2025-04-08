package funoform.mdp;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Gui extends JFrame {
	private static final long serialVersionUID = 1L;

	public Gui() {

		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			// Oh well, run with with whatever the default L&F is on this system. This is
			// probably a windows platform thus doesn't support GTK
			e.printStackTrace();
		}

		this.setLayout(new BorderLayout());
		this.add(new JButton("foo"), BorderLayout.NORTH);
		this.add(new JScrollPane(), BorderLayout.CENTER);
		this.add(new JTextArea("bar"), BorderLayout.SOUTH);
		this.setSize(new Dimension(200, 200));
		this.setVisible(true);
	}
}
