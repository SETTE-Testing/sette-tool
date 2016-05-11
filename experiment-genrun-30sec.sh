#!/bin/bash 
# Usage: ./experiment-genrun-30sec.sh TOOL FROM TO
# Example: ./experiment-genrun-30sec.sh randoop 5 7
# It will generate runs with 30 second timeout numbered from 05 to 07
TIMEOUT="30s"
TOOL=$1
FROM=$2
TO=$3
PROJ=sette-snippets/java/sette-snippets-core
projectName="$(sed -r -e 's#.*/##g' <<< $PROJ)"

echo "$TOOL from $FROM to $TO"

set -v
for i in `seq $FROM $TO`;
do
    TAG=run-$(printf "%02d" $i)-30sec
	echo == Execution $i generator
	./run-sette.sh --snippet-project-dir $PROJ --tool $TOOL --task generator --runner-project-tag $TAG --backup SKIP --runner-timeout $TIMEOUT | tee sette___${TOOL}___${TAG}___${projectName}___generator.log
	echo == Execution $i runner
	./run-sette.sh --snippet-project-dir $PROJ --tool $TOOL --task runner    --runner-project-tag $TAG --backup SKIP --runner-timeout $TIMEOUT | tee sette___${TOOL}___${TAG}___${projectName}___runner.log
done

echo Finished

