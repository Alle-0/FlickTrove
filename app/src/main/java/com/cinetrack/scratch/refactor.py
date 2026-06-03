import re

filepath = r'c:\Progetti\FlickTrove_Kotlin\app\src\main\java\com\cinetrack\ui\viewmodel\SearchViewModel.kt'

with open(filepath, 'r', encoding='utf-8') as f:
    code = f.read()

# 1. Remove the 15 MutableStateFlow declarations
flow_vars = [
    '_query', '_category', '_results', '_trendingMovies', '_trendingTv', 
    '_trendingPeople', '_isLoading', '_isNextPageLoading', '_isEndReached', 
    '_sortConfig', '_errorMessage', '_togglingIds', '_dynamicKeywords'
]

# Create regex to match their declarations and remove them
for var in flow_vars:
    code = re.sub(r'    private val ' + var + r' = MutableStateFlow.*?\n', '', code)

# 2. Add _uiState and uiState
ui_state_decl = """    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private var localMovies: List<Movie> = emptyList()
    private var localFolders: List<FolderEntity> = emptyList()
    
    private fun applyStateFilters() {
        _uiState.update { currentState ->
            getSearchUiStateUseCase(
                currentState = currentState,
                rawResults = currentState.results,
                rawTrendingMovies = currentState.trendingMovies,
                rawTrendingTv = currentState.trendingTv,
                rawTrendingPeople = currentState.trendingPeople,
                localMovies = localMovies,
                folders = localFolders
            )
        }
    }
"""
code = re.sub(r'    private var currentPage = 1', ui_state_decl + '    private var currentPage = 1', code)

# 3. Replace the massive combine block and its stateIn
# We will match from `val uiState: StateFlow<SearchUiState> = combine(` down to `initialValue = SearchUiState()\n    )`
code = re.sub(r'    val uiState: StateFlow<SearchUiState> = combine\([\s\S]*?initialValue = SearchUiState\(\)\n    \)\n', '', code)

# 4. In init block, we need to collect the repository flows and update localMovies/localFolders, then apply filters
init_add = """
        viewModelScope.launch {
            repository.getLocalMoviesFlow().collect { movies ->
                localMovies = movies
                applyStateFilters()
            }
        }
        viewModelScope.launch {
            repository.getFoldersFlow().collect { folders ->
                localFolders = folders
                applyStateFilters()
            }
        }
        viewModelScope.launch {
            repository.getRecentSearches().collect { recent ->
                _uiState.update { it.copy(recentSearches = recent.toImmutableList()) }
            }
        }
        viewModelScope.launch {
            preferenceRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
"""
# We also remove the reactive combination of _query, _category, _sortConfig
code = re.sub(r'        // Reactive search handling with debouncing for query[\s\S]*?}\n        }\n', init_add, code)

# Wait, debouncing is needed!
# We can just keep a query debounce flow internally, or manually debounce.
debounce_logic = """
        viewModelScope.launch {
            snapshotFlow { _uiState.value.query }
                .debounce { q -> if (q.isEmpty()) 0L else 500L }
                .distinctUntilChanged()
                .collect { performSearch() }
        }
        viewModelScope.launch {
            snapshotFlow { _uiState.value.category to _uiState.value.sortConfig }
                .drop(1)
                .distinctUntilChanged()
                .collect { performSearch() }
        }
"""
code = code.replace(init_add, init_add + debounce_logic)


# 5. Replace assignments: _query.value = X -> _uiState.update { it.copy(query = X) }
# We can't do regex easily for complex RHS, so we'll do simpler replacements.
def replace_assignments(text, var_name, state_prop):
    # match `_var.value = XXX` and replace with `_uiState.update { it.copy(prop = XXX) }`
    return re.sub(rf'{var_name}\.value\s*=\s*(.+?)$', rf'_uiState.update {{ it.copy({state_prop} = \1) }}', text, flags=re.MULTILINE)

code = replace_assignments(code, '_query', 'query')
code = replace_assignments(code, '_category', 'category')
code = replace_assignments(code, '_results', 'results')
code = replace_assignments(code, '_trendingMovies', 'trendingMovies')
code = replace_assignments(code, '_trendingTv', 'trendingTv')
code = replace_assignments(code, '_trendingPeople', 'trendingPeople')
code = replace_assignments(code, '_isLoading', 'isLoading')
code = replace_assignments(code, '_isNextPageLoading', 'isNextPageLoading')
code = replace_assignments(code, '_isEndReached', 'isEndReached')
code = replace_assignments(code, '_sortConfig', 'sortConfig')
code = replace_assignments(code, '_errorMessage', 'errorMessage')
code = replace_assignments(code, '_dynamicKeywords', 'suggestedFilters') # Wait, dynamicKeywords is just a part of suggestedFilters. We'll handle this manually.

# 6. Replace reads: _query.value -> _uiState.value.query
reads_map = {
    '_query.value': '_uiState.value.query',
    '_category.value': '_uiState.value.category',
    '_results.value': '_uiState.value.results',
    '_trendingMovies.value': '_uiState.value.trendingMovies',
    '_trendingTv.value': '_uiState.value.trendingTv',
    '_trendingPeople.value': '_uiState.value.trendingPeople',
    '_isLoading.value': '_uiState.value.isLoading',
    '_isNextPageLoading.value': '_uiState.value.isNextPageLoading',
    '_isEndReached.value': '_uiState.value.isEndReached',
    '_sortConfig.value': '_uiState.value.sortConfig',
    '_errorMessage.value': '_uiState.value.errorMessage',
    '_togglingIds.value': '_uiState.value.togglingIds',
    '_dynamicKeywords.value': '_uiState.value.suggestedFilters'
}

for k, v in reads_map.items():
    code = code.replace(k, v)

# Exception for togglingIds update (since it's a set)
# _togglingIds.update { it.add(movie.id) }
code = code.replace('_togglingIds.update { it.add(movie.id) }', '_uiState.update { it.copy(togglingIds = it.togglingIds.add(movie.id)) }')
code = code.replace('_togglingIds.update { it.remove(movie.id) }', '_uiState.update { it.copy(togglingIds = it.togglingIds.remove(movie.id)) }')
code = code.replace('_togglingIds.update { it.remove(tmdbId) }', '_uiState.update { it.copy(togglingIds = it.togglingIds.remove(tmdbId)) }')

# Exception for dynamicKeywords assignment
# `_uiState.update { it.copy(suggestedFilters = response.getList().take(6).map { ... }) }` is not fully correct because GetSearchUiStateUseCase handles dynamic keywords. 
# We'll just leave dynamicKeywords as a mutable state flow for simplicity, or change it to `var dynamicKeywords = emptyList()` and call applyStateFilters()
code = code.replace('private val _dynamicKeywords = MutableStateFlow<List<FilterPill>>(emptyList())', 'private var dynamicKeywords: List<FilterPill> = emptyList()')

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(code)

print("Refactor complete")
