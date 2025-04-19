# Function Over Form (Fun-O-Form) Music Directory Player - Java

This application plays local music files. It does not read MP3 tags nor does it create database for searching your collection. Instead it does only one thing, plays all the music files in whatever directory you specify. You may have it play the files in the current directory (not recursive) or include all the music files in any subdirectories (recrusive).

## Fun-o-form Approach
* Minimal dependencies
* Minimal hardware requirements
* Prioritize functionality over asthetics

## Features
1. A command line user interface
2. A graphical user interface (eventualy)
3. Control via MPRIS DBus - allows controlling the player through many Linux built-in media controls

## Target Platform
This application is written specfically for the **Librem 5** Linux phone running PureOS / Phosh. Let's face it, if you have a keyboard+mouse and a big screen, there are numerous other local media players you could use.

As a Java application this will run on a variety of platforms not just the Librem 5. This will probably work fine on Mac or Windows. But it is not tested on those platforms. Here are some considerations for using non-Linux platforms.

1. The CLI utilizes ANSI escape sequences to update text in place. Windows consoles typically don't support these escape sequences (aside from mingwin) thus the CLI would be very annoying to use on Windows.

## Related Projects
1. Music Dir Player (Rust) - This Java application was also written as a RUST application. The expectation is the Rust application will provide the same capability while consuming fewer system resources. However I am learning Rust as I go and progress has been slow. I developed this Java application to utilize while I keep developing the Rust version. As a benefit, this allows conducting an apples-to-apples comparison of the two apps' overall performance.

## Installing on Librem5
1. sudo apt install openjdk-17-jre (uses ~200Mb of disk space)
2. Copy jar to your phone
3. Run with `java -jar ./...`

## Open Source usage

| What | How Used |
| -- | -- |
| (Java Stream Player (Library))[https://github.com/goxr3plus/java-stream-player/tree/master] | The backend for playing music files. | 
| (Stencil Media Controls Icons)[https://icons8.com/icons/set/media-controls--style-stencil] | Icons used on the GUI. |