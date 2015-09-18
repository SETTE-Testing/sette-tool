#!/bin/bash
# This script downloads sets up the two repositories required by JPF/SPF (jpf-core and jpf-symbc) and sets up JPF/SPF. In addition, this script also updates the repositories and builds the tool.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

# remove previous repositories and configuration
rm -R -f "$CWD/jpf-core"
rm -R -f "$CWD/jpf-symbc"
rm -R -f ~/.jpf

# clone repositories
hg clone "http://babelfish.arc.nasa.gov/hg/jpf/jpf-core" "$CWD/jpf-core"
hg clone "http://babelfish.arc.nasa.gov/hg/jpf/jpf-symbc" "$CWD/jpf-symbc"

# configure JPF
mkdir ~/.jpf

jpfSiteConfiguration="# JPF site configuration
jpf-core = $CWD/jpf-core
jpf-symbc = $CWD/jpf-symbc

extensions=\${jpf-core},\${jpf-symbc}
"

echo "$jpfSiteConfiguration" > ~/.jpf/site.properties

echo "=> Don't forget to update and build the tool"
