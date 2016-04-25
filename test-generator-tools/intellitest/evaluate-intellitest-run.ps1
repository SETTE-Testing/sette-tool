

param (
	[string] [Parameter(Mandatory=$true)] $Run
)

$OPENCOVER = "C:\tools\opencover\OpenCover.Console.exe"
$REPORT_GENERATOR = "C:\tools\ReportGenerator\bin\ReportGenerator.exe"
$MSTEST = "C:\Program Files\Microsoft Visual Studio 14.0\Common7\IDE\mstest.exe"

$testDir = ".\sette-results\sette-snippets.Tests-$RUN"

Push-Location $testDir

& $OPENCOVER "-target:$MSTEST" "-register:user" "-targetargs:/testcontainer:bin\Debug\sette-snippets.Tests.dll" "-output:sette-coverage.xml"

& $REPORT_GENERATOR "-reports:sette-coverage.xml" "-targetdir:coverage"

Pop-Location

.\Process-OpenCoverReport.ps1 -CoverageReport $testDir\sette-coverage.xml -OutputFileName $testDir\sette-coverage.csv

.\Process-IntelliTestProject.ps1 -TestProject $testdir -OutputFileName $testDir\sette-evaluation.csv -Run $Run -CoverageFileName $testDir\sette-coverage.csv

