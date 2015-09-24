$snippetProject = "sette-snippets"
# $snippetProject = "sette-snippets-performance-time"

$tools = @("catg", "evosuite", "jpet", "randoop", "spf")
# $tools = @("evosuite")

$from =  1
$to   = 10

$tags = @()
for ($i = $from; $i -le $to; $i++) {  
    $tags += "run-{0:D2}-30sec" -f $i
}

$tasks = @("parser", "test-generator", "test-runner", "export-csv")

mkdir explog -f > $null

foreach ($tool in $tools) {
    foreach ($tag in $tags) {
        $i = 3
        foreach ($task in $tasks) {
            echo "$tool $tag $task ..."
            java -Xmx4G -jar sette-all.jar --snippet-project $snippetProject --tool $tool --task $task --runner-project-tag $tag > "explog/${tool}_${tag}_${i}_${task}.log" 2>&1
            $i++
        }
    }
}

$tool = $tools -join ','
$tag = $tags -join ','
java -jar sette-all.jar --snippet-project $snippetProject --tool $tool --task "export-csv-batch" --runner-project-tag $tag
