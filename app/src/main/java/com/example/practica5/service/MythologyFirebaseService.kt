package com.example.practica5.service

import com.example.practica5.model.MythEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class MythologyFirebaseService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val COLLECTION_NAME = "Dioses Olimpicos"
    }

    private val mythsCollection = firestore.collection(COLLECTION_NAME)

    //Obtiene el catalogo de forma alfabetica
    suspend fun getAllMyths(): Result<List<MythEntity>> = runCatching {
        val snapshot = mythsCollection
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .await()

        snapshot.documents.mapNotNull { document ->
            document.toObject(MythEntity::class.java)?.sanitized()
        }
    }

    //La siguiente parte se encarga de consultar y filtrar para poder obtener los registros
    suspend fun getMythsByCategory(category: String): Result<List<MythEntity>> = runCatching {
        val cleanCategory = category.trim()
        require(cleanCategory.isNotEmpty()) { "La categoría de consulta no puede estar vacía." }

        val snapshot = mythsCollection
            .whereEqualTo("category", cleanCategory)
            .get()
            .await()

        snapshot.documents
            .mapNotNull { it.toObject(MythEntity::class.java)?.sanitized() }
            .sortedBy { it.name }
    }

    //La búsqueda se hace con prefijos
    suspend fun searchMythsByName(prefix: String): Result<List<MythEntity>> = runCatching {
        val cleanPrefix = prefix.trim()
        if (cleanPrefix.isEmpty()) {
            return getAllMyths()
        }

        val snapshot = mythsCollection
            .orderBy("name")
            .whereGreaterThanOrEqualTo("name", cleanPrefix)
            .whereLessThanOrEqualTo("name", cleanPrefix + "\uf8ff")
            .get()
            .await()

        snapshot.documents.mapNotNull { document ->
            document.toObject(MythEntity::class.java)?.sanitized()
        }
    }

    //Encargado de hacer la inserción de otros registros directamente de lo que se da en la APP
    suspend fun addMyth(myth: MythEntity): Result<String> = runCatching {
        require(myth.isValidForSave()) {
            "Datos inválidos: verifica que nombre, categoría y descripción cumplan el formato."
        }

        val sanitizedMyth = myth.sanitized()
        val payload = hashMapOf(
            "name" to sanitizedMyth.name,
            "category" to sanitizedMyth.category,
            "domain" to sanitizedMyth.domain,
            "symbol" to sanitizedMyth.symbol,
            "description" to sanitizedMyth.description,
            "imageUrl" to sanitizedMyth.imageUrl
        )

        val documentRef = mythsCollection.add(payload).await()
        documentRef.id
    }
}