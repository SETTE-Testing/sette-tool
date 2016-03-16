#!/bin/bash
# This script starts SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

cd "$CWD"
export ANT_OPTS=-Xmx4g
java -jar "$CWD/sette-all.jar" "$@"
