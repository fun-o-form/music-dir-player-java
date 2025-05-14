package funoform.mdp.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import funoform.mdp.Controller;

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
	private static final int BUTTON_SIZE = 32;
	private Icon mIconPlay;
	private Icon mIconRecursive;
	private Controller mCtrl;

	public DirectoryPicker(Controller ctrl, PathSelectionListener l) {
		mCtrl = ctrl;
		mListener = l;

		try {
			mIconPlay = GuiUtils.getIcon("icons8-play-64.png", BUTTON_SIZE);
			mIconRecursive = GuiUtils.getIcon("recursive-64.png", BUTTON_SIZE);
		} catch (IOException e) {
			// oh well, run without icons
		}

		// Box layout is better than GridLayout in this case for one simple reason. In
		// the GridLayout, a JSeparator is the full height of all other entries. In the
		// box layout it is just a thin line not nearly as tall as the other entries.
		mPanel.setLayout(new BoxLayout(mPanel, BoxLayout.Y_AXIS));

		// make the scroll bar scroll faster
		mScrollPane.getVerticalScrollBar().setUnitIncrement(14);

		if (GuiUtils.isLibrem()) {
			// make the scroll bar width 30 instead of 20 when using a touch interface
			// TODO: read value from config
			mScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(30, 1));
		}
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
				mPanel.repaint();

				// Allow the user to go up from here to parent directories
				processParents(p, 0);

				// now list all the sub-directories
				for (Path subDir : subDirs) {
					PathMenuItem pmi = new PathMenuItem(subDir,
							firstNChars(subDir.getFileName().toString(), MAX_DIR_CHARS), false);
					mPanel.add(pmi);
				}
			}
		});
	}

	public void scrollDownToNextPage() {
		Rectangle oldView = mScrollPane.getVisibleRect();
		int newY = oldView.y + oldView.height;
		Rectangle newView = new Rectangle(oldView.x, newY, oldView.width, oldView.height);
		mScrollPane.scrollRectToVisible(newView);
	}

	public void scrollUpToPriorPage() {
		Rectangle oldView = mScrollPane.getVisibleRect();
		int newY = oldView.y - oldView.height;
		if (newY < 0) {
			newY = 0;
		}
		Rectangle newView = new Rectangle(oldView.x, newY, oldView.width, oldView.height);
		mScrollPane.scrollRectToVisible(newView);
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
			PathMenuItem pmi = new PathMenuItem(p, label, true);
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

			PathMenuItem pmi = new PathMenuItem(p, sb.toString(), true);
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
		private static final String TOOLTIP_PLAY = "Play files in directory";
		private static final String TOOLTIP_PLAY_REC = "Play files in directory And Sub-directories";
		private static final int DESIRED_HEIGHT = 32;
		private static final int MAX_HEIGHT = 60;

		public PathMenuItem(Path p, String displayLabel, boolean isBold) {
			// We may have just a couple directories on tall portrait screen. Don't let
			// those few directories get too tall. It looks weird
			this.setMaximumSize(new Dimension(Integer.MAX_VALUE, MAX_HEIGHT));
			this.setPreferredSize(new Dimension(-1, DESIRED_HEIGHT));

			JButton dir = new JButton(displayLabel);
			dir.setHorizontalAlignment(SwingConstants.LEFT);
			if (isBold) {
				dir.setFont(dir.getFont().deriveFont(Font.BOLD));
			} else {
				dir.setFont(dir.getFont().deriveFont(Font.PLAIN));
			}
			JButton play = new JButton(mIconPlay);
			JButton playRec = new JButton(mIconRecursive);

			play.setToolTipText(TOOLTIP_PLAY);
			playRec.setToolTipText(TOOLTIP_PLAY_REC);

			// make the directory button not obviously a button
			dir.setBorderPainted(false);

			JPanel btnPanel = new JPanel(new GridLayout(1, 0));
			btnPanel.add(play);
			btnPanel.add(playRec);
			btnPanel.setPreferredSize(new Dimension(MAX_HEIGHT, -1));

			this.setLayout(new BorderLayout());
			this.add(btnPanel, BorderLayout.WEST);
			this.add(dir, BorderLayout.CENTER);

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
