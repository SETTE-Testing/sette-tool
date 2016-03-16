<#

.SYNOPSIS
Runs the evaluation tasks on all tools for the 30sec results.

#>

[CmdletBinding()]

Param(
  [int] $From = 1,
  [int] $To = 10,
  [string[]] $Tools = @("catg", "evosuite", "jpet", "randoop", "spf"),
  [string[]] $Tasks = @("parser", "test-generator", "test-runner", "export-csv"),
  [string] $JavaHeapMemory = "4G",
  [boolean] $SkipExisting = $true,
  [boolean] $ExportCsvBatch = $true
)

$SNIPPET_PROJECT = "sette-snippets"
$SNIPPET_PROJECT_DIR = "sette-snippets/java/sette-snippets"
$LOG_DIR = "explog"

$tags = @()
for ($i = $From; $i -le $To; $i++) {  
    $tags += "run-{0:D2}-30sec" -f $i
}

mkdir $LOG_DIR -f > $null

foreach ($tool in $Tools) {
    foreach ($tag in $tags) {
		$dir = "${SNIPPET_PROJECT}___${tool}___${tag}"
		
		if ($SkipExisting -and (Test-Path "../sette-results/$dir/sette-evaluation.csv")) {
            Write-Warning "Skipping $tool $tag"
        } else {
			$i = 3
			foreach ($task in $tasks) {
				Write-Progress -Activity $tool -Status $tag -CurrentOperation $task
				java "-Xmx$JavaHeapMemory" -jar sette-all.jar --snippet-project-dir $SNIPPET_PROJECT_DIR --tool $tool --task $task --runner-project-tag $tag > "$LOG_DIR/${tool}_${tag}_${i}_${task}.log" 2>&1
				$i++			
			}
        }
    }
}

if ($ExportCsvBatch){
	$tag = $tags -join ','
	java -jar sette-all.jar --snippet-project-dir $SNIPPET_PROJECT_DIR --task "export-csv-batch" --runner-project-tag $tag
}
