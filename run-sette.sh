#!/bin/bash
# This script starts SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

cd "$CWD"

if [ -z "$ANT_OPTS" ]; then
    export ANT_OPTS=-Xmx4g
fi
echo "ANT_OPTS: $ANT_OPTS"
java -jar "$CWD/sette-all.jar" "$@"
