#!/bin/bash 
# Usage: ./experiment-genrun-test.sh TOOL
# Example: ./experiment-genrun-test.sh randoop
# It will run the selected tool with a "test" tag without backing up for the sette-snippets-core project
tool=$1
PROJ=sette-snippets/java/sette-snippets-core
TAG=test
projectName="$(sed -r -e 's#.*/##g' <<< $PROJ)"

set -v
echo == Execution $tool generator
./run-sette.sh --snippet-project-dir $PROJ --tool $tool --task generator --runner-project-tag $TAG --backup SKIP | tee sette___${tool}___${TAG}___${projectName}___generator.log
echo == Execution $tool runner
./run-sette.sh --snippet-project-dir $PROJ --tool $tool --task runner    --runner-project-tag $TAG --backup SKIP | tee sette___${tool}___${TAG}___${projectName}___runner.log

echo Finished

