const fs = require('fs');

// ==========================================
// 1. INSERISCI QUI I TUOI DATI
// ==========================================
const FIGMA_TOKEN = 'figd_rXIHi88XvSbqmBDoxb82JuCW1dAcQEHdYhD8lmS4'; 

const FILE_KEYS = [
    'pIzODEhi9I01e61ZnEjxzP',
    '3BkKmU4aohOaMFilzrJ8v2',
    'Vchsk3fhnunjfZZosgi1Y5',
    'vWq2aaQh1QOD0o0VpJlM41',
    'RAArQodbG1KFN8IR2jKzFH',
    'Udfu7EcU4OjFdeNtaZLUG7',
    'rB874MPTReiCixNCHhSveo',
    'yV2I6Ft3DbOKE0poWyDBPn',
    'yBBY0NIQ31H43zOh5UpzHY',
    '8FaRMM5nA41vr8DChb5k4Y',
    'YeyWtrLeZEPhG6JHGYPS9r',
    'Nw0lBWN0vsVGIYV5g1Ch5d',
    'bHn4NhUb7HPP4TYUbXa8GS',
    'zB0yDeFa3UW3mbnYiItQKE',
    'OcrACWzPe0WPrCmmXkZcwj',
    'P6uQW55ERb9e4lIUCtB5zG',
    'v7xPqrFUwZj89yP4ZtETCz',
    '1YLkzGFjP6kliixQbIwzec',
    'ZsWTPmPLtdTzGXtLaeSydG',
    'HIuV9c3ZJP3qVkmYi1XUhX',
    'gO55Cdxyv0CLQ3Ux6KlAYL',
    'HHMe8Psv4D3aBA5pRZ03V1',
    '06CM9Znu6pbvwKyITlbwkA',
    'Bk9r3GwgPYVxfsWjpp2t1F',
    'a0RDVIk3TZgYHwspUiTB60',
    'ayT01OkEN3nFAx2iuHgOQI',
    '6Ou3LqKzWEGs7kXgZEy32A',
    'BEpTGBacd1813kOSq7TojD',
    'lcxVHaHwUdBg7GuFiey1Yw',
    '4c2LzCIbRUXGI0M6q5Nd0L',
    'yx9AGTp1bZMHO23uDnvV6Y',
    '7CiNlmQZFjOoCoNDULZlKl',
    'f03RKJkqAa3KzlnrfZ2QP6',
    'vJxZ538NWx27ZRAbwYU8LP',
    'wZPrghK7Yac1dsgGLWVFL9',
    'BQ59QeYEVIW3yx1bonxhvF',
    '2EUrlBxhE7XuB3Kiz6CIVY',
    '8pT7OBri1jsDhqHc8AWUPb',
    'dJpwKcqkUCbTbb9Lk5ZLEb',
    '7I6X93aE0LOjy4ek4fkhlF',
    'N6lGnQkjx89vZNUXwqqkVJ',
    'V0QX81euTy14tkafD5H7Ke',
    'VoIuJTplLiOvfh0yZdsCgM',
    'ivPun0aVo9bB19i22BbNfx',
    'rCBdlPHO1dtpwBkjJegqKj',
    'hDi6Dds5Rvyq9lKQmGHytT',
    'vYZoHRKH3wba0pvAu4uCap',
    'zSGqg7iPQIfwYtTgQYHRN4',
    '4xus6BP77SwdUYvS4OgoYL',
    '9Z0WQ2WWhp11m15keuGNgI',
    'fWyZtmgTQvvjvaozPeuG5g',
    'OamevwMTTzeEzzLjaoI4Ki',
    'IWsNLXzpCVUB0szJ8Sn8dj',
    'cfmp7A2kJIh1upwx2cfwjB',
    'VgoDAuTQw7Tjl6bqz50lmT',
    'sf8ohVhk8M61Lt6sv6Olu0',
    'jnUnwCv2p9fj5t53kIetsN',
    'fzfS6ROWeVODSSWXA3zmSl',
    'yFK21Rr3E8VDl41VgIc0zg',
    'utiaFq2sqnx7xKSf2PFYnD',
    '2rQC36XWyHcNLWJ0Tb0s7V',
    'A0OlEQBMbGtSqfKuky3mW5',
    'qvwSsDR8LGO9Ix9ngyaqOE',
    'ZcVgHaCmSw2MtnQQ874gqO',
    'NvRQrRhvrEVt09Iz7E5Fcx',
    'ZKFxvlGufGdd7f8UHacPKc',
    'mWXekscONuGotFQhRERRJn',
    'Pow12RUZERQvOWiGQmdxFl',
    'zfgs9eWlod38b0c9Z7nc4f',
    'K5ECLgNdaSmXz8cywmp9SH',
    '6MXtFJm3DaWfX6zzRk4osr',
    '79yrmzVypyOPx4FWHTClLi',
    'NgX79JfeoKvixn0nYNNAAj',
    'uA8kSDYagDHZjIgLaLNZFW',
    'NZOemWLBKBmZHC79TYyLn5',
    'uZTUYVtemlLg50xPBbibNw',
    'jp2ZKgz0xVGE0NOMlQ3WS4',
    'OtMfJHRrRN5Y37TwhqbjJo',
    'QeEfDvttkVKyiAMKMQEIaZ',
    'gOfCbXjYhOWZMdlmn5LASE',
    'eiueWUmVzexQbXf1QkWjqT',
    'Cm7y463MZMVqpd0385OyK0',
    'PT2uaieNRy8OrpfKPl7w3A',
    'bIl4kMwnGwRxzi2DqKJezA',
    'RUCC4munXabLhrHkjLJ1eJ',
    'Vi1mWeTswmy9TaNfbnJKRO',
    'Nh2yUkOX3EOYz7OOXGtKIO',
    'E5vPcxoHYB6r2B6UTitMXZ',
    'Onpld6VT2POpC3HnQfZYgJ',
    'cwc3ZSmgW79PKPJVcEcxR7',
    'NyOjjyh1PqykPE3ZTYNw3F',
    'LcaoKFSoccgwXTfSCV3xot',
    'vLHxNZaq5hxMU6zJWpv9xI',
    'oZD73cl2sNlVsmmD9iv4SF',
    'khxK5zbfSSuFFvIcC8rm2j',
    'HW7P5nrZSNH0EwiAdRzwlQ',
    '9ztPFsKaE60OB8NbIbtdmv',
    'G6lAXb2ArtmJ9P6it3i09H',
    'GdqKoFUr73dmVpCalBmHFu',
    'J9n8TZmVBpWlB7rbxjtZsb',
    'FUk0uoxYDT0DDqg3w9onFN',
    'Bul6omyRwVnGz02YLAxGk6',
    'NGity8B6e8CnJ9gElV0fPa',
    'smaQy06KJrxqewvFp8jSbB',
    'acI3Gi2dQruQI8fzRTE2Vg',
    'TJ2YKs3aynbIqdUoGpOouX',
    'vqF9LeCQmGY6YhRA2BaS5J',
    'zFaDYNK0emGChCfliwnbiW',
    'PwgG1Kf90EQH0mgusr6rAh',
    '05RtWUaVSeQ64iQTnbsFw6'
]; 
// ==========================================

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function fetchFigma(endpoint) {
    let retries = 0;
    while (retries < 3) {
        const response = await fetch(`https://api.figma.com/v1${endpoint}`, {
            headers: { 'X-Figma-Token': FIGMA_TOKEN }
        });
        
        if (response.status === 429) {
            console.log(`⚠️ Raggiunto il limite di richieste (429). Pausa di 60 secondi per sblocco...`);
            await sleep(60000);
            retries++;
            continue;
        }
        
        if (!response.ok) throw new Error(`Figma API Error: ${response.status} ${response.statusText}`);
        return response.json();
    }
    throw new Error("Troppi tentativi falliti per 429.");
}

async function exportFrames() {
    if (FIGMA_TOKEN === 'INSERISCI_QUI_IL_TUO_TOKEN') {
        console.log("❌ ERRORE: Devi prima inserire il tuo FIGMA_TOKEN nello script!");
        return;
    }

    for (const fileKey of FILE_KEYS) {
        console.log(`\n⏳ Caricamento del file ${fileKey} in corso...`);
        
        try {
            // 1. Ottiene la struttura del file per trovare i Frame
            const fileData = await fetchFigma(`/files/${fileKey}`);
            const frames = [];
            
            // Cerca i frame (o componenti) nella prima pagina del documento
            const firstPage = fileData.document.children[0]; 
            firstPage.children.forEach(node => {
                if (node.type === 'FRAME' || node.type === 'COMPONENT' || node.type === 'SECTION') {
                    frames.push({ id: node.id, name: node.name });
                }
            });

            if (frames.length === 0) {
                console.log(`⚠️ Nessun frame trovato in ${fileKey}.`);
                continue;
            }

            console.log(`✅ Trovati ${frames.length} frame. Richiesta dei file SVG a Figma...`);
            const ids = frames.map(f => f.id).join(',');
            
            // 2. Richiede a Figma di generare gli SVG per quegli ID
            const imageData = await fetchFigma(`/images/${fileKey}?ids=${ids}&format=svg`);
            const images = imageData.images;

            const exportDir = './figma_exports_svg';
            if (!fs.existsSync(exportDir)) {
                fs.mkdirSync(exportDir);
            }

            // 3. Scarica fisicamente gli SVG salvandoli su disco
            let count = 0;
            const safeFileName = fileData.name.replace(/[^a-z0-9]/gi, '_').toLowerCase();
            
            for (const frame of frames) {
                const url = images[frame.id];
                if (url) {
                    // Se c'è un solo frame usa il nome del file, altrimenti aggiunge il nome del frame
                    const safeFrameName = frame.name.replace(/[^a-z0-9]/gi, '_').toLowerCase();
                    const finalName = frames.length === 1 ? safeFileName : `${safeFileName}_${safeFrameName}`;
                    
                    console.log(`📥 Scaricamento: ${finalName}.svg`);
                    
                    const svgResponse = await fetch(url);
                    const svgText = await svgResponse.text();
                    
                    fs.writeFileSync(`${exportDir}/${finalName}.svg`, svgText);
                    count++;
                }
            }
            
            console.log(`🎉 Finito per ${fileKey}! ${count} file scaricati.`);
        } catch (e) {
            console.error(`❌ Errore durante l'esportazione di ${fileKey}:`, e.message);
        }
        await sleep(1000); // Pausa di 1 secondo tra i file per evitare il blocco API
    }
}

exportFrames();
