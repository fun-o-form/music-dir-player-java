package funoform.mdp.types;

/**
 * Send by the MusicPlayer to the Controller to provide updates on the song
 * playback progress, such as how far into the song are we and when the playback
 * is complete.
 */
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
