#!/bin/bash
# This script builds SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

ant -f "$CWD/src/sette/build.xml"
echo "=> Don't forget to properly set up sette.properties"
