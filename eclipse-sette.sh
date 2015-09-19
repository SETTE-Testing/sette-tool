#!/bin/bash
# This script creates Eclipse project files for SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

cd "$CWD/src"
./gradlew eclipse
cd "$CWD"
