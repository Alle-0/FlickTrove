import sys
import re

file_path = "app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Trova tutte le occorrenze di hazeGlass e assicurati che abbiano alpha = alpha
# Sostituiamo `state = hazeState,` con `state = hazeState, alpha = alpha,`
new_content = re.sub(r'state = (hazeState|localHazeState),', r'state = \1, alpha = alpha,', content)

# Check if there are double alpha = alpha
new_content = new_content.replace('alpha = alpha, alpha = alpha,', 'alpha = alpha,')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)
