const fs = require('fs');
const files = [
  'app/src/main/res/drawable/ic_launcher_foreground_vector.xml',
  'app/src/main/res/drawable/ic_launcher_foreground_amber.xml',
  'app/src/main/res/drawable/ic_launcher_foreground_blue.xml',
  'app/src/main/res/drawable/ic_launcher_foreground_pink.xml',
  'app/src/main/res/drawable/ic_launcher_foreground_purple.xml',
  'app/src/main/res/drawable/logo_start.xml'
];
files.forEach(file => {
  if (fs.existsSync(file)) {
    let content = fs.readFileSync(file, 'utf8');
    
    // Replace strokeWidth with 10
    if (content.includes('android:strokeWidth')) {
       content = content.replace(/android:strokeWidth="\d+"/g, 'android:strokeWidth="10"');
    } else {
       content = content.replace(/(android:pathData="[^"]+")/g, '$1\n            android:strokeWidth="10"\n            android:strokeLineCap="round"');
    }
    
    fs.writeFileSync(file, content);
  }
});
console.log('Fixed strokeWidth to 10');
