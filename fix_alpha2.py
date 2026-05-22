import sys

file_path = "app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

target = 'val alpha by transition.animateFloat(label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }'
replacement = 'val alpha by transition.animateFloat(transitionSpec = { tween(200) }, label = "blurAlpha") { if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }'

new_content = content.replace(target, replacement)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)
