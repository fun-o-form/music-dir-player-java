package funoform.mdp;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import funoform.mdp.Controller.SettingsListener;
import funoform.mdp.types.SettingsChanged;

public class Cli {
	private Controller mCtrl;
	private Thread mThread = new Thread(new CliRunnable());
	private boolean mShouldRun = true;
	private SettingsChanged mLastPrintedSettings = null;
	private AtomicBoolean mPausePrintingStatus = new AtomicBoolean(false);
	private boolean mRecursive = false;

	// Colors
	private static final String ANSI_BLUE = "\u001B[0;34m";
	private static final String ANSI_BLUE_BG = "\u001B[37;44m";
	private static final String ANSI_RED = "\u001B[0;31m";
	private static final String ANSI_RED_BG = "\u001B[37;101m";
	private static final String ANSI_SWAP_FG_BG = "\u001B[7m";
	private static final String ANSI_RESET = "\u001B[0m";
	// Cursor control. See
	// https://gist.github.com/ConnerWill/d4b6c776b509add763e17f9f113fd25b
	private static final String ANSI_ROW1_LEFT = "\u001B[1;0H";
	private static final String ANSI_ROW2_LEFT = "\u001B[2;0H";
	private static final String ANSI_ROW3_LEFT = "\u001B[3;0H";
	private static final String ANSI_ROW4_LEFT = "\u001B[4;0H";
	private static final String ANSI_ROW5_LEFT = "\u001B[5;0H";
	private static final String ANSI_ERASE_SCREEN = "\u001B[2J";
	private static final String ANSI_ERASE_LINE = "\u001B[2K";
	private static final String ANSI_ERASE_SCREEN_FROM_CUR = "\u001B[0J";
	private static final String ANSI_SAVE_CUR_POS = "\u001B7";
	private static final String ANSI_RESTORE_CUR_POS = "\u001B8";

	// "\u001B[2K" Erases entire line but leaves cursor where it was (moves
	// repeating text right)

	public Cli(Controller ctrl) {
		warnIfNotAnsiCompatible();

		mCtrl = ctrl;
		mThread.start();

		printStatus(SettingsChanged.blank(), true, mRecursive);

		mCtrl.registerSettingsListener(new SettingsListener() {
			@Override
			public void settingsChanged(SettingsChanged newSettings) {
				if (!mPausePrintingStatus.get()) {
					if (didSettingChangeMeaningfully(mLastPrintedSettings, newSettings)) {
						mLastPrintedSettings = newSettings;
						printStatus(newSettings, true, mRecursive);
					}
				}
			}
		});
	}

	public void stop() {
		mShouldRun = false;
		System.out.print(ANSI_ERASE_SCREEN + ANSI_ROW1_LEFT);
		mThread.interrupt();
		try {
			mThread.join();
		} catch (InterruptedException e) {
			// oh well
		}
	}

	private static void warnIfNotAnsiCompatible() {
		String os = System.getProperty("os.name");
		if (os.toLowerCase().contains("windows")) {
			String msg = "WARNING: This application provides an NCurses-like console that relies "
					+ " on ANSI escape codes to properly display content. Your operating system "
					+ "likely doesn't support these ANSI escape codes. The CLI will likely not "
					+ "look good. Continue at your own risk.";
			System.out.println(msg);
		}
	}

	private static synchronized void printStatus(SettingsChanged newSettings, boolean redrawAll, boolean recursive) {
		// row 1
		if (redrawAll) {
			System.out.print(ANSI_ERASE_SCREEN);
			System.out.print(ANSI_ROW1_LEFT);
			System.out.print("cmd> ");
			System.out.print(ANSI_SAVE_CUR_POS);
		}

		// row 2
		System.out.print(ANSI_ROW2_LEFT);
		System.out.print("-------- Fun-O-Form Music Directory Player --------");

		// row 3
		System.out.print(ANSI_ROW3_LEFT + ANSI_ERASE_LINE);
		System.out.print(ANSI_BLUE_BG + String.format("[%4s]", newSettings.queuedSongs));
		System.out.print(ANSI_RESET + " ");
		if (null != newSettings.playingDir) {
			System.out.print(ANSI_BLUE + lastNChars(newSettings.playingDir.toString(), 50));
		}
		System.out.print(ANSI_RESET);

		// row 4
		System.out.print(ANSI_ROW4_LEFT + ANSI_ERASE_LINE);
		System.out.print(ANSI_RED_BG + String.format("[%3s%%]", newSettings.pbPercentage.getPercentage()));
		System.out.print(ANSI_RESET + " ");
		if (null != newSettings.songPlaying) {
			System.out.print(ANSI_RED + lastNChars(newSettings.songPlaying.getFileName().toString(), 44));
		}
		System.out.print(ANSI_RESET);
		System.out.println("");

		// row 5
		System.out.print(ANSI_ROW5_LEFT + ANSI_ERASE_LINE);
		if (newSettings.isRandom) {
			System.out.print(ANSI_SWAP_FG_BG + "[1-Random On]" + ANSI_RESET + " ");
		} else {
			System.out.print("[1-Random Off] ");
		}
		if (newSettings.isRepeat) {
			System.out.print(ANSI_SWAP_FG_BG + "[2-Repeat On]" + ANSI_RESET + " ");
		} else {
			System.out.print("[2-Repeat Off] ");
		}
		if (recursive) {
			System.out.println(ANSI_SWAP_FG_BG + "[3-Recursive On]" + ANSI_RESET + " ");
		} else {
			System.out.println("[3-Recursive Off]");
		}

		// [1-Random On] [2-Repeat Off] [3-Recursive On]

		// row 6-n
		System.out.println("s - Stop");
		System.out.println("n - Next");
		System.out.println("x - Exit");
		System.out.println("d - Specify Directory to Play");

		if (redrawAll) {
			// Put the cursor back where it was before we started. If the user was typing in
			// a command, they should be able to continue without their cursor jumping away
			System.out.print(ANSI_RESTORE_CUR_POS);
		} else {
			System.out.print("\u001B[1;6H");
		}
	}

	private static String lastNChars(String str, int numChars) {
		if (str.length() > numChars) {
			int start = str.length() - numChars;
			return str.substring(start);
		}
		return str;
	}

	/**
	 * Help cut down on spamming CLI console updates by checking to see if anything
	 * has changed that the user would actually notice.
	 * 
	 * @param old   The last settings that were printed to console.
	 * @param newer The new settings
	 * 
	 * @return False if the two settings are the same regarding the fields we
	 *         actually print to the console. True if the settings are different
	 *         enough to warrant printing the new settings.
	 */
	private static boolean didSettingChangeMeaningfully(SettingsChanged old, SettingsChanged newer) {
		if (null != old) {
			if (old.playingDir == newer.playingDir) {
				if (old.songPlaying == newer.songPlaying) {
					if (old.pbPercentage.getPercentage() == newer.pbPercentage.getPercentage()) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private class CliRunnable implements Runnable {
		private Scanner mScanner = new Scanner(System.in);

		@Override
		public void run() {
			while (mShouldRun) {
				String userInput = mScanner.nextLine();
				switch (userInput) {
				case "1":
					mCtrl.setRandom(!mCtrl.getRandom());
					break;
				case "2":
					mCtrl.setRepeat(!mCtrl.getRepeat());
					break;
				case "3":
					mRecursive = !mRecursive;
					break;
				case "s":
					mCtrl.stop();
					break;
				case "n":
					mCtrl.nextTrack();
					break;
				case "x":
					stop();
					mCtrl.exitApp(0);
					break;
				case "d":
					handleDirCmd(mCtrl.getCurrentDir());
					break;
				default:
					// User entered unknown command
					break;
				}

				// Clear out out whatever the user typed in, albeit a valid or invalid command.
				// And trigger a full refresh without waiting for the next meaningful playback
				// status update
				mLastPrintedSettings = null;
				printStatus(SettingsChanged.blank(), true, mRecursive);

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		private void handleDirCmd(Path curPath) {
			// stop printing normal song updates
			mPausePrintingStatus.set(true);

			List<Path> subDirs = mCtrl.getAvailableDirs(curPath);

			System.out.print(ANSI_ERASE_SCREEN);
			System.out.print(ANSI_ROW1_LEFT);

			System.out.println("-- " + curPath.toString() + " --");
			System.out.println("q - ../");

			String options = "wertasdfgzxcvb";
			int max = Math.min(subDirs.size(), options.length());
			for (int i = 0; i < max; i++) {
				System.out.println(options.charAt(i) + " - " + subDirs.get(i).getFileName().toString());
			}
			
			if(subDirs.size()>options.length()) {
				// let the user know we have cut off the number of directories we are showing
				System.out.println("    ...");
			}

			System.out.println("n - Type it in yourself");

			String userInput = mScanner.nextLine();
			Path selectedPath = null;
			switch (userInput) {
			case "q":
				selectedPath = curPath.getParent();
				break;
			case "w":
				selectedPath = subDirs.get(0);
				break;
			case "e":
				selectedPath = subDirs.get(1);
				break;
			case "r":
				selectedPath = subDirs.get(2);
				break;
			case "t":
				selectedPath = subDirs.get(3);
				break;
			case "a":
				selectedPath = subDirs.get(4);
				break;
			case "s":
				selectedPath = subDirs.get(5);
				break;
			case "d":
				selectedPath = subDirs.get(6);
				break;
			case "f":
				selectedPath = subDirs.get(7);
				break;
			case "g":
				selectedPath = subDirs.get(8);
				break;
			case "z":
				selectedPath = subDirs.get(9);
				break;
			case "x":
				selectedPath = subDirs.get(10);
				break;
			case "c":
				selectedPath = subDirs.get(11);
				break;
			case "v":
				selectedPath = subDirs.get(12);
				break;
			case "b":
				selectedPath = subDirs.get(13);
				break;
			case "n":
				System.out.print(ANSI_ERASE_SCREEN);
				System.out.print(ANSI_ROW1_LEFT);
				System.out.println("Current = " + curPath.toString());
				System.out.print("Next = ");
				String manualDir = mScanner.nextLine();
				selectedPath = Paths.get(manualDir);
				break;
			default:
				break;
			}

			mPausePrintingStatus.set(false);
			if (null != selectedPath) {
				mCtrl.playDir(selectedPath, false);
			}
		}
	}
}
