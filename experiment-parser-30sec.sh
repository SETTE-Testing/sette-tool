#!/bin/bash 
# Usage: ./experiment-parser-30sec.sh TOOL FROM TO
# Example: ./experiment-parser-30sec.sh randoop 5 7
# It will execute the parser task for results with 30 second tiemout numbered from 05 to 07
TIMEOUT=30
TOOL=$1
FROM=$2
TO=$3

echo "$TOOL from $FROM to $TO"

set -v
for i in `seq $FROM $TO`;
do
    TAG=run-$(printf "%02d" $i)-30sec
	echo == Execution $i parser
	./run-sette.sh --tool $TOOL --task parser --runner-project-tag $TAG --skip-backup --runner-timeout $TIMEOUT 2>&1 | tee sette___${TOOL}___${TAG}___parser.log
done

echo Finished

