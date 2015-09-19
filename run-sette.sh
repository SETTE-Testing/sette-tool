#!/bin/bash
# This script starts SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

cd "$CWD"

VMARGS=

if [ -f "$CWD/sette-log4j2.xml" ]; then
    VMARGS=-Dlog4j.configurationFile=sette-log4j2.xml
fi

java $VMARGS -jar "$CWD/sette-all.jar" "$@"

