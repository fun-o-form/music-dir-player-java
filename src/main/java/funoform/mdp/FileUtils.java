package funoform.mdp;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class FileUtils {

	// Just a bunch of static methods so don't let someone create this thing
	private FileUtils() {
	}

	public static List<Path> getSubDirectories(Path startingDir) throws IOException {
		DirectoryStream.Filter<Path> hiddenFilter = new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				if (Files.isHidden(entry))
					return false;
				return true;
			}
		};

		// get all the directories that aren't hidden
		List<Path> dirs = new ArrayList<>();
		try (DirectoryStream<Path> s = Files.newDirectoryStream(startingDir, hiddenFilter)) {
			Iterator<Path> itr = s.iterator();
			while (itr.hasNext()) {
				Path next = itr.next();
				if (Files.isDirectory(next)) {
					dirs.add(next);
				}
			}
		}
		return dirs;
	}

	/**
	 * Gets a list of all the playable music files in the specified directory.
	 * 
	 * @param dir                 The directory to search.
	 * @param recursive           If false, we only search the specified directory.
	 *                            If true, we keep searching sub-directories as we
	 *                            find them.
	 * @param maxListFilesTimeSec A time limit for how long this method is allowed
	 *                            to run before returning. For example, suppose you
	 *                            are searching "/" recursively with a
	 *                            maxListFilesTimesSec set to 10 seconds. Then after
	 *                            10 seconds we will return whatever we have found
	 *                            so far even though we didn't finish searching
	 *                            every sub-directory.
	 * @return The list of playable music files.
	 * @throws IOException
	 */
	public static List<Path> listMusicFiles(Path dir, boolean recursive, int maxListFilesTimeSec) throws IOException {
		int depth = 1;
		if (recursive) {
			// set a limit on how deep we will search, just in case we have a bug in our
			// search or the user did something dumb like have a folder contain a sym link
			// to itself. We don't want to turn off following sym links for music
			// collections entirely because I could see users using them.
			depth = 20;
		}

		// Some number of seconds in the future we will stop searching and just return
		// what we got so far
		long quitAtMs = System.currentTimeMillis() + maxListFilesTimeSec * 1000;

		List<Path> ret = listMusicFiles(dir.toFile(), depth, quitAtMs);

		if (null != ret) {
			// sort file names alphabetically ignoring case
			Collections.sort(ret, new Comparator<Path>() {
				@Override
				public int compare(Path o1, Path o2) {
					return o1.getFileName().toString().toLowerCase()
							.compareTo(o2.getFileName().toString().toLowerCase());
				}
			});
			return ret;
		} else {
			return new ArrayList<Path>();
		}
	}

	static List<Path> listMusicFiles(File dir, int depthRemaining, long quitAtMs) throws IOException {
		if (0 == depthRemaining) {
			// break out of our recursive loop once we have exhausted our full allowable
			// search depth
			return null;
		}

		List<Path> ret = new ArrayList<>();

		File[] filesInDir = dir.listFiles();

		if (filesInDir != null) {
			for (File fileInDir : filesInDir) {

				// Check to see if we have taken too long. If so, break out now to stop
				// searching and return what we got so far
				if (quitAtMs <= System.currentTimeMillis()) {
					return ret;
				}

				try {
					// If this is a sub-directory, follow it down recursively
					if (fileInDir.isDirectory()) {
						List<Path> subFiles = listMusicFiles(fileInDir, depthRemaining - 1, quitAtMs);
						if (null != subFiles) {
							ret.addAll(subFiles);
						}
					}
					// if its just a file, not a directory, add the file directly if it is a music
					// file
					else if (fileInDir.isFile()) {
						Path p = fileInDir.toPath();
						if (isSupportedAudioFile(p)) {
							ret.add(p);
						}
					}
				} catch (Exception e) {
					// Probably a permissions denied exception. Just ignore this directory and move
					// on to the next one.
				}
			}
		}

		return ret;
	}

	private static boolean isSupportedAudioFile(Path path) {
		// Globs are case sensitive on case sensitive operating systems. Our quick fix
		// is just to look for the various permutations of upper and lower case song
		// extensions. The alternative is to get every file and match on our own code.

		// NOTE: We could add flacs to our list, e.g.
		// "glob:*.{mp3,MP3,Mp3,flac,Flac,FLAC}". But flacs only seem to work half the
		// time with our music playing library. So just leave flacs out for now.
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.{mp3,MP3,Mp3}");
		return matcher.matches(path.getFileName());
	}
}
