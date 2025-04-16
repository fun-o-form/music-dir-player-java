package funoform.mdp.gui;

import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.util.Enumeration;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public class GuiUtils {
	/**
	 * Sets all fonts globally in swing to be a different size.
	 * 
	 * This method is a hack. Normally you should trust the font size set at the
	 * system level.
	 * 
	 * @param multiplier A multiplier of 1.0 is the standard font size. Passing in
	 *                   1.2 will make all text 20% larger.
	 */
	public static void scaleAllFontSize(float multiplier) {
		UIDefaults defaults = UIManager.getDefaults();
		@SuppressWarnings("unused")
		int i = 0;
		for (Enumeration<Object> e = defaults.keys(); e.hasMoreElements(); i++) {
			Object key = e.nextElement();
			Object value = defaults.get(key);
			if (value instanceof Font) {
				Font font = (Font) value;
				int newSize = Math.round(font.getSize() * multiplier);
				if (value instanceof FontUIResource) {
					defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), newSize));
				} else {
					defaults.put(key, new Font(font.getName(), font.getStyle(), newSize));
				}
			}
		}
	}

	/**
	 * Our best attempt at determining if this app is running on a Librem5 or not.
	 * 
	 * Correctness not guaranteed. Using this method is a hack. And yet sometimes
	 * these hacks can make a big difference in the usability of the app on various
	 * platforms.
	 * 
	 * @return
	 */
	public static boolean isLibrem() {
		// On a Librem Evergreen running Byzantium, this returns "6.6.0-1-librem5"
		String osVer = System.getProperties().getProperty("os.version");
		boolean isLibrem = osVer.toLowerCase().contains("librem");
		return isLibrem;
	}

	/**
	 * Converts a number of seconds (e.g. 245) into a human readable duration string
	 * (e.g. 04:05). Will omit the hours unless the duration is over 1 hour long.
	 * 
	 * @param secs
	 * @return
	 */
	public static String secsToTimeStr(long secs) {
		int hours = (int) (secs / 3600);
		int minutes = (int) ((secs % 3600) / 60);
		int seconds = (int) (secs % 60);
		// we always include minutes and seconds, but only include hours if the duration
		// is at least 1 hour long
		if (0 < hours) {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%02d:%02d", minutes, seconds);
		}
	}

	/**
	 * Gets an image out of the jar's resources.
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Image getImage(String filename) throws IOException {
		return ImageIO.read(ClassLoader.getSystemResource("icons/" + filename));
	}

	/**
	 * Gets an icon out of the jar's resources.
	 * 
	 * @param filename
	 * @param size
	 * @return
	 * @throws IOException
	 */
	public static Icon getIcon(String filename, int size) throws IOException {
		Image image = getImage(filename).getScaledInstance(size, size, Image.SCALE_DEFAULT);
		Icon icon = new ImageIcon(image);
		return icon;
	}
}
