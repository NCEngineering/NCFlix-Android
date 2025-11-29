package com.ncflix.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncflix.app.data.MovieRepository
import com.ncflix.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {
    private val repository = MovieRepository()

    private val _streamState = MutableStateFlow<Resource<String>>(Resource.Loading)
    val streamState: StateFlow<Resource<String>> = _streamState.asStateFlow()

    fun loadStream(url: String) {
        viewModelScope.launch {
            _streamState.value = Resource.Loading
            _streamState.value = repository.extractStreamUrl(url)
        }
    }
}