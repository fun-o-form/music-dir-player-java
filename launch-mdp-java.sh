#!/bin/sh

# -XX:+UseZGC -XX:+ZGenerational <== Uses a garbage collector that minimizes stop the world time. Avoids microskips in songs due to garbage collection

# check if we are on java21+ where generational garbage collection is supported
java -XX:+UseZGC -XX:+ZGenerational --version > /dev/null 2>&1
if [ $? -eq 0 ]; then
	# java 21 or newer, use specialized garbage collector
	java -XX:+UseZGC -XX:+ZGenerational -jar ./mdp*.jar "$@"
else
	# less than 21, use default garbage collector
	java -jar ./mdp*.jar "$@"
fi

