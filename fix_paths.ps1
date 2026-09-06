$htmlFiles = Get-ChildItem -Path .\docs\*.html
foreach ($file in $htmlFiles) {
    (Get-Content $file.FullName) -replace 'src="/assets/', 'src="./assets/' -replace 'href="/assets/', 'href="./assets/' | Set-Content $file.FullName
}

$cssFiles = Get-ChildItem -Path .\docs\*.css
foreach ($file in $cssFiles) {
    (Get-Content $file.FullName) -replace 'url\("/assets/', 'url("./assets/' -replace 'url\(''/assets/', 'url(''./assets/' -replace 'url\(/assets/', 'url(./assets/' | Set-Content $file.FullName
}
