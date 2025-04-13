package funoform.mdp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import funoform.mdp.Controller.SettingsListener;
import funoform.mdp.types.SettingsChanged;

/**
 * Provides a graphical user interface for controlling the music.
 */
public class Gui extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final Logger sLogger = Logger.getLogger(Gui.class.getName());
	private Controller mCtrl;
	private Path mCurBrowsingDir = null;
	private JButton mBtnDir = new JButton();
	private JPopupMenu mPopupDir = new JPopupMenu();
	private JProgressBar mPbSongDuration = new JProgressBar(0, 0);
	private JButton mBtnPlayPause = new JButton();
	private JButton mBtnNext = new JButton();
	private JButton mBtnBack = new JButton();
	private JButton mBtnStop = new JButton();
	private JToggleButton mTbRandom = new JToggleButton();
	private JToggleButton mTbRepeat = new JToggleButton();
	private JToggleButton mTbRecursive = new JToggleButton();
	private DefaultListModel<Path> mListSongModel = new DefaultListModel<>();
	private JList<Path> mListSongs = new JList<>(mListSongModel);
	private AtomicBoolean mDisableSongListEvents = new AtomicBoolean(false);

	private Icon mIconPlay = null;
	private Icon mIconPause = null;
	private static final int BUTTON_SIZE = 32;

	public Gui(Controller ctrl) {
		mCtrl = ctrl;
		mCurBrowsingDir = mCtrl.getCurrentDir();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				init();
				addActionListeners();
				createWindowIfNeededAndSetVisible();
			}
		});
	}

	private void init() {
		populateDirSongList();

		JPanel pnlTop = new JPanel(new BorderLayout());
		// width doesn't matter as it will fill width. Height matters
		mPbSongDuration.setPreferredSize(new Dimension(10, 20));
		pnlTop.add(mPbSongDuration, BorderLayout.NORTH);
		pnlTop.add(mBtnDir, BorderLayout.CENTER);

		JPanel pnlCenter = new JPanel(new GridLayout(0, 1));
		JScrollPane songs = new JScrollPane(mListSongs);
		mListSongs.setCellRenderer(new PrettyPathRenderer());
		pnlCenter.add(songs);

		JPanel pnlBottom = new JPanel(new GridLayout(1, 0));
		pnlBottom.add(mBtnPlayPause);
		pnlBottom.add(mBtnNext);
		pnlBottom.add(mBtnBack);
		pnlBottom.add(mBtnStop);
		pnlBottom.add(mTbRandom);
		pnlBottom.add(mTbRepeat);
		pnlBottom.add(mTbRecursive);

		this.setLayout(new BorderLayout());
		this.add(pnlTop, BorderLayout.NORTH);
		this.add(pnlCenter, BorderLayout.CENTER);
		this.add(pnlBottom, BorderLayout.SOUTH);
		this.setPreferredSize(new Dimension(300, 400));

		this.setBackground(Color.CYAN);
		this.setBackground(Color.magenta);

		mBtnPlayPause.setToolTipText("Play/Pause");
		mBtnNext.setToolTipText("Next Track");
		mBtnBack.setToolTipText("Previous Track");
		mBtnStop.setToolTipText("Stop");
		mTbRandom.setToolTipText("Random");
		mTbRecursive.setToolTipText("Recursive");
		mTbRepeat.setToolTipText("Repeat All");
		mPbSongDuration.setToolTipText("Song Playback Progress");
		mBtnDir.setToolTipText("Current Playing Directory");

		// only allow the user to select at most one directory, same with songs
		mListSongs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		try {
			mIconPlay = getIcon("icons8-play-64.png", BUTTON_SIZE);
			mIconPause = getIcon("icons8-pause-64.png", BUTTON_SIZE);
			mBtnPlayPause.setIcon(mIconPlay);
			mBtnBack.setIcon(getIcon("icons8-rewind-64.png", BUTTON_SIZE));
			mBtnNext.setIcon(getIcon("icons8-fast-forward-64.png", BUTTON_SIZE));
			mBtnStop.setIcon(getIcon("icons8-stop-64.png", BUTTON_SIZE));
			mTbRandom.setSelectedIcon(getIcon("icons8-shuffle-64.png", BUTTON_SIZE));
			mTbRandom.setIcon(getIcon("icons8-shuffle-64-colorless.png", BUTTON_SIZE));
			mTbRecursive.setSelectedIcon(getIcon("icons8-eject-64.png", BUTTON_SIZE));
			mTbRecursive.setIcon(getIcon("icons8-eject-64-colorless.png", BUTTON_SIZE));
			mTbRepeat.setSelectedIcon(getIcon("icons8-repeat-64.png", BUTTON_SIZE));
			mTbRepeat.setIcon(getIcon("icons8-repeat-64-colorless.png", BUTTON_SIZE));
		} catch (IOException e) {
			sLogger.log(Level.WARNING, "Unable to load icons for GUI");
		}

		// Don't let this get too small when there are no sub-directories, just ..
//		mPopupDir.setPreferredSize(new Dimension(200, this.getHeight()));
	}

	private void addActionListeners() {
		mBtnPlayPause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mCtrl.playPause();
			}
		});

		mBtnNext.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mCtrl.nextTrack();
			}
		});

		mBtnBack.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO: implement back in ctrl
			}
		});

		mBtnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mCtrl.stop();
			}
		});

		mTbRandom.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				mCtrl.setRandom(ItemEvent.SELECTED == e.getStateChange());
			}
		});

		mTbRepeat.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				mCtrl.setRepeat(ItemEvent.SELECTED == e.getStateChange());
			}
		});

		mTbRecursive.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				mCtrl.setRecursive(ItemEvent.SELECTED == e.getStateChange());
			}
		});

		mBtnDir.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mPopupDir.show(Gui.this, 0, 0);
			}
		});

		mListSongs.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// We set selection when the controller tells us that currently playing song
				// changes. We don't want to fire an event in that case otherwise we would keep
				// restarting the same song. Ignore all selections except those by the user
				if (!mDisableSongListEvents.get()) {
					// Only respond once the event is finalized
					if (e.getValueIsAdjusting()) {
						return;
					}
					Path selPath = mListSongs.getSelectedValue();
					if (null != selPath) {
						mCtrl.playSong(selPath);
					}
				}
			}
		});

		mCtrl.registerSettingsListener(new SettingsListener() {
			@Override
			public void settingsChanged(SettingsChanged settings) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						// show the current playing song, and scroll to that entry in the list, but
						// don't fire the action listener
						mDisableSongListEvents.set(true);
						if (null != settings.songPlaying) {
							mListSongs.setSelectedValue(settings.songPlaying, true);
						} else {
							mListSongs.clearSelection();
						}
						mDisableSongListEvents.set(false);

						mBtnDir.setText(settings.playingDir.toString());
						mPbSongDuration.setMinimum(0);
						mPbSongDuration.setMaximum((int) settings.pbPercentage.getMaxTimeSecs());
						mPbSongDuration.setValue((int) settings.pbPercentage.getCurTimeSecs());
						mTbRandom.setSelected(settings.isRandom);
						mTbRepeat.setSelected(settings.isRepeat);
						mTbRecursive.setSelected(settings.isRecursive);

						boolean isSongPlaying = (0 < settings.pbPercentage.getMaxTimeSecs());
						mBtnStop.setEnabled(isSongPlaying);
						mBtnNext.setEnabled(isSongPlaying);
						mBtnBack.setEnabled(isSongPlaying);
						if (isSongPlaying) {
							mBtnPlayPause.setIcon(mIconPause);
							String progress = secsToTimeStr(settings.pbPercentage.getCurTimeSecs()) + " / "
									+ secsToTimeStr(settings.pbPercentage.getMaxTimeSecs());
							mPbSongDuration.setString(progress);
							mPbSongDuration.setStringPainted(true);
						} else {
							mBtnPlayPause.setIcon(mIconPlay);
							mPbSongDuration.setString("");
							mPbSongDuration.setStringPainted(false);
						}
					}
				});
			}
		});
	}

	private void populateDirSongList() {
		List<Path> dirs = mCtrl.getAvailableDirs(mCurBrowsingDir);
		mPopupDir.removeAll();

		// always add the ".." directory to go up a directory
		JMenuItem menuItem = new JMenuItem("..");
		mPopupDir.add(menuItem);
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Path up = mCurBrowsingDir.getParent();
				mCurBrowsingDir = up;
				mCtrl.playDir(up);
				populateDirSongList();
			}
		});

		// now add all the sub-directories
		for (Path dir : dirs) {
			menuItem = new JMenuItem(dir.getFileName().toString());
			mPopupDir.add(menuItem);
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					mCurBrowsingDir = dir;
					mCtrl.playDir(dir);
					populateDirSongList();
				}
			});
		}

		List<Path> songs = mCtrl.getAvailableSongs(mCurBrowsingDir);
		mListSongModel.clear();
		mListSongModel.addAll(songs);
	}

	/**
	 * Hack that creates the correct type of top level window for the current
	 * platform and makes that window visible.
	 */
	private void createWindowIfNeededAndSetVisible() {
		// On a Librem Evergreen running Byzantium, this returns "6.6.0-1-librem5"
		String osVer = System.getProperties().getProperty("os.version");
		boolean isLibrem = osVer.toLowerCase().contains("librem");

		// PureOS runs applications with no menu bar. On Java, this results in some
		// wonky behavior. If you run a JFrame, it will get set to its preferred size,
		// then maximized, yet the content pane will remain the smaller preferred size.
		// The window goes full screen but the content doesn't expand to fill it. Super
		// annoying. If you run a JWindow instead of a JFrame, the JWindow properly
		// expands the content pane to fill the maximized window. But using just a
		// JWindow is normally a bad idea as JWindows don't have title bars or window
		// buttons (e.g. close, min, max).
		//
		// Our hack, and I do mean hack, is to detect if we are running on PureOS, and
		// if we are, then just run with a JWindow. If we aren't, then its safe to use a
		// JFrame.
		//
		// I don't want to make this decision based on app config or cmd line args. I
		// dumped all the java properties as shown below and found a value that is
		// unique when on the Librem5 at least, though its probably not set when running
		// PureOS on some other device.
		//
		// System.getProperties().list(System.out);
		//
		if (isLibrem) {
			// Just run with the title-bar-less JWindow. It works correctly on the Librem
			// where the title bar and window buttons wouldn't be shown anyways
			JWindow win = new JWindow();
			win.getContentPane().add(this);
			win.pack();
			win.setVisible(true);
		} else {
			// We are not on the Librem. Give the user the traditional JFrame experience
			// which includes the title bar and min/max/close buttons.
			JFrame frm = new JFrame();
			frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frm.setTitle("Fun-O-Form Music Dir Player");
			try {
				frm.setIconImage(getImage("icons8-play-64.png"));
			} catch (IOException e) {
				// Oh well
			}
			frm.getContentPane().add(this);
			frm.pack();
			frm.setVisible(true);
		}
	}

	/**
	 * Controls how Paths are displayed in a list, namely with a short name rather
	 * than showing the full absolute path.
	 */
	private class PrettyPathRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			Path p = (Path) value;
			setText(p.getFileName().toString());
			return this;
		}
	}

	/**
	 * Gets an image out of the jar's resources.
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private static Image getImage(String filename) throws IOException {
		return ImageIO.read(ClassLoader.getSystemResource("icons/" + filename));
	}

	/**
	 * Gets an icon out of the jar's resources.
	 * 
	 * @param filename
	 * @param size
	 * @return
	 * @throws IOException
	 */
	private static Icon getIcon(String filename, int size) throws IOException {
		Image image = getImage(filename).getScaledInstance(size, size, Image.SCALE_DEFAULT);
		Icon icon = new ImageIcon(image);
		return icon;
	}

	/**
	 * Converts a number of seconds (e.g. 245) into a human readable duration string
	 * (e.g. 04:05). Will omit the hours unless the duration is over 1 hour long.
	 * 
	 * @param secs
	 * @return
	 */
	private static String secsToTimeStr(long secs) {
		int hours = (int) (secs / 3600);
		int minutes = (int) ((secs % 3600) / 60);
		int seconds = (int) (secs % 60);
		// we always include minutes and seconds, but only include hours if the duration
		// is at least 1 hour long
		if (0 < hours) {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%02d:%02d", minutes, seconds);
		}
	}

	// about
	// <a target="_blank" href="https://icons8.com/icon/7MtGYX1kdQnR/play">Play</a>
	// icon by <a target="_blank" href="https://icons8.com">Icons8</a>
}
