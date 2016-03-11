#!/bin/bash 
# Usage: ./experiment-genrun-time.sh TOOL FROM TO TIMEOUT
# Example: ./experiment-genrun-time.sh randoop 5 7 60s
# It will generate runs with 60 second timeout numbered from 05 to 07
TIMEOUT=$4
TOOL=$1
FROM=$2
TO=$3
PROJ=sette-snippets/java/sette-snippets-performance-time
projectName="$(sed -r -e 's#.*/##g' <<< $PROJ)"

echo "$TOOL from $FROM to $TO with $TIMEOUT on $PROJ"

set -v
for i in `seq $FROM $TO`;
do
    TAG=run-$(printf "%02d" $i)-${TIMEOUT}ec
	echo == Execution $i generator
	./run-sette.sh --snippet-project-dir $PROJ --tool $TOOL --task generator --runner-project-tag $TAG --backup SKIP --runner-timeout $TIMEOUT | tee sette___${TOOL}___${TAG}___${projectName}___${TIMEOUT}___generator.log
	echo == Execution $i runner
	./run-sette.sh --snippet-project-dir $PROJ --tool $TOOL --task runner    --runner-project-tag $TAG --backup SKIP --runner-timeout $TIMEOUT | tee sette___${TOOL}___${TAG}___${projectName}___${TIMEOUT}___runner.log
done

echo Finished

