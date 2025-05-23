package funoform.mdp;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import funoform.mdp.types.SettingsChanged;

/**
 * Responsible for loading and saving user preferences related to this
 * application to the java preferences store so they can be restored on the next
 * launch.
 * 
 * We don't know or care exactly where the preferences are stored or how they
 * are serialized. We just ask Java to store some preferences, which it does,
 * and to give us back those same values the next time the app starts up.
 * 
 * One detail worth mentioning, if this is the first time the app is launched,
 * none of the values exist in the preferences store. In that case, you just get
 * back the default value specified with each call.
 */
public class ConfigManager {
	private static final Logger sLogger = Logger.getLogger(ConfigManager.class.getName());
	private Preferences mPrefs = Preferences.userNodeForPackage(this.getClass());

	private static final String STARTING_DIR = "startingDir";
	private static final String RANDOM = "isRandom";
	private static final String REPEAT = "isRepeat";
	private static final String RECURSIVE = "isRecursive";
	private static final String AUTO_START = "isAutoStart";
	private static final String FONT_SCALE = "fontScale";
	private static final String SCROLL_BAR_WIDTH = "scrollBarWidth";
	private static final String PREV_TRACK_BUTTON = "isShowPrevTrackButton";
	private static final String LOOK_AND_FEEL = "lookAndFeel";

	public ConfigManager() {
		// log values at startup
		try {
			StringBuilder sb = new StringBuilder(String.valueOf(mPrefs.keys().length));
			sb.append(" keys defined in config at startup:");
			for (String key : mPrefs.keys()) {
				sb.append(key);
				sb.append(", ");
			}
			sLogger.log(Level.FINE, sb.toString());
		} catch (BackingStoreException e) {
			// oh well, we tried. But ultimately this is just for debugging
		}
		sLogger.log(Level.FINE, "   " + STARTING_DIR + "=" + getStartingDir());
		sLogger.log(Level.FINE, "   " + RANDOM + "=" + getIsRandom());
		sLogger.log(Level.FINE, "   " + REPEAT + "=" + getIsRepeat());
		sLogger.log(Level.FINE, "   " + RECURSIVE + "=" + getIsRecursive());
		sLogger.log(Level.FINE, "   " + AUTO_START + "=" + getIsAutoStart());
		sLogger.log(Level.FINE, "   " + PREV_TRACK_BUTTON + "=" + getIsAutoStart());
		sLogger.log(Level.FINE, "   " + LOOK_AND_FEEL + "=" + getLookAndFeel());
	}

	public String getStartingDir() {
		// TODO: Remove Music from path
		Path defaultPath = Path.of(System.getProperty("user.home"), "Music");
		return mPrefs.get(STARTING_DIR, defaultPath.toString());
	}

	public boolean getIsAutoStart() {
		return mPrefs.getBoolean(AUTO_START, true);
	}
	
	public void saveAutoStart(boolean autoStart) {
		mPrefs.putBoolean(AUTO_START, autoStart);
	}
	
	public boolean getIsShowPrevTrackBtn() {
		return mPrefs.getBoolean(PREV_TRACK_BUTTON, false);
	}
	
	public void saveShowPrevTrackBtn(boolean shouldShow) {
		mPrefs.putBoolean(PREV_TRACK_BUTTON, shouldShow);
	}

	public boolean getIsRandom() {
		return mPrefs.getBoolean(RANDOM, false);
	}

	public boolean getIsRepeat() {
		return mPrefs.getBoolean(REPEAT, true);
	}

	public boolean getIsRecursive() {
		return mPrefs.getBoolean(RECURSIVE, false);
	}

	public double getFontScale() {
		return mPrefs.getDouble(FONT_SCALE, 1.5d);
	}

	public void saveFontScale(double fontScale) {
		mPrefs.putDouble(FONT_SCALE, fontScale);
	}

	public int getScrollBarWidth() {
		return mPrefs.getInt(SCROLL_BAR_WIDTH, 30);
	}

	public void saveScrollBarWidth(int barWidth) {
		mPrefs.putInt(SCROLL_BAR_WIDTH, barWidth);
	}
	
	public String getLookAndFeel() {
		return mPrefs.get(LOOK_AND_FEEL, null);
	}
	
	public void saveLookAndFeel(String laf) {
		mPrefs.put(LOOK_AND_FEEL, laf);
	}
	

	public void savePreferences(SettingsChanged newSettings) {
		mPrefs.put(STARTING_DIR, newSettings.playingDir.toString());
		mPrefs.putBoolean(RANDOM, newSettings.isRandom);
		mPrefs.putBoolean(REPEAT, newSettings.isRepeat);
		try {
			mPrefs.flush();
		} catch (BackingStoreException e) {
			sLogger.log(Level.WARNING, "Failed to save preferences due to exception = " + e.getMessage());
		}
		// TODO: no way to change recursive
	}
}
