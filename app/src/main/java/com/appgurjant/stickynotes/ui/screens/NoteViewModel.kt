package com.appgurjant.stickynotes.ui.screens

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.data.security.SecureStorage
import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import com.app.domain.model.TextStyleConfig
import com.app.domain.usecase.AddNoteUseCase
import com.app.domain.usecase.AllNoteUseCase
import com.app.domain.usecase.DeleteNoteUseCase
import com.app.domain.usecase.GetNoteDetailFromLocalUseCase
import com.app.domain.usecase.RequestSyncUseCase
import com.app.domain.usecase.UpdateNoteDetailFromLocalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val addNoteUseCase: AddNoteUseCase,
    private val allNoteUseCase: AllNoteUseCase,
    private val getNoteDetailUseCase: GetNoteDetailFromLocalUseCase,
    private val updateNoteDetailFromLocalUseCase: UpdateNoteDetailFromLocalUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val requestSyncUseCase: RequestSyncUseCase,
) : ViewModel() {
    var noteTitle by  mutableStateOf("")
    var noteDescription by   mutableStateOf("")
    var textStyleConfig by   mutableStateOf(TextStyleConfig())

    private val _noteSaveState = MutableStateFlow<String?>(null)
    val noteSaveState: StateFlow<String?> = _noteSaveState

    private val _getAllNotesFromDB = MutableStateFlow<List<Note>>(emptyList())
    val getAllNotesFromDB: StateFlow<List<Note>> = _getAllNotesFromDB

    private val _notesInitiallyLoaded = MutableStateFlow(false)
    val notesInitiallyLoaded: StateFlow<Boolean> = _notesInitiallyLoaded.asStateFlow()
    private val _getNotesDetailByIdFromLocal= MutableStateFlow<Note?>(null)
    val getNotesDetailByIdFromLocal: StateFlow<Note?> = _getNotesDetailByIdFromLocal
    private val _notesUpdateInLocal= MutableStateFlow<StandardResponse?>(null)
    val notesUpdateInLocal: StateFlow<StandardResponse?> = _notesUpdateInLocal

    private val _notesDeleteFromLocal= MutableStateFlow<StandardResponse?>(null)
    val notesDeleteFromLocal: StateFlow<StandardResponse?> = _notesDeleteFromLocal

    // Track if content has been modified
    var isContentModified by mutableStateOf(false)
    var currentContentJson by  mutableStateOf( "")



    fun onTitleChange(newTitle: String) { noteTitle = newTitle }
    fun onDescriptionChange(newDesc: String) { noteDescription = newDesc }
    fun onTextStyleConfigChange(newTextStyleConfig: TextStyleConfig) {
        textStyleConfig = newTextStyleConfig
    }


    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // In NoteViewModel
    private val _shouldShowWelcomeNotification = MutableStateFlow(secureStorage.isWelcomeNotificationShown())
    val shouldShowWelcomeNotification: StateFlow<Boolean> = _shouldShowWelcomeNotification.asStateFlow()

    fun setWelcomeNotificationShown(shown: Boolean) {
        secureStorage.setWelcomeNotificationShown(shown)
        _shouldShowWelcomeNotification.value = shown
    }

    val filteredNotes = combine(_getAllNotesFromDB, _searchQuery) { notes, query ->
        if (query.isBlank()) {
            notes
        } else {
            notes.filter { note ->
                val inTitle = note.title.orEmpty().contains(query, ignoreCase = true)
                val inDesc = note.description.orEmpty().contains(query, ignoreCase = true)
                val inJson = note.contentJson.orEmpty().contains(query, ignoreCase = true)
                inTitle || inDesc || inJson
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveNote(note: Note) {
        Log.d("NoteViewModel", "saveNote: $note")
        viewModelScope.launch() {
            try {
                addNoteUseCase(note).collect { it ->
                    _noteSaveState.value = it
                }
                // Newly inserted rows are isSync = 0 — kick the sync worker.
                requestSyncUseCase()
            } catch (e: Exception) {
                _noteSaveState.value = e.message.toString()
            }
        }
    }



    fun getAllNotes() {
        viewModelScope.launch() {
            allNoteUseCase().collect { it ->
                _getAllNotesFromDB.value = it
                _notesInitiallyLoaded.value = true
            }
        }
    }

    fun getNoteById(noteId: String) {
        Log.d("NoteViewModel", "getNoteById: $noteId")
        viewModelScope.launch {
            try {
                getNoteDetailUseCase(noteId).collect { note ->
                    Log.e("NoteViewModel", "Response : "+note)
                    _getNotesDetailByIdFromLocal.value = note

                }
            } catch (e: Exception) {
                Log.e("NoteViewModel", "Error fetching note by ID: ${e.message}")
                // Handle the error, maybe log it or show a message
            }
        }
    }

    fun updateNote(note: Note) {
        Log.e("NoteViewModel", "updateNote : "+note)
        viewModelScope.launch() {
            try {
                updateNoteDetailFromLocalUseCase(note).collect { it ->
                    _notesUpdateInLocal.value = it
                }
                // Updates also reset isSync = 0 (see NoteMapper) — re-sync.
                requestSyncUseCase()
            } catch (e: Exception) {
                Log.e("NoteViewModel", "Response : "+e.message.toString())
            }
        }
    }



    fun deleteNoteById(noteId: String) {
        if(noteId!=""){
            viewModelScope.launch() {
                try {
                    deleteNoteUseCase(noteId.toInt()).collect { it ->
                        _notesDeleteFromLocal.value = it
                    }
                    // Best-effort: ask for a sync pass so any *other* pending
                    // notes still get uploaded. The deleted note itself is
                    // gone locally; full delete-replication requires a future
                    // tombstones table (out of scope for v1).
                    requestSyncUseCase()
                } catch (e: Exception) {
                    Log.e("NoteViewModel", "Response : "+e.message.toString())
                }
            }
        }

    }

    fun updateCurrentContentJson(contentJson: String) {
        currentContentJson = contentJson
        isContentModified = true
    }

    fun setAppPin(pin: String){
      secureStorage.setManualAppPIN(pin)
    }
    fun getPin():String{
        return secureStorage.getManualAppPIN().orEmpty()
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        secureStorage.setFingerprintEnabled(enabled)
    }

    fun isFingerprintEnabled(): Boolean {
        return secureStorage.isFingerprintEnabled()
    }

}