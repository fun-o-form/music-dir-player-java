package funoform.mdp;

public class PlaybackStatus {
	public boolean isPlaybackComplete;
	public PlaybackPercentage pbPercentage;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[PlaybackStatus: isComplete=");
		sb.append(isPlaybackComplete);
		sb.append(" ");
		sb.append(pbPercentage.toString());
		sb.append("]");
		return sb.toString();
	}
}
