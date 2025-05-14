package funoform.mdp.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import funoform.mdp.ConfigManager;
import funoform.mdp.Controller;
import funoform.mdp.Controller.SettingsListener;
import funoform.mdp.DisplayUtils;
import funoform.mdp.dbus.RaiseWindowRequestListener;
import funoform.mdp.gui.DirectoryPicker.PathSelectionListener;
import funoform.mdp.gui.OptionsDialog.IOptionsDoneListener;
import funoform.mdp.types.SettingsChanged;

/**
 * Provides a graphical user interface for controlling the music.
 */
public class Gui extends JPanel implements RaiseWindowRequestListener {
	private static final long serialVersionUID = 1L;
	private static final Logger sLogger = Logger.getLogger(Gui.class.getName());
	private static final int BUTTON_SIZE = 32;
	private static final String CARD_MUSIC = "card-music";
	private static final String CARD_DIR = "card-dir";
	private static final String CARD_SETTINGS = "card-settings";

	private transient Controller mCtrl;
//	private GestureDetector mGestures = null;
	private transient DirectoryPicker mDirSelector;
	private transient OptionsDialog mOptsDialog;
	private transient Path mCurBrowsingDir = null;
	private transient Path mCurSongPlaying = null;

	private JButton mBtnDir = new JButton();
	private JProgressBar mPbSongDuration = new JProgressBar(0, 0);
	private JButton mBtnPlayPause = new JButton();
	private JButton mBtnNext = new JButton();
	private JButton mBtnBack = new JButton();
	private JButton mBtnStop = new JButton();
	private JButton mBtnSettings = new JButton("S");
	private JToggleButton mTbRandom = new JToggleButton();
	private JToggleButton mTbRepeat = new JToggleButton();
	private DefaultListModel<Path> mListSongModel = new DefaultListModel<>();
	private JList<Path> mListSongs = new JList<>(mListSongModel);
	private AtomicBoolean mDisableSongListEvents = new AtomicBoolean(false);

	private java.awt.Window mTopLevelWindow;

	public Gui(Controller ctrl, ConfigManager cfg) {
		mCtrl = ctrl;
		mCurBrowsingDir = mCtrl.getCurrentDir();
		mOptsDialog = new OptionsDialog(cfg, new IOptionsDoneListener() {
			@Override
			public void doneWithOptions() {
				CardLayout cl = (CardLayout) Gui.this.getLayout();
				cl.show(Gui.this, CARD_MUSIC);
			}
		});

		mDirSelector = new DirectoryPicker(mCtrl, new PathSelectionListener() {
			@Override
			public void setPathSelected(Path selPath, boolean isRecursive) {
				mCurBrowsingDir = selPath;
				ctrl.playDir(selPath, isRecursive);
				populateDirSongList();

				CardLayout cl = (CardLayout) Gui.this.getLayout();
				cl.show(Gui.this, CARD_MUSIC);
			}
		});

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

		JPanel cardMusicPlayer = new JPanel();

		JPanel pnlTop = new JPanel(new BorderLayout());
		// width doesn't matter as it will fill width. Height matters
		mPbSongDuration.setPreferredSize(new Dimension(10, 20));
		pnlTop.add(mPbSongDuration, BorderLayout.NORTH);
		pnlTop.add(mBtnDir, BorderLayout.CENTER);

		JPanel pnlCenter = new JPanel(new GridLayout(0, 1));
		JScrollPane songs = new JScrollPane(mListSongs);
		if (GuiUtils.isLibrem()) {
			// make the scroll bar width 30 instead of 20 when using a touch interface
			songs.getVerticalScrollBar().setPreferredSize(new Dimension(30, 1));
		}
		mListSongs.setCellRenderer(new PrettyPathRenderer());
		pnlCenter.add(songs);

		JPanel pnlBottom = new JPanel(new GridLayout(1, 0));
		pnlBottom.add(mBtnPlayPause);
		pnlBottom.add(mBtnNext);
		pnlBottom.add(mBtnBack);
		pnlBottom.add(mBtnStop);
		pnlBottom.add(mTbRandom);
		pnlBottom.add(mTbRepeat);
		pnlBottom.add(mBtnSettings);

		cardMusicPlayer.setLayout(new BorderLayout());
		cardMusicPlayer.add(pnlTop, BorderLayout.NORTH);
		cardMusicPlayer.add(pnlCenter, BorderLayout.CENTER);
		cardMusicPlayer.add(pnlBottom, BorderLayout.SOUTH);

		this.setLayout(new CardLayout());
		this.add(cardMusicPlayer, CARD_MUSIC);
		this.add(mDirSelector.getComponent(), CARD_DIR);
		this.add(mOptsDialog.getComponent(), CARD_SETTINGS);
		this.setPreferredSize(new Dimension(300, 400));

		mBtnPlayPause.setToolTipText("Play/Pause");
		mBtnNext.setToolTipText("Next Track");
		mBtnBack.setToolTipText("Previous Track");
		mBtnStop.setToolTipText("Stop");
		mTbRandom.setToolTipText("Random");
		mTbRepeat.setToolTipText("Repeat All");
		mPbSongDuration.setToolTipText("Song Playback Progress");
		mBtnDir.setToolTipText("Current Playing Directory");
		setSongDirBtnText(mCtrl.getCurrentDir(), mCtrl.getQueuedSongs().size());

		// only allow the user to select at most one directory, same with songs
		mListSongs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		try {
			mBtnPlayPause.setIcon(GuiUtils.getIcon("icons8-play-64.png", BUTTON_SIZE));
			mBtnBack.setIcon(GuiUtils.getIcon("icons8-rewind-64.png", BUTTON_SIZE));
			mBtnNext.setIcon(GuiUtils.getIcon("icons8-fast-forward-64.png", BUTTON_SIZE));
			mBtnStop.setIcon(GuiUtils.getIcon("icons8-stop-64.png", BUTTON_SIZE));
			mTbRandom.setSelectedIcon(GuiUtils.getIcon("icons8-shuffle-64.png", BUTTON_SIZE));
			mTbRandom.setIcon(GuiUtils.getIcon("icons8-shuffle-64-colorless.png", BUTTON_SIZE));
			mTbRepeat.setSelectedIcon(GuiUtils.getIcon("icons8-repeat-64.png", BUTTON_SIZE));
			mTbRepeat.setIcon(GuiUtils.getIcon("icons8-repeat-64-colorless.png", BUTTON_SIZE));
		} catch (IOException e) {
			sLogger.log(Level.WARNING, "Unable to load icons for GUI");
		}
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
				mCtrl.priorTrack();
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

		mBtnSettings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CardLayout cl = (CardLayout) Gui.this.getLayout();
				cl.show(Gui.this, CARD_SETTINGS);
			}
		});

		mBtnDir.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CardLayout cl = (CardLayout) Gui.this.getLayout();
				cl.show(Gui.this, CARD_DIR);
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

						// if the song has changed since last time, consider scrolling the song list to
						// the new song
						if (mCurSongPlaying != settings.songPlaying) {
							mCurSongPlaying = settings.songPlaying;
							// show the current playing song, and scroll to that entry in the list, but
							// don't fire the action listener
							mDisableSongListEvents.set(true);
							if (null != settings.songPlaying) {
								if (mListSongs.getSelectedValue() == settings.songPlaying) {
									// the correct song is already selected
								} else {
									// The song that just started playing wasn't selected. Select it
									mListSongs.setSelectedValue(settings.songPlaying, true);
								}
							}
							mDisableSongListEvents.set(false);
						}

						setSongDirBtnText(settings.playingDir, settings.queuedSongs);
						mPbSongDuration.setMinimum(0);
						mPbSongDuration.setMaximum((int) settings.pbPercentage.getMaxTimeSecs());
						mPbSongDuration.setValue((int) settings.pbPercentage.getCurTimeSecs());
						mTbRandom.setSelected(settings.isRandom);
						mTbRepeat.setSelected(settings.isRepeat);

						boolean isSongPlaying = (0 < settings.pbPercentage.getMaxTimeSecs());
						mBtnStop.setEnabled(isSongPlaying);
						if (isSongPlaying) {
							String progress = GuiUtils.secsToTimeStr(settings.pbPercentage.getCurTimeSecs()) + " / "
									+ GuiUtils.secsToTimeStr(settings.pbPercentage.getMaxTimeSecs());
							mPbSongDuration.setString(progress);
							mPbSongDuration.setStringPainted(true);
						} else {
							mPbSongDuration.setString("");
							mPbSongDuration.setStringPainted(false);
						}
					}
				});
			}
		});
	}

	private void setSongDirBtnText(Path dir, int count) {
		String dirLabel = dir.getFileName().toString() + " (" + count + ")";
		mBtnDir.setText(dirLabel);
	}

	private void populateDirSongList() {
		mDirSelector.setStartingDir(mCurBrowsingDir);

		List<Path> songs = mCtrl.getQueuedSongs();
		mListSongModel.clear();
		mListSongModel.addAll(songs);
	}

	/**
	 * Hack that creates the correct type of top level window for the current
	 * platform and makes that window visible, then registers a listener for mouse
	 * (touch screen) gestures.
	 */
	private void createWindowIfNeededAndSetVisible() {
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
		if (GuiUtils.isLibrem()) {

			// Just run with the title-bar-less JWindow. It works correctly on the Librem
			// where the title bar and window buttons wouldn't be shown anyways
			JWindow win = new JWindow();
			win.getContentPane().add(this);
			win.pack();
			win.setVisible(true);
			mTopLevelWindow = win;
		} else {
			// We are not on the Librem. Give the user the traditional JFrame experience
			// which includes the title bar and min/max/close buttons.
			JFrame frm = new JFrame();
			frm.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frm.setTitle("Fun-O-Form Music Dir Player");
			try {
				frm.setIconImage(GuiUtils.getImage("icons8-play-64.png"));
			} catch (IOException e) {
				// Oh well
			}
			frm.getContentPane().add(this);
			frm.pack();
			frm.setVisible(true);
			mTopLevelWindow = frm;
		}

		// if someone closes the GUI, attempt to close the entire app
		mTopLevelWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				mCtrl.exitApp(0);
			}
		});

		// Detect mouse gestures over the entire window and respond accordingly
//		mGestures = new GestureDetector(topLevelWindow, new IGestureListener() {
//			@Override
//			public void gestureDetected(Gesture g) {
//
//				switch (g) {
//				case SWIPE_UP:
//					// make list go down, show lower entries
//					scrollListDown();
//					break;
//				case SWIPE_DOWN:
//					// make list go up, show higher entries
//					scrollListUp();
//					break;
//				case SWIPE_LEFT:
//					mCtrl.priorTrack();
//					break;
//				case SWIPE_RIGHT:
//					mCtrl.nextTrack();
//					break;
//				case TRIPLE_TAP:
//					mCtrl.playPause();
//					break;
//				}
//			}
//		});
	}

	public void shutdown() {
		mTopLevelWindow.setVisible(false);
		mTopLevelWindow.dispose();
	}

	/**
	 * Is smart enough to scroll the currently displayed list down, where we could
	 * be displaying either the song list or the directory picker list.
	 */
	private void scrollListDown() {
		if (mDirSelector.getComponent().isVisible()) {
			mDirSelector.scrollDownToNextPage();
		} else if (mListSongs.isVisible()) {
			int numPerPage = mListSongs.getLastVisibleIndex() - mListSongs.getFirstVisibleIndex();
			int lastOneNextPage = mListSongs.getLastVisibleIndex() + numPerPage;
			if (lastOneNextPage > mListSongModel.getSize()) {
				lastOneNextPage = mListSongModel.getSize();
			}
			mListSongs.ensureIndexIsVisible(lastOneNextPage);
		}
	}

	/**
	 * Is smart enough to scroll the currently displayed list up, where we could be
	 * displaying either the song list or the directory picker list.
	 */
	private void scrollListUp() {
		if (mDirSelector.getComponent().isVisible()) {
			mDirSelector.scrollUpToPriorPage();
		} else if (mListSongs.isVisible()) {
			int numPerPage = mListSongs.getLastVisibleIndex() - mListSongs.getFirstVisibleIndex();
			int firstOnePriorPage = mListSongs.getFirstVisibleIndex() - numPerPage;
			if (firstOnePriorPage < 0) {
				firstOnePriorPage = 0;
			}
			mListSongs.ensureIndexIsVisible(firstOnePriorPage);
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
			// strip the extension before display
			setText(DisplayUtils.getFileNameLengthLimited(p, -1));

			if (isSelected) {
				// Make the selected row slightly easier to read at a glance
				Font bigger = this.getFont().deriveFont(Font.BOLD, (float) (this.getFont().getSize2D() * 1.2));
				this.setFont(bigger);
			}
			return this;
		}
	}

	@Override
	public void raiseWindowRequested() {
		// attempt to make the window visible as requested by DBUS.
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				mTopLevelWindow.setVisible(true);
				mTopLevelWindow.toFront();
				mTopLevelWindow.repaint();
			}
		});
	}
}
