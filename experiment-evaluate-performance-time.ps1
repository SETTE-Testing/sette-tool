$snippetProject = "sette-snippets-performance-time"
$simulate = $false # does not will call SETTE
#$simulate = $true

$tasks = @{"3" = "parser"; "4" =  "test-generator"; "5" = "test-runner"; "6" = "export-csv"}

$targets = @{
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

mkdir "explog" -f > $null
mkdir "explog/$snippetProject" -f > $null

$csvs = @()

foreach ($tool in $targets.Keys | Sort-Object) {
    foreach($timeKey in $targets[$tool].Keys | Sort-Object) {
        $time = $timeKey.Split("|")[1]
        
        foreach($num in $targets[$tool][$timeKey]) {
            $num = "{0:D2}" -f $num
            $tag = "run-${num}-${time}"
            $dir = "${snippetProject}___${tool}___${tag}"

            $csvs += "../sette-results/$dir/sette-evaluation.csv"

            if (!(Test-Path "../sette-results/$dir")) {
                echo "Missing $dir"
                exit
            } elseif (Test-Path "../sette-results/$dir/sette-evaluation.csv") {
                Write-Verbose "Skipping $tool $tag"
            } else {
                foreach ($taskNum in $tasks.Keys | Sort-Object) {
                    $task = $tasks[$taskNum]

                    echo "$snippetProject $tool $tag $task ..."
                    if (!$simulate) {
                        java -Xmx4G -jar sette-all.jar --snippet-project $snippetProject --tool $tool --task $task --runner-project-tag $tag > "explog/$snippetProject/${tool}_${tag}_${taskNum}_${task}.log" 2>&1
                    }
                }
            }
        }
    }
}

#$tool = $tools -join ','
#$tag = $tags -join ','
#java -jar sette-all.jar --snippet-project $snippetProject --tool $tool --task "export-csv-batch" --runner-project-tag $tag

echo "Merging csvs"
$mergedCsv = @()

foreach ($csv in $csvs) {
    $mergedCsv += Import-Csv $csv
}

$mergedCsv | Export-Csv "performance-time.csv"
