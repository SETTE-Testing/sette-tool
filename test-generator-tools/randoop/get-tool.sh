#!/bin/bash
# This script downloads Randoop.
VERSION=2.1.0
CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

rm -f "$CWD/randoop.jar"
wget "https://github.com/mernst/randoop/releases/download/v$VERSION/randoop-$VERSION.jar" -O "$CWD/randoop.jar"
chmod +x "$CWD/randoop.jar"

echo "$VERSION" > "$CWD/VERSION"
