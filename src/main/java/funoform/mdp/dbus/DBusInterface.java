package funoform.mdp.dbus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;
import org.mpris.MediaPlayer2;
import org.mpris.mediaplayer2.Player;

import funoform.mdp.Controller;
import funoform.mdp.Controller.SettingsListener;
import funoform.mdp.types.SettingsChanged;

/**
 * MPRIS (Media Player Remote Interfacing Specification). See:
 * 
 * https://specifications.freedesktop.org/mpris-spec/latest/
 * https://mpris2.readthedocs.io/en/latest/index.html
 * https://dbus.freedesktop.org/doc/dbus-java/dbus-java.pdf
 * https://rm5248.com/d-bus-tutorial/
 * 
 * https://hypfvieh.github.io/dbus-java/ https://github.com/hypfvieh/dbus-java
 * https://github.com/NGMusic/mpris-java/blob/master/extensions/test/xerus/mpris/MPRISPlayer.kt
 * 
 * Freedesktop.org's "dbus-java" implementation threw noClassDefFoundError when
 * attempting to open the DBusConnection. Not surprising, it is from 2009 and it
 * looks like class loading has changed since then. So I switched to
 * hypfvieh/dbus-java
 */
public class DBusInterface implements MediaPlayer2, Player {
	private static final Logger sLogger = Logger.getLogger(DBusInterface.class.getName());
	private Controller mCtrl;
	private DBusConnection mDbusConn;
	private RaiseWindowRequestListener mRaiseListener;
	private SettingsChanged mLastSettings = null;
	private Object mLastSettingsLock = new Object();

	public DBusInterface(Controller ctrl, RaiseWindowRequestListener l) throws DBusException {

		mRaiseListener = l;
		mCtrl = ctrl;

		// Get a connection to the session bus so we can request a bus name
		mDbusConn = DBusConnectionBuilder.forSessionBus().build();
		// REQ: "Each media player must request a unique bus name which begins with
		// org.mpris.MediaPlayer2"
		mDbusConn.requestBusName("org.mpris.MediaPlayer2.fofmusicdirplayer");

		// Export this object onto the bus using the path '/'
		mDbusConn.exportObject(getObjectPath(), this);

//		mDbusConn.addSigHandler(PropertiesChanged.class, new AbstractPropertiesChangedHandler() {
//			@Override
//			public void handle(PropertiesChanged _signal) {
//				System.out.println("DBus: Properties changed");
//			}
//		});

		mCtrl.registerSettingsListener(new SettingsListener() {
			@Override
			public void settingsChanged(SettingsChanged newSettings) {
				synchronized (mLastSettingsLock) {
					if (null == mLastSettings) {
						mLastSettings = newSettings;
					} else if (mLastSettings.songPlaying.toString()
							.compareTo(newSettings.songPlaying.toString()) != 0) {
						mLastSettings = newSettings;
					} else {
						return;
					}
				}
				try {
					List<String> invalidatedProperties = new ArrayList<>();

					// TODO: set icon to MDP desktop icon

					Map<String, Variant<?>> md = getMetadata();

					// https://github.com/hypfvieh/dbus-java/issues/74
					Variant<?> metaDataVariant = new Variant<>(md, "a{sv}");
					Map<String, Variant<?>> changedProperties = new HashMap<>();
					changedProperties.put("Metadata", metaDataVariant);

					mDbusConn.sendMessage(
							new org.freedesktop.dbus.interfaces.Properties.PropertiesChanged(getObjectPath(),
									"org.mpris.MediaPlayer2.Player", changedProperties, invalidatedProperties));
				} catch (DBusException e) {
					sLogger.log(Level.WARNING,
							"Failed to publish dbus settings change. The song info displayed through dbus will be incorrect. Exception = "
									+ e.getMessage());
				}
			}
		});
	}

	@Override
	public String getObjectPath() {
		// REQ: "The media player must expose the /org/mpris/MediaPlayer2 object path"
		return "/org/mpris/MediaPlayer2";
	}

	@Override
	public boolean isRemote() {
		/* Whenever you are implementing an object, always return false */
		return false;
	}

	@Override
	public void Previous() {
		sLogger.log(Level.FINE, "DBus: Previous");
		mCtrl.priorTrack();
	}

	@Override
	public void Next() {
		sLogger.log(Level.FINE, "DBus: Next");
		mCtrl.nextTrack();
	}

	@Override
	public void Stop() {
		sLogger.log(Level.FINE, "DBus: Stop");
		mCtrl.stop();
	}

	@Override
	public void Play() {
		sLogger.log(Level.FINE, "DBus: Play");
		mCtrl.playPause();
	}

	@Override
	public void Pause() {
		sLogger.log(Level.FINE, "DBus: Pause");
		mCtrl.playPause();
	}

	@Override
	public void PlayPause() {
		sLogger.log(Level.FINE, "DBus: Play/Pause");
		mCtrl.playPause();
	}

	@Override
	public void Seek(long _arg0) {
		sLogger.log(Level.WARNING, "DBus Seek is not supported");
	}

	@Override
	public void OpenUri(String _arg0) {
		System.out.println("DBus: OpenURI");
	}

	@Override
	public void SetPosition(DBusPath _arg0, long _arg1) {
		sLogger.log(Level.WARNING, "DBus SetPosition is not supported");
	}

	@Override
	public void Quit() {
		sLogger.log(Level.FINE, "DBus: Quit");
		mCtrl.exitApp(0);
	}

	@Override
	public void Raise() {
		sLogger.log(Level.FINE, "DBus: Raise Window");
		if (null != mRaiseListener) {
			mRaiseListener.raiseWindowRequested();
		}
	}

	@Override
	public String getIdentity() {
		return "funoformmusicdirplayer";
	}

	@Override
	public boolean getCanControl() {
		return true;
	}

	@Override
	public boolean getCanPlay() {
		return true;
	}

	@Override
	public boolean getCanPause() {
		return true;
	}

	@Override
	public String getDesktopEntry() {
		return "firefox";
	}

	@Override
	public List<String> getSupportedMimeTypes() {
		List<String> ret = new ArrayList<>();
		ret.add("audio/mpeg");
		return ret;
	}

	@Override
	public List<String> getSupportedUriSchemes() {
		List<String> ret = new ArrayList<String>();
		ret.add("file");
		return ret;
	}

	@Override
	public boolean getHasTrackList() {
		return false;
	}

	@Override
	public boolean getCanQuit() {
		return true;
	}

	@Override
	public boolean getCanSetFullscreen() {
		return false;
	}

	@Override
	public void setFullscreen(boolean isFullscreen) {
		sLogger.log(Level.WARNING, "DBus: setFullscreen is not supported");
	}

	@Override
	public boolean getFullscreen() {
		return false;
	}

	@Override
	public boolean getCanRaise() {
		return true;
	}

	@Override
	public boolean getCanGoNext() {
		return true;
	}

	@Override
	public boolean getCanGoPrevious() {
		return true;
	}

	@Override
	public boolean getCanSeek() {
		return false;
	}

	@Override
	public boolean getShuffle() {
		return true;
	}

	@Override
	public void setShuffle(boolean shuffle) {
		sLogger.log(Level.FINE, "DBus: Suffle");
		mCtrl.setRandom(true);
	}

	@Override
	public String getPlaybackStatus() {
		// Per the spec, the playback status must be "Playing", "Paused", or "Stopped"
		synchronized (mLastSettingsLock) {
			if (null == mLastSettings || null == mLastSettings.songPlaying) {
				return "Stopped";
			} else if (mLastSettings.isPaused) {
				return "Paused";
			} else {
				return "Playing";
			}
		}
	}

	@Override
	public Map<String, Variant<?>> getMetadata() {
		// https://specifications.freedesktop.org/mpris-spec/latest/Track_List_Interface.html#Mapping:Metadata_Map
		// https://www.freedesktop.org/wiki/Specifications/mpris-spec/metadata/
		Map<String, Variant<?>> md = new HashMap<>();

		md.put("mpris:trackid", new Variant<String>("KNEZ-trackId"));
		md.put("xesam:title", new Variant<String>("KNEZ-title"));
		md.put("xesam:album", new Variant<String>("KNEZ-album"));
		String[] tempArtist = { "KNEZ-artist" };
		md.put("xesam:artist", new Variant<String[]>(tempArtist));
		md.put("mpris:length", new Variant<Integer>(30000));

		if (null != mLastSettings) {
			int trackLenInMicroSecs = (int) (mLastSettings.pbPercentage.getMaxTimeSecs() * 10000);
			String filePlaying = mLastSettings.songPlaying.getFileName().toString();
			String dirPlaying = mLastSettings.playingDir.getFileName().toString();

			md.put("mpris:trackid", new Variant<String>(filePlaying));
			md.put("mpris:artUrl",
					new Variant<String>("https://upload.wikimedia.org/wikipedia/en/d/db/Clippy-letter.PNG")); // DISPLAYED

			md.put("mpris:length", new Variant<Integer>(trackLenInMicroSecs));
			md.put("xesam:title", new Variant<String>(filePlaying)); // DISPLAYED
			md.put("xesam:album", new Variant<String>(dirPlaying)); // DISPLAYED
			String[] artist = { dirPlaying };
			md.put("xesam:artist", new Variant<String[]>(artist)); // DISPLAYED
		}

		return md;
	}
}
