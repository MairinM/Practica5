package com.example.practica5.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica5.model.MythUiState
import com.example.practica5.viewmodel.MythologyViewModel

//Pantalla principal
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MythologyScreen(
    mythologyViewModel: MythologyViewModel = viewModel()
) {
    val uiState by mythologyViewModel.uiState.collectAsState()
    val selectedCategory by mythologyViewModel.selectedCategory.collectAsState()
    val searchQuery by mythologyViewModel.searchQuery.collectAsState()
    val actionMessage by mythologyViewModel.actionMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            mythologyViewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compendio del Olimpo",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { mythologyViewModel.loadAllMyths() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar desde Firebase"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar entidad mitológica",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Barra de búsqueda (Consulta NoSQL por prefijo en Firestore)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { mythologyViewModel.onSearchQueryChange(it) },
                placeholder = { Text("Buscar por nombre (ej. Atenea)...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { mythologyViewModel.loadAllMyths() }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filtros por categoría (Consulta NoSQL whereEqualTo)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(mythologyViewModel.categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { mythologyViewModel.selectCategory(category) },
                        label = { Text(text = category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenido dinámico según el estado de la petición a Firebase
            when (val state = uiState) {
                is MythUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Consultando registros en Firebase...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                is MythUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = state.message,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { mythologyViewModel.loadAllMyths() }) {
                                Text("Reintentar conexión")
                            }
                        }
                    }
                }

                is MythUiState.Success -> {
                    if (state.myths.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron figuras mitológicas con ese filtro.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 84.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.myths,
                                key = { it.id.ifBlank { it.name } }
                            ) { myth ->
                                MythCard(myth = myth)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMythDialog(
            categories = mythologyViewModel.categories.filter { it != "Todos" },
            onDismiss = { showAddDialog = false },
            onSave = { name, category, domain, symbol, description, imageUrl ->
                mythologyViewModel.addNewMyth(
                    name = name,
                    category = category,
                    domain = domain,
                    symbol = symbol,
                    description = description,
                    imageUrl = imageUrl,
                    onSuccessClose = { showAddDialog = false }
                )
            }
        )
    }
}