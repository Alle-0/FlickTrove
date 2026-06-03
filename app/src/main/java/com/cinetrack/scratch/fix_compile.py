import re

filepath = r'c:\Progetti\FlickTrove_Kotlin\app\src\main\java\com\cinetrack\ui\viewmodel\SearchViewModel.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    code = f.read()

# Fix combine(_category, _sortConfig)
code = re.sub(r'combine\(_category, _sortConfig\) { c, s -> c to s }', 'kotlinx.coroutines.flow.snapshotFlow { _uiState.value.category to _uiState.value.sortConfig }', code)

# Fix emptyList() to ImmutableList assignments
def to_immutable(text):
    return text.replace('emptyList()', 'kotlinx.collections.immutable.persistentListOf()')

code = re.sub(r'_uiState\.update \{ it\.copy\(results = emptyList\(\)\) \}', '_uiState.update { it.copy(results = kotlinx.collections.immutable.persistentListOf()) }', code)
code = re.sub(r'_uiState\.update \{ it\.copy\(trendingMovies = emptyList\(\)\) \}', '_uiState.update { it.copy(trendingMovies = kotlinx.collections.immutable.persistentListOf()) }', code)
code = re.sub(r'_uiState\.update \{ it\.copy\(trendingTv = emptyList\(\)\) \}', '_uiState.update { it.copy(trendingTv = kotlinx.collections.immutable.persistentListOf()) }', code)
code = re.sub(r'_uiState\.update \{ it\.copy\(trendingPeople = emptyList\(\)\) \}', '_uiState.update { it.copy(trendingPeople = kotlinx.collections.immutable.persistentListOf()) }', code)

# Fix .toImmutableList() for results assignments
# e.g., _uiState.update { it.copy(results = accumulatedResults) }
code = re.sub(r'_uiState\.update \{ it\.copy\(results = accumulatedResults\) \}', '_uiState.update { it.copy(results = accumulatedResults.toImmutableList()) }', code)
code = re.sub(r'_uiState\.update \{ it\.copy\(results = results\) \}', '_uiState.update { it.copy(results = results.toImmutableList()) }', code)
code = re.sub(r'_uiState\.update \{ it\.copy\(trendingMovies = movies\) \}', '_uiState.update { it.copy(trendingMovies = movies.toImmutableList()) }', code)
code = re.sub(r'_uiState\.update \{ it\.copy\(trendingTv = tv\) \}', '_uiState.update { it.copy(trendingTv = tv.toImmutableList()) }', code)
code = re.sub(r'_uiState\.update \{ it\.copy\(trendingPeople = people\) \}', '_uiState.update { it.copy(trendingPeople = people.toImmutableList()) }', code)


# Fix togglingIds Set -> PersistentSet
# val togglingIds: ImmutableSet<Long> -> val togglingIds: PersistentSet<Long>
code = code.replace('val togglingIds: ImmutableSet<Long> = persistentSetOf()', 'val togglingIds: PersistentSet<Long> = persistentSetOf()')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(code)

print("Fixes applied")
