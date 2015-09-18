#!/bin/bash
# This script updates the JPF/SPF repositories.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

hg pull -u -R "$CWD/jpf-core"
hg pull -u -R "$CWD/jpf-symbc"

ID_CORE=`hg id -i "$CWD/jpf-core"`
ID_SYMBC=`hg id -i "$CWD/jpf-symbc"`

echo "${ID_SYMBC}_${ID_CORE}" > "$CWD/VERSION"

echo "=> Don't forget to build the tool"
