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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

// 📌 Modelo de datos
data class Compra(
    val producto: String = "",
    val cantidad: String = "",
    val precio: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ListaCompartidaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ListaDeComprasScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ListaDeComprasScreen(modifier: Modifier = Modifier) {
    var producto by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var listaCompras by remember { mutableStateOf(listOf<Compra>()) }

    val database = Firebase.database
    val myRef = database.getReference("compras")

    // 🔥 Escuchar cambios en Firebase
    LaunchedEffect(Unit) {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Compra>()
                for (item in snapshot.children) {
                    item.getValue(Compra::class.java)?.let { items.add(it) }
                }
                listaCompras = items
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "🛒 Lista de Compras", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de producto
        OutlinedTextField(
            value = producto,
            onValueChange = { producto = it },
            label = { Text("Producto") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo de cantidad
        OutlinedTextField(
            value = cantidad,
            onValueChange = { cantidad = it },
            label = { Text("Cantidad") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campo de precio
        OutlinedTextField(
            value = precio,
            onValueChange = { precio = it },
            label = { Text("Precio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botón para agregar
        Button(
            onClick = {
                if (producto.isNotBlank()) {
                    val key = myRef.push().key
                    val nuevaCompra = Compra(producto, cantidad, precio)
                    key?.let {
                        myRef.child(it).setValue(nuevaCompra)
                    }
                    producto = ""
                    cantidad = ""
                    precio = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Agregar a la lista")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar lista de compras
        LazyColumn {
            items(listaCompras) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📦 Producto: ${item.producto}", style = MaterialTheme.typography.bodyLarge)
                        Text("🔢 Cantidad: ${item.cantidad}", style = MaterialTheme.typography.bodyMedium)
                        Text("💲 Precio: ${item.precio}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
