package com.example.practica5.model

import com.google.firebase.firestore.DocumentId

//Modelo de los datos que fueron utilizados en la Firebase
data class MythEntity(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val category: String = "",      // Ej. "Dioses Olímpicos", "Héroes", "Criaturas"
    val domain: String = "",        // Ej. "Trueno y Cielo", "Sabiduría y Estrategia"
    val symbol: String = "",        // Ej. "Rayo y Águila", "Búho y Olivo"
    val description: String = "",
    val imageUrl: String = ""
) {
    //Normaliza los campos
    fun sanitized(): MythEntity {
        val cleanName = name.trim().ifBlank { "Entidad desconocida" }
        val cleanCategory = category.trim().ifBlank { "Sin categoría" }
        val cleanDomain = domain.trim().ifBlank { "Dominio no especificado" }
        val cleanSymbol = symbol.trim().ifBlank { "Sin símbolo registrado" }
        val cleanDescription = description.trim()
            .replace(Regex("\\s+"), " ")
            .ifBlank { "Descripción no disponible en los registros del Olimpo." }
        val cleanUrl = imageUrl.trim().takeIf {
            it.startsWith("https://") || it.startsWith("http://")
        } ?: ""

        return copy(
            name = cleanName,
            category = cleanCategory,
            domain = cleanDomain,
            symbol = cleanSymbol,
            description = cleanDescription,
            imageUrl = cleanUrl
        )
    }

    //Valida los datos registrados
    fun isValidForSave(): Boolean {
        return name.trim().length >= 2 &&
                category.trim().isNotEmpty() &&
                description.trim().length >= 10
    }
}

//Estados posibles de la Firebase
sealed interface MythUiState {
    data object Loading : MythUiState
    data class Success(val myths: List<MythEntity>) : MythUiState
    data class Error(val message: String) : MythUiState
}