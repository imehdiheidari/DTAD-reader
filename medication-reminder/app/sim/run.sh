#!/bin/bash
# Runs the real Alarms.java on a desktop JVM with stand-in Android classes (stubs/) and walks one dose through
# due -> notification -> Later -> repeat -> notification #2 -> Done. Usage: ./run.sh [fa|en|de] [minutes between Later reminders]
set -e
cd "$(dirname "$0")"
OUT=$(mktemp -d); JSON=$OUT/json.jar
curl -sSfL -o $JSON https://repo1.maven.org/maven2/org/json/json/20231013/json-20231013.jar
javac -nowarn -cp $JSON -d $OUT $(find stubs ir -name '*.java') ../src/ir/mehdi/doseclock/Alarms.java
java -Dstdout.encoding=UTF-8 -cp $OUT:$JSON ir.mehdi.doseclock.Sim "${1:-fa}" ${2:+"$2"}
