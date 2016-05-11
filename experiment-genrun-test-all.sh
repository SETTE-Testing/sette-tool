#!/bin/bash 
# Usage: ./experiment-genrun-test-all.sh
# It will run all tools with a "test" tag without backing up for the sette-snippets-core project
TOOLS=(catg evosuite jpet randoop spf)
PROJ=sette-snippets/java/sette-snippets-core
projectName="$(sed -r -e 's#.*/##g' <<< $PROJ)"
TAG=test

set -v
for tool in ${TOOLS[@]};
do
	echo == Execution $tool generator
	./run-sette.sh --snippet-project-dir $PROJ --tool $tool --task generator --runner-project-tag $TAG --backup SKIP | tee sette___${tool}___${TAG}___${projectName}___generator.log
	echo == Execution $tool runner
	./run-sette.sh --snippet-project-dir $PROJ --tool $tool --task runner    --runner-project-tag $TAG --backup SKIP | tee sette___${tool}___${TAG}___${projectName}___runner.log
done

echo Finished

