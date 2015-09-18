#!/bin/bash
# This script downloads Randoop.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

rm -f "$CWD/randoop.jar"
wget "https://github.com/mernst/randoop/releases/download/v1.3.6/randoop-1.3.6.jar" -O "$CWD/randoop.jar"
chmod +x "$CWD/randoop.jar"

echo "1.3.6" > "$CWD/VERSION"
