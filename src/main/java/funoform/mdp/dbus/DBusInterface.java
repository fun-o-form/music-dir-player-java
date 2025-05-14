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
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
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
 * looks like class loading has changed since then. So I switched to the
 */
public class DBusInterface implements MediaPlayer2, Player {
	private static final Logger sLogger = Logger.getLogger(DBusInterface.class.getName());
	private Controller mCtrl;
	private DBusConnection mDbusConn;
	private RaiseWindowRequestListener mRaiseListener;
	private SettingsChanged mLastSettings = null;

	public DBusInterface(Controller ctrl, RaiseWindowRequestListener l) throws DBusException {
		mRaiseListener = l;

		// TODO: anything needed to mute on receiving a phone call? Seems like Firefox
		// mutes automatically and is unlikely to have read the modem/sim/network
		// connection info over D-Bus (https://developer.puri.sm/Librem5/APIs.html)
		// TODO: send D-Bus signal to raise keyboard upon entering CLI

		mCtrl = ctrl;

		// Get a connection to the session bus so we can request a bus name
		mDbusConn = DBusConnectionBuilder.forSessionBus().build();
		// REQ: "Each media player must request a unique bus name which begins with
		// org.mpris.MediaPlayer2"
		mDbusConn.requestBusName("org.mpris.MediaPlayer2.fofmusicdirplayer");

		// Export this object onto the bus using the path '/'
		mDbusConn.exportObject(getObjectPath(), this);

		mDbusConn.addSigHandler(PropertiesChanged.class, new AbstractPropertiesChangedHandler() {
			@Override
			public void handle(PropertiesChanged _signal) {
				System.out.println("DBus: Properties changed");
			}
		});

		mCtrl.registerSettingsListener(new SettingsListener() {
			@Override
			public void settingsChanged(SettingsChanged newSettings) {

//				mDbusConn.sendMessage(new org.freedesktop.dbus.interfaces.Properties.PropertiesChanged(getObjectPath(), "org.mpris.MediaPlayer2.fofmusicdirplayer", getMetadata(), getSupportedMimeTypes());
				mLastSettings = newSettings;
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
		System.out.println("DBus: Previous");
		mCtrl.priorTrack();
	}

	@Override
	public void Next() {
		System.out.println("DBus: Next");
		mCtrl.nextTrack();
	}

	@Override
	public void Stop() {
		System.out.println("DBus: Stop");
		mCtrl.stop();
	}

	@Override
	public void Play() {
		System.out.println("DBus: Play");
		mCtrl.playPause();
	}

	@Override
	public void Pause() {
		System.out.println("DBus: Pause");
		mCtrl.playPause();
	}

	@Override
	public void PlayPause() {
		System.out.println("DBus: PlayPause");
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
		System.out.println("DBus: Quit");
		mCtrl.exitApp(0);
	}

	@Override
	public void Raise() {
		System.out.println("DBus: Raise");
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
		System.out.println("DBus: Shuffle");
		mCtrl.setRandom(true); // TODO: probably not right any more
	}

	@Override
	public String getPlaybackStatus() {
		return "Playing"; // TODO: Playing, Paused, or Stopped
	}

	@Override
	public Map<String, Variant<?>> getMetadata() {
		// https://specifications.freedesktop.org/mpris-spec/latest/Track_List_Interface.html#Mapping:Metadata_Map
		// https://www.freedesktop.org/wiki/Specifications/mpris-spec/metadata/
		Map<String, Variant<?>> md = new HashMap<>();
		md.put("mpris:trackid", new Variant<String>("notUsedButRequired"));
		md.put("mpris:artUrl", new Variant<String>("https://upload.wikimedia.org/wikipedia/en/d/db/Clippy-letter.PNG")); // DISPLAYED

		if (null != mLastSettings) {
			int trackLenInMicroSecs = (int) (mLastSettings.pbPercentage.getMaxTimeSecs() * 10000);
			md.put("mpris:length", new Variant<Integer>(trackLenInMicroSecs));
			md.put("xesam:title", new Variant<String>(mLastSettings.songPlaying.getFileName().toString())); // DISPLAYED
			md.put("xesam:album", new Variant<String>(mLastSettings.playingDir.getFileName().toString())); // DISPLAYED
			String[] artist = { "myArtist" };
			md.put("xesam:artist", new Variant<String[]>(artist)); // DISPLAYED
		}
		return md;
	}
}
