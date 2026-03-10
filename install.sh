#!/bin/sh

set -e

INSTALL_DIR=~/.local/share/applications/music-dir-player
echo "Installing to $INSTALL_DIR"

# The user downloaded our release package. Copy the entire directory into the user's home
# Note, the mdp.desktop file will be found by the OS even though it is in a subdir of
# ~/.local/share/applications
rm -rf $INSTALL_DIR
mkdir -p $INSTALL_DIR
cp -r ./* $INSTALL_DIR

# Copy in the icon to the standard location
cp ./mdp.png ~/.local/share/icons

echo "All done. It will show up as Music Dir Player in your application list"

if ! command -v java $> /dev/null; then
	echo "Java was not found. This is required to run the player. Install it as shown below:"
	echo "   PureOS (Debian-based): sudo apt install openjdk-17-jre"
    echo "   PostmarketOS (Alpine-based): sudo apk add openjdk25-jre"
fi