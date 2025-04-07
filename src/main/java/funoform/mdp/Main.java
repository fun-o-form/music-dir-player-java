package funoform.mdp;

import java.io.IOException;

import org.freedesktop.dbus.exceptions.DBusException;

import funoform.mdp.Controller.SettingsListener;

public class Main {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws InterruptedException, IOException, DBusException {
		System.out.println("Starting player");

		Controller ctrl = new Controller("/home/jonathan/Music/", false);
		ctrl.setRandom(true);
		ctrl.registerSettingsListener(new SettingsListener() {
			@Override
			public void settingsChanged(SettingsChanged newSettings) {
//				System.out.println("Settings = " + newSettings);
			}
		});
		ctrl.playBrowsingDir();

		Cli cli = new Cli(ctrl);
		DBusInterface dbi = new DBusInterface(ctrl);
	}
}