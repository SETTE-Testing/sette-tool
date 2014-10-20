#!/bin/bash
# This script build JPF and SPF.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

ant -f "$CWD/jpf-core/build.xml"
ant -f "$CWD/jpf-symbc/build.xml"
