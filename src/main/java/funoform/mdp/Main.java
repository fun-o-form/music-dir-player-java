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

import funoform.mdp.dbus.DBusInterface;
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

			ConfigManager cfg = new ConfigManager();
			Path startingDir = Paths.get(cfg.getStartingDir());

			// Set the Swing look and feel. This only impacts the Gui, but must be done
			// before the Gui is constructed otherwise it will end up with half the default
			// and half the GTK theme. Note, we can't make this call in the Gui itself
			// because by then we can't theme fully applied. Too bad.
			String laf = cfg.getLookAndFeel();
			if (null != laf) {
				try {
					UIManager.setLookAndFeel(laf);
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
						| UnsupportedLookAndFeelException e) {
					// Oh well, run with with whatever the default L&F is on this system. This is
					// probably a windows platform thus doesn't support GTK
					sLogger.log(Level.WARNING, "Failed to set look and feel. Will run with default. " + e.getMessage());
				}
			}

			Controller ctrl = new Controller(cfg);
			Cli cli = new Cli(ctrl);

			// The default font size, as utilized by Java at least, is too small on the
			// librem5. Make it bigger. This must be done before creating any GUI components
			if (GuiUtils.isLibrem()) {
				GuiUtils.scaleAllFontSize(cfg.getFontScale());
			}
			Gui gui = new Gui(ctrl, cfg);

			DBusInterface dbi = new DBusInterface(ctrl, gui);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}