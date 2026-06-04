const fs = require('fs');
const path = require('path');

const srcDir = 'c:\\Progetti\\FlickTrove_Kotlin\\figma_exports_svg';
const destDir = 'c:\\Progetti\\FlickTrove_Kotlin\\app\\src\\main\\res\\drawable';

function mapFilename(file) {
    let newName = file.toLowerCase();
    newName = newName.replace(/\+/g, 'plus');
    newName = newName.replace(/à/g, 'a');
    newName = newName.replace(/è/g, 'e');
    newName = newName.replace(/é/g, 'e');
    newName = newName.replace(/ì/g, 'i');
    newName = newName.replace(/ò/g, 'o');
    newName = newName.replace(/ù/g, 'u');
    newName = newName.replace(/[^a-z0-9.]/g, '_');
    newName = newName.replace(/_+/g, '_');
    if (!newName.startsWith('ic_')) {
        newName = 'ic_' + newName;
    }
    newName = newName.replace(/\.svg$/, '.xml');
    return newName;
}

function processSVG(svgContent) {
    let paths = [];
    
    // Extractor that keeps attributes
    function addPath(d, attrsStr) {
        let isFill = attrsStr.includes('fill="black"') || attrsStr.includes('fill="#000000"');
        let isStroke = attrsStr.includes('stroke="black"') || attrsStr.includes('stroke="#000000"');
        if (!isFill && !isStroke) {
            // default to stroke
            isStroke = true;
        }
        
        let pathXml = `    <path\n        android:pathData="${d}"`;
        if (isFill) {
            pathXml += `\n        android:fillColor="#FF000000"`;
        }
        if (isStroke) {
            pathXml += `\n        android:strokeColor="#FF000000"\n        android:strokeWidth="4"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round"`;
        }
        pathXml += `/>\n`;
        paths.push(pathXml);
    }
    
    // Extract <path>
    const pathRegex = /<path([^>]*)d="([^"]+)"([^>]*)>/g;
    let match;
    while ((match = pathRegex.exec(svgContent)) !== null) {
        let attrs = (match[1] + match[3]).trim();
        addPath(match[2], attrs);
    }
    
    // Extract <circle>
    const circleRegex = /<circle([^>]*)cx="([^"]+)"([^>]*)cy="([^"]+)"([^>]*)r="([^"]+)"([^>]*)>/g;
    while ((match = circleRegex.exec(svgContent)) !== null) {
        let attrs = (match[1] + match[3] + match[5] + match[7]).trim();
        const cx = parseFloat(match[2]);
        const cy = parseFloat(match[4]);
        const r = parseFloat(match[6]);
        const d = `M ${cx},${cy-r} a ${r},${r} 0 1,0 0,${2*r} a ${r},${r} 0 1,0 0,${-2*r}`;
        addPath(d, attrs);
    }
    
    // Extract <rect>
    const rectRegex = /<rect([^>]*)x="([^"]+)"([^>]*)y="([^"]+)"([^>]*)width="([^"]+)"([^>]*)height="([^"]+)"([^>]*)>/g;
    while ((match = rectRegex.exec(svgContent)) !== null) {
        let attrs = (match[1] + match[3] + match[5] + match[7] + match[9]).trim();
        if (attrs.includes('fill="white"')) {
            // ignore background white rects
            continue;
        }
        
        const x = parseFloat(match[2]);
        const y = parseFloat(match[4]);
        const w = parseFloat(match[6]);
        const h = parseFloat(match[8]);
        
        let rx = 0;
        const rxMatch = attrs.match(/rx="([^"]+)"/);
        if (rxMatch) {
            rx = parseFloat(rxMatch[1]);
        }
        
        let d;
        if (rx > 0) {
            d = `M ${x+rx},${y} h ${w-2*rx} a ${rx},${rx} 0 0,1 ${rx},${rx} v ${h-2*rx} a ${rx},${rx} 0 0,1 ${-rx},${rx} h ${-(w-2*rx)} a ${rx},${rx} 0 0,1 ${-rx},${-rx} v ${-(h-2*rx)} a ${rx},${rx} 0 0,1 ${rx},${-rx} z`;
        } else {
            d = `M ${x},${y} h ${w} v ${h} h ${-w} z`;
        }
        addPath(d, attrs);
    }
    
    // Sometimes <rect> doesn't have x/y but just width/height
    const rectNoXYRegex = /<rect([^>]*)width="([^"]+)"([^>]*)height="([^"]+)"([^>]*)>/g;
    while ((match = rectNoXYRegex.exec(svgContent)) !== null) {
        let attrs = (match[1] + match[3] + match[5]).trim();
        if (attrs.includes('fill="white"')) {
            continue;
        }
        if (!attrs.includes('x=')) {
            const w = parseFloat(match[2]);
            const h = parseFloat(match[4]);
            let rx = 0;
            const rxMatch = attrs.match(/rx="([^"]+)"/);
            if (rxMatch) {
                rx = parseFloat(rxMatch[1]);
            }
            let d;
            if (rx > 0) {
                d = `M ${rx},0 h ${w-2*rx} a ${rx},${rx} 0 0,1 ${rx},${rx} v ${h-2*rx} a ${rx},${rx} 0 0,1 ${-rx},${rx} h ${-(w-2*rx)} a ${rx},${rx} 0 0,1 ${-rx},${-rx} v ${-(h-2*rx)} a ${rx},${rx} 0 0,1 ${rx},${-rx} z`;
            } else {
                d = `M 0,0 h ${w} v ${h} h ${-w} z`;
            }
            addPath(d, attrs);
        }
    }
    
    let out = `<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="48dp"\n    android:height="48dp"\n    android:viewportWidth="48"\n    android:viewportHeight="48">\n`;
    paths.forEach(p => {
        out += p;
    });
    out += `</vector>\n`;
    return out;
}

const files = fs.readdirSync(srcDir);
files.forEach(f => {
    if (f.endsWith('.svg')) {
        const svg = fs.readFileSync(path.join(srcDir, f), 'utf-8');
        const xml = processSVG(svg);
        const newName = mapFilename(f);
        fs.writeFileSync(path.join(destDir, newName), xml);
        console.log("Converted " + f + " -> " + newName);
    }
});
