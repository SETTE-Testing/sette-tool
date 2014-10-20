#!/bin/bash
# This script downloads CATG (janala2 v1.03) and makes the tool ready to be used by SETTE.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

# remove previous tool
rm -R -f "$CWD/tool"

# download
wget "https://github.com/ksen007/janala2/archive/v1.03.tar.gz" -O "$CWD/catg-v1.03.tar.gz"

# unpack, rename, delete archive
tar xfz "$CWD/catg-v1.03.tar.gz" -C "$CWD"
mv "$CWD/janala2-1.03" "$CWD/tool"
rm -f "$CWD/catg-v1.03.tar.gz"

# remove unnecessary files
rm -R -f "$CWD/tool/.idea"
rm -f "$CWD/tool/concolic.cygwin"
rm -f "$CWD/tool/Janala.iml"
rm -f "$CWD/tool/janala.pptx"
rm -f "$CWD/tool/testall"
rm -f "$CWD/tool/lib/iagent.jar"
rm -R -f "$CWD/tool/src/database"
rm -R -f "$CWD/tool/src/tests"
rm -R -f "$CWD/tool/tests"
rm -R -f "$CWD/tool/testdata"

echo "1.03" > "$CWD/VERSION"

# patch build.xml (it is needed by SETTE)
patch -N --no-backup-if-mismatch -r - "$CWD/tool/build.xml" "$CWD/build.xml.diff"
