$langs = @{
    "values" = "Have a complex idea or want to discuss the roadmap? Join r/FlickTrove or open an Issue on GitHub!"
    "values-de" = "Haben Sie eine komplexe Idee oder möchten Sie die Roadmap diskutieren? Treten Sie r/FlickTrove bei oder eröffnen Sie ein Issue auf GitHub!"
    "values-es" = "¿Tienes una idea compleja o quieres discutir la hoja de ruta? ¡Únete a r/FlickTrove o abre un Issue en GitHub!"
    "values-fr" = "Vous avez une idée complexe ou vous souhaitez discuter de la feuille de route ? Rejoignez r/FlickTrove ou ouvrez une Issue sur GitHub !"
    "values-hi" = "क्या आपके पास कोई जटिल विचार है या आप रोडमैप पर चर्चा करना चाहते हैं? r/FlickTrove से जुड़ें या GitHub पर एक Issue खोलें!"
    "values-it" = "Hai un\'idea complessa o vuoi discutere la roadmap? Unisciti a r/FlickTrove o apri una Issue su GitHub!"
    "values-pt-rBR" = "Tem uma ideia complexa ou quer discutir o roteiro? Junte-se ao r/FlickTrove ou abra uma Issue no GitHub!"
    "values-ru" = "Есть сложная идея или хотите обсудить дорожную карту? Присоединяйтесь к r/FlickTrove или откройте Issue на GitHub!"
}

foreach ($lang in $langs.Keys) {
    $path = "c:\Progetti\FlickTrove_Kotlin\app\src\main\res\$lang\strings.xml"
    $content = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
    
    # Remove import_rewatch strings
    $content = $content -replace '(?s)\s*<string name="import_rewatch_dialog_title">.*?</string>', ''
    $content = $content -replace '(?s)\s*<string name="import_rewatch_dialog_desc">.*?</string>', ''
    $content = $content -replace '(?s)\s*<string name="import_rewatch_keep_latest">.*?</string>', ''
    $content = $content -replace '(?s)\s*<string name="import_rewatch_keep_first">.*?</string>', ''
    
    # Insert new string
    $text = $langs[$lang]
    $newLine = "    <string name="feedback_roadmap_note">$text</string>
</resources>"
    $content = $content -replace '</resources>', $newLine
    
    [System.IO.File]::WriteAllText($path, $content, [System.Text.Encoding]::UTF8)
}
