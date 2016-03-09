#!/bin/bash
# This script builds SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

echo "Deleting sette.config.json"
rm -rf "$CWD/sette.config.json"
cd "$CWD/src"

echo "Building SETTE (without tests)"
./gradlew clean build deployLocal -x test
