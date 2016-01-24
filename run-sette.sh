#!/bin/bash
# This script starts SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

cd "$CWD"
java -jar "$CWD/sette-all.jar" "$@"
