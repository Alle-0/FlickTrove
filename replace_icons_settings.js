const fs = require('fs');

let content = fs.readFileSync('c:/Progetti/FlickTrove_Kotlin/app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt', 'utf8');

// 1. Remove states
content = content.replace(/    var showThemeDialog by remember \{ mutableStateOf\(false\) \}\r?\n/g, '');
content = content.replace(/    var showLanguageDialog by remember \{ mutableStateOf\(false\) \}\r?\n/g, '');

content = content.replace(/showThemeDialog \|\| showLanguageDialog/g, 'false');

// 2. Remove Generali section
const generaliRegex = /\/\/ Section: Generali[\s\S]*?\/\/ Section: Accessibilità/;
content = content.replace(generaliRegex, '// Section: Accessibilità');

// 3. Replace all Icons.Rounded occurrences with the custom ones
const replacements = [
    { search: "Icons.Rounded.Accessibility", replace: "ImageVector.vectorResource(id = R.drawable.ic_accessibilita)" },
    { search: "Icons.Rounded.FormatSize", replace: "ImageVector.vectorResource(id = R.drawable.ic_maiuscolo)" },
    { search: "Icons.Rounded.Animation", replace: "ImageVector.vectorResource(id = R.drawable.ic_sparkle)" },
    { search: "Icons.Rounded.Dashboard", replace: "ImageVector.vectorResource(id = R.drawable.ic_interfaccia)" },
    { search: "Icons.Rounded.Palette", replace: "ImageVector.vectorResource(id = R.drawable.ic_palette)" },
    { search: "Icons.Rounded.AppShortcut", replace: "ImageVector.vectorResource(id = R.drawable.ic_smartphone_magia)" },
    { search: "Icons.Rounded.Bookmarks", replace: "ImageVector.vectorResource(id = R.drawable.ic_segnalibro)" },
    { search: "Icons.Rounded.Style", replace: "ImageVector.vectorResource(id = R.drawable.ic_rank)" },
    { search: "Icons.Rounded.GridView", replace: "ImageVector.vectorResource(id = R.drawable.ic_grid)" },
    { search: "Icons.Rounded.NotificationsActive", replace: "ImageVector.vectorResource(id = R.drawable.ic_bell_piena)" },
    { search: "Icons.Rounded.Notifications", replace: "ImageVector.vectorResource(id = R.drawable.ic_bell_vibra)" },
    { search: "Icons.Rounded.Dns", replace: "ImageVector.vectorResource(id = R.drawable.ic_world)" },
    { search: "Icons.Rounded.DeleteSweep", replace: "ImageVector.vectorResource(id = R.drawable.ic_svuota_trash)" },
    { search: "Icons.Rounded.CloudSync", replace: "ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud)" },
    { search: "Icons.Rounded.Storage", replace: "ImageVector.vectorResource(id = R.drawable.ic_cartella)" },
    { search: "Icons.Rounded.Person", replace: "ImageVector.vectorResource(id = R.drawable.ic_persona)" },
    { search: "Icons.Rounded.ChatBubbleOutline", replace: "ImageVector.vectorResource(id = R.drawable.ic_comment)" },
    { search: "Icons.Rounded.Security", replace: "ImageVector.vectorResource(id = R.drawable.ic_scudo_privacy)" }
];

replacements.forEach(r => {
    content = content.replace(new RegExp(r.search.replace(/\./g, '\\.'), 'g'), r.replace);
});

// 4. Remove AnimatedVisibility blocks for Theme and Language at the end
const themeDialogRegex = /        AnimatedVisibility\(\s*visible = showThemeDialog,[\s\S]*?\}\s*\)\s*\}/;
content = content.replace(themeDialogRegex, '');

const langDialogRegex = /        AnimatedVisibility\(\s*visible = showLanguageDialog,[\s\S]*?\}\s*\)\s*\}/;
content = content.replace(langDialogRegex, '');

fs.writeFileSync('c:/Progetti/FlickTrove_Kotlin/app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt', content);
console.log("Replaced successfully!");
