package funoform.mdp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.freedesktop.dbus.exceptions.DBusException;

import funoform.mdp.Controller.SettingsListener;
import funoform.mdp.types.SettingsChanged;

public class Main {

	private static final Logger sLogger = Logger.getLogger(Main.class.getName());

	@SuppressWarnings("unused")
	public static void main(String[] args) throws InterruptedException, IOException, DBusException {

		// First things first, load our logging properties file. This sets the log
		// values to play nice with the CLI. However, if someone went through the pain
		// of manually specifying "-Djava.util.logging.config.file" on the command line
		// when launching this app, respect their wish and use those logging settings
		// regardless of what they do to the readability of our CLI.
		if (null == LogManager.getLogManager().getProperty("java.util.logging.config.file")) {
			try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
				LogManager.getLogManager().readConfiguration(stream);
			}
		}

		sLogger.log(Level.INFO, "Starting Player");

		ConfigManager cfg = new ConfigManager();
		Path startingDir = Paths.get(cfg.getStartingDir());

		Controller ctrl = new Controller(startingDir, cfg.getIsRecursive());
		ctrl.setRandom(cfg.getIsRandom());
		ctrl.setRepat(cfg.getIsRepeat());
		ctrl.registerSettingsListener(new SettingsListener() {
			@Override
			public void settingsChanged(SettingsChanged newSettings) {
//				System.out.println("Settings = " + newSettings);
			}
		});

		if (cfg.getIsAutoStart()) {
			ctrl.playBrowsingDir();
		}

		Cli cli = new Cli(ctrl);

		//Gui gui = new Gui();

		DBusInterface dbi = new DBusInterface(ctrl);

		// TODO: before exiting, get current settings from ctrl and save them
		// cfg.saveSettings(...);
	}
}