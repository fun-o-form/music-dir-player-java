package funoform.mdp;

import java.util.Scanner;

import funoform.mdp.Controller.SettingsListener;

public class Cli {
	private Controller mCtrl;
	private Thread mThread = new Thread(new CliRunnable());
	private boolean mShouldRun = true;

	// Colors
	private static final String ANSI_GREEN = "\u001B[32m";
	private static final String ANSI_MAGENTA = "\u001B[35m";
	private static final String ANSI_WHITE = "\u001B[37m";
	private static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
	private static final String ANSI_RESET = "\u001B[0m";
	// Cursor control. See
	// https://gist.github.com/ConnerWill/d4b6c776b509add763e17f9f113fd25b
	private static final String ANSI_ROW1_LEFT = "\u001B[1;0H";
	private static final String ANSI_ROW2_LEFT = "\u001B[2;0H";
	private static final String ANSI_ROW3_LEFT = "\u001B[3;0H";
	private static final String ANSI_ERASE_SCREEN = "\u001B[2J";
	private static final String ANSI_ERASE_LINE = "\u001B[2K";
	private static final String ANSI_SAVE_CUR_POS = "\u001B7";
	private static final String ANSI_RESTORE_CUR_POS = "\u001B8";

	// "\u001B[2K" Erases entire line but leaves cursor where it was (moves
	// repeating text right)

	public Cli(Controller ctrl) {
		warnIfNotAnsiCompatible();

		mCtrl = ctrl;
		mThread.start();

		printStatus(SettingsChanged.blank(), true);

		mCtrl.registerSettingsListener(new SettingsListener() {
			@Override
			public void settingsChanged(SettingsChanged newSettings) {
				printStatus(newSettings, false);
			}
		});
	}

	public void stop() {
		mShouldRun = false;
		System.out.print(ANSI_ERASE_SCREEN + "Goodbye");
		mThread.interrupt();
		try {
			mThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
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

	private static synchronized void printStatus(SettingsChanged newSettings, boolean redrawAll) {
		// row 1
		if (redrawAll) {
			System.out.print(ANSI_ERASE_SCREEN);
			System.out.print(ANSI_ROW1_LEFT);
			System.out.print("-------- Fun-O-Form Music Directory Player --------");
		} else {
			// Save the current cursor position so we can restore to it later. Saves from
			// interrupting the users's typing of commands
			System.out.print(ANSI_SAVE_CUR_POS);
		}

		// row 2
		System.out.print(ANSI_ROW2_LEFT + ANSI_ERASE_LINE);
		System.out.print(ANSI_BLUE_BACKGROUND + ANSI_WHITE + lastNChars(newSettings.playingDir, 50));
		System.out.print(ANSI_RESET);

		// row 3
		System.out.print(ANSI_ROW3_LEFT + ANSI_ERASE_LINE);
		System.out.print(ANSI_GREEN + String.format("[%3s%%] ", newSettings.pbPercentage.getPercentage()));
		System.out.print(ANSI_MAGENTA + lastNChars(newSettings.songPlaying, 44));
		System.out.print(ANSI_RESET);
		System.out.println("");

		// row 4-n
		if (redrawAll) {
			System.out.println("s - Stop");
			System.out.println("n - Next");
			System.out.println("x - Exit");
			System.out.println("r - Refresh CLI");
			System.out.print("cmd>");
		} else {
			// Put the cursor back where it was before we started. If the user was typing in
			// a command, they should be able to continue without their cursor jumping away
			System.out.print(ANSI_RESTORE_CUR_POS);
		}
	}
	
	private static String lastNChars(String str, int numChars) {
		if(str.length() > numChars) {
			int start = str.length() - numChars;
			return str.substring(start);
		}
		return str;
	}

	private class CliRunnable implements Runnable {
		private Scanner mScanner = new Scanner(System.in);

		@Override
		public void run() {
			while (mShouldRun) {
				String userInput = mScanner.nextLine();
				switch (userInput) {
				case "s":
					mCtrl.stop();
					break;
				case "n":
					mCtrl.nextTrack();
					break;
				case "x":
					System.exit(0);
					break;
				case "r":
					// Refresh the display. This is automatic
					break;
				default: 
					// User entered unknown command
					break;
				}
				
				// Clear out out whatever the user typed in, albeit a valid or invalid command
				printStatus(SettingsChanged.blank(), true);

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}
	}
}
