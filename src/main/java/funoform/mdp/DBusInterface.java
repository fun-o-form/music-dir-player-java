package funoform.mdp;

import java.util.ArrayList;
import java.util.List;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.mpris.MediaPlayer2;
import org.mpris.mediaplayer2.Player;

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
	private Controller mCtrl;
	private DBusConnection mDbusConn;

	public DBusInterface(Controller ctrl) throws DBusException {
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
	}

	@Override
	public void Next() {
		System.out.println("DBus: Next");
		mCtrl.nextTrack();
	}

	@Override
	public void Stop() {
		System.out.println("DBus: Stop");
	}

	@Override
	public void Play() {
		System.out.println("DBus: Play");
	}

	@Override
	public void Pause() {
		System.out.println("DBus: Pause");
	}

	@Override
	public void PlayPause() {
		System.out.println("DBus: PlayPause");
	}

	@Override
	public void Seek(long _arg0) {
		System.out.println("DBus: Seek");
	}

	@Override
	public void OpenUri(String _arg0) {
		System.out.println("DBus: OpenURI");
	}

	@Override
	public void SetPosition(DBusPath _arg0, long _arg1) {
		System.out.println("DBus: SetPositon");
	}

	@Override
	public void Quit() {
		System.out.println("DBus: Quit");
	}

	@Override
	public void Raise() {
		System.out.println("DBus: Raise");
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PropertySupportedMimeTypesType getSupportedMimeTypes() {
		List<String> ret = new ArrayList<>();
		ret.add("audio/mpeg");
		return (PropertySupportedMimeTypesType) ret;
	}

	@Override
	public PropertySupportedUriSchemesType getSupportedUriSchemes() {
		List<String> ret = new ArrayList<String>();
		ret.add("file");
		return (PropertySupportedUriSchemesType) ret;
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
		return true;
	}

	@Override
	public void setFullscreen(boolean isFullscreen) {
	}

	@Override
	public boolean getFullscreen() {
		return false;
	}

	@Override
	public boolean getCanRaise() {
		return true;
	}
}
