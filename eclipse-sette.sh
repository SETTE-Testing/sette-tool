#!/bin/bash
# This script builds SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

cd "$CWD/src"
./gradlew eclipse
cd "$CWD"
