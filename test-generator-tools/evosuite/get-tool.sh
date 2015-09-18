#!/bin/bash
# This script downloads EvoSuite.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

rm -f "$CWD/evosuite.jar"
wget "http://evosuite.org/files/evosuite-0.2.0.jar" -O "$CWD/evosuite.jar"
chmod +x "$CWD/evosuite.jar"

echo "0.2.0" > "$CWD/VERSION"
