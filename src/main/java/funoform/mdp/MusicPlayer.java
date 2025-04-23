package funoform.mdp;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.goxr3plus.streamplayer.enums.Status;
import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.goxr3plus.streamplayer.stream.StreamPlayerException;

import funoform.mdp.types.PlaybackPercentage;
import funoform.mdp.types.PlaybackStatus;

public class MusicPlayer {

	private static final Logger sLogger = Logger.getLogger(MusicPlayer.class.getName());
	private IPlaybackStatusListener mPbL;
	private Thread mPlaybackMonitor;
	private Object mLockNowPlaying = new Object();
	private StreamPlayer mPlayer = new StreamPlayer(sLogger);
	private Status mLastStatus = Status.NOT_SPECIFIED;

	public MusicPlayer() {

		// monitor the song playback by running this task every second
		mPlaybackMonitor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}

					synchronized (mLockNowPlaying) {
						// Get the player's current status (playing, stopped, paused, etc.)
						Status s = mPlayer.getStatus();

						// figure out the current playback status
						PlaybackStatus pbs = new PlaybackStatus();

						if (Status.PLAYING == s || Status.PAUSED == s) {
							// if a song is playing, report on the playback
							mLastStatus = s;
							pbs.isPlaybackComplete = false;

							long curTime = 0;
							long totalTime = 0;
							try {
								// eve if the player said it was playing, there is no guarantee that on the next
								// line it is still playing. If it just stopped playing, we will get a NPE.
								// Ignore it
								curTime = mPlayer.getSourceDataLine().getMicrosecondPosition() / 1000000;
								totalTime = mPlayer.getDurationInSeconds();
							} catch (Exception e) {
								// just send a time of 0/0 and get a better update next time
							}
							pbs.pbPercentage = new PlaybackPercentage(curTime, totalTime);
						} else if (s != mLastStatus) {
							// if a song isn't play, notify the user the playback is complete. But only do
							// this once immediately after playback stops. Don't keep sending the same
							// playback complete status over and over
							mLastStatus = s;
							pbs.isPlaybackComplete = true;
							pbs.pbPercentage = new PlaybackPercentage(0, 0);

						} else {
							// Don't keep repeating ourselves when there is nothing new to report on
							continue;
						}

						// notify the listener of the current playback status
						synchronized (mPlaybackMonitor) {
							if (null != mPbL) {
								mPbL.playbackStatusChanged(pbs);
							}
						}
					}
				}
			}
		}, "PlaybackMonitor");
		mPlaybackMonitor.start();
	}

	public synchronized boolean playMusicFile(Path path) {
		synchronized (mLockNowPlaying) {
			stop();
			try {
				mPlayer.open(path.toFile());
				mPlayer.play();
			} catch (StreamPlayerException e) {
				sLogger.log(Level.SEVERE, "Exception while trying to start playing the song: " + e.getMessage());
			}
		}

		return true;
	}

	public void togglePauseResume() {
		synchronized (mLockNowPlaying) {
			if (mPlayer.isPlaying()) {
				// pause
				mPlayer.pause();
			} else if (mPlayer.isPaused()) {
				// resume
				mPlayer.resume();
			}
		}
	}

	public void init(IPlaybackStatusListener l) {
		synchronized (mPlaybackMonitor) {
			mPbL = l;
		}
	}

	public void stop() {
		synchronized (mLockNowPlaying) {
			mPlayer.stop();
		}
	}

	public interface IPlaybackStatusListener {
		public void playbackStatusChanged(PlaybackStatus status);
	}
}
