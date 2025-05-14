#!/bin/sh

# -XX:+UseZGC -XX:+ZGenerational <== Uses a garbage collector that minimizes stop the world time. Avoids microskips in songs due to garbage collection
java -XX:+UseZGC -XX:+ZGenerational -jar ./mdp*.jar