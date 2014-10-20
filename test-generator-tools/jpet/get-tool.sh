#!/bin/bash
# This script downloads jPET.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

rm -f "$CWD/pet"
wget "http://costa.ls.fi.upm.es/pet/pet" -O "$CWD/pet"
chmod +x "$CWD/pet"

echo "0.4" > "$CWD/VERSION"
