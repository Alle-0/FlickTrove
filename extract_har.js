const fs = require('fs');

const harPath = 'C:\\\\Users\\\\39338\\\\Desktop\\\\www.figma.com.har';
console.log('Lettura del file HAR...');
const harContent = fs.readFileSync(harPath, 'utf8');

const keys = new Set();
// Cerchiamo le chiavi nei payload JSON
for (const match of harContent.matchAll(/"key":"([a-zA-Z0-9]{22})"/g)) {
    keys.add(match[1]);
}

// Cerchiamo anche dai link diretti per sicurezza
for (const match of harContent.matchAll(/https:\/\/www\.figma\.com\/(?:file|design)\/([a-zA-Z0-9]{22})/g)) {
    keys.add(match[1]);
}

const keysArray = Array.from(keys);
console.log(`Trovate ${keysArray.length} chiavi uniche!`);

// Se abbiamo trovato delle chiavi, aggiorniamo figma_export.js
if (keysArray.length > 0) {
    const exportScriptPath = './figma_export.js';
    let scriptContent = fs.readFileSync(exportScriptPath, 'utf8');
    
    // Costruiamo il nuovo array
    const newArrayString = 'const FILE_KEYS = [\n' + keysArray.map(k => `    '${k}'`).join(',\n') + '\n];';
    
    // Sostituiamo il blocco esistente
    scriptContent = scriptContent.replace(/const FILE_KEYS = \[[^\]]*\];/g, newArrayString);
    
    fs.writeFileSync(exportScriptPath, scriptContent);
    console.log('File figma_export.js aggiornato con successo!');
} else {
    console.log('Nessuna chiave trovata.');
}
