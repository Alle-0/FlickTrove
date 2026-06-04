const fs = require('fs');

const path = 'app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt';
let content = fs.readFileSync(path, 'utf8');

// Replace generic icons with Custom Vector Drawables
content = content.replace(/icon = Icons\.Rounded\.ColorLens,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_palette),');
content = content.replace(/icon = Icons\.Rounded\.Smartphone,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_smartphone_magia),');
content = content.replace(/icon = Icons\.Rounded\.CloudSync,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_ricarica_cloud),');
content = content.replace(/icon = Icons\.Rounded\.CloudDownload,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_cloud),');
content = content.replace(/icon = Icons\.Rounded\.BookmarkBorder,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_segnalibro),');
content = content.replace(/icon = Icons\.Rounded\.AutoAwesome,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_sparkle),');
content = content.replace(/icon = Icons\.Rounded\.FormatSize,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_maiuscolo),');
content = content.replace(/icon = Icons\.Rounded\.DisplaySettings,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_interfaccia),');
content = content.replace(/icon = Icons\.Rounded\.ViewCozy,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_grid_a_4),');
content = content.replace(/icon = Icons\.Rounded\.AccessibilityNew,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_accessibilita),');
content = content.replace(/icon = Icons\.Rounded\.Vibration,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_bell_vibra),');

// Tema App -> ic_temi
content = content.replace(/icon = Icons\.Rounded\.DarkMode,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_temi),');

// Backup Dispositivo -> ic_cartella_piena
content = content.replace(/icon = Icons\.Rounded\.Backup,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_cartella_piena),');
// Svuota Cache Immagini -> ic_svuota_trash
content = content.replace(/icon = Icons\.Rounded\.DeleteOutline,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_trash),'); // Elimina account uses trash, wait
content = content.replace(/icon = Icons\.Rounded\.CleaningServices,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_svuota_trash),'); // maybe cache
content = content.replace(/icon = Icons\.Rounded\.HighQuality,/g, 'icon = ImageVector.vectorResource(id = R.drawable.ic_hd),');

// Just doing a safe replace for what I know exists.
fs.writeFileSync(path, content);
console.log('SettingsScreen icons updated');
