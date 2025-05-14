package funoform.mdp.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import funoform.mdp.ConfigManager;

/**
 * Let's the user change some configuration settings for the app.
 */
public class OptionsDialog {
	private ConfigManager mCm;
	private IOptionsDoneListener mL;

	private JPanel mPnl = new JPanel();

	public OptionsDialog(ConfigManager cm, IOptionsDoneListener l) {
		mCm = cm;
		mL = l;

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				init();
			}
		});
	}

	public JComponent getComponent() {
		return mPnl;
	}

	private void init() {
		JSpinner spinFontScale = new JSpinner(new SpinnerNumberModel(mCm.getFontScale(), 0.1, 3.0, 0.1));
		JSpinner spinBarWidth = new JSpinner(new SpinnerNumberModel(mCm.getScrollBarWidth(), 5, 200, 1));
		JCheckBox checkAutoStart = new JCheckBox("Automatically start playing music on startup");
		checkAutoStart.setSelected(mCm.getIsAutoStart());
		JButton btnBack = new JButton("Back");
		
		JPanel gbPnl = new JPanel(new GridBagLayout());

		GridBagConstraints left = new GridBagConstraints();
		left.fill = GridBagConstraints.BOTH;
		left.weightx = 0.8;
		left.gridx = 0;
		left.insets = new Insets(5, 5, 5, 5);

		GridBagConstraints right = new GridBagConstraints();
		right.fill = GridBagConstraints.BOTH;
		right.weightx = 0.2;
		right.gridx = 1;
		right.insets = new Insets(5, 5, 5, 5);

		left.gridy = 0;
		right.gridy = 0;
		gbPnl.add(OptionsDialog.textArea(
				"Font scale. 1.0 is normal. Only applied when running on the Librem 5. Takes effect upon restart."),
				left);
		gbPnl.add(spinFontScale, right);

		left.gridy = 1;
		right.gridy = 1;
		gbPnl.add(OptionsDialog.textArea(
				"Scroll bar width. 20 is normal. Only applied when running on the Librem 5. Takes effect upon restart."),
				left);
		gbPnl.add(spinBarWidth, right);

		GridBagConstraints bottom = new GridBagConstraints();
		bottom.fill = GridBagConstraints.NONE;
		bottom.weightx = 1.0;
		bottom.gridx = 0;
		bottom.gridwidth = 2;
		bottom.gridy = 2;
		bottom.anchor = GridBagConstraints.LINE_START;
		gbPnl.add(checkAutoStart, bottom);

		bottom.gridy = 3;
		bottom.anchor = GridBagConstraints.CENTER;
		gbPnl.add(btnBack, bottom);
		
		mPnl.setLayout(new BorderLayout());
		mPnl.add(gbPnl, BorderLayout.NORTH);

		spinFontScale.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mCm.saveFontScale((double) spinFontScale.getModel().getValue());
			}
		});

		spinBarWidth.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				mCm.saveScrollBarWidth((int) spinBarWidth.getModel().getValue());
			}
		});

		checkAutoStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				mCm.saveAutoStart(checkAutoStart.isSelected());
			}
		});

		btnBack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mL.doneWithOptions();
			}
		});
	}

	private static JComponent textArea(String text) {
		JLabel lbl = new JLabel("<html><p>" + text + "</p></html>");
		lbl.setVerticalAlignment(SwingConstants.TOP);
		lbl.setOpaque(true);
		return lbl;
	}

	public interface IOptionsDoneListener {
		public void doneWithOptions();
	}
}
