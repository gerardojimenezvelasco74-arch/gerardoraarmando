package com.example.listacompartida

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.listacompartida.ui.theme.ListaCompartidaTheme
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

// 📌 Modelo de datos de compras
data class Compra(
    val producto: String = "",
    val cantidad: String = "",
    val precio: String = "",
    val id: String = ""
)

// 📌 Modelo de datos de sección
data class Seccion(
    val nombre: String = "",
    val fechaCreacion: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ListaCompartidaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ListaDeSeccionesScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ListaDeSeccionesScreen(modifier: Modifier = Modifier) {
    val database = Firebase.database
    val seccionesRef = database.getReference("compras")

    var secciones by remember { mutableStateOf(listOf<Pair<String, Seccion>>()) }
    var nuevaSeccion by remember { mutableStateOf("") }

    // 🔥 Escuchar las secciones disponibles
    LaunchedEffect(Unit) {
        seccionesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lista = mutableListOf<Pair<String, Seccion>>()
                for (child in snapshot.children) {
                    val seccion = child.child("info").getValue(Seccion::class.java)
                    if (seccion != null) {
                        lista.add(child.key!! to seccion)
                    }
                }
                secciones = lista
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📂 Secciones de compras", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Campo para crear nueva sección
        OutlinedTextField(
            value = nuevaSeccion,
            onValueChange = { nuevaSeccion = it },
            label = { Text("Nombre de la nueva sección") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (nuevaSeccion.isNotBlank()) {
                    val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                    val seccionInfo = Seccion(nuevaSeccion, fecha)
                    val nuevaRef = seccionesRef.push() // generar id único
                    nuevaRef.child("info").setValue(seccionInfo)
                    nuevaSeccion = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("➕ Crear sección")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar todas las secciones
        LazyColumn {
            items(secciones) { (id, seccion) ->
                SeccionCard(id, seccion, seccionesRef.child(id))
            }
        }
    }
}

@Composable
fun SeccionCard(id: String, seccion: Seccion, ref: DatabaseReference) {
    var mostrarFormulario by remember { mutableStateOf(false) }
    var mostrarCompras by remember { mutableStateOf(false) }
    var editandoNombre by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf(seccion.nombre) }

    var producto by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var compras by remember { mutableStateOf(listOf<Compra>()) }

    var editandoId by remember { mutableStateOf<String?>(null) }

    // 🔥 Escuchar compras de esta sección
    LaunchedEffect(Unit) {
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Compra>()
                for (item in snapshot.children) {
                    if (item.key != "info") {
                        item.getValue(Compra::class.java)?.copy(id = item.key ?: "")?.let { items.add(it) }
                    }
                }
                compras = items
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    val totalGasto = compras.sumOf { it.precio.toDoubleOrNull() ?: 0.0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Nombre y fecha
            if (!editandoNombre) {
                Text("👤 Sección: ${seccion.nombre}", style = MaterialTheme.typography.titleMedium)
                Text("📅 Creada: ${seccion.fechaCreacion}", style = MaterialTheme.typography.bodySmall)
            } else {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nuevo nombre de sección") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (nuevoNombre.isNotBlank()) {
                            ref.child("info").setValue(seccion.copy(nombre = nuevoNombre))
                            editandoNombre = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💾 Guardar nombre")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botones principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { mostrarFormulario = !mostrarFormulario }) {
                    Text("➕ Agregar compra")
                }
                Button(onClick = { mostrarCompras = !mostrarCompras }) {
                    Text("👀 Ver compras")
                }
            }

            // Opciones de la sección
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { editandoNombre = true }) {
                    Text("✏ Editar nombre")
                }
                Button(onClick = { ref.removeValue() }) {
                    Text("🗑 Eliminar sección")
                }
            }

            // Formulario de compras
            if (mostrarFormulario) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = producto,
                    onValueChange = { producto = it },
                    label = { Text("Producto") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Cantidad") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (producto.isNotBlank()) {
                            val nuevaCompra = Compra(producto, cantidad, precio)
                            if (editandoId == null) {
                                val key = ref.push().key
                                key?.let { ref.child(it).setValue(nuevaCompra) }
                            } else {
                                ref.child(editandoId!!).setValue(nuevaCompra)
                                editandoId = null
                            }
                            producto = ""
                            cantidad = ""
                            precio = ""
                            mostrarFormulario = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editandoId == null) "✅ Guardar compra" else "✏ Actualizar compra")
                }
            }

            // Lista de compras
            if (mostrarCompras) {
                Spacer(modifier = Modifier.height(8.dp))
                compras.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("📦 Producto: ${item.producto}", style = MaterialTheme.typography.bodyLarge)
                            Text("🔢 Cantidad: ${item.cantidad}", style = MaterialTheme.typography.bodyMedium)
                            Text("💲 Precio: ${item.precio}", style = MaterialTheme.typography.bodyMedium)

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        producto = item.producto
                                        cantidad = item.cantidad
                                        precio = item.precio
                                        editandoId = item.id
                                        mostrarFormulario = true
                                    }
                                ) {
                                    Text("✏ Editar")
                                }
                                Button(
                                    onClick = { ref.child(item.id).removeValue() }
                                ) {
                                    Text("🗑 Eliminar")
                                }
                            }

                        }
                    }
                }

                // Total de gastos
                Spacer(modifier = Modifier.height(8.dp))
                Text("💰 Total de gastos: $totalGasto", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
