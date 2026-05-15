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
        // Start background balance checker only if automation enabled
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

// ... (DashboardScreen, InfoCard, OffersScreen, AddOfferDialog, TransactionsScreen remain unchanged) ...

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
                        // Master automation toggle
                        SwitchRow("Enable Automation", "Turn on/off SMS & USSD processing", Icons.Default.PowerSettingsNew,
                            checked = automationEnabled,
                            onChange = {
                                automationEnabled = it
                                prefs.edit().putBoolean("automation_enabled", it).apply()
                                if (it) {
                                    context.startService(Intent(context, BalanceChecker::class.java))
                                } else {
                                    context.stopService(Intent(context, BalanceChecker::class.java))
                                }
                            })
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        // SIM selection
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SimCard, "SIM", tint = Color(0xFF1A73E8), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Select SIM for USSD", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text(selectedSimLabel, fontSize = 13.sp, color = Color.Gray)
                            }
                            TextButton(onClick = { showSimDialog = true }) {
                                Text("Change")
                            }
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
        SimSelectionDialog(
            simList = simList,
            currentSelection = selectedSim,
            onDismiss = { showSimDialog = false },
            onSelect = { simId ->
                prefs.edit().putInt("selected_sim_id", simId).apply()
                selectedSim = simId
                showSimDialog = false
            }
        )
    }
}

@SuppressLint("MissingPermission")
fun getAvailableSims(context: Context): List<SubscriptionInfo> {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }
    val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        subscriptionManager.activeSubscriptionInfoList ?: emptyList()
    } else emptyList()
}

@Composable
fun SimSelectionDialog(
    simList: List<SubscriptionInfo>,
    currentSelection: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select SIM for USSD") },
        text = {
            if (simList.isEmpty()) {
                Text("No SIM cards found or permission not granted.")
            } else {
                Column {
                    simList.forEach { sim ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sim.subscriptionId == currentSelection,
                                onClick = { onSelect(sim.subscriptionId) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${sim.displayName} (Slot ${sim.simSlotIndex + 1})")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = null
    )
}

// --- The rest of the composables (DashboardScreen, OffersScreen, etc.) remain as before ---
// (Copy them from the previous fully corrected version)