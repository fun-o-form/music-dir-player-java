package funoform.mdp;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {

	public static List<Path> getSubDirectories(String startingDir) throws IOException {
		Path dir = Paths.get(startingDir);
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(Files::isDirectory).collect(Collectors.toList());
		}
	}

	public static List<Path> listMusicFile(String dirToScan, boolean recursive) throws IOException {
		Path dir = Paths.get(dirToScan);
		int depth = 1;
		if (recursive) {
			// set a limit on how deep we will search, just in case we have a bug in our
			// search or the user did something dumb like have a folder contain a sym link
			// to itself. We don't want to turn off following sym links for music
			// collections entirely because I could see users using them.
			depth = 100;
		}
		try (Stream<Path> s = Files.walk(dir, depth)) {
			return s.filter(Files::isRegularFile).filter(p -> isSupportedAudioFile(p)).collect(Collectors.toList());
		}
	}

	private static boolean isSupportedAudioFile(Path path) {
		// TODO: does this work for *.MP3 as well?
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.mp3");
		return matcher.matches(path.getFileName());
	}
}
