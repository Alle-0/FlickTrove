import codecs

filepath = r'c:\Progetti\FlickTrove_Kotlin\app\src\main\java\com\cinetrack\MainActivity.kt'
with codecs.open(filepath, 'r', 'utf-8') as f:
    lines = f.readlines()

# Verify the indices
if 'val detailPadding = PaddingValues' in lines[1155] and 'MODALS AND OVERLAYS' in lines[870]:
    block = lines[1155:1254]
    del lines[1155:1254]
    
    # After deleting, line 870 is still 870 because 870 < 1155
    lines = lines[:870] + block + lines[870:]
    
    with codecs.open(filepath, 'w', 'utf-8') as f:
        f.writelines(lines)
    print('Move successful.')
else:
    print('Indices mismatch. Check the file.')
    print('Line 1155: ' + lines[1155])
    print('Line 870: ' + lines[870])
