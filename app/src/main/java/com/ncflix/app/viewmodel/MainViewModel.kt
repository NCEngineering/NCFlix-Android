package com.ncflix.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncflix.app.data.MovieRepository
import com.ncflix.app.model.Movie
import com.ncflix.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository = MovieRepository()

    private val _homeState = MutableStateFlow<Resource<Pair<Movie, List<Movie>>>>(Resource.Loading)
    val homeState: StateFlow<Resource<Pair<Movie, List<Movie>>>> = _homeState.asStateFlow()

    private val _seriesState = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val seriesState: StateFlow<Resource<List<Movie>>> = _seriesState.asStateFlow()

    private val _moviesState = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val moviesState: StateFlow<Resource<List<Movie>>> = _moviesState.asStateFlow()

    private val _mostViewedState = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val mostViewedState: StateFlow<Resource<List<Movie>>> = _mostViewedState.asStateFlow()

    private val _malaysiaState = MutableStateFlow<Resource<List<Movie>>>(Resource.Loading)
    val malaysiaState: StateFlow<Resource<List<Movie>>> = _malaysiaState.asStateFlow()

    private val _searchState = MutableStateFlow<Resource<List<Movie>>?>(null)
    val searchState: StateFlow<Resource<List<Movie>>?> = _searchState.asStateFlow()

    init {
        loadHomeData()
        loadOtherSections()
    }

    fun searchMovies(query: String) {
        if (query.isBlank()) {
            _searchState.value = null
            return
        }
        viewModelScope.launch {
            _searchState.value = Resource.Loading
            _searchState.value = repository.searchMovies(query)
        }
    }

    fun clearSearch() {
        _searchState.value = null
    }

    private fun loadOtherSections() {
        viewModelScope.launch { _seriesState.value = repository.fetchLatestSeries() }
        viewModelScope.launch { _moviesState.value = repository.fetchLatestMovies() }
        viewModelScope.launch { _mostViewedState.value = repository.fetchMostViewed() }
        viewModelScope.launch { _malaysiaState.value = repository.fetchTop10Malaysia() }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.value = Resource.Loading
            val result = repository.fetchHomeData()
            
            // Transform Resource<Pair<Movie?, List<Movie>>> to Resource<Pair<Movie, List<Movie>>>
            // We need to ensure Hero is not null for Success
            when (result) {
                is Resource.Success -> {
                    val (hero, list) = result.data
                    if (hero != null) {
                        _homeState.value = Resource.Success(Pair(hero, list))
                    } else {
                        _homeState.value = Resource.Error("No featured movie found")
                    }
                }
                is Resource.Error -> {
                    _homeState.value = Resource.Error(result.message, result.exception)
                }
                is Resource.Loading -> {
                     _homeState.value = Resource.Loading
                }
            }
        }
    }
}