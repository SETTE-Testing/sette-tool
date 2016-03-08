#!/bin/bash
# This script downloads EvoSuite.

VERSION=1.0.3
CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

rm -f "$CWD/evosuite.jar"
wget "https://github.com/EvoSuite/evosuite/releases/download/v$VERSION/evosuite-$VERSION.jar" -O "$CWD/evosuite.jar"
chmod +x "$CWD/evosuite.jar"

echo "$VERSION" > "$CWD/VERSION"
