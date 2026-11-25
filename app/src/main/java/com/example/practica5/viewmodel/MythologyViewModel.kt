package com.example.practica5.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practica5.model.MythEntity
import com.example.practica5.model.MythUiState
import com.example.practica5.service.MythologyFirebaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel encargado de gestionar el estado de la interfaz y coordinar
 * las consultas asíncronas hacia [MythologyFirebaseService].
 */
class MythologyViewModel(
    private val service: MythologyFirebaseService = MythologyFirebaseService()
) : ViewModel() {

    private val _uiState = MutableStateFlow<MythUiState>(MythUiState.Loading)
    val uiState: StateFlow<MythUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val categories = listOf("Todos", "Dioses Olímpicos", "Héroes", "Criaturas")

    init {
        loadAllMyths()
    }

    /**
     * Carga el listado completo de entidades mitológicas desde Firestore.
     */
    fun loadAllMyths() {
        _selectedCategory.value = "Todos"
        _searchQuery.value = ""
        viewModelScope.launch {
            _uiState.value = MythUiState.Loading
            service.getAllMyths()
                .onSuccess { myths ->
                    _uiState.value = MythUiState.Success(myths)
                }
                .onFailure { error ->
                    _uiState.value = MythUiState.Error(
                        error.localizedMessage ?: "Error al conectar con Firebase."
                    )
                }
        }
    }

    /**
     * Filtra las entidades mediante consulta NoSQL (`whereEqualTo`) según la categoría elegida.
     */
    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _searchQuery.value = ""
        if (category == "Todos") {
            loadAllMyths()
            return
        }

        viewModelScope.launch {
            _uiState.value = MythUiState.Loading
            service.getMythsByCategory(category)
                .onSuccess { myths ->
                    _uiState.value = MythUiState.Success(myths)
                }
                .onFailure { error ->
                    _uiState.value = MythUiState.Error(
                        error.localizedMessage ?: "Error al filtrar por categoría."
                    )
                }
        }
    }

    /**
     * Ejecuta una búsqueda por prefijo en Firestore conforme el usuario escribe.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _selectedCategory.value = "Todos"
        viewModelScope.launch {
            _uiState.value = MythUiState.Loading
            service.searchMythsByName(query)
                .onSuccess { myths ->
                    _uiState.value = MythUiState.Success(myths)
                }
                .onFailure { error ->
                    _uiState.value = MythUiState.Error(
                        error.localizedMessage ?: "Error al buscar en el compendio."
                    )
                }
        }
    }

    /**
     * Valida los datos del formulario y guarda una nueva figura mitológica en Cloud Firestore.
     */
    fun addNewMyth(
        name: String,
        category: String,
        domain: String,
        symbol: String,
        description: String,
        imageUrl: String,
        onSuccessClose: () -> Unit
    ) {
        val newMyth = MythEntity(
            name = name,
            category = category,
            domain = domain,
            symbol = symbol,
            description = description,
            imageUrl = imageUrl
        )

        if (!newMyth.isValidForSave()) {
            _actionMessage.value = "Completa nombre (mín. 2 letras), categoría y descripción (mín. 10 letras)."
            return
        }

        viewModelScope.launch {
            service.addMyth(newMyth)
                .onSuccess {
                    _actionMessage.value = "${newMyth.sanitized().name} se registró en el Olimpo."
                    onSuccessClose()
                    loadAllMyths()
                }
                .onFailure { error ->
                    _actionMessage.value = "No se pudo guardar: ${error.localizedMessage}"
                }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}