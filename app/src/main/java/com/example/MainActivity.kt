package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.database.BenchmarkResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.ChatMessage
import com.example.data.database.ChatThread
import com.example.data.database.DownloadedModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LlmViewModel
import com.example.util.PhoneSpecs
import kotlinx.coroutines.launch

// High-performance extensions on ColorScheme to deliver specialized developer colors
val ColorScheme.lightText: Color get() = Color(0xFFF0F6FC)
val ColorScheme.mutedText: Color get() = Color(0xFF8B949E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LlmStudioApp()
            }
        }
    }
}

@Composable
fun LlmStudioApp() {
    val viewModel: LlmViewModel = viewModel()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val completedTutorial by viewModel.completedTutorial.collectAsStateWithLifecycle()
    
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 640
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Listen for cellular/WiFi warnings
    LaunchedEffect(Unit) {
        viewModel.wifiError.collect { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Long
            )
        }
    }

    if (!completedTutorial) {
        OnboardingTutorialScreen(viewModel)
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isTablet) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
                            label = { Text("Playground") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("tab_chats")
                        )
                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Models") },
                            label = { Text("Models") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("tab_models")
                        )
                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            icon = { Icon(Icons.Default.Speed, contentDescription = "Benchmarks") },
                            label = { Text("Benchmarks") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("tab_benchmarks")
                        )
                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { viewModel.selectTab(3) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("tab_settings")
                        )
                    }
                }
            }
        ) { paddingValues ->
            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Adaptive Navigation Drawer for large width screens (Tablets/Landscape)
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight(),
                        header = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = "LLM Studio Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "LLM Studio",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        NavigationRailItem(
                            selected = currentTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
                            label = { Text("Playground") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationRailItem(
                            selected = currentTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Models") },
                            label = { Text("Catalog") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationRailItem(
                            selected = currentTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            icon = { Icon(Icons.Default.Speed, contentDescription = "Benchmarks") },
                            label = { Text("Benchmarks") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        NavigationRailItem(
                            selected = currentTab == 3,
                            onClick = { viewModel.selectTab(3) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (currentTab) {
                            0 -> ChatPlaygroundScreen(viewModel, true)
                            1 -> ModelsCatalogScreen(viewModel)
                            2 -> BenchmarksScreen(viewModel)
                            3 -> SettingsAndHardwareScreen(viewModel)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        0 -> ChatPlaygroundScreen(viewModel, false)
                        1 -> ModelsCatalogScreen(viewModel)
                        2 -> BenchmarksScreen(viewModel)
                        3 -> SettingsAndHardwareScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingTutorialScreen(viewModel: LlmViewModel) {
    var step by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val workspacePath by viewModel.workspacePath.collectAsStateWithLifecycle()
    val workspaceStatus by viewModel.workspaceStatus.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0C20), // Dark Cosmic Slate
                        Color(0xFF15102A)  // Deep Violet Slate
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (step == index) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step == index) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                when (step) {
                    0 -> {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Welcome to LLM Studio Mobile",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Execute state-of-the-art open-weights Large Language Models (LLMs) directly inside your phone's memory. 100% offline, private, and secure.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.mutedText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { step = 1 },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Next: Storage Setup")
                        }
                    }
                    1 -> {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Structured Storage Directories",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "To keep things fully organized, we must configure a local folders architecture on your disk space. We will create a parent folder containing nested folders representing model weights and active chats:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.mutedText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Folders tree
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                FolderLineItem(name = "📁 LLM_Studio (Parent)", depth = 0)
                                FolderLineItem(name = "  📁 models     (Drop weights here!)", depth = 1)
                                FolderLineItem(name = "  📁 chat_data  (Auto-saves conversations)", depth = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (workspacePath.isNullOrEmpty()) {
                            Button(
                                onClick = { viewModel.initializeWorkspaceDirectories(context) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Workspace Directories")
                            }
                        } else {
                            Surface(
                                color = Color(0xFF1E3A1E),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Workspace initialized inside storage!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Green,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (workspaceStatus != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = workspaceStatus ?: "",
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { step = 0 }) {
                                Text("Back")
                            }
                            Button(
                                onClick = { step = 2 },
                                enabled = !workspacePath.isNullOrEmpty()
                            ) {
                                Text("Next")
                            }
                        }
                    }
                    2 -> {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Physical Model Sideloads",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You don't need active WiFi to load! You can transfer downloaded GGUF/bin weights from elsewhere on your PC, place them inside the 'LLM_Studio/models' subdirectory on your storage space, and clicking 'Scan Folder' inside LLM Studio will auto-discover and load them instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.mutedText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Path:\n${workspacePath}/models/",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { step = 1 }) {
                                Text("Back")
                            }
                            Button(
                                onClick = { viewModel.completeOnboarding() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Enter Sandbox Hub")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderLineItem(name: String, depth: Int) {
    Text(
        text = name,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = if (depth == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.mutedText,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun BenchmarksScreen(viewModel: LlmViewModel) {
    val models by viewModel.models.collectAsStateWithLifecycle()
    val benchmarks by viewModel.benchmarks.collectAsStateWithLifecycle()
    val isBenchmarking by viewModel.isBenchmarking.collectAsStateWithLifecycle()
    val progressText by viewModel.benchmarkProgressText.collectAsStateWithLifecycle()
    val progressVal by viewModel.benchmarkProgressVal.collectAsStateWithLifecycle()
    val activeProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()

    val downloadedModels = remember(models) { models.filter { it.isDownloaded } }
    var selectedModelId by remember { mutableStateOf("") }
    
    // Auto-select the first downloaded model
    LaunchedEffect(downloadedModels) {
        if (selectedModelId.isEmpty() && downloadedModels.isNotEmpty()) {
            selectedModelId = downloadedModels.first().id
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Text(
                "DEEP HARDWARE BENCHMARK SUITE",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                "Profile local INT4 tensor decompression, decoding matrices, memory bandwidths and thermals safely on your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.mutedText,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        if (isBenchmarking) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { progressVal },
                            modifier = Modifier.size(72.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = progressText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progressVal },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Estimated Time Remaining: ${(4.0 * (1.0f - progressVal)).toInt()}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.mutedText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "RUN BENCHMARK ENGINE",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (downloadedModels.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "No Local Weights Installed!",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "You must download or sideload at least one model from the Catalog tab before starting the local hardware profiling.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        } else {
                            Text(
                                "SELECT MODEL FOR PROFILING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.mutedText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            var expandedDropdown by remember { mutableStateOf(false) }
                            val activeModelSelected = downloadedModels.find { it.id == selectedModelId }
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedDropdown = true }
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = activeModelSelected?.name ?: "Select downloaded model",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    downloadedModels.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m.name, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                selectedModelId = m.id
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "COMPUTATION DELEGATE BACKEND",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.mutedText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("GPU-Vulkan", "CPU-TFLite", "NNAPI-Hexagon").forEach { prov ->
                                    val isSel = activeProvider == prov
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.selectedProvider.value = prov },
                                        color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = prov.replace("-", "\n"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { viewModel.runModelBenchmark(selectedModelId) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Execute Chips Benchmark")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PROFILING HISTORY RECORDS",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                if (benchmarks.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearBenchmarks() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (benchmarks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.mutedText.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No Benchmark History Logged Yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.mutedText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(benchmarks, key = { it.id }) { record ->
                BenchmarkResultRow(record = record, onDelete = { viewModel.deleteBenchmark(record) })
            }
        }
    }
}

@Composable
fun BenchmarkResultRow(record: BenchmarkResult, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.modelName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = record.executionProvider,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = java.text.SimpleDateFormat("MMM dd, yyyy - HH:mm", java.util.Locale.getDefault()).format(record.timestamp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.mutedText
                        )
                    }
                }

                Surface(
                    color = when {
                        record.score >= 1200 -> Color(0xFFD4AF37).copy(alpha = 0.18f) // Gold
                        record.score >= 600 -> Color(0xFFC0C0C0).copy(alpha = 0.18f) // Silver
                        else -> Color(0xFFCD7F32).copy(alpha = 0.18f) // Bronze
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            record.score >= 1200 -> Color(0xFFD4AF37)
                            record.score >= 600 -> Color(0xFFC0C0C0)
                            else -> Color(0xFFCD7F32)
                        }
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "INDEX SCORE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${record.score}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                record.score >= 1200 -> Color(0xFFD4AF37)
                                record.score >= 600 -> Color(0xFFE2E2E2)
                                else -> Color(0xFFCD7F32)
                            }
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

            // Micro dashboard metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MicroMetricItem(
                    label = "DECODE SPEED",
                    value = "${"%.1f".format(record.tokensPerSecond)} t/s",
                    desc = "Higher is better"
                )
                MicroMetricItem(
                    label = "PREFILL LATENCY",
                    value = "${record.timeToFirstTokenMs} ms",
                    desc = "Lower is better"
                )
                MicroMetricItem(
                    label = "HEAP LOAD",
                    value = "${"%.0f".format(record.ramConsumedMb)} MB",
                    desc = "RAM footprint"
                )
                MicroMetricItem(
                    label = "THERMAL INFL",
                    value = "+${"%.1f".format(record.tempDeltaCelsius)}°C",
                    desc = "Temp Delta"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = MaterialTheme.colorScheme.mutedText.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MicroMetricItem(label: String, value: String, desc: String) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.mutedText
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.mutedText.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ImportDiskFolderCard(viewModel: LlmViewModel) {
    val context = LocalContext.current
    val workspacePath by viewModel.workspacePath.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LOCAL SIDELOADING PORT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Transfer downloaded GGUF/bin weights into your subfolders, then trigger the auto-import scan below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.mutedText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (!workspacePath.isNullOrEmpty()) ".../LLM_Studio/models/" else "Workspace not configured!",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { viewModel.scanLocalModelsFolder(context) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan 'LLM_Studio/models' Folder", style = MaterialTheme.typography.labelMedium)
            }

            if (scanProgress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = scanProgress ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModelsCatalogScreen(viewModel: LlmViewModel) {
    val models by viewModel.models.collectAsStateWithLifecycle()
    val downloadMetrics by viewModel.downloadMetrics.collectAsStateWithLifecycle()
    val phoneSpecs = viewModel.phoneSpecs
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // High-end hardware checking module (PROFILER)
        item {
            HardwareProfilerCard(phoneSpecs = phoneSpecs)
            Spacer(modifier = Modifier.height(16.dp))
            ImportDiskFolderCard(viewModel = viewModel)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "AVAILABLE MOBILE MODELS",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(models, key = { it.id }) { model ->
            val metrics = downloadMetrics[model.id]
            val isRecommended = model.id == phoneSpecs.recommendedModelId

            // Warning checking logic, e.g. if the user only has 4GB and model is 5GB weights
            val totalRamGb = phoneSpecs.totalRamGb
            val isRamHazard = (model.id == "phi-3-mini" && totalRamGb < 5.0) ||
                    (model.id == "llama-3-8b-it" && totalRamGb < 9.0) ||
                    (model.id == "gemma-4-9b-it" && totalRamGb < 9.5)

            ModelCatalogRow(
                model = model,
                metrics = metrics,
                isRecommended = isRecommended,
                isRamHazard = isRamHazard,
                onDownload = { viewModel.startDownload(context, model.id) },
                onDelete = { viewModel.deleteDownloadedModel(model.id) },
                onUse = { viewModel.createNewChat(model.id) }
            )
        }
    }
}

@Composable
fun ChatPlaygroundScreen(viewModel: LlmViewModel, isTablet: Boolean) {
    val chatThreads by viewModel.chatThreads.collectAsStateWithLifecycle()
    val activeThreadId by viewModel.activeThreadId.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val activeModelId by viewModel.activeModelId.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var promptInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Persistent sidebar showing chat history list on large screens
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(0.dp))
                    .padding(12.dp)
            ) {
                Button(
                    onClick = {
                        val firstDl = models.find { it.isDownloaded } ?: models.firstOrNull()
                        firstDl?.let { viewModel.createNewChat(it.id) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_sidebar_new_chat"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Chat Session")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "SESSIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chatThreads, key = { it.id }) { thread ->
                        ChatThreadRow(
                            thread = thread,
                            isSelected = thread.id == activeThreadId,
                            onSelect = { viewModel.selectThread(thread.id) },
                            onDelete = { viewModel.deleteThread(thread) }
                        )
                    }
                }
            }

            // Action Core Playground Area for Tablet
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                PlaygroundMainContent(
                    viewModel = viewModel,
                    activeModelId = activeModelId,
                    models = models,
                    chatThreads = chatThreads,
                    activeThreadId = activeThreadId,
                    activeMessages = activeMessages,
                    isGenerating = isGenerating,
                    isTablet = true,
                    promptInput = promptInput,
                    onPromptInputChange = { promptInput = it },
                    keyboardController = keyboardController
                )
            }
        }
    } else {
        // Simple direct full-screen visual viewport for Mobile
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            PlaygroundMainContent(
                viewModel = viewModel,
                activeModelId = activeModelId,
                models = models,
                chatThreads = chatThreads,
                activeThreadId = activeThreadId,
                activeMessages = activeMessages,
                isGenerating = isGenerating,
                isTablet = false,
                promptInput = promptInput,
                onPromptInputChange = { promptInput = it },
                keyboardController = keyboardController
            )
        }
    }
}

@Composable
fun ColumnScope.PlaygroundMainContent(
    viewModel: LlmViewModel,
    activeModelId: String,
    models: List<DownloadedModel>,
    chatThreads: List<ChatThread>,
    activeThreadId: Int?,
    activeMessages: List<ChatMessage>,
    isGenerating: Boolean,
    isTablet: Boolean,
    promptInput: String,
    onPromptInputChange: (String) -> Unit,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    // Interactive Mini Header for Active Model and stats
    ActiveChatHeader(
        viewModel = viewModel,
        activeModelId = activeModelId,
        models = models,
        chatThreads = chatThreads,
        activeThreadId = activeThreadId,
        isTablet = isTablet
    )

    val isLoadingModel by viewModel.isLoadingModel.collectAsStateWithLifecycle()
    val modelLoadingProgressVal by viewModel.modelLoadingProgressVal.collectAsStateWithLifecycle()
    val modelLoadingProgressText by viewModel.modelLoadingProgressText.collectAsStateWithLifecycle()

    val activeModel = models.find { it.id == activeModelId }
    val isDownloaded = activeModel?.isDownloaded ?: false
    val isDownloading = activeModel?.isDownloading ?: false
    val metricsMap by viewModel.downloadMetrics.collectAsStateWithLifecycle()
    val activeMetrics = metricsMap[activeModelId]
    val context = LocalContext.current

    if (isLoadingModel != null) {
        ModelInitializationOverlay(
            viewModel = viewModel,
            modelId = isLoadingModel!!,
            progress = modelLoadingProgressVal,
            progressText = modelLoadingProgressText
        )
    } else {
        if (!isDownloaded) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isDownloading) Icons.Default.CloudDownload else Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isDownloading) "Downloading Model Weights" else "Weights Download Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isDownloading) {
                                "Broadcasting quantized parameters of ${activeModel?.name ?: "selected model"} directly into sandboxed app storage. Please persist connection."
                            } else {
                                "To run ${activeModel?.name ?: "selected model"} locally on your $activeModelId context with absolute data privacy, download its validated parameter weights."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (isDownloading) {
                            val pct = activeMetrics?.progressPercent ?: (activeModel?.downloadProgress?.times(100))?.toInt() ?: 0
                            val speedStr = activeMetrics?.speedMbSeconds?.let { "%.1f MB/s".format(it) } ?: "Fast Sim"
                            val timeStr = activeMetrics?.timeRemainingSeconds?.let { "${it}s remaining" } ?: "Streaming..."
                            val sizeStr = activeMetrics?.totalMbDownloaded?.let { "%.1f / %.1f MB".format(it, activeMetrics.totalMbSize) } ?: ""

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$pct% Completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$speedStr • $timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                if (sizeStr.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = sizeStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        } else {
                            val sizeGiga = activeModel?.sizeBytes?.let { String.format("%.2f GB", it.toDouble() / (1024*1024*1024)) } ?: "1.5 GB"
                            Button(
                                onClick = { viewModel.startDownload(context, activeModelId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("download_model_playground_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Download Model Weights ($sizeGiga)",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { viewModel.selectTab(1) }, // Catalog page
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Browse Complete Catalog", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            // Dynamic conversation viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeThreadId == null || chatThreads.isEmpty()) {
                    EmptyChatState { selectedModelId ->
                        viewModel.createNewChat(selectedModelId)
                    }
                } else {
                    val listState = rememberLazyListState()

                    // Scroll to bottom every time a message is added or streamed
                    LaunchedEffect(activeMessages.size, isGenerating) {
                        if (activeMessages.isNotEmpty()) {
                            listState.animateScrollToItem(activeMessages.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
                    ) {
                        items(activeMessages, key = { it.id }) { message ->
                            ChatMessageBubble(message = message, phoneSpecs = viewModel.phoneSpecs)
                        }
                        if (isGenerating && activeMessages.lastOrNull()?.sender == "user") {
                            item {
                                AssistantLoadingBubble()
                            }
                        }
                    }
                }

                // Cozy Bottom floating visual panel of prompt suggestions
                if (activeMessages.isEmpty() && activeThreadId != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 96.dp)
                    ) {
                        SuggestionRow { tappedPrompt ->
                            onPromptInputChange(tappedPrompt)
                        }
                    }
                }
            }

            // Input Send Field Frame (Only if the model is downloaded!)
            if (activeThreadId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val model = models.find { it.id == activeModelId }
                                model?.let {
                                    onPromptInputChange("Under ${viewModel.selectedProvider.value}, analyze structural performance metrics of ${it.name} models on custom ${viewModel.phoneSpecs.cpuCores}-core chips.")
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Quick Query Prompt Helper",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextField(
                            value = promptInput,
                            onValueChange = onPromptInputChange,
                            placeholder = { Text("Ask local model anything...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (promptInput.isNotBlank() && !isGenerating) {
                                        viewModel.sendMessageInActiveThread(promptInput)
                                        onPromptInputChange("")
                                        keyboardController?.hide()
                                    }
                                }
                            ),
                            enabled = !isGenerating,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (promptInput.isNotBlank() && !isGenerating) {
                                    viewModel.sendMessageInActiveThread(promptInput)
                                    onPromptInputChange("")
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (promptInput.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    shape = CircleShape
                                )
                                .testTag("chat_send_button"),
                            enabled = promptInput.isNotBlank() && !isGenerating
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send prompt button",
                                tint = if (promptInput.isNotBlank() && !isGenerating) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveChatHeader(
    viewModel: LlmViewModel,
    activeModelId: String,
    models: List<DownloadedModel>,
    chatThreads: List<ChatThread>,
    activeThreadId: Int?,
    isTablet: Boolean
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var renameExpanded by remember { mutableStateOf(false) }
    var selectedModelDetails = models.find { it.id == activeModelId }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isTablet) {
                // Interactive bottom-sheet styled model list trigger
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { dropdownExpanded = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedModelDetails?.name ?: "No model selected",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.lightText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(8.dp)
                                    .background(
                                        if (selectedModelDetails?.isDownloaded == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (selectedModelDetails?.isDownloaded == true) "Local [INT4 Quantized]" else "Cloud Gateway [Direct REST API]",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.mutedText
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        models.forEach { m ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(m.name, color = MaterialTheme.colorScheme.onBackground)
                                        if (m.isDownloaded) {
                                            Text(
                                                "Offline",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.createNewChat(m.id)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                // Tablet header (Simple name and specs row)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedModelDetails?.name ?: "No Model Selected",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.lightText
                    )
                    Text(
                        text = "Quantization: ${selectedModelDetails?.quantization ?: "N/A"} • Accelerator: ${viewModel.selectedProvider.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.mutedText
                    )
                }
            }

            // Quick Hardware Profile Tag! Displays phone specification rating and warning highlights
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = viewModel.selectedProvider.value.split("-").last(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Active session drop anchor list menu
            if (!isTablet && chatThreads.isNotEmpty()) {
                var sessionMenuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { sessionMenuExpanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Session Manager Options",
                        tint = MaterialTheme.colorScheme.mutedText
                    )
                }

                DropdownMenu(
                    expanded = sessionMenuExpanded,
                    onDismissRequest = { sessionMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    chatThreads.forEach { thread ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = thread.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (thread.id == activeThreadId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteThread(thread) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Chat Thread",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.selectThread(thread.id)
                                sessionMenuExpanded = false
                            }
                        )
                    }
                    Divider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("New Session", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            viewModel.createNewChat(activeModelId)
                            sessionMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(onStartChat: (String) -> Unit) {
    val suggestedModels = listOf(
        Triple("gemma-2b-it", "Gemma 2B", "Google's optimized compiler model"),
        Triple("tinyllama-1.1b-instruct", "TinyLlama 1.1B", "Fast, low footprint model"),
        Triple("phi-3-mini", "Phi-3 Mini", "High logic Microsoft edge model")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Welcome to LLM Studio Mobile",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "To begin local on-device weight compilation, select a pre-seeded target model to spin up a standalone playground thread.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.mutedText,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "SPIN UP A LOCAL THREAD",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        suggestedModels.forEach { (id, name, desc) ->
            Card(
                onClick = { onStartChat(id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.lightText)
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.mutedText)
                    }
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ChatThreadRow(
    thread: ChatThread,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.mutedText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = thread.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.lightText else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.62f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage, phoneSpecs: PhoneSpecs) {
    val isUser = message.sender == "user"
    val backgroundBrush = if (isUser) {
        Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
        )
    }

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .background(backgroundBrush, shape)
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = shape
                )
                .padding(14.dp)
        ) {
            Column {
                if (!isUser) {
                    Text(
                        text = "LOCAL MODEL RESPONSE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                val content = message.content
                if (!isUser && content.contains("```")) {
                    MarkdownCodeFormatter(content)
                } else {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Immersive diagnostics overlay details for local executing metrics
        if (!isUser && message.tokensPerSecond != null) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.mutedText,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${"%.1f".format(message.tokensPerSecond)} t/s  •  TTFT: ${message.timeToFirstTokenMs}ms  •  Engine: ${message.executionProvider ?: "CPU"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.mutedText
                )
            }
        }
    }
}

@Composable
fun MarkdownCodeFormatter(content: String) {
    val items = content.split("```")
    Column {
        items.forEachIndexed { idx, part ->
            if (idx % 2 == 1) {
                // Code block payload
                val codeWithLang = part.trim()
                val lines = codeWithLang.split("\n")
                val lang = lines.firstOrNull()?.take(8) ?: "code"
                val codeBody = if (lines.size > 1) lines.drop(1).joinToString("\n") else codeWithLang

                Surface(
                    color = Color(0xFF07090C),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.mutedText, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = codeBody,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                if (part.isNotEmpty()) {
                    Text(
                        text = part,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantLoadingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp),
            modifier = Modifier
                .widthIn(max = 240.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    "MODEL PIPELINE STATUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Compiling weights graph...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.mutedText
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionRow(onSelectPrompt: (String) -> Unit) {
    val prompts = listOf(
        "Explain INT4 quantization",
        "Write a Kotlin Coroutine helper",
        "Explain GPU vs CPU inference"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        prompts.forEach { text ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectPrompt(text) }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        minLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}



@Composable
fun HardwareProfilerCard(phoneSpecs: PhoneSpecs) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DeveloperMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "HARDWARE DELEGATE PROFILER",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.lightText
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "VOUCHED BY SYSTEM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    ProfilerInfoItem(label = "PHONE MODEL", value = "${phoneSpecs.manufacturer} ${phoneSpecs.model}")
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfilerInfoItem(label = "AVAILABLE METRICS", value = "${phoneSpecs.cpuCores} CPU Cores / ${phoneSpecs.cpuArch}")
                }
                Column(modifier = Modifier.weight(1f)) {
                    ProfilerInfoItem(label = "PHYSICAL RAM", value = "${"%.2f".format(phoneSpecs.totalRamGb)} GB RAM")
                    Spacer(modifier = Modifier.height(10.dp))
                    ProfilerInfoItem(label = "STORAGE DELEGATION", value = "${"%.1f".format(phoneSpecs.freeStorageGb)} GB Free")
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 14.dp))

            // Shows a beautiful highlighted target recommendation
            Column {
                Text(
                    "BEST SYSTEM MATCH",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.mutedText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = phoneSpecs.recommendedModelName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilerInfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.mutedText)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun ModelInitializationOverlay(
    viewModel: LlmViewModel,
    modelId: String,
    progress: Float,
    progressText: String
) {
    val models by viewModel.models.collectAsStateWithLifecycle()
    val model = models.find { it.id == modelId }
    val modelName = model?.name ?: "Local Model"
    val specs = viewModel.phoneSpecs

    val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val angleReverse by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_reverse"
    )
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            ) {}
            
            // Outer Reverse Spinner
            CircularProgressIndicator(
                modifier = Modifier
                    .size(165.dp)
                    .rotate(angleReverse),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                strokeWidth = 3.dp,
                trackColor = Color.Transparent
            )

            // Main Rotating Progress Spinner
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .size(140.dp)
                    .scale(scalePulse)
                    .rotate(angle),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "INITIALIZING MODEL ENGINE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = modelName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DeveloperBoard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${viewModel.selectedProvider.value} Hardware Acceleration Enabled",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F0B1D)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                reverseLayout = true
            ) {
                item {
                    Text(
                        text = ">>> MEMORY STAGE: ALLOCATED ${model?.sizeBytes?.let { String.format("%.2f", it.toDouble() / (1024*1024*1024)) } ?: "1.8"} GB",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF64FFDA)
                    )
                }
                item {
                    Text(
                        text = ">>> CPU CORES DETECTED: ${specs.cpuCores} threads enabled",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
                item {
                    Text(
                        text = ">>> ACTIVE HARDWARE INTERPRETER: TFLite (Vulkan GPU Native Bound)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
                item {
                    Text(
                        text = ">>> VULKAN COMPUTE INSTANCE COMPILING RUNTIME PARAMS...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
                item {
                    Text(
                        text = ">>> GGUF TENSOR ARCHITECTURE: ${model?.quantization ?: "INT4"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }
                item {
                    Text(
                        text = "--- ON-DEVICE SYSTEM INTERFACING LOGS ---",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModelCatalogRow(
    model: DownloadedModel,
    metrics: LlmViewModel.ModelDownloadMetrics?,
    isRecommended: Boolean,
    isRamHazard: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onUse: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                width = if (isRecommended) 1.5.dp else 1.dp,
                color = when {
                    isRecommended -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    isRamHazard -> Color.Red.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Model title details and dynamic recommendation stars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        model.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.lightText
                    )
                    Text(
                        "Params: ${model.parameterCount}  •  Quantization: ${model.quantization}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.mutedText
                    )
                }

                // Star badge for recommended, and danger warning for RAM hazards
                Row {
                    if (isRecommended) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                "BEST MATCH ⭐",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (isRamHazard) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "RAM OUT OF RANGE ⚠️",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description info block
            Text(
                model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.mutedText,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row / Downloading bar specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${"%.2f".format(model.sizeBytes.toDouble() / (1024 * 1024 * 1024))} GB Weights",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                if (model.isDownloaded) {
                    Row {
                        TextButton(
                            onClick = { onDelete() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.72f))
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete File")
                        }
                        Button(
                            onClick = { onUse() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Chat with Model", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else if (model.isDownloading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = model.downloadProgress,
                                modifier = Modifier
                                    .width(100.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            metrics?.let {
                                Text(
                                    "${it.progressPercent}% at ${"%.1f".format(it.speedMbSeconds)} MB/s",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        metrics?.let {
                            Text(
                                "${it.timeRemainingSeconds}s remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.mutedText
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { onDownload() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("download_model_${model.id}")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Weights")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndHardwareScreen(viewModel: LlmViewModel) {
    val temp by viewModel.temperature.collectAsStateWithLifecycle()
    val topKVal by viewModel.topK.collectAsStateWithLifecycle()
    val topPVal by viewModel.topP.collectAsStateWithLifecycle()
    val activeProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val showEdgeOnly by viewModel.showEdgeGalleryOnly.collectAsStateWithLifecycle()
    val customKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val customUrl by viewModel.customModelUrl.collectAsStateWithLifecycle()
    val customName by viewModel.customModelName.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        item {
            Text(
                "ADVANCED GENERATION SPECIFICATIONS",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Temperature Controller
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Temperature", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.lightText)
                        Text("${"%.2f".format(temp)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Controls diversity of weights. Higher temperatures make output more creative but potentially unpredictable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.mutedText,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Slider(
                        value = temp,
                        onValueChange = { viewModel.temperature.value = it },
                        valueRange = 0.1f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Top-p and Top-k controllers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top-K Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Top-K", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.lightText)
                            Text("$topKVal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = topKVal.toFloat(),
                            onValueChange = { viewModel.topK.value = it.toInt() },
                            valueRange = 1f..100f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // Top-P Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Top-P", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.lightText)
                            Text("${"%.2f".format(topPVal)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = topPVal,
                            onValueChange = { viewModel.topP.value = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Google AI Edge Execution backend settings
            Text(
                "GOOGLE AI EDGE / DELEGATE ENGINE",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Inference Execution Core",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.lightText,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Select which physical hardware layer LLM Studio executes tensor compilation on.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.mutedText,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val providers = listOf(
                        "GPU-Vulkan" to "GPU Acceleration (MTL/Vulkan)",
                        "CPU-TFLite" to "Standard Multi-Core CPU (9.6 t/s)",
                        "NNAPI-Hexagon" to "NPU Neural Co-Processor (Qualcomm NNAPI)"
                    )

                    providers.forEach { (provId, provName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectedProvider.value = provId }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = activeProvider == provId,
                                onClick = { viewModel.selectedProvider.value = provId },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(provName, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Cloud Google Gemini API Gateway Overrides
            Text(
                "CLOUD GATEWAY & DEVELOPMENT API",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Configure Own Gemini API Key",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.lightText
                    )
                    Text(
                        "Allows the system to connect online for complex intelligence fallback when models are un-downloaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.mutedText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customKey,
                        onValueChange = { viewModel.customApiKey.value = it },
                        label = { Text("Gemini API Overwrite Key") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_field"),
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // HuggingFace model compiler registration
            Text(
                "HUGGINGFACE MODEL CUSTOM EXPORTER",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Stream custom GGUF/WASM weights",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.lightText
                    )
                    Text(
                        "Input any valid Direct GGUF weights download link (HuggingFace Resolve URL) to load custom models into your mobile system.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.mutedText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { viewModel.customModelName.value = it },
                        label = { Text("Model Visual Designation Name") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_model_name"),
                        placeholder = { Text("e.g. MyCustomGemma-3B") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { viewModel.customModelUrl.value = it },
                        label = { Text("Direct GGUF Weights URL") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_model_url"),
                        placeholder = { Text("https://huggingface.co/...") }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.registerCustomConfigAndDownload() },
                        enabled = customUrl.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_custom_url_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Register and Compile weights file", color = Color(0xFF040D14))
                    }
                }
            }
        }
    }
}
