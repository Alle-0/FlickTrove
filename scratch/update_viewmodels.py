import os
import re

directory = r'c:\Progetti\FlickTrove_Kotlin\app\src\main\java\com\cinetrack\ui\viewmodel'

for filename in os.listdir(directory):
    if not filename.endswith('.kt'):
        continue
    filepath = os.path.join(directory, filename)
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if 'cycleMovieStatus(' not in content:
        continue

    # Add import
    if 'import com.cinetrack.domain.CycleMovieStatusUseCase' not in content:
        content = content.replace('import com.cinetrack.data.repository.MovieRepository', 'import com.cinetrack.data.repository.MovieRepository\nimport com.cinetrack.domain.CycleMovieStatusUseCase')
    
    # Add constructor param
    if 'private val cycleMovieStatusUseCase' not in content:
        content = content.replace('@Inject constructor(\n', '@Inject constructor(\n    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,\n')
        content = content.replace('@Inject \nconstructor(\n', '@Inject \nconstructor(\n    private val cycleMovieStatusUseCase: CycleMovieStatusUseCase,\n')
    
    # Fix calls
    content = content.replace('repository.cycleMovieStatus(', 'cycleMovieStatusUseCase(')
    content = content.replace('movieRepository.cycleMovieStatus(', 'cycleMovieStatusUseCase(')
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

print('DONE')
