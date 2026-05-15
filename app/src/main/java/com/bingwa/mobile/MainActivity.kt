@file:OptIn(ExperimentalMaterial3Api::class)

package com.bingwa.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("automation_enabled", true)) {
            startService(Intent(this, BalanceChecker::class.java))
        }
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

    DisposableEffect(Unit) {
        BalanceChecker.balanceCallback = { balance ->
            airtimeBalance = balance
            val regex = Regex("Ksh\\.?\\s*(\\d+\\.?\\d*)", RegexOption.IGNORE_CASE)
            val match = regex.find(balance)
            if (match != null) {
                airtimeBalance = "KSh ${match.groupValues[1]}"
            }
        }
        onDispose { BalanceChecker.balanceCallback = null }
    }

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

// ─── Dashboard ───
@Composable
fun DashboardScreen(tokenBalance: Int, airtimeBalance: String) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF1557B0))))
                .padding(24.dp)
        ) {
            Column {
                Text("Bingwa Mobile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Automated M-PESA Agent", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Token, "Tokens", tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("$tokenBalance", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("Tokens", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))) {
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
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("How It Works", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121)) }
            item { InfoCard(Icons.Default.Add, "Buy Tokens", "Send money to VICTOR NGETICH via M-PESA.\nKSh 10 = 90 tokens | KSh 50 = 500 tokens | KSh 100 = 1000 tokens", Color(0xFF1A73E8)) }
            item { InfoCard(Icons.Default.ShoppingCart, "Sell Data", "Client sends money to YOUR M-PESA.\nApp auto-deducts tokens and dials USSD to buy data for the client.", Color(0xFF34A853)) }
            item { InfoCard(Icons.Default.Refresh, "Airtime Balance", "Checks *144# every 4 seconds.\nShows real-time balance on dashboard.\nYou use airtime to sell data bundles.", Color(0xFFFF9800)) }
        }
    }
}

@Composable
fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, color: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
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

// ─── Offers ───
@Composable
fun OffersScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("DataOffers", Context.MODE_PRIVATE)
    val gson = remember { Gson() }
    var offers by remember {
        mutableStateOf(try {
            val json = prefs.getString("offers", "[]") ?: "[]"
            val type = object : TypeToken<List<DataOffer>>() {}.type
            gson.fromJson<List<DataOffer>>(json, type)
        } catch (e: Exception) {
            listOf(DataOffer("250MB Data Bundle", 20, 10, "*180*5*2*1#", "ADVANCED"), DataOffer("1GB Data Bundle", 50, 25, "*180*5*2*2#", "ADVANCED"), DataOffer("Airtime KSh 20", 20, 8, "*188*1*1#", "SIMPLE"))
        })
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingOffer by remember { mutableStateOf<DataOffer?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Data Offers", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Configure products & USSD codes", fontSize = 14.sp, color = Color.Gray)
                }
                Button(onClick = { showAddDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(offers) { index, offer ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(offer.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("USSD: ${offer.ussdCode}", fontSize = 13.sp, color = Color.Gray)
                                Text("Price: KSh ${offer.price} | Tokens: ${offer.tokenCost} | Mode: ${offer.executionMode}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = { editingOffer = offer; showAddDialog = true }) { Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF1A73E8)) }
                                IconButton(onClick = {
                                    offers = offers.toMutableList().also { it.removeAt(index) }
                                    prefs.edit().putString("offers", gson.toJson(offers)).apply()
                                }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFF44336)) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAddDialog) {
        AddOfferDialog(existingOffer = editingOffer, onDismiss = { showAddDialog = false; editingOffer = null }, onSave = { offer ->
            offers = offers.toMutableList().also { list ->
                val idx = list.indexOfFirst { it.name == offer.name }
                if (idx >= 0) list[idx] = offer else list.add(offer)
            }
            prefs.edit().putString("offers", gson.toJson(offers)).apply()
            showAddDialog = false; editingOffer = null
        })
    }
}

@Composable
fun AddOfferDialog(existingOffer: DataOffer?, onDismiss: () -> Unit, onSave: (DataOffer) -> Unit) {
    var name by remember { mutableStateOf(existingOffer?.name ?: "") }
    var price by remember { mutableStateOf(existingOffer?.price?.toString() ?: "") }
    var tokenCost by remember { mutableStateOf(existingOffer?.tokenCost?.toString() ?: "") }
    var ussdCode by remember { mutableStateOf(existingOffer?.ussdCode ?: "") }
    var executionMode by remember { mutableStateOf(existingOffer?.executionMode ?: "ADVANCED") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existingOffer != null) "Edit Offer" else "Add New Offer") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (KSh)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tokenCost, onValueChange = { tokenCost = it }, label = { Text("Token Cost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = ussdCode, onValueChange = { ussdCode = it }, label = { Text("USSD Code (e.g., *180*5*2*1#)") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Execution Mode: ", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = executionMode == "SIMPLE", onClick = { executionMode = "SIMPLE" }, label = { Text("SIMPLE") })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = executionMode == "ADVANCED", onClick = { executionMode = "ADVANCED" }, label = { Text("ADVANCED") })
            }
        }
    }, confirmButton = {
        Button(onClick = {
            val newOffer = DataOffer(name = name, price = price.toIntOrNull() ?: 0, tokenCost = tokenCost.toIntOrNull() ?: 0, ussdCode = ussdCode, executionMode = executionMode)
            if (newOffer.price > 0 && newOffer.tokenCost > 0 && newOffer.ussdCode.isNotEmpty()) onSave(newOffer)
        }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─── Transactions ───
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

// ─── Settings ───
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    var automationEnabled by remember { mutableStateOf(prefs.getBoolean("automation_enabled", true)) }
    var notifications by remember { mutableStateOf(true) }
    var selectedSim by remember { mutableStateOf(prefs.getInt("selected_sim_id", -1)) }
    var showSimDialog by remember { mutableStateOf(false) }

    val simList = remember { getAvailableSims(context) }
    val selectedSimLabel = if (selectedSim == -1) "Default SIM" else simList.find { it.subscriptionId == selectedSim }?.displayName?.toString() ?: "Unknown"

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Bingwa Mobile v1.0", fontSize = 14.sp, color = Color.Gray)
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column {
                        SwitchRow("Enable Automation", "Turn on/off SMS & USSD processing", Icons.Default.PowerSettingsNew, automationEnabled) {
                            automationEnabled = it
                            prefs.edit().putBoolean("automation_enabled", it).apply()
                            if (it) context.startService(Intent(context, BalanceChecker::class.java))
                            else context.stopService(Intent(context, BalanceChecker::class.java))
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SimCard, "SIM", tint = Color(0xFF1A73E8), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Select SIM for USSD", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(selectedSimLabel, fontSize = 13.sp, color = Color.Gray)
                            }
                            TextButton(onClick = { showSimDialog = true }) { Text("Change") }
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchRow("Notifications", "Transaction alerts", Icons.Default.Notifications, notifications) { notifications = it }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        SwitchRow("Auto-Renew", "Keep services active", Icons.Default.Autorenew, true) { }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bingwa Mobile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Version 1.0.0", fontSize = 13.sp, color = Color.Gray)
                        Text("Powered by Victor Ngetich", fontSize = 12.sp, color = Color(0xFF1A73E8))
                    }
                }
            }
        }
    }

    if (showSimDialog) {
        SimSelectionDialog(simList = simList, currentSelection = selectedSim, onDismiss = { showSimDialog = false }, onSelect = { simId ->
            prefs.edit().putInt("selected_sim_id", simId).apply()
            selectedSim = simId
            showSimDialog = false
        })
    }
}

@SuppressLint("MissingPermission")
fun getAvailableSims(context: Context): List<SubscriptionInfo> {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return emptyList()
    val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) subscriptionManager.activeSubscriptionInfoList ?: emptyList() else emptyList()
}

@Composable
fun SimSelectionDialog(simList: List<SubscriptionInfo>, currentSelection: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select SIM for USSD") }, text = {
        if (simList.isEmpty()) Text("No SIM cards found or permission not granted.")
        else Column {
            simList.forEach { sim ->
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = sim.subscriptionId == currentSelection, onClick = { onSelect(sim.subscriptionId) })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${sim.displayName} (Slot ${sim.simSlotIndex + 1})")
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }, dismissButton = null)
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