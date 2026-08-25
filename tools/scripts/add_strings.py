import os
import re

langs = {
    "values": "Have a complex idea or want to discuss the roadmap? Join r/FlickTrove or open an Issue on GitHub!",
    "values-de": "Haben Sie eine komplexe Idee oder möchten Sie die Roadmap diskutieren? Treten Sie r/FlickTrove bei oder eröffnen Sie ein Issue auf GitHub!",
    "values-es": "¿Tienes una idea compleja o quieres discutir la hoja de ruta? ¡Únete a r/FlickTrove o abre un Issue en GitHub!",
    "values-fr": "Vous avez une idée complexe ou vous souhaitez discuter de la feuille de route ? Rejoignez r/FlickTrove ou ouvrez une Issue sur GitHub !",
    "values-hi": "क्या आपके पास कोई जटिल विचार है या आप रोडमैप पर चर्चा करना चाहते हैं? r/FlickTrove से जुड़ें या GitHub पर एक Issue खोलें!",
    "values-it": "Hai un\\'idea complessa o vuoi discutere la roadmap? Unisciti a r/FlickTrove o apri una Issue su GitHub!",
    "values-pt-rBR": "Tem uma ideia complexa ou quer discutir o roteiro? Junte-se ao r/FlickTrove ou abra uma Issue no GitHub!",
    "values-ru": "Есть сложная идея или хотите обсудить дорожную карту? Присоединяйтесь к r/FlickTrove или откройте Issue на GitHub!"
}

for lang, text in langs.items():
    path = f"app/src/main/res/{lang}/strings.xml"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    content = re.sub(r'\s*<string name="import_rewatch_dialog_title">.*?</string>', '', content, flags=re.DOTALL)
    content = re.sub(r'\s*<string name="import_rewatch_dialog_desc">.*?</string>', '', content, flags=re.DOTALL)
    content = re.sub(r'\s*<string name="import_rewatch_keep_latest">.*?</string>', '', content, flags=re.DOTALL)
    content = re.sub(r'\s*<string name="import_rewatch_keep_first">.*?</string>', '', content, flags=re.DOTALL)
    
    new_line = f'    <string name="feedback_roadmap_note">{text}</string>\n</resources>'
    content = content.replace('</resources>', new_line)
    
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
