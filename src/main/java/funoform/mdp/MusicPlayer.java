package funoform.mdp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import funoform.mdp.types.PlaybackPercentage;
import funoform.mdp.types.PlaybackStatus;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.JavaSoundAudioDeviceFactory;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackListener;

public class MusicPlayer {

	private static final Logger sLogger = Logger.getLogger(MusicPlayer.class.getName());
	private IPlaybackStatusListener mPbL;
	private Thread mPlaybackMonitor;
	private Thread mPlaybackThread;
	private Path mNextSong = null;

	private Object mLockNowPlaying = new Object();
	private AdvancedPlayer mNowPlaying = null;
	private AudioDevice mAudioDevice = null;
	private int mPausedPosition = -1;

	private AtomicBoolean mIsCurrentlyPlaying = new AtomicBoolean(false);

	private int mCurSongLengthSecs = 0;
	
	public MusicPlayer() {

		// monitor the song playback by running this task every second
		mPlaybackMonitor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}

					if (null == mNowPlaying) {
						// don't bother sending status if we aren't playing anything
						continue;
					}

					// figure out the current playback status
					PlaybackStatus status = new PlaybackStatus();
					status.isPlaybackComplete = true;
					status.pbPercentage = new PlaybackPercentage(0, 0);

					synchronized (mLockNowPlaying) {
						if (null != mNowPlaying) {

							if (mIsCurrentlyPlaying.get()) {
								status.isPlaybackComplete = false;
								status.pbPercentage = new PlaybackPercentage(mAudioDevice.getPosition() / 1000,
										mCurSongLengthSecs);
							}
						}
						// notify the listener of the current playback status
						synchronized (mPlaybackMonitor) {
							if (null != mPbL) {
								mPbL.playbackStatusChanged(status);
							}
						}
					}

				}
			}
		}, "PlaybackMonitor");
		mPlaybackMonitor.start();

		// This thread is always running, always looking for the next song to play and
		// then playing it to its completion, then repeating waiting for the next song.
		mPlaybackThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					// We only proceed once we have been told the next song to play. Until then we
					// spin
					Path nextSong = mNextSong;
					mNextSong = null;
					if (null == nextSong) {
						mIsCurrentlyPlaying.set(false);
						try {
							Thread.sleep(10);
							continue;
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}

					// If we get here, we were told the next song to play so play it
					try (FileInputStream fis = new FileInputStream(nextSong.toString());) {
						Bitstream bitstream = new Bitstream(fis);
						Header h = bitstream.readFrame();
						mCurSongLengthSecs = (int) (h.totalMs((int) fis.getChannel().size())) / 1000;

						synchronized (mLockNowPlaying) {
							// Create the audio device we will play sound over. We need this also so we can
							// determine how long playback has been going for.
							JavaSoundAudioDeviceFactory factory = new JavaSoundAudioDeviceFactory();
							try {
								mAudioDevice = factory.createAudioDevice();
							} catch (JavaLayerException e) {
								sLogger.log(Level.SEVERE, "Failed to create the audio device. The app will now exit");
								e.printStackTrace();
								System.exit(100);
							}

							mNowPlaying = new AdvancedPlayer(fis, mAudioDevice);
							mIsCurrentlyPlaying.set(true);
							// Not used except to avoid a null pointer exception when issuing a
							// mNowPlaying.stop() command
							mNowPlaying.setPlayBackListener(new PlaybackListener() {
							});
						}
						// this blocks until the playback is complete or until the playback is stopped
						if (-1 != mPausedPosition) {
							mNowPlaying.play(mPausedPosition, Integer.MAX_VALUE);
						} else {
							mNowPlaying.play();
						}

					} catch (JavaLayerException | IOException e) {
						sLogger.log(Level.WARNING, "Failed to play music file due to exception: " + e.getMessage());
					}
				}
			}
		}, "ThreadRunningAdvancedPlayer");
		mPlaybackThread.start();
	}

	public synchronized boolean playMusicFile(Path path) {
		mPausedPosition = -1;
		mNextSong = path;
		stop();
		return true;
	}

	public void togglePauseResume() {
		synchronized (mLockNowPlaying) {
			if (mIsCurrentlyPlaying.get()) {
				// pause
				mPausedPosition = mAudioDevice.getPosition();
				mNowPlaying.stop();
			} else {
				// resume
				try {
					mNowPlaying.play(mPausedPosition, Integer.MAX_VALUE);
					mPausedPosition = -1;
				} catch (JavaLayerException e) {
					sLogger.log(Level.WARNING, "Unable to resume paused song");
				}
			}
		}
	}

	public void init(IPlaybackStatusListener l) {
		synchronized (mPlaybackMonitor) {
			mPbL = l;
		}
	}

	public void stop() {
		if (mIsCurrentlyPlaying.get()) {
			synchronized (mLockNowPlaying) {
				if (null != mNowPlaying) {
					mNowPlaying.close();
				}
			}
		}
	}

	public interface IPlaybackStatusListener {
		public void playbackStatusChanged(PlaybackStatus status);
	}
}
