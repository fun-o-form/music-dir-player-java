package funoform.mdp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import funoform.mdp.MusicPlayer.IPlaybackStatusListener;
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
	private Path mBrowsingDir;
	private List<Path> mQueuedMusicFiles;
	private SettingsChanged mSettings = new SettingsChanged();
	private int mCurPlayingIndex = -1;
	private List<SettingsListener> mSettingsListeners = new ArrayList<>();

	public Controller(Path startingDir, boolean isRecursive) throws FileNotFoundException {
		// Make sure the specified directory exists
		mBrowsingDir = startingDir;
		if (!Files.exists(mBrowsingDir)) {
			throw new FileNotFoundException(
					"The specified starting directory does not exist or is not accessible: " + startingDir);
		}

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

	public List<String> getAvailableDirs(String startingDir) {
		List<String> ret;
		try {
			List<Path> subDirs = FileUtils.getSubDirectories(startingDir);
			ret = subDirs.stream().map(Path::toString).toList();
		} catch (IOException e) {
			sLogger.log(Level.WARNING, "Exception while getting available subdirectories. " + e.getMessage());
			ret = new ArrayList<>();
		}
		return ret;
	}

	public void playBrowsingDir() throws IOException {
		mSettings.playingDir = mBrowsingDir.toString();
		// get a list of music files in the directory
		mQueuedMusicFiles = FileUtils.listMusicFile(mBrowsingDir.toString(), mSettings.isRecursive);
		// now play one
		nextTrack();
	}

	public void playSong(int index) {
		mCurPlayingIndex = index;
		Path song = mQueuedMusicFiles.get(index);
		mSettings.songPlaying = song.getFileName().toString();
		mPlayer.playMusicFile(song);
	}

	public void stop() {
		mCurPlayingIndex = -1;
		mSettings.songPlaying = "";
		mPlayer.stop();
	}

	public void nextTrack() {
		if (null == mQueuedMusicFiles || mQueuedMusicFiles.isEmpty()) {
			System.out.println("There are no queued music files to play");
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

	public void setRepat(boolean isRepeat) {
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
