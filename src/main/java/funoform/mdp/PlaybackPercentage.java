package funoform.mdp;

public class PlaybackPercentage {
	private long mCurTime = 0;
	private long mMaxTime = 0;
	private int mPercentage = 0;

	public PlaybackPercentage(long curTime, long maxTime) {
		mCurTime = curTime;
		mMaxTime = maxTime;
		// don't sweat the potential overflow. We are talking about songs here with
		// maybe 10 minute durations.
		if(0<maxTime) {
			mPercentage = (int) (curTime * 100 / maxTime);
		}
	}

	public long getCurTime() {
		return mCurTime;
	}

	public long getMaxTime() {
		return mMaxTime;
	}

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
