package org.mpris;

import java.util.List;

import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Adapted from from dbus-java examples:
 * https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/org/mpris/MediaPlayer2.java
 */
public interface MediaPlayer2 extends DBusInterface {

	@DBusBoundProperty(access = Access.READ, name = "Identity")
	String getIdentity();

	@DBusBoundProperty(access = Access.READ, name = "DesktopEntry")
	String getDesktopEntry();

	@DBusBoundProperty(access = Access.READ, name = "SupportedMimeTypes")
	List<String> getSupportedMimeTypes();

	@DBusBoundProperty(access = Access.READ, name = "SupportedUriSchemes")
	List<String> getSupportedUriSchemes();

	@DBusBoundProperty(access = Access.READ, name = "HasTrackList")
	boolean getHasTrackList();

	@DBusBoundProperty(access = Access.READ, name = "CanQuit")
	boolean getCanQuit();

	@DBusBoundProperty(access = Access.READ, name = "CanSetFullscreen")
	boolean getCanSetFullscreen();

	@DBusBoundProperty(access = Access.WRITE, name = "Fullscreen")
	void setFullscreen(boolean isFullscreen);

	@DBusBoundProperty(access = Access.READ, name = "Fullscreen")
	boolean getFullscreen();

	@DBusBoundProperty(access = Access.READ, name = "CanRaise")
	boolean getCanRaise();

	void Quit();

	void Raise();
}