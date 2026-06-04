const fs = require('fs');

let content = fs.readFileSync('c:/Progetti/FlickTrove_Kotlin/app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt', 'utf8');

// Match the entire Accessibilità section
// It starts with "// Section: Accessibilità" and ends right before "// Section: Interfaccia"
const accessibilitaRegex = /(\/\/ Section: Accessibilità\s+item \{\s+SettingsSection\([\s\S]*?\}\s+\})\s+(\/\/ Section: Interfaccia)/;

// Let's actually match the block.
// We can use a simpler approach: finding the string indices and slicing.
const accStart = content.indexOf('                    // Section: Accessibilità');
const intStart = content.indexOf('                    // Section: Interfaccia');
const notifStart = content.indexOf('                    // Section: Notifiche e Vibrazione');

if (accStart !== -1 && intStart !== -1 && notifStart !== -1) {
    const accBlock = content.substring(accStart, intStart);
    const intBlock = content.substring(intStart, notifStart);

    const before = content.substring(0, accStart);
    const after = content.substring(notifStart);

    // Swap them!
    const newContent = before + intBlock + accBlock + after;
    fs.writeFileSync('c:/Progetti/FlickTrove_Kotlin/app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt', newContent);
    console.log("Swapped successfully");
} else {
    console.log("Could not find sections");
}
