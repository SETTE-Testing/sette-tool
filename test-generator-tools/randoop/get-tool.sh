#!/bin/bash
# This script downloads Randoop.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

rm -f "$CWD/randoop.jar"
wget "https://randoop.googlecode.com/files/randoop.1.3.4.jar" -O "$CWD/randoop.jar"
chmod +x "$CWD/randoop.jar"

echo "1.3.4" > "$CWD/VERSION"
