package funoform.mdp;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	public static List<Path> listMusicFiles(Path dir, boolean recursive) throws IOException {
		int depth = 1;
		if (recursive) {
			// set a limit on how deep we will search, just in case we have a bug in our
			// search or the user did something dumb like have a folder contain a sym link
			// to itself. We don't want to turn off following sym links for music
			// collections entirely because I could see users using them.
			depth = 100;
		}
		List<Path> ret = new ArrayList<>();
		try (Stream<Path> s = Files.walk(dir, depth)) {
			ret = s.filter(Files::isRegularFile).filter(p -> isSupportedAudioFile(p)).collect(Collectors.toList());
		}

		// sort file names alphabetically ignoring case
		Collections.sort(ret, new Comparator<Path>() {
			@Override
			public int compare(Path o1, Path o2) {
				return o1.getFileName().toString().toLowerCase().compareTo(o2.getFileName().toString().toLowerCase());
			}
		});

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
