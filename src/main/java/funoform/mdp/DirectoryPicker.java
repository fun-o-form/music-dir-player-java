package funoform.mdp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Provides a nice GUI for selecting the directory of music to play or play
 * recursively.
 * 
 * This panel lists the current directory, a couple directories above, and all
 * the directories below. For each directory the user gets a button that lets
 * them play the files in that directory, or play the files in that directory
 * and all sub-directories (recursive).
 * 
 * Once the user makes their selection, the {@link PathSelectionListener} is
 * notified. It is expected that the {@link PathSelectionListener} handler will
 * do something to hide the DirectoryPicker user interface once this selection
 * is made. Similarly, it is up to the caller to show the DirectoryPicker when
 * appropriate.
 */
public class DirectoryPicker {
	private PathSelectionListener mListener;
	private JPanel mPanel = new JPanel();
	private JScrollPane mScrollPane = new JScrollPane(mPanel);
	private static final int MAX_DIR_CHARS = 40;
	private static final int MAX_PARENTS = 3;
	private static final int ICON_SIZE = 24;
	private Icon mIconPlay;
	private Icon mIconRecursive;
	private Controller mCtrl;

	public DirectoryPicker(Controller ctrl, PathSelectionListener l) {
		mCtrl = ctrl;
		mListener = l;

		try {
			mIconPlay = Gui.getIcon("icons8-play-64.png", ICON_SIZE);
			mIconRecursive = Gui.getIcon("icons8-eject-64.png", ICON_SIZE);
		} catch (IOException e) {
			// oh well, run without icons
		}

		mPanel.setLayout(new GridLayout(0, 1));
	}

	public JComponent getComponent() {
		return mScrollPane;
	}

	public void setStartingDir(Path p) {
		List<Path> subDirs = mCtrl.getAvailableDirs(p);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mPanel.removeAll();
				mPanel.invalidate();
				mPanel.revalidate();

				// Allow the user to go up from here to parent directories
				processParents(p, 0);

				mPanel.add(new JSeparator(JSeparator.HORIZONTAL));

				// now list all the sub-directories
				for (Path subDir : subDirs) {
					PathMenuItem pmi = new PathMenuItem(subDir,
							firstNChars(subDir.getFileName().toString(), MAX_DIR_CHARS));
					mPanel.add(pmi);
				}
			}
		});
	}

	/**
	 * 
	 * @param p
	 * @param depth
	 * 
	 * @return The depth of the oldest valid parent.
	 */
	private int processParents(Path p, int depth) {
		// We only go so far up looking for parents. We only want to show a few layers
		// of parents. After that, stop showing them.
		if (MAX_PARENTS <= depth) {
			return depth;
		}
		// If there is no filename then this path isn't valid
		if (null == p.getFileName()) {
			return depth;
		}

		int oldestParentDepth = processParents(p.getParent(), depth + 1);

		if (oldestParentDepth == depth) {
			// There is no one older than me. Start creating the menu items with me as the
			// oldest
			String label = firstNChars(p.getFileName().toString() + "/", MAX_DIR_CHARS);
			PathMenuItem pmi = new PathMenuItem(p, label);
			mPanel.add(pmi);
		} else {
			// I have a parent that is older than me. Indent based on my level relative to
			// that parent
			StringBuilder sb = new StringBuilder();
			for (int i = depth; i < oldestParentDepth - 1; i++) {
				sb.append("   ");
			}
			sb.append(firstNChars(p.getFileName().toString(), MAX_DIR_CHARS));
			sb.append("/");

			PathMenuItem pmi = new PathMenuItem(p, sb.toString());
			mPanel.add(pmi);
		}

		return oldestParentDepth;
	}

	private void notifyListener(Path p, boolean isRecursive) {
		mListener.setPathSelected(p, isRecursive);
	}

	public interface PathSelectionListener {
		public void setPathSelected(Path selPath, boolean isRecursive);
	}

	private class PathMenuItem extends JPanel {
		private static final long serialVersionUID = 1L;
		private static final Dimension BTN_DIM = new Dimension(ICON_SIZE + 5, ICON_SIZE + 5);

		public PathMenuItem(Path p, String displayLabel) {
			JButton dir = new JButton(displayLabel);
			dir.setHorizontalAlignment(SwingConstants.LEFT);
			JButton play = new JButton(mIconPlay);
			JButton playRec = new JButton(mIconRecursive);

			// make the directory button not obviously a button
			dir.setBorderPainted(false);

			play.setPreferredSize(BTN_DIM);
			playRec.setPreferredSize(BTN_DIM);

			JPanel btnPanel = new JPanel(new GridLayout(1, 0));
			btnPanel.add(play);
			btnPanel.add(playRec);

			this.setLayout(new BorderLayout());
			this.add(dir, BorderLayout.CENTER);
			this.add(btnPanel, BorderLayout.LINE_END);

			dir.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setStartingDir(p);
				}
			});

			play.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					notifyListener(p, false);
				}
			});

			playRec.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					notifyListener(p, true);
				}
			});
		}
	}

	private static String firstNChars(String str, int numChars) {
		if (str.length() > numChars) {
			return str.substring(0, numChars);
		}
		return str;
	}
}
