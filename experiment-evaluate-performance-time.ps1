[CmdletBinding()]

Param(
  [switch] $MergeCsv
)

$SNIPPET_PROJECT = "sette-snippets-performance-time"
$SNIPPET_PROJECT_DIR = "sette-snippets/java/sette-snippets-performance-time"
$LOG_DIR = "explog"

$tasks = @{"3" = "parser"; "4" =  "test-generator"; "5" = "test-runner"; "6" = "export-csv"}

$targets = @{
    "catg" = @{
        "1|15sec" = (1..10);
        "2|45sec" = (1..10);
        "3|60sec" = (1..10);
        "4|120sec" = (1..1);
        "5|180sec" = (1..1);
        "6|300sec" = (1..10);
    };
    "evosuite" = @{
        "1|15sec" = (1..10);
        "2|45sec" = (1..10);
        "3|60sec" = (1..10);
        "4|120sec" = (1..1);
        "5|180sec" = (1..1);
        "6|300sec" = (1..10);
    };
    "randoop" = @{
        "1|15sec" = (1..10);
        "2|45sec" = (1..10);
        "3|60sec" = (1..10);
        "4|120sec" = (1..1);
        "5|180sec" = (1..1);
        "6|300sec" = (1..10);
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

            if (Test-Path "../sette-results/$dir/sette-evaluation.csv") {
                Write-Verbose "Skipping $tool $tag"
            } else {
                foreach ($taskNum in $tasks.Keys | Sort-Object) {
                    $task = $tasks[$taskNum]

                    Write-Verbose "$SNIPPET_PROJECT $tool $tag $task ..."
                    if (! $WhatIfPreference) {
                        java -Xmx4G -jar sette-all.jar --snippet-project $SNIPPET_PROJECT_DIR --tool $tool --task $task --runner-project-tag $tag > "$LOG_DIR/$SNIPPET_PROJECT/${tool}_${tag}_${taskNum}_${task}.log" 2>&1
                    } else {
						echo "java -Xmx4G -jar sette-all.jar --snippet-project $SNIPPET_PROJECT_DIR --tool $tool --task $task --runner-project-tag $tag > $LOG_DIR/$SNIPPET_PROJECT/${tool}_${tag}_${taskNum}_${task}.log 2>&1"
                    }                    
                }
            }
        }
    }
}

if ($MergeCsv) {
	Write-Verbose "Merging csvs"
	$mergedCsv = @()

	foreach ($csv in $csvs) {
		$mergedCsv += Import-Csv $csv
	}

	$mergedCsv | Export-Csv "performance-time.csv"
}