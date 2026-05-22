import re
import sys

file_path = "app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update Dialog Signatures
dialogs = ["BackupDialog", "TraktMigrationDialog", "FeedbackDialog", "ColorSelectionDialog"]

for dialog in dialogs:
    # Add alpha: Float = 1f to signature
    sig_pattern = rf"(@Composable\s+fun {dialog}\([\s\S]*?)(?=\))"
    def repl_sig(m):
        return m.group(1) + ",\n    alpha: Float = 1f"
    content = re.sub(sig_pattern, repl_sig, content)

# 2. Update hazeGlass calls inside the file
# We want to add `alpha = alpha` to hazeGlass calls inside Dialogs.
# We can just match `.hazeGlass(` and replace with `.hazeGlass(alpha = alpha, `
# But wait, not all hazeGlass calls have alpha available.
# Let's just find the specific ones in the dialogs and AnimatedVisibilities.

# It's safer to just inject `val blurAlpha by transition.animateFloat { if (it == EnterExitState.Visible) 1f else 0f }`
# inside each AnimatedVisibility block.
anim_pattern = r"(AnimatedVisibility\([\s\S]*?\) \{\n)(\s+)(?!(?:val blurAlpha|Box|ColorSelectionDialog|FeedbackDialog|BackupDialog|TraktMigrationDialog))"
# Actually regex is tricky. Let's do it procedurally.

# Let's just use regex to replace hazeGlass calls.
# We will just pass alpha=alpha to all hazeGlass inside the dialogs.
content = content.replace("hazeState = localHazeState,", "hazeState = localHazeState, alpha = alpha,")
content = content.replace("hazeState = hazeState,", "hazeState = hazeState, alpha = alpha,")

# Now inject blurAlpha in AnimatedVisibility
parts = content.split("AnimatedVisibility(")
new_content = parts[0]
for part in parts[1:]:
    if "showDeleteDialog" in part or "showCacheConfirm" in part or "showLogoutConfirm" in part or "showColorDialog" in part or "showFeedbackDialog" in part or "showBackupDialog" in part or "showTraktDialog" in part or "isBackupLoading" in part:
        # Find the first brace
        brace_idx = part.find(") {")
        if brace_idx != -1:
            insertion_idx = brace_idx + 3
            # insert blurAlpha
            indent = "            "
            insertion = f"\n{indent}val alpha by transition.animateFloat(label = \"blurAlpha\") {{ if (it == androidx.compose.animation.EnterExitState.Visible) 1f else 0f }}"
            part = part[:insertion_idx] + insertion + part[insertion_idx:]
    new_content += "AnimatedVisibility(" + part

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_content)
