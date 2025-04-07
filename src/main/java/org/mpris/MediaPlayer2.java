package org.mpris;

import java.util.List;

import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusBoundProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * Copied from dbus-java examples:
 * https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/org/mpris/MediaPlayer2.java
 */
//@DBusProperty(name = "Identity", type = String.class, access = Access.READ)
//@DBusProperty(name = "DesktopEntry", type = String.class, access = Access.READ)
//@DBusProperty(name = "SupportedMimeTypes", type = MediaPlayer2.PropertySupportedMimeTypesType.class, access = Access.READ)
//@DBusProperty(name = "SupportedUriSchemes", type = MediaPlayer2.PropertySupportedUriSchemesType.class, access = Access.READ)
//@DBusProperty(name = "HasTrackList", type = Boolean.class, access = Access.READ)
//@DBusProperty(name = "CanQuit", type = Boolean.class, access = Access.READ)
//@DBusProperty(name = "CanSetFullscreen", type = Boolean.class, access = Access.READ)
//@DBusProperty(name = "Fullscreen", type = Boolean.class, access = Access.READ_WRITE)
//@DBusProperty(name = "CanRaise", type = Boolean.class, access = Access.READ)
//@SuppressWarnings({"checkstyle:methodname", "checkstyle:hideutilityclassconstructor", "checkstyle:visibilitymodifier"})
public interface MediaPlayer2 extends DBusInterface {

	@DBusBoundProperty(access = Access.READ, name = "Identity")
	String getIdentity();

	@DBusBoundProperty(access = Access.READ, name = "DesktopEntry")
	String getDesktopEntry();

	@DBusBoundProperty(access = Access.READ, name = "SupportedMimeTypes")
//	List<String> getSupportedMimeTypes();
	PropertySupportedMimeTypesType getSupportedMimeTypes();

	@DBusBoundProperty(access = Access.READ, name = "SupportedUriSchemes")
	PropertySupportedUriSchemesType getSupportedUriSchemes();

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

	interface PropertySupportedMimeTypesType extends TypeRef<List<String>> {
	}

	interface PropertySupportedUriSchemesType extends TypeRef<List<String>> {
	}
}