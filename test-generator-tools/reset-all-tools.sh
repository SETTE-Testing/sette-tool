#!/bin/bash
# This script removes old tools and redownloads them.

CWD="$(
  cd "$(dirname "$(readlink "$0" || printf %s "$0")")"
  pwd -P 
)"

# CATG, EvoSuite, jPet and Randoop only needs a download
"/$CWD/catg/get-tool.sh"
"/$CWD/evosuite/get-tool.sh"
"/$CWD/jpet/get-tool.sh"
"/$CWD/randoop/get-tool.sh"

# SPF needs a checkout, update and ant build
"$CWD/spf/get-tool.sh"
"$CWD/spf/update-tool.sh"
"$CWD/spf/build-tool.sh"
# Second exection has a shorter and faster output if the first was successful
"$CWD/spf/build-tool.sh"
