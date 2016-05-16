<#
.SYNOPSIS
  Runs the selected evaluation tasks on the selected tools for the selected 
results.

.DESCRIPTION
  This script runs the selected evaluation tasks on the selected tools for 
  the selected results or the selected project.

.EXAMPLE
  Run complete evaluation for all tools for core snippets for 10 runs with 
  30 and 60 second timeouts and merge CSV:
    .\experiment-evaluate.ps1 -Project core -Timeouts (30) -MergeCsv
    Covers: sette-snippets-core___[catg|evosuite|jpet|randoop|spf]___run[01-10]-[30|60]sec

.EXAMPLE
  Run parser for EvoSuite for extra snippets for runs 2-4 with 60 sec timeout:
    .\experiment-evaluate.ps1 -Project extra -Runs (2..4) -Tasks ("test-runner", "export-csv") -Tools "randoop" -Timeouts (30,60)
    Covers: sette-snippets-extra_randoop___run[01-10]-[30|60]sec and merges CSV
#>
[CmdletBinding()]

Param(
  [ValidateSet('core', 'performance-time', 'extra')] [string] $Project,
  [int[]] $Timeouts,
  [int[]] $Runs = (1..10),
  [string[]] $Tools = @("catg", "evosuite", "jpet", "randoop", "spf"),
  [string[]] $Tasks,
  [boolean] $SkipExisting = $true,
  [switch] $MergeCsv,
  [switch] $DryRun,
  [string] $JavaHeapMemory = "4G",
  [string] $AntOptions = "-Xmx4g",
  [string] $SnippetSelector = ".+"
)

$SNIPPET_PROJECT = "sette-snippets-$Project"
$SNIPPET_PROJECT_DIR = "sette-snippets/java/$SNIPPET_PROJECT"
$LOG_DIR = "experiment-log"
$Env:ANT_OPTS = $AntOptions

$AllTasks = @{"3" = "parser"; "4" =  "test-generator"; "5" = "test-runner"; "6" = "export-csv"}

if ($Tasks) {
    $SelectedTasks = @{}

    foreach ($task in $Tasks) {
        if ($task -and $AllTasks.Values -notcontains $task) {
            $Host.UI.WriteErrorLine("Unknown task: $task")
            exit 1
        }
    }

    foreach ($taskNum in $AllTasks.Keys | Sort-Object) {
        $task = $AllTasks[$taskNum]
        if ($Tasks -contains $task) {
            $SelectedTasks[$taskNum] = $task
        }
    }
} else {
    $SelectedTasks = $AllTasks
}

Write-Output "Project: $SNIPPET_PROJECT"

mkdir $LOG_DIR -f > $null
mkdir "$LOG_DIR/$SNIPPET_PROJECT" -f > $null

$tags = @()
foreach ($timeout in $Timeouts) {
    foreach ($run in $Runs) {
        $tags += "run-{0:D2}-${timeout}sec" -f $run
    }
}

$csvs = @()

foreach ($tool in $Tools) {
    foreach ($tag in $tags) {
        $runnerProject = "${SNIPPET_PROJECT}___${tool}___${tag}"
        $dir = "../sette-results/$runnerProject"
        $csvFile = "$dir/sette-evaluation.csv"

        if (!(Test-Path $dir) -and $false) {
            Write-Warning "Missing $runnerProject"
            continue
        }

        if ($SkipExisting -and (Test-Path $csvFile)) {
            Write-Output "Skipping $tool $tag"
        } else {
            foreach ($taskNum in $SelectedTasks.Keys | Sort-Object) {
                $task = $SelectedTasks[$taskNum]

                if ($DryRun) {
                    Write-Output "${tool}: $taskNum $task for $tag"
                } else {
                    Write-Progress -Activity $tool -Status $tag -CurrentOperation $task
                    $logFile = "$LOG_DIR/$SNIPPET_PROJECT/${tool}_${tag}_${taskNum}_${task}.log"
                    java "-Xmx$JavaHeapMemory" -jar sette-all.jar `
                        --snippet-project-dir $SNIPPET_PROJECT_DIR `
                        --tool $tool --task $task --runner-project-tag $tag `
                        --snippet-selector $SnippetSelector 2>&1 | % {"$_"} | Out-File $logFile
                }
            }
            $csvs += $csvFile
        }
    }
}

if ($MergeCsv -and !$DryRun){
    Write-Progress "Merging csvs"
    $mergedCsv = @()

    foreach ($csv in $csvs) {
        $mergedCsv += Import-Csv $csv
    }

    $mergedCsv | Export-Csv "merged-${SNIPPET_PROJECT}.csv" -NoTypeInformation
}
