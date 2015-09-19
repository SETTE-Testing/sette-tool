#!/bin/bash
# This script builds SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

cd "$CWD/src"
./gradlew clean build deployLocal
echo "=> Don't forget to properly set up sette.properties"
cd "$CWD"
