<#

.SYNOPSIS
Processes the output from OpenCover coverage report

#>

[CmdletBinding()]

Param(
  [string] [Parameter(Mandatory=$true)] $CoverageReport,
  [string] $OutputFileName = "sette-coverage.csv"
)

[xml] $report = Get-Content $CoverageReport

$s = 0

$coverages = @()

$report.CoverageSession.Modules.Module | % {
	if ($_.ModuleName -eq "sette-snippets" -or $_.ModuleName -eq "sette-snippets-external"){
		# There are two sette-snippets and sette-snippets-external modules, the one in the TestResult folder are instrumented
		if ($_.ModulePath -like "*TestResult*") {
			Write-Verbose "-- $($_.ModuleName)"
			$_.Classes.Class | ? {$_.FullName -like "BME*" -and -not ($_.FullName -like "*/*")} |  % {
                $class = ($_.FullName.Split("."))[-1]
                $cat = $class.Split("_")[0]
				Write-Verbose "  -- $class"
				$_.Methods.Method | % {
					$m = $_.Name.Split("::")[2].Split("(")[0]
					if ($m -ne ".cctor" -and $m -ne ".ctor") {
						Write-Verbose "    -- $m : $($_.sequenceCoverage)"
                        $s++
                                                
                        $coverages += [pscustomobject]@{
                            Snippet = $cat + "_" + $m;
                            Coverage = $_.sequenceCoverage
                        }
					}
				}
			}
		}
	}
}

Write-Verbose "Snippets: $s"

$coverages | Sort-Object -Property Snippet | Export-Csv -Path $OutputFileName -NoTypeInformation -Encoding ASCII -Delimiter ","