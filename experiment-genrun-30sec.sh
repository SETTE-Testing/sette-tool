#!/bin/bash 
# Usage: ./experiment-genrun-30sec.sh TOOL FROM TO
# Example: ./experiment-genrun-30sec.sh randoop 5 7
# It will generate runs with 30 second timeout numbered from 05 to 07
TIMEOUT=30
TOOL=$1
FROM=$2
TO=$3

echo "$TOOL from $FROM to $TO"

set -v
for i in `seq $FROM $TO`;
do
	echo == Execution $i generator
	./run-sette.sh --tool $TOOL --task generator --runner-project-tag run-$(printf "%02d" $i)-30sec --skip-backup --runner-timeout $TIMEOUT
	echo == Execution $i runner
	./run-sette.sh --tool $TOOL --task runner    --runner-project-tag run-$(printf "%02d" $i)-30sec --skip-backup --runner-timeout $TIMEOUT
done

echo Finished

