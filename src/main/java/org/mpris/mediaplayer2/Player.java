package org.mpris.mediaplayer2;

import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/**
 * Adapted from dbus-java examples:
 * https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/org/mpris/mediaplayer2/Player.java
 */
@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
@DBusProperty(name = "LoopStatus", type = String.class, access = Access.READ_WRITE)
@DBusProperty(name = "Volume", type = Double.class, access = Access.READ_WRITE)
@DBusProperty(name = "Shuffle", type = Double.class, access = Access.READ_WRITE)
@DBusProperty(name = "Position", type = Integer.class, access = Access.READ)
@DBusProperty(name = "Rate", type = Double.class, access = Access.READ_WRITE)
@DBusProperty(name = "MinimumRate", type = Double.class, access = Access.READ_WRITE)
@DBusProperty(name = "MaximumRate", type = Double.class, access = Access.READ_WRITE)
public interface Player extends DBusInterface {

	@DBusBoundProperty(access = Access.READ, name = "Metadata")
	Map<String, Variant<?>> getMetadata();

	@DBusBoundProperty(access = Access.READ, name = "CanControl")
	boolean getCanControl();

	@DBusBoundProperty(access = Access.READ, name = "CanPlay")
	boolean getCanPlay();

	@DBusBoundProperty(access = Access.READ, name = "CanPause")
	boolean getCanPause();

	@DBusBoundProperty(access = Access.READ, name = "CanGoNext")
	boolean getCanGoNext();

	@DBusBoundProperty(access = Access.READ, name = "CanGoPrevious")
	boolean getCanGoPrevious();

	@DBusBoundProperty(access = Access.READ, name = "CanSeek")
	boolean getCanSeek();

	@DBusBoundProperty(access = Access.READ_WRITE, name = "Shuffle")
	boolean getShuffle();

	void setShuffle(boolean shuffle);

	@DBusBoundProperty(access = Access.READ, name = "PlaybackStatus")
	String getPlaybackStatus();

	void Previous();

	void Next();

	void Stop();

	void Play();

	void Pause();

	void PlayPause();

	void Seek(long _arg0);

	void OpenUri(String _arg0);

	void SetPosition(DBusPath _arg0, long _arg1);

//	interface PropertyMetadataType extends TypeRef<Map<String, Variant<?>>> {
//		Map<String, Variant<?>> props = new HashMap<>();
//	}

	class Seeked extends DBusSignal {
		private final long timeInUs;

		public Seeked(String _path, long _timeInUs) throws DBusException {
			super(_path);
			timeInUs = _timeInUs;
		}

		public long getTimeInUs() {
			return timeInUs;
		}

	}
}