package com.example.sfsymbols

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.SfSymbolsCatalog
import com.composables.sfsymbols.dualtone.*
import com.composables.sfsymbols.monochrome.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF007AFF),
                    secondary = Color(0xFF5856D6),
                    background = Color(0xFFF2F2F7),
                    surface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SfSymbolsGalleryScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SfSymbolsGalleryScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var isDualtone by remember { mutableStateOf(true) }
    var selectedColor by remember { mutableStateOf(Color(0xFF007AFF)) }

    val filteredSymbols = remember(searchQuery) {
        SfSymbolsCatalog.search(searchQuery).take(120)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SF Symbols Compose", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("7,007 Apple SF Symbols for Jetpack Compose", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name or category (e.g. heart, circle, arrow)...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Controls: Dualtone Switch & Color Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isDualtone) "Dualtone (Default)" else "Monochrome",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isDualtone,
                        onCheckedChange = { isDualtone = it }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val colors = listOf(
                        Color(0xFF007AFF), // Blue
                        Color(0xFFFF2D55), // Pink/Red
                        Color(0xFF34C759), // Green
                        Color(0xFFFF9500), // Orange
                        Color(0xFF5856D6), // Purple
                        Color(0xFF000000)  // Black
                    )
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = color,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Showing ${filteredSymbols.size} symbols (${if (isDualtone) "Dualtone" else "Monochrome"})",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Featured Showcase
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SampleIconItem("Heart", isDualtone, selectedColor)
                    SampleIconItem("Checkmark", isDualtone, selectedColor)
                    SampleIconItem("Star", isDualtone, selectedColor)
                    SampleIconItem("Trash", isDualtone, selectedColor)
                    SampleIconItem("Folder", isDualtone, selectedColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Symbol Catalog Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSymbols) { symbol ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = symbol.pascalName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = symbol.appleName,
                                fontSize = 9.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = symbol.categories.firstOrNull() ?: "General",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SampleIconItem(name: String, isDualtone: Boolean, tint: Color) {
    val vector = when (name) {
        "Heart" -> if (isDualtone) SfSymbols.Dualtone.SFHeartFill else SfSymbols.Monochrome.SFHeartFill
        "Checkmark" -> if (isDualtone) SfSymbols.Dualtone.SFCheckmarkCircleFill else SfSymbols.Monochrome.SFCheckmarkCircleFill
        "Star" -> if (isDualtone) SfSymbols.Dualtone.SFStarFill else SfSymbols.Monochrome.SFStarFill
        "Trash" -> if (isDualtone) SfSymbols.Dualtone.SFTrashFill else SfSymbols.Monochrome.SFTrashFill
        else -> if (isDualtone) SfSymbols.Dualtone.SFFolderFill else SfSymbols.Monochrome.SFFolderFill
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = vector,
            contentDescription = name,
            tint = tint,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
