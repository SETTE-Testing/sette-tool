#!/bin/bash 
# Usage: ./experiment-genrun-time.sh TOOL FROM TO TIMEOUT
# Example: ./experiment-genrun-time.sh randoop 5 7 60
# It will generate runs with 60 second timeout numbered from 05 to 07
TIMEOUT=$4
TOOL=$1
FROM=$2
TO=$3
PROJ=sette-snippets-performance-time

echo "$TOOL from $FROM to $TO with $TIMEOUT on $PROJ"

set -v
for i in `seq $FROM $TO`;
do
    TAG=run-$(printf "%02d" $i)-${TIMEOUT}sec
	echo == Execution $i generator
	./run-sette.sh --snippet-project $PROJ --tool $TOOL --task generator --runner-project-tag $TAG --skip-backup --runner-timeout $TIMEOUT | tee sette___${TOOL}___${TAG}___${PROJ}___${TIMEOUT}___generator.log
	echo == Execution $i runner
	./run-sette.sh --snippet-project $PROJ --tool $TOOL --task runner    --runner-project-tag $TAG --skip-backup --runner-timeout $TIMEOUT | tee sette___${TOOL}___${TAG}___${PROJ}___${TIMEOUT}___runner.log
done

echo Finished

