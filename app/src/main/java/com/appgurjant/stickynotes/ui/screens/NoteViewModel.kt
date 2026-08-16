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

    /**
     * Persists the note to Room. After a successful local write, [AddNoteUseCase]
     * schedules a background Firebase sync so the UI never waits on the network.
     */
    fun saveNote(note: Note) {
        Log.d("NoteViewModel", "saveNote: $note")
        viewModelScope.launch() {
            try {
                addNoteUseCase(note).collect { it ->
                    _noteSaveState.value = it
                }
            } catch (e: Exception) {
                _noteSaveState.value = e.message.toString()
            }
        }
    }

    private var observingNotes = false

    fun getAllNotes() {
        if (observingNotes) return
        observingNotes = true
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

    /**
     * Persists edits to Room. The mapper resets `isSync` to pending so the
     * background worker uploads the same note id after the local write
     * succeeds — never on each keystroke.
     */
    fun updateNote(note: Note) {
        Log.e("NoteViewModel", "updateNote : "+note)
        viewModelScope.launch() {
            try {
                updateNoteDetailFromLocalUseCase(note).collect { it ->
                    _notesUpdateInLocal.value = it
                }
            } catch (e: Exception) {
                Log.e("NoteViewModel", "Response : "+e.message.toString())
            }
        }
    }



    /**
     * Deletes a note. Signed-in users get an immediate local tombstone so
     * the note disappears even offline; [DeleteNoteUseCase] then schedules
     * a background Firebase delete of the same id. Signed-out users get a
     * local-only hard delete.
     */
    fun deleteNoteById(noteId: String) {
        if (noteId.isEmpty()) return
        viewModelScope.launch {
            try {
                deleteNoteUseCase(noteId.toInt()).collect { response ->
                    _notesDeleteFromLocal.value = response
                }
            } catch (e: Exception) {
                Log.e("NoteViewModel", "Delete failed: ${e.message}")
            }
        }
    }

    /** Clears one-shot update/delete events after the UI has consumed them. */
    fun consumeDeleteResult() {
        _notesDeleteFromLocal.value = null
    }

    fun consumeUpdateResult() {
        _notesUpdateInLocal.value = null
    }

    fun clearNoteDetailState() {
        _getNotesDetailByIdFromLocal.value = null
        _notesDeleteFromLocal.value = null
        _notesUpdateInLocal.value = null
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