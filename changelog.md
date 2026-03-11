# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-03-10
### Fixed
- Previously you couldn't use the directory browser to go all the way to the root directory `/`. Now you can.
- Fixed issues listing songs in the root directory `/`.
- Fixed issue where config changes were not saved on mobile platform. Previously we saved prefs during exit, but on Phosh, the app dies without nicely exiting. So now we save prefs immediately upon value change.
- Fixed issue where playing a directory via the CLI would not show cause the queued songs to display on the GUI.
### Added
- Added the `maxListFilesWaitTimeSec` config option. When finding playable songs in a directory, once the scan reaches `maxListFilesWaitTimeSec` seconds, it will stop and return whatever it found so far. 
  - This is particularly important on large or network mounted directories.
  - This option defaults to 10 seconds but can be set to 1 - 600 seconds from the options dialog.
- You can now choose whether the the directory played at startup is played recursively or not.
- At startup the app now checks if you are trying to play root recursively. If so, it does you a favor and turns off recursion.
- install.sh script now reminds you to install java if it is not found on the system.
- Command line argument now controls whether you run the GUI, CLI, or both. `--gui` = run only GUI. `--both` = run both. Specify nothing to get only the CLI.