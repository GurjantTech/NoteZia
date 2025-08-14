package com.appgurjant.stickynotes.ui.screens

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.domain.model.Note
import com.app.domain.model.StandardResponse
import com.app.domain.usecase.AddNoteUseCase
import com.app.domain.usecase.AllNoteUseCase
import com.app.domain.usecase.DeleteNoteUseCase
import com.app.domain.usecase.GetNoteDetailFromLocalUseCase
import com.app.domain.usecase.UpdateNoteDetailFromLocalUseCase
import com.app.domain.utils.UIState
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
    private val addNoteUseCase: AddNoteUseCase,
    private val allNoteUseCase: AllNoteUseCase,
    private val getNoteDetailUseCase: GetNoteDetailFromLocalUseCase,
    private val updateNoteDetailFromLocalUseCase: UpdateNoteDetailFromLocalUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
) : ViewModel() {
    var noteTitle by  mutableStateOf("")
    var noteDescription by   mutableStateOf("")

    private val _noteSaveState = MutableStateFlow<UIState>(UIState.Loading)
    val noteSaveState: StateFlow<UIState> = _noteSaveState

    private val _getAllNotesFromDB = MutableStateFlow<List<Note>>(emptyList())
    val getAllNotesFromDB: StateFlow<List<Note>> = _getAllNotesFromDB
    private val _getNotesDetailByIdFromLocal= MutableStateFlow<Note?>(null)
    val getNotesDetailByIdFromLocal: StateFlow<Note?> = _getNotesDetailByIdFromLocal
    private val _notesUpdateInLocal= MutableStateFlow<StandardResponse?>(null)
    val notesUpdateInLocal: StateFlow<StandardResponse?> = _notesUpdateInLocal

    private val _notesDeleteFromLocal= MutableStateFlow<StandardResponse?>(null)
    val notesDeleteFromLocal: StateFlow<StandardResponse?> = _notesDeleteFromLocal

    // Track if content has been modified
    var isContentModified by   mutableStateOf(false)
    var currentContentJson by  mutableStateOf( "")


    fun onTitleChange(newTitle: String) { noteTitle = newTitle }
    fun onDescriptionChange(newDesc: String) { noteDescription = newDesc }


    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()


    val filteredNotes = combine(_getAllNotesFromDB, _searchQuery) { notes, query ->
        if (query.isBlank()) {
            notes
        } else {
            notes.filter { it.title.toString().contains(query, ignoreCase = true) || it.description.toString().contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveNote(note: Note) {
        Log.d("NoteViewModel", "saveNote: $note")
        viewModelScope.launch() {
            _noteSaveState.emit(UIState.Loading)
            try {
                addNoteUseCase(note).collect { it ->
                    _noteSaveState.value = UIState.Success(it)
                }
            } catch (e: Exception) {
                _noteSaveState.value = UIState.Error(e.message.toString())
            }
        }
    }



    fun getAllNotes() {
        viewModelScope.launch() {
            allNoteUseCase().collect { it ->
                _getAllNotesFromDB.value = it
            }
        }
    }

    fun getNoteById(noteId: String) {
        Log.d("NoteViewModel", "getNoteById: $noteId")
        viewModelScope.launch {
            try {
                getNoteDetailUseCase(noteId).collect { note ->
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
            _noteSaveState.emit(UIState.Loading)
            try {
                updateNoteDetailFromLocalUseCase(note).collect { it ->
                    _notesUpdateInLocal.value = it
                }
            } catch (e: Exception) {
                Log.e("NoteViewModel", "Response : "+e.message.toString())
            }
        }
    }



    fun deleteNoteById(noteId: String) {
        if(noteId!=""){
            viewModelScope.launch() {
                _noteSaveState.emit(UIState.Loading)
                try {
                    deleteNoteUseCase(noteId.toInt()).collect { it ->
                        _notesDeleteFromLocal.value = it
                    }
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

}