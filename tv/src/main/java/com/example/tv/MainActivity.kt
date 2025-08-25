package com.example.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.tv.ui.theme.ListaCompartidaTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

// 🔹 Modelo de datos igual que en la app de móvil
data class Compra(
    val producto: String = "",
    val cantidad: String = "",
    val precio: String = ""
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ListaCompartidaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    ListaDeComprasScreen()
                }
            }
        }
    }
}

@Composable
fun ListaDeComprasScreen() {
    var lista by remember { mutableStateOf(listOf<Compra>()) }

    // Escuchar cambios en Firebase
    LaunchedEffect(Unit) {
        val database = Firebase.database
        val myRef = database.getReference("compras")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<Compra>()
                for (item in snapshot.children) {
                    try {
                        item.getValue(Compra::class.java)?.let { items.add(it) }
                    } catch (e: Exception) {
                        // ignorar datos viejos o corruptos
                    }
                }
                lista = items
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // fondo blanco
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título principal
        Text(
            text = "🛒 Lista de Compras",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Lista de elementos
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(lista) { compra ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(Color(0xFFF5F5F5)) // Tarjeta gris clara
                        .padding(16.dp)
                ) {
                    Text(
                        text = compra.producto,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cantidad: ${compra.cantidad}",
                        fontSize = 20.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Precio: $${compra.precio}",
                        fontSize = 20.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}
