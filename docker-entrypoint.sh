#! /bin/bash

git clone https://github.com/juanmahidalgo/xssScanner.git

cd xssScanner/src

javac -cp ../lib/leveldbjni-all-1.8.jar:../lib/jsoup-1.8.3.jar:.  Main.java 

java -cp .:../lib/jsoup-1.8.3.jar:../lib/leveldbjni-all-1.8.jar Main https://xss-game.appspot.com/level1/frame 3


