const fs = require('fs');

let content = fs.readFileSync('c:/Progetti/FlickTrove_Kotlin/app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt', 'utf8');

// 1. Rename section and change icon
content = content.replace('title = "Archiviazione e Rete",', 'title = "Immagini",');
content = content.replace('icon = ImageVector.vectorResource(id = R.drawable.ic_world)', 'icon = ImageVector.vectorResource(id = R.drawable.ic_image)');

// 2. Swap Svuota Cache Immagini and Qualità Immagini
const sectionRegex = /(\/\/ Section: Archiviazione e Rete\s+item \{\s+SettingsSection\([\s\S]*?\}\s+\})/;
const sectionMatch = content.match(sectionRegex);
if (sectionMatch) {
    let sectionBlock = sectionMatch[1];
    sectionBlock = sectionBlock.replace('// Section: Archiviazione e Rete', '// Section: Immagini');
    
    // Split the sectionBlock into the two items. This is a bit tricky, let's just do it manually with indexOf.
    const cacheStart = sectionBlock.indexOf('SettingsItem(\n                                icon = ImageVector.vectorResource(id = R.drawable.ic_svuota_trash),');
    const qualityStart = sectionBlock.indexOf('SettingsItem(\n                                icon = ImageVector.vectorResource(id = R.drawable.ic_hd),');
    const qualityEnd = sectionBlock.indexOf('                            )\n                        }\n                    }');
    
    if (cacheStart !== -1 && qualityStart !== -1 && qualityEnd !== -1) {
        const beforeItems = sectionBlock.substring(0, cacheStart);
        const cacheItem = sectionBlock.substring(cacheStart, qualityStart);
        const qualityItem = sectionBlock.substring(qualityStart, qualityEnd + 30); // includes the ')\n'
        
        // Let's actually use a regex to grab exactly the SettingsItem block
        const cacheRegex = /(SettingsItem\(\s*icon = ImageVector\.vectorResource\(id = R\.drawable\.ic_svuota_trash\),[\s\S]*?onClick = \{[\s\S]*?showCacheConfirm = true\s*\}\s*\))/;
        const qualityRegex = /(SettingsItem\(\s*icon = ImageVector\.vectorResource\(id = R\.drawable\.ic_hd\),[\s\S]*?onClick = \{\s*\},[\s\S]*?customContent = \{[\s\S]*?\}\s*\))/;
        
        const cacheMatch = sectionBlock.match(cacheRegex);
        const qualityMatch = sectionBlock.match(qualityRegex);
        
        if (cacheMatch && qualityMatch) {
            let newSectionBlock = sectionBlock.replace(cacheMatch[1], '###QUALITY###');
            newSectionBlock = newSectionBlock.replace(qualityMatch[1], cacheMatch[1]);
            newSectionBlock = newSectionBlock.replace('###QUALITY###', qualityMatch[1]);
            
            content = content.replace(sectionMatch[1], newSectionBlock);
        }
    }
}

// 3. Replace remaining generic icons
const replacements = [
    { search: "Icons.Rounded.Info", replace: "ImageVector.vectorResource(id = R.drawable.ic_punto_cerchiato)" },
    { search: "Icons.Rounded.Download", replace: "ImageVector.vectorResource(id = R.drawable.ic_scaricare)" },
    { search: "Icons.Rounded.Upload", replace: "ImageVector.vectorResource(id = R.drawable.ic_caricare)" },
    { search: "if (!isGuest) Icons.AutoMirrored.Rounded.Logout else Icons.AutoMirrored.Rounded.Login", replace: "ImageVector.vectorResource(id = R.drawable.ic_exit)" },
    { search: "Icons.AutoMirrored.Rounded.Logout", replace: "ImageVector.vectorResource(id = R.drawable.ic_exit)" },
    { search: "Icons.AutoMirrored.Rounded.Help", replace: "ImageVector.vectorResource(id = R.drawable.ic_question_mark_pieno)" },
    { search: "Icons.AutoMirrored.Rounded.Article", replace: "ImageVector.vectorResource(id = R.drawable.ic_documento)" }
];

replacements.forEach(r => {
    content = content.replace(new RegExp(r.search.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g'), r.replace);
});

fs.writeFileSync('c:/Progetti/FlickTrove_Kotlin/app/src/main/java/com/cinetrack/ui/screens/SettingsScreen.kt', content);
console.log("Updated correctly.");
