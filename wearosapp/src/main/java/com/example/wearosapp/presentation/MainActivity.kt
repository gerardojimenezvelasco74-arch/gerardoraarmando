package com.example.wearosapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.wearosapp.presentation.theme.ListaCompartidaTheme
import com.google.firebase.database.*

// ---------- MODELOS ----------
data class Compra(
    val producto: String = "",
    val cantidad: String = "",
    val precio: String = ""
)

data class Seccion(
    val nombre: String = "",
    val fechaCreacion: String = "",
    val compras: List<Compra> = emptyList()
)

// ---------- MAIN ACTIVITY ----------
class MainActivity : ComponentActivity() {

    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = FirebaseDatabase.getInstance().getReference("compras")

        setContent {
            ListaCompartidaTheme {
                var secciones by remember { mutableStateOf(emptyList<Seccion>()) }
                var seleccionada by remember { mutableStateOf<Seccion?>(null) }

                // Escuchar cambios en Firebase
                LaunchedEffect(Unit) {
                    db.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val lista = mutableListOf<Seccion>()

                            for (seccionSnap in snapshot.children) {
                                val infoSnap = seccionSnap.child("info")
                                val nombre = infoSnap.child("nombre").getValue(String::class.java) ?: "Sin nombre"
                                val fecha = infoSnap.child("fechaCreacion").getValue(String::class.java) ?: "Sin fecha"

                                val compras = seccionSnap.children.mapNotNull { compraSnap ->
                                    if (compraSnap.key != "info") {
                                        val producto = compraSnap.child("producto").getValue(String::class.java) ?: ""
                                        val cantidad = compraSnap.child("cantidad").getValue(String::class.java) ?: ""
                                        val precio = compraSnap.child("precio").getValue(String::class.java) ?: ""
                                        Compra(producto, cantidad, precio)
                                    } else null
                                }

                                lista.add(Seccion(nombre, fecha, compras))
                            }

                            secciones = lista
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }

                // UI con fondo oscuro
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    if (seleccionada == null) {
                        ListaSecciones(secciones) { seleccionada = it }
                    } else {
                        ListaCompras(seccion = seleccionada!!) {
                            seleccionada = null
                        }
                    }
                }
            }
        }
    }
}

// ---------- LISTA DE SECCIONES ----------
@Composable
fun ListaSecciones(secciones: List<Seccion>, onClick: (Seccion) -> Unit) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🔹 Título
        item {
            Text(
                text = "🛒 Compras",
                color = Color.White,
                style = MaterialTheme.typography.title2,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // 🔹 Lista de secciones
        items(secciones) { seccion ->
            Text(
                text = "📂 ${seccion.nombre}",
                color = Color(0xFFFFC107), // amarillo dorado
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .clickable { onClick(seccion) }
                    .padding(vertical = 6.dp)
            )
        }
    }
}

// ---------- LISTA DE PRODUCTOS ----------
@Composable
fun ListaCompras(seccion: Seccion, onBack: () -> Unit) {
    val total = seccion.compras.sumOf {
        it.precio.toDoubleOrNull() ?: 0.0
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Botón volver
        item {
            Text(
                text = "⬅️ Volver",
                color = Color.Cyan,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(vertical = 4.dp)
            )
        }

        // Nombre de sección
        item {
            Text(
                text = "📂 ${seccion.nombre}",
                color = Color(0xFFFFC107),
                style = MaterialTheme.typography.title2,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Productos
        if (seccion.compras.isEmpty()) {
            item {
                Text(
                    text = "No hay productos",
                    color = Color.Gray,
                    style = MaterialTheme.typography.body2
                )
            }
        } else {
            items(seccion.compras) { compra ->
                Text(
                    text = "• ${compra.producto} (${compra.cantidad})  $${compra.precio}",
                    color = Color.White,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            // Mostrar total
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💰 Total: $${"%.2f".format(total)}",
                    color = Color.Green,
                    style = MaterialTheme.typography.title3
                )
            }
        }
    }
}
