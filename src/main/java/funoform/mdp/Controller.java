package funoform.mdp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
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
	private MusicPlayer mPlayer = new MusicPlayer();
	private List<Path> mQueuedMusicFiles;
	private SettingsChanged mSettings = new SettingsChanged();
	private int mCurPlayingIndex = -1;
	private List<SettingsListener> mSettingsListeners = new ArrayList<>();

	public Controller(Path startingDir, boolean isRecursive) throws FileNotFoundException {
		// Make sure the specified directory exists
		if (!Files.exists(startingDir)) {
			throw new FileNotFoundException(
					"The specified starting directory does not exist or is not accessible: " + startingDir);
		}

		mSettings.playingDir = startingDir;
		mSettings.isRecursive = isRecursive;

		// Receive notifications about playback % complete and when the song ends
		mPlayer.init(new IPlaybackStatusListener() {
			@Override
			public void playbackStatusChanged(PlaybackStatus status) {
				if (status.isPlaybackComplete) {
					sLogger.log(Level.FINE, "Playback status changed: " + status);
					nextTrack();
				}

				mSettings.pbPercentage = status.pbPercentage;
				notifySettingsListeners();
			}
		});
	}

	public List<Path> getAvailableDirs(Path dir) {
		List<Path> ret;
		try {
			ret = FileUtils.getSubDirectories(dir);
			
			//alphabetize list ignoring case
			Collections.sort(ret, new Comparator<Path>() {
				@Override
				public int compare(Path o1, Path o2) {
					return o1.getFileName().toString().toLowerCase().compareTo(o2.getFileName().toString().toLowerCase());
				}
			});
		} catch (IOException e) {
			sLogger.log(Level.WARNING, "Exception while getting available subdirectories. " + e.getMessage());
			ret = new ArrayList<>();
		}
		return ret;
	}

	public List<Path> getAvailableSongs(Path dir) {
		try {
			return FileUtils.listMusicFiles(dir, mSettings.isRecursive);
		} catch (IOException e) {
			return new ArrayList<Path>();
		}
	}

	public Path getCurrentDir() {
		return mSettings.playingDir;
	}

	public void playPause() {
		mPlayer.togglePauseResume();
	}

	public void playDir(Path dir) {
		playDir(dir, null);
	}

	private void playDir(Path dir, Path song) {
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
			mQueuedMusicFiles = FileUtils.listMusicFiles(dir, mSettings.isRecursive);
		} catch (IOException e) {
			sLogger.log(Level.SEVERE, "Exception while trynig to open the directory to play. Directory = " + dir
					+ ". Exception = " + e.getMessage());
			return;
		}
		if (null == song) {
			// play whatever song comes up next
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
			mCurPlayingIndex = index;
			Path song = mQueuedMusicFiles.get(index);
			mSettings.songPlaying = song;
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
			playDir(song.getParent(), song);
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
		mPlayer.stop();
		mCurPlayingIndex = -1;
		mSettings.songPlaying = null;
		mSettings.pbPercentage = new PlaybackPercentage(0, 0);
		notifySettingsListeners();
	}

	public void nextTrack() {
		if (null == mQueuedMusicFiles || mQueuedMusicFiles.isEmpty()) {
			System.out.println("There are no queued music files to play");
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
			nextIndex = new Random().nextInt(mQueuedMusicFiles.size());
		}

		// sanity check the results then play that song
		if (0 > nextIndex) {
			stop();
		} else {
			playSong(nextIndex);
		}
	}

	public void setRandom(boolean isRandom) {
		mSettings.isRandom = isRandom;
		notifySettingsListeners();
	}

	public void setRepeat(boolean isRepeat) {
		mSettings.isRepeat = isRepeat;
		notifySettingsListeners();
	}

	public void setRecursive(boolean isRecursive) {
		mSettings.isRecursive = isRecursive;
		notifySettingsListeners();
	}

	public void registerSettingsListener(SettingsListener l) {
		synchronized (mSettingsListeners) {
			mSettingsListeners.add(l);
		}
	}

	private void notifySettingsListeners() {
		SettingsChanged settingsCopy = mSettings.clone();
		synchronized (mSettingsListeners) {
			for (SettingsListener sl : mSettingsListeners) {
				sl.settingsChanged(settingsCopy);
			}
		}
	}

	public interface SettingsListener {
		public void settingsChanged(SettingsChanged newSettings);
	}
}
