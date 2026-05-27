package com.omaster.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.model.Preset
import com.omaster.app.service.CommunityService
import com.omaster.app.ui.components.PresetCard
import com.omaster.app.ui.theme.AccentPrimary
import com.omaster.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun CommunityScreen(
    onBack: () -> Unit,
    onPresetClick: (Preset) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val communityService = remember { CommunityService(context) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var communityPresets by remember { mutableStateOf<List<CommunityService.CommunityPreset>>(emptyList()) }
    var popularTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        isLoading = true
        val result = communityService.browseCommunityPresets()
        if (result.success) {
            communityPresets = result.presets ?: emptyList()
        }
        popularTags = communityService.getPopularTags()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("社区") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索社区预设...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(popularTags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = if (selectedTag == tag) null else tag },
                        label = { Text(tag) }
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("热门") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("最新") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("精选") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (communityPresets.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "🌟",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "社区暂时没有预设",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "成为第一个分享者吧！",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(communityPresets, key = { it.id }) { communityPreset ->
                            CommunityPresetCard(
                                communityPreset = communityPreset,
                                onClick = { onPresetClick(communityPreset.preset) },
                                onDownload = {
                                    scope.launch {
                                        val result = communityService.downloadCommunityPreset(communityPreset.id)
                                        if (result.success && result.preset != null) {
                                            viewModel.importPreset(result.preset.preset)
                                            Toast.makeText(context, "下载成功", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onLike = {
                                    scope.launch {
                                        communityService.likePreset(communityPreset.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityPresetCard(
    communityPreset: CommunityService.CommunityPreset,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onLike: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                coil.compose.AsyncImage(
                    model = "https://picsum.photos/seed/${communityPreset.preset.coverPath}/600/400",
                    contentDescription = communityPreset.preset.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    color = AccentPrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "★ ${String.format("%.1f", communityPreset.rating)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = communityPreset.preset.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = communityPreset.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${communityPreset.downloads}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${communityPreset.likes}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (communityPreset.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(communityPreset.tags.take(3)) { tag ->
                            Surface(
                                color = AccentPrimary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLike,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("点赞")
                    }
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("下载")
                    }
                }
            }
        }
    }
}
