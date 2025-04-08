package funoform.mdp.types;

/**
 * Represents the progress of a song playback showing the current time into the
 * song, the total time, and the percentage complete.
 */
public class PlaybackPercentage {
	private long mCurTime = 0;
	private long mMaxTime = 0;
	private int mPercentage = 0;

	public PlaybackPercentage(long curTime, long maxTime) {
		mCurTime = curTime;
		mMaxTime = maxTime;
		// don't sweat the potential overflow. We are talking about songs here with
		// maybe 10 minute durations.
		if (0 < maxTime) {
			mPercentage = (int) (curTime * 100 / maxTime);
		}
	}

	public long getCurTimeSecs() {
		return mCurTime;
	}

	public long getMaxTimeSecs() {
		return mMaxTime;
	}

	/**
	 * Represented as 0-100, whole numbers only, no decimal points.
	 * 
	 * @return
	 */
	public int getPercentage() {
		return mPercentage;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[PlaybackPercentage: ");
		sb.append(mCurTime);
		sb.append(" / ");
		sb.append(mMaxTime);
		sb.append(" (");
		sb.append(mPercentage);
		sb.append("%)]");
		return sb.toString();
	}
}
