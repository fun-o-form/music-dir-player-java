package funoform.mdp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.freedesktop.dbus.exceptions.DBusException;

import funoform.mdp.gui.Gui;
import funoform.mdp.gui.GuiUtils;

public class Main {

	private static final Logger sLogger = Logger.getLogger(Main.class.getName());

	@SuppressWarnings("unused")
	public static void main(String[] args) throws InterruptedException, IOException, DBusException {
		try {
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

			// Set the Swing look and feel. This only impacts the Gui, but must be done
			// before the Gui is constructed otherwise it will end up with half the default
			// and half the GTK theme. Note, we can't make this call in the Gui itself
			// because by then we can't theme fully applied. Too bad.
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e) {
				// Oh well, run with with whatever the default L&F is on this system. This is
				// probably a windows platform thus doesn't support GTK
				e.printStackTrace();
			}

			ConfigManager cfg = new ConfigManager();
			Path startingDir = Paths.get(cfg.getStartingDir());

			Controller ctrl = new Controller(startingDir);
			ctrl.setRandom(cfg.getIsRandom());
			ctrl.setRepeat(cfg.getIsRepeat());
			if (cfg.getIsAutoStart()) {
				ctrl.playDir(startingDir, false);
			}

			Cli cli = new Cli(ctrl);

			// The default font size, as utilized by Java at least, is too small on the
			// librem5. Make it bigger. This must be done before creating any GUI components
			if (GuiUtils.isLibrem()) {
				GuiUtils.scaleAllFontSize(1.5f);
			}
			Gui gui = new Gui(ctrl);

//		DBusInterface dbi = new DBusInterface(ctrl);

			// TODO: before exiting, get current settings from ctrl and save them
			// cfg.saveSettings(...);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}