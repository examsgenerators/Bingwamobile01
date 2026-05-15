package com.bingwa.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Start background balance checker
        startService(Intent(this, BalanceChecker::class.java))
        
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF1A73E8),
                secondary = Color(0xFF34A853),
                background = Color(0xFFF8F9FA),
                surface = Color.White
            )) {
                BingwaApp()
            }
        }
    }
}

@Composable
fun BingwaApp() {
    var selectedScreen by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var tokenBalance by remember { mutableStateOf(tokenManager.getBalance()) }
    var airtimeBalance by remember { mutableStateOf("Checking...") }
    
    // Listen for balance updates
    DisposableEffect(Unit) {
        BalanceChecker.balanceCallback = { balance ->
            airtimeBalance = balance
            // Extract just the KSh amount if possible
            val regex = Regex("Ksh\\.?\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE)
            val match = regex.find(balance)
            if (match != null) {
                airtimeBalance = "KSh ${match.groupValues[1]}"
            }
        }
        onDispose { BalanceChecker.balanceCallback = null }
    }
    
    // Refresh token balance when screen changes
    LaunchedEffect(selectedScreen) {
        tokenBalance = tokenManager.getBalance()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val screens = listOf(
                    "Dashboard" to Icons.Default.Home,
                    "Offers" to Icons.Default.ShoppingCart,
                    "Transactions" to Icons.Default.History,
                    "Settings" to Icons.Default.Settings
                )
                screens.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedScreen == index,
                        onClick = { selectedScreen = index },
                        icon = { Icon(icon, label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1A73E8),
                            selectedTextColor = Color(0xFF1A73E8),
                            unselectedIconColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedScreen) {
                0 -> DashboardScreen(tokenBalance, airtimeBalance)
                1 -> OffersScreen()
                2 -> TransactionsScreen()
                3 -> SettingsScreen()
            }
        }
    }
}

// ──────────────────── DASHBOARD SCREEN ────────────────────
@Composable
fun DashboardScreen(tokenBalance: Int, airtimeBalance: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF1557B0))))
                .padding(24.dp)
        ) {
            Column {
                Text("Bingwa Mobile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Automated M-PESA Agent", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                
                // Two stat cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Token Balance Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Token, "Tokens", tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$tokenBalance", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("Tokens", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    
                    // Airtime Balance Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Phone, "Airtime", tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(airtimeBalance, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("Airtime", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        
        // Pricing Info
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("How It Works", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
            }
            item {
                InfoCard(
                    icon = Icons.Default.Add,
                    title = "Buy Tokens",
                    description = "Send money to VICTOR NGETICH via M-PESA.\nKSh 10 = 90 tokens | KSh 50 = 500 tokens | KSh 100 = 1000 tokens",
                    color = Color(0xFF1A73E8)
                )
            }
            item {
                InfoCard(
                    icon = Icons.Default.ShoppingCart,
                    title = "Sell Data",
                    description = "Client sends money to YOUR M-PESA.\nApp auto-deducts tokens and dials USSD to buy data for the client.",
                    color = Color(0xFF34A853)
                )
            }
            item {
                InfoCard(
                    icon = Icons.Default.Refresh,
                    title = "Airtime Balance",
                    description = "Checks *144# every 4 seconds.\nShows real-time balance on dashboard.\nYou use airtime to sell data bundles.",
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, title, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

// ──────────────────── OFFERS SCREEN (Add/Edit Data Offers) ────────────────────
@Composable
fun OffersScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("DataOffers", Context.MODE_PRIVATE)
    val gson = remember { Gson() }
    
    var offers by remember {
        mutableStateOf(
            try {
                val json = prefs.getString("offers", "[]") ?: "[]"
                val type = object : TypeToken<List<DataOffer>>() {}.type
                gson.fromJson<List<DataOffer>>(json, type)
            } catch (e: Exception) {
                listOf(
                    DataOffer("250MB Data Bundle", 20, 10, "*180*5*2*1#", "ADVANCED"),
                    DataOffer("1GB Data Bundle", 50, 25, "*180*5*2*2#", "ADVANCED"),
                    DataOffer("Airtime KSh 20", 20, 8, "*188*1*1#", "SIMPLE")
                )
            }
        )
    }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<DataOffer?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Data Offers", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Configure products & USSD codes", fontSize = 14.sp, color = Color.Gray)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(offers) { index, offer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(offer.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("USSD: ${offer.ussdCode}", fontSize = 13.sp, color = Color.Gray)
                                Text("Price: KSh ${offer.price} | Tokens: ${offer.tokenCost} | Mode: ${offer.executionMode}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = {
                                    editingOffer = offer
                                    showAddDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF1A73E8))
                                }
                                IconButton(onClick = {
                                    offers = offers.toMutableList().also { it.removeAt(index) }
                                    val json = gson.toJson(offers)
                                    prefs.edit().putString("offers", json).apply()
                                }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFF44336))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog) {
        AddOfferDialog(
            existingOffer = editingOffer,
            onDismiss = {
                showAddDialog = false
                editingOffer = null
            },
            onSave = { offer ->
                offers = offers.toMutableList().also { list ->
                    val index = list.indexOfFirst { it.name == offer.name }
                    if (index >= 0) list[index] = offer else list.add(offer)
                }
                val json = gson.toJson(offers)
                prefs.edit().putString("offers", json).apply()
                showAddDialog = false
                editingOffer = null
            }
        )
    }
}

@Composable
fun AddOfferDialog(existingOffer: DataOffer?, onDismiss: () -> Unit, onSave: (DataOffer) -> Unit) {
    var name by remember { mutableStateOf(existingOffer?.name ?: "") }
    var price by remember { mutableStateOf(existingOffer?.price?.toString() ?: "") }
    var tokenCost by remember { mutableStateOf(existingOffer?.tokenCost?.toString() ?: "") }
    var ussdCode by remember { mutableStateOf(existingOffer?.ussdCode ?: "") }
    var executionMode by remember { mutableStateOf(existingOffer?.executionMode ?: "ADVANCED") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingOffer != null) "Edit Offer" else "Add New Offer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (KSh)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tokenCost, onValueChange = { tokenCost = it }, label = { Text("Token Cost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ussdCode, onValueChange = { ussdCode = it }, label = { Text("USSD Code (e.g., *180*5*2*1#)") }, modifier = Modifier.fillMaxWidth())
                
                // Execution Mode Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Execution Mode: ", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = executionMode == "SIMPLE",
                        onClick = { executionMode = "SIMPLE" },
                        label = { Text("SIMPLE") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = executionMode == "ADVANCED",
                        onClick = { executionMode = "ADVANCED" },
                        label = { Text("ADVANCED") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val newOffer = DataOffer(
                    name = name,
                    price = price.toIntOrNull() ?: 0,
                    tokenCost = tokenCost.toIntOrNull() ?: 0,
                    ussdCode = ussdCode,
                    executionMode = executionMode
                )
                if (newOffer.price > 0 && newOffer.tokenCost > 0 && newOffer.ussdCode.isNotEmpty()) {
                    onSave(newOffer)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ──────────────────── TRANSACTIONS SCREEN ────────────────────
@Composable
fun TransactionsScreen() {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Transaction History", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Recent automated purchases", fontSize = 14.sp, color = Color.Gray)
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, "History", tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No transactions yet", fontSize = 16.sp, color = Color.Gray)
                Text("Transactions will appear here", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

// ──────────────────── SETTINGS SCREEN ────────────────────
@Composable
fun SettingsScreen() {
    var notifications by remember { mutableStateOf(true) }
    var autoRenew by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Bingwa Mobile v1.0", fontSize = 14.sp, color = Color.Gray)
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column {
                        SwitchRow("Notifications", "Transaction alerts", Icons.Default.Notifications, notifications) { notifications = it }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchRow("Auto-Renew", "Keep services active", Icons.Default.Autorenew, autoRenew) { autoRenew = it }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Bingwa Mobile", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                        Text("Version 1.0.0", fontSize = 13.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Built for automated M-PESA services", fontSize = 12.sp, color = Color(0xFF1A73E8))
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
            Icon(icon, title, tint = Color(0xFF1A73E8), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1A73E8)))
    }
}
