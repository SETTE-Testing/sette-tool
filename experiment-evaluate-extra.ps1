<#

.SYNOPSIS
Runs the evaluation tasks on hard-coded extra snippet results.

#>

[CmdletBinding()]

Param(
  [boolean] $SkipExisting = $true,
  [switch] $MergeCsv,
  [string] $JavaHeapMemory = "4G",
  [string] $AntOptions = "-Xmx4g",
  [string] $SnippetSelector = ".+",
  [string[]] $SelectedTasks
)

$SNIPPET_PROJECT = "sette-snippets-extra"
$SNIPPET_PROJECT_DIR = "sette-snippets/java/sette-snippets-extra"
$LOG_DIR = "explog"

$Env:ANT_OPTS = $AntOptions

$tasks = @{"3" = "parser"; "4" =  "test-generator"; "5" = "test-runner"; "6" = "export-csv"}

if ($SelectedTasks) {
    $allTasks = $tasks
    $tasks = @{}

    foreach ($stask in $SelectedTasks) {
        if ($stask -and $allTasks.Values -notcontains $stask) {
            $Host.UI.WriteErrorLine("Unknown task $stask")
            exit 1
        }
    }
    
    foreach ($taskNum in $allTasks.Keys | Sort-Object) {
        $task = $allTasks[$taskNum]
        if ($SelectedTasks -contains $task) {
            $tasks[$taskNum] = $task
        }
    }
}

$targets = @{
    "catg" = @{
        "1|30sec" = (1..10);
    };
    "evosuite" = @{
        "1|30sec" = (1..10);
    };
    "jpet" = @{
        "1|30sec" = (1..10);
    };
    "randoop" = @{
        "1|30sec" = (1..10);
    };
    "spf" = @{
        "1|30sec" = (1..10);
    }
}

mkdir "$LOG_DIR" -f > $null
mkdir "$LOG_DIR/$SNIPPET_PROJECT" -f > $null

$csvs = @()

foreach ($tool in $targets.Keys | Sort-Object) {
    foreach($timeKey in $targets[$tool].Keys | Sort-Object) {
        $time = $timeKey.Split("|")[1]
        
        foreach($num in $targets[$tool][$timeKey]) {
            $num = "{0:D2}" -f $num
            $tag = "run-${num}-${time}"
            $dir = "${SNIPPET_PROJECT}___${tool}___${tag}"

            if (!(Test-Path "../sette-results/$dir")) {
                Write-Warning "Missing $dir"
                continue
            }

            $csvs += "../sette-results/$dir/sette-evaluation.csv"

            if ( $SkipExisting -and (Test-Path "../sette-results/$dir/sette-evaluation.csv")) {
                Write-Output "Skipping $tool $tag"
            } else {
                foreach ($taskNum in $tasks.Keys | Sort-Object) {
                    $task = $tasks[$taskNum]

                    Write-Progress -Activity "$SNIPPET_PROJECT $tool" -Status $tag -CurrentOperation $task
                    java "-Xmx$JavaHeapMemory" -jar sette-all.jar --snippet-project-dir $SNIPPET_PROJECT_DIR --tool $tool --task $task --runner-project-tag $tag --snippet-selector $SnippetSelector 2>&1 | % {"$_"} | Out-File "$LOG_DIR/$SNIPPET_PROJECT/${tool}_${tag}_${taskNum}_${task}.log"
                }
            }
        }
    }
}

if ($MergeCsv) {
    Write-Progress "Merging csvs"
    $mergedCsv = @()
    
    foreach ($csv in $csvs) {
        $mergedCsv += Import-Csv $csv
    }
    
    $mergedCsv | Export-Csv "snippets-extra.csv" -NoTypeInformation
}