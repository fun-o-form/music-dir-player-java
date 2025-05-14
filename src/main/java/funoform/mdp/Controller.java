package funoform.mdp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import funoform.mdp.MusicPlayer.IPlaybackStatusListener;
import funoform.mdp.types.PlaybackPercentage;
import funoform.mdp.types.PlaybackStatus;
import funoform.mdp.types.SettingsChanged;

/**
 * Controls the playback of music (through ownership of the
 * {@link MusicPlayer}), advancing tracks, reporting out playback status, and in
 * general being the single point something like the CLI or GUI needs to
 * effectively control the app.
 */
public class Controller {
	private static final Logger sLogger = Logger.getLogger(Main.class.getName());
	private ConfigManager mCfg;
	private MusicPlayer mPlayer = new MusicPlayer();
	private List<Path> mQueuedMusicFiles = new ArrayList<>();
	private SettingsChanged mSettings = new SettingsChanged();
	private int mCurPlayingIndex = -1;
	private List<SettingsListener> mSettingsListeners = new ArrayList<>();
	private AtomicBoolean mShouldBePlaying = new AtomicBoolean(false);
	private Random mRandom = new Random();

	public Controller(ConfigManager cfg) {
		mCfg = cfg;

		// Receive notifications about playback % complete and when the song ends
		mPlayer.init(new IPlaybackStatusListener() {
			@Override
			public void playbackStatusChanged(PlaybackStatus status) {
				if (status.isPlaybackComplete) {
					sLogger.log(Level.FINE, "Playback status changed: " + status);
					if (mShouldBePlaying.get()) {
						nextTrack();
					}
				}

				mSettings.pbPercentage = status.pbPercentage;
				notifySettingsListeners();
			}
		});

		// apply initial config
		mSettings.isRandom = mCfg.getIsRandom();
		mSettings.isRepeat = mCfg.getIsRepeat();
		if (mCfg.getIsAutoStart()) {
			playDir(Path.of(mCfg.getStartingDir()), mCfg.getIsRecursive());
		} else {
			playDir(Path.of(mCfg.getStartingDir()), mCfg.getIsRecursive());
			stop();
		}
	}

	public List<Path> getAvailableDirs(Path dir) {
		List<Path> ret;
		try {
			ret = FileUtils.getSubDirectories(dir);

			// alphabetize list ignoring case
			Collections.sort(ret, new Comparator<Path>() {
				@Override
				public int compare(Path o1, Path o2) {
					return o1.getFileName().toString().toLowerCase()
							.compareTo(o2.getFileName().toString().toLowerCase());
				}
			});
		} catch (IOException e) {
			sLogger.log(Level.WARNING, "Exception while getting available subdirectories. " + e.getMessage());
			ret = new ArrayList<>();
		}
		return ret;
	}

	public List<Path> getQueuedSongs() {
		return Collections.unmodifiableList(mQueuedMusicFiles);
	}

	public Path getCurrentDir() {
		return mSettings.playingDir;
	}

	public void playPause() {
		boolean isPlaying = mPlayer.togglePauseResume();
		mSettings.isPaused = !isPlaying;
		notifySettingsListeners();
	}

	public void playDir(Path dir, boolean recursive) {
		playDir(dir, recursive, null);
	}

	private void playDir(Path dir, boolean recursive, Path song) {
		// sanity checks
		if (!Files.isDirectory(dir)) {
			sLogger.log(Level.SEVERE, "Asked to play a directory that wasn't a directory: " + dir.toString());
			return;
		}
		if (!Files.exists(dir)) {
			sLogger.log(Level.SEVERE, "Asked to play a directory that doesn't exist: " + dir.toString());
			return;
		}

		mSettings.playingDir = dir;
		// get a list of music files in the directory
		try {
			mQueuedMusicFiles = FileUtils.listMusicFiles(dir, recursive);
			mSettings.queuedSongs = mQueuedMusicFiles.size();
			saveSettings();
		} catch (IOException e) {
			sLogger.log(Level.SEVERE, "Exception while trying to open the directory to play. Directory = " + dir
					+ ". Exception = " + e.getMessage());
			return;
		}
		if (null == song) {
			// Reset the now playing index. This allows us to play the first song in the
			// directory if random is disabled
			mCurPlayingIndex = -1;
			nextTrack();
		} else {
			// play the specific song
			int songIndex = getSongIndexInQueue(song);
			playSong(songIndex);
		}
	}

	public void playSong(int index) {
		if (0 <= index && mQueuedMusicFiles.size() > index) {
			stop();
			mShouldBePlaying.set(true);
			mCurPlayingIndex = index;
			Path song = mQueuedMusicFiles.get(index);
			mSettings.songPlaying = song;
			mSettings.isPaused = false;
			mPlayer.playMusicFile(song);
			// immediately notify listeners so they don't have to wait up to half a second
			// for an update triggered by the player
			notifySettingsListeners();
		} else {
			sLogger.log(Level.WARNING, "Asked to play a song outside the range of our song queue. Queue size = "
					+ mQueuedMusicFiles.size() + ". Index = " + index);
		}
	}

	public void playSong(Path song) {
		// sanity checks
		if (Files.isDirectory(song)) {
			sLogger.log(Level.SEVERE, "Asked to play a song that is actually a directory: " + song.toString());
			return;
		}
		if (!Files.exists(song)) {
			sLogger.log(Level.SEVERE, "Asked to play a song file that doesn't exist: " + song.toString());
			return;
		}

		int songIndex = getSongIndexInQueue(song);
		if (-1 != songIndex) {
			// The song is in our existing queue. Play it.
			playSong(songIndex);
		} else {
			// The song isn't in our queue. Play the directory containing the song, then
			// make sure that specific song plays immediately
			playDir(song.getParent(), false, song);
		}
	}

	private int getSongIndexInQueue(Path song) {
		for (int i = 0; i < mQueuedMusicFiles.size(); i++) {
			if (0 == song.compareTo(mQueuedMusicFiles.get(i))) {
				return i;
			}
		}
		return -1;
	}

	public void stop() {
		mShouldBePlaying.set(false);
		mPlayer.stop();
		mCurPlayingIndex = -1;
		mSettings.songPlaying = null;
		mSettings.pbPercentage = new PlaybackPercentage(0, 0);
		notifySettingsListeners();
	}

	public void nextTrack() {
		if (null == mQueuedMusicFiles || mQueuedMusicFiles.isEmpty()) {
			sLogger.log(Level.WARNING, "There are no queued music files to play");
			stop();
			return;
		}

		int nextIndex = -1;
		if (!mSettings.isRandom) {
			nextIndex = mCurPlayingIndex + 1;
			if (nextIndex >= mQueuedMusicFiles.size()) {
				if (mSettings.isRepeat) {
					nextIndex = 0;
				} else {
					// we are out of songs to play
					nextIndex = -1;
				}
			}
		} else {
			nextIndex = mRandom.nextInt(mQueuedMusicFiles.size());
		}

		// sanity check the results then play that song
		if (0 > nextIndex) {
			stop();
		} else {
			playSong(nextIndex);
		}
	}

	public void priorTrack() {
		// if playing randomly, there is no going back. Just go forward
		if (mSettings.isRandom) {
			nextTrack();
			return;
		}

		if (null == mQueuedMusicFiles || mQueuedMusicFiles.isEmpty()) {
			sLogger.log(Level.WARNING, "There are no queued music files to play");
			stop();
			return;
		}

		int nextIndex = mCurPlayingIndex - 1;
		// you can't go further back than the first song
		if (nextIndex < 0) {
			nextIndex = 0;
		}

		stop();
		playSong(nextIndex);
	}

	public void setRandom(boolean isRandom) {
		mSettings.isRandom = isRandom;
		notifySettingsListeners();
		saveSettings();
	}

	public boolean getRandom() {
		return mSettings.isRandom;
	}

	public void setRepeat(boolean isRepeat) {
		mSettings.isRepeat = isRepeat;
		notifySettingsListeners();
		saveSettings();
	}

	public boolean getRepeat() {
		return mSettings.isRepeat;
	}

	public void exitApp(int returnCode) {
		saveSettings();
		System.exit(returnCode);
	}

	public void registerSettingsListener(SettingsListener l) {
		synchronized (mSettingsListeners) {
			mSettingsListeners.add(l);
		}
	}

	private void notifySettingsListeners() {
		SettingsChanged settingsCopy = mSettings.copy();
		synchronized (mSettingsListeners) {
			for (SettingsListener sl : mSettingsListeners) {
				try {
					sl.settingsChanged(settingsCopy);
				} catch (Exception e) {
					// Don't let one jerk ruin it for everyone. Log the error and move on to the
					// next listener
					sLogger.log(Level.SEVERE, "Exception while handling new settings: " + e.getMessage());
				}
			}
		}
	}

	private void saveSettings() {
		mCfg.savePreferences(mSettings);
	}

	/**
	 * How the {@link Controller} notifies listeners of playback status and
	 * settings.
	 */
	public interface SettingsListener {
		public void settingsChanged(SettingsChanged newSettings);
	}
}
