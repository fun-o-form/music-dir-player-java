package funoform.mdp.types;

/**
 * Represents the settings in use by the Controller, and also the status of
 * directory / song being played back by the Controller.
 */
public class SettingsChanged {
	public boolean isRecursive;
	public boolean isRepeat;
	public boolean isRandom;
	public String playingDir;
	public String browsingDir;
	public String songPlaying;
	public PlaybackPercentage pbPercentage;

	/**
	 * Performs a deep copy of a SettingsChanged object.
	 */
	public SettingsChanged clone() {
		SettingsChanged c = new SettingsChanged();
		c.isRecursive = this.isRecursive;
		c.isRepeat = this.isRepeat;
		c.isRandom = this.isRandom;
		c.playingDir = this.playingDir;
		c.browsingDir = this.browsingDir;
		c.songPlaying = this.songPlaying;
		c.pbPercentage = this.pbPercentage;
		return c;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("[SettingsChanged: recursive=");
		sb.append(isRecursive);
		sb.append(", repeat=");
		sb.append(isRepeat);
		sb.append(", random=");
		sb.append(isRandom);
		sb.append(", dir=");
		sb.append(playingDir);
		sb.append(", song=");
		sb.append(songPlaying);
		sb.append(", playback=");
		sb.append(pbPercentage);
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Gets a empty instance of a SettingsChanged object.
	 * 
	 * @return
	 */
	public static SettingsChanged blank() {
		SettingsChanged blank = new SettingsChanged();
		blank.songPlaying = "None";
		blank.playingDir = "None";
		blank.pbPercentage = new PlaybackPercentage(0, 0);
		return blank;
	}
}