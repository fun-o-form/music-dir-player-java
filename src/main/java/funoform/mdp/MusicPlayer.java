package funoform.mdp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

import funoform.mdp.types.PlaybackPercentage;
import funoform.mdp.types.PlaybackStatus;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class MusicPlayer {

	private static final Logger sLogger = Logger.getLogger(MusicPlayer.class.getName());
	private IPlaybackStatusListener mPbL;
	private Timer mPlaybackMonitor = new Timer("PlaybackMonitor", true);
	private Player mNowPlaying = null;
	private Thread mThread = null;
	private Object mLockNowPlaying = new Object();
	private int mCurSongLengthSecs = 0;

	public MusicPlayer() {
		// monitor the song playback by running this task every second
		mPlaybackMonitor.schedule(new TimerTask() {
			@Override
			public void run() {
				if (null == mNowPlaying) {
					// don't bother sending status if we aren't playing anything
					return;
				}

				// figure out the current playback status
				PlaybackStatus status = new PlaybackStatus();
				status.isPlaybackComplete = true;
				status.pbPercentage = new PlaybackPercentage(0, 0);

				synchronized (mLockNowPlaying) {
					if (null != mNowPlaying) {
						if (!mNowPlaying.isComplete()) {
							status.isPlaybackComplete = false;
							status.pbPercentage = new PlaybackPercentage(mNowPlaying.getPosition() / 1000,
									mCurSongLengthSecs);
						}
					}
				}
				// notify the listener of the current playback status
				synchronized (mPlaybackMonitor) {
					if (null != mPbL) {
						mPbL.playbackStatusChanged(status);
					}
				}
			}
		}, 0, 1000);
	}

	public boolean playMusicFile(Path path) {
		synchronized (mLockNowPlaying) {
			// we may already be playing a song. Stop it first. This locks internally. But
			// since we already got the lock above we automatically pass the next lock test
			stop();

			mThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try (FileInputStream fis = new FileInputStream(path.toString());) {

						Bitstream bitstream = new Bitstream(fis);
						Header h = bitstream.readFrame();
						mCurSongLengthSecs = (int) (h.totalMs((int) fis.getChannel().size())) / 1000;

						synchronized (mLockNowPlaying) {
							mNowPlaying = new Player(fis);
						}
						mNowPlaying.play();
					} catch (JavaLayerException | IOException e) {
						sLogger.log(Level.WARNING, "Failed to play music file due to exception: " + e.getMessage());
						e.printStackTrace();
					}
				}
			});
			mThread.start();
		}
		return true;
	}

	public void init(IPlaybackStatusListener l) {
		synchronized (mPlaybackMonitor) {
			mPbL = l;
		}
	}

	public void stop() {
		synchronized (mLockNowPlaying) {
			if (null != mNowPlaying) {
				mNowPlaying.close();
				mNowPlaying = null;
			}
		}
	}

	public interface IPlaybackStatusListener {
		public void playbackStatusChanged(PlaybackStatus status);
	}

	public static List<AudioFormat> getSupportedAudioFormats() {
		List<AudioFormat> result = new ArrayList<>();
		for (Line.Info info : AudioSystem.getSourceLineInfo(new Line.Info(SourceDataLine.class))) {
			if (info instanceof SourceDataLine.Info) {
				Collections.addAll(result, ((SourceDataLine.Info) info).getFormats());
			}
		}
		return result;
	}
}
