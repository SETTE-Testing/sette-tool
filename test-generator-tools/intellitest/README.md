# IntelliTest functionality in Visual Studio 2015

IntelliTest currently cannot be invoked programatically.
(see http://stackoverflow.com/questions/35627244/create-run-intellitests-automatically)

Currently no reporting is available (e.g. coverage, duration).

## Usage:

0. Assign a keyboard shortcut to

       EditorContextMenus.CodeWindow.CreateIntelliTest

       EditorContextMenus.CodeWindow.RunIntelliTest

1. Open a class, right click on a class name, select `Create IntelliTest`

2. Select MSTest as test framework, create first a new test project (for the next classes, use the previously created test project)
 - This will create a test file with `PexMethods` for each method in the class under test
 - Manually remove the test stub `calledFunction` in `B5a2_CallPublic` (beacuse it is not a snippet, but a helper function)

3. Modify each PexMethod attribute to add TimeOut. In PowerShell navigate inside the sette-snippets.Tests folder, then execute:

       ls *Test.cs -Recurse | % {(Get-Content $_ )| % { $_ -replace "PexMethod", "PexMethod(Timeout=30)"} | Set-Content $_ }

4. In the Solution Explorer select one of the test class files.
 - Run IntelliTest with the keyboard shortcut.
 - This will run IntelliTest on all PexMethods

5. The generated tests can be found in the .g.cs files.

6. Download OpenCover from https://github.com/OpenCover/opencover

7. Open a Visual Studio Command Prompt
 - navigate inside the `sette-snippets.Tests` folder
 - call OpenCover, it will execute the generated tests with mstest.exe and collect code coverage

         OpenCover.Console.exe -target:mstest.exe -register:user -targetargs:/testcontainer:bin\Debug\sette-snippets.Tests.dll -output:sette.coverage.xml

8. Get coverage information from OpenCover's report

       .\Process-OpenCoverReport.ps1 -CoverageReport ...

9. Use this information and the details of the generated tests to get evaluation results

       .\Process-IntelliTestProject.ps1 -TestProject ... -Run ...

10. Optionally download ReportGenerator (https://github.com/danielpalme/ReportGenerator) and create HTML report from the coverage

        ReportGenerator.exe -reports:sette.coverage.xml -targetdir:...
