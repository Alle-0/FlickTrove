$content = Get-Content -Path "C:\Progetti\FlickTrove_Kotlin\app\src\main\java\com\cinetrack\ui\screens\SearchScreen.kt"
$range = 310..320
foreach ($i in $range) {
    $line = $content[$i-1]
    $spaces = ($line -split "[^ ]")[0].Length
    Write-Output "$i ($spaces spaces): $line"
}
