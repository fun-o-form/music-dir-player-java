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
	}

	public String getStartingDir() {
		// TODO: Remove Music from path
		Path defaultPath = Path.of(System.getProperty("user.home"), "Music");
		return mPrefs.get(STARTING_DIR, defaultPath.toString());
	}

	public boolean getIsAutoStart() {
		return mPrefs.getBoolean(AUTO_START, true);
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

	public void savePreferences(SettingsChanged newSettings) {
		mPrefs.put(STARTING_DIR, newSettings.playingDir.toString());
		mPrefs.putBoolean(RANDOM, newSettings.isRandom);
		mPrefs.putBoolean(REPEAT, newSettings.isRepeat);
		// TODO: no way to change auto-start
		// TODO: no way to change recursive
	}
}
