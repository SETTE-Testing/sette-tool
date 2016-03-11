<#

.SYNOPSIS
Runs the evaluation tasks on all tools for the 30sec results.

#>

[CmdletBinding()]

Param(
  [int] $From = 1,
  [int] $To = 10,
  [string] $JavaHeapMemory = "4G"
)

$SNIPPET_PROJECT = "sette-snippets"
$SNIPPET_PROJECT_DIR = "sette-snippets/java/sette-snippets"
$LOG_DIR = "explog"

$tools = @("catg", "evosuite", "jpet", "randoop", "spf")

$tags = @()
for ($i = $From; $i -le $To; $i++) {  
    $tags += "run-{0:D2}-30sec" -f $i
}

$tasks = @("parser", "test-generator", "test-runner", "export-csv")

mkdir $LOG_DIR -f > $null

foreach ($tool in $tools) {
    foreach ($tag in $tags) {
        $i = 3
        foreach ($task in $tasks) {
            echo "$tool $tag $task ..."
            java "-Xmx$JavaHeapMemory" -jar sette-all.jar --snippet-project-dir $SNIPPET_PROJECT_DIR --tool $tool --task $task --runner-project-tag $tag > "$LOG_DIR/${tool}_${tag}_${i}_${task}.log" 2>&1
            $i++
        }
    }
}

$tag = $tags -join ','
java -jar sette-all.jar --snippet-project-dir $SNIPPET_PROJECT_DIR --task "export-csv-batch" --runner-project-tag $tag
