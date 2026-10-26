package com.yuyue.app.ui.shell

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.toRoute
import com.yuyue.app.data.YuyueRepository
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.EntryMediaEntity
import com.yuyue.app.data.local.EntryRevisionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Detail UI state: never treat the initial null emission as “deleted”. */
sealed interface EntryDetailState {
    data object Loading : EntryDetailState
    data object Missing : EntryDetailState
    data class Present(val entry: EntryEntity) : EntryDetailState
}

/**
 * Entry detail observes Room by [entryId], not a stale Entity snapshot from navigation args.
 * [entryId] is restored via [SavedStateHandle] / typed route for process death.
 */
class EntryViewModel(
    private val repository: YuyueRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Routes.Entry>()
    val entryId: String = route.id

    val detail: StateFlow<EntryDetailState> =
        repository.observeEntry(entryId)
            .map { e ->
                if (e == null) EntryDetailState.Missing else EntryDetailState.Present(e)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryDetailState.Loading)

    val media: StateFlow<List<EntryMediaEntity>> =
        repository.observeMediaForEntry(entryId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _revisions = MutableStateFlow<List<EntryRevisionEntity>>(emptyList())
    val revisions: StateFlow<List<EntryRevisionEntity>> = _revisions.asStateFlow()

    init {
        viewModelScope.launch {
            _revisions.value = runCatching { repository.listRevisions(entryId) }.getOrDefault(emptyList())
        }
    }

    class Factory(
        private val repository: YuyueRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return EntryViewModel(repository, handle) as T
        }
    }
}

/** Edit route also restores id from SavedStateHandle; body loads via observeEntry. */
class EntryEditViewModel(
    private val repository: YuyueRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<Routes.EntryEdit>()
    val entryId: String = route.id

    val detail: StateFlow<EntryDetailState> =
        repository.observeEntry(entryId)
            .map { e ->
                if (e == null) EntryDetailState.Missing else EntryDetailState.Present(e)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntryDetailState.Loading)

    class Factory(
        private val repository: YuyueRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val handle = extras.createSavedStateHandle()
            return EntryEditViewModel(repository, handle) as T
        }
    }
}
