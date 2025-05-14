package funoform.mdp;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Static functions for displaying file names.
 */
public class DisplayUtils {

	/**
	 * Returns just the file name with leading paths and trailing extension removed.
	 * Also optionally limited to a maximum string length.
	 * 
	 * @param file
	 * @param maxChars
	 * @return
	 */
	public static String getFileNameLengthLimited(Path file, int maxChars) {
		String ret = "";
		if (null != file) {
			String fileName = file.getFileName().toString();

			if (!Files.isDirectory(file)) {
				// strip the file extension
				int extIndex = fileName.lastIndexOf('.');
				if (-1 != extIndex) {
					fileName = fileName.substring(0, extIndex);
				}
			}

			if (-1 != maxChars) {
				ret = lastNChars(fileName, maxChars);
			} else {
				ret = fileName;
			}
		}
		return ret;
	}

	/**
	 * If the input string is over numChars long, return only the numChars right
	 * most characters, effectively cutting off the start of the string.
	 * 
	 * @param str
	 * @param numChars
	 * @return
	 */
	public static String lastNChars(String str, int numChars) {
		if (str.length() > numChars) {
			int start = str.length() - numChars;
			return str.substring(start);
		}
		return str;
	}
}
