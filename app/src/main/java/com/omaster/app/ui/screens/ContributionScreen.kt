package com.omaster.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omaster.app.data.Contributor
import com.omaster.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    communityService: com.omaster.app.data.CommunityService
) {
    val scope = rememberCoroutineScope()
    val agreement = remember { mutableStateOf("") }
    val isAgreementLoaded = remember { mutableStateOf(false) }
    val isAgreementAccepted = remember { mutableStateOf(false) }
    val licenseType = remember { mutableStateOf("CC_BY_SA_4.0") }
    val isSubmitting = remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            agreement.value = communityService.getContributionAgreement()
            isAgreementLoaded.value = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("贡献预设") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceElevated,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 步骤指示器
            ContributionSteps(currentStep = 1)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 协议内容
            if (isAgreementLoaded.value) {
                AgreementSection(
                    agreement = agreement.value,
                    isAccepted = isAgreementAccepted.value,
                    onAcceptedChange = { isAgreementAccepted.value = it }
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OppoOrange)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 开源协议选择
            LicenseSection(
                selectedLicense = licenseType.value,
                onLicenseSelected = { licenseType.value = it }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 提交按钮
            Button(
                onClick = {
                    if (isAgreementAccepted.value) {
                        isSubmitting.value = true
                        scope.launch {
                            // 模拟提交
                            delay(1500)
                            onSubmit()
                        }
                    }
                },
                enabled = isAgreementAccepted.value && !isSubmitting.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OppoOrange)
            ) {
                if (isSubmitting.value) {
                    CircularProgressIndicator(color = DeepSpace)
                } else {
                    Text("同意并提交", color = DeepSpace, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ContributionSteps(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepIndicator(step = 1, label = "协议", isActive = currentStep >= 1)
        Box(Modifier.height(1.dp).width(40.dp).background(BorderSubtle))
        StepIndicator(step = 2, label = "选择协议", isActive = currentStep >= 2)
        Box(Modifier.height(1.dp).width(40.dp).background(BorderSubtle))
        StepIndicator(step = 3, label = "确认", isActive = currentStep >= 3)
    }
}

@Composable
fun StepIndicator(step: Int, label: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isActive) OppoOrange else SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = DeepSpace
                )
            } else {
                Text(step.toString(), color = TextSecondary)
            }
        }
        Text(label, fontSize = 12.sp, color = if (isActive) OppoOrange else TextTertiary)
    }
}

@Composable
fun AgreementSection(
    agreement: String,
    isAccepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("原创内容贡献协议", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.FileText, contentDescription = null, tint = TextTertiary)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(agreement, fontSize = 13.sp, lineHeight = 18.sp)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAccepted,
                    onCheckedChange = onAcceptedChange,
                    colors = CheckboxDefaults.colors(checkedColor = OppoOrange)
                )
                Text("我已阅读并同意以上协议", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun LicenseSection(
    selectedLicense: String,
    onLicenseSelected: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("选择开源协议", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val licenses = listOf(
                "CC_BY_SA_4.0" to "CC BY-SA 4.0（署名-相同方式共享）",
                "CC_BY_4.0" to "CC BY 4.0（署名）",
                "MIT" to "MIT（宽松许可）",
                "APACHE_2.0" to "Apache 2.0"
            )
            
            licenses.forEach { (id, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selectedLicense == id) OppoOrange.copy(alpha = 0.1f) else Surface)
                        .clickable { onLicenseSelected(id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLicense == id,
                        onClick = { onLicenseSelected(id) },
                        colors = RadioButtonDefaults.colors(selectedColor = OppoOrange)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(name, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    communityService: com.omaster.app.data.CommunityService
) {
    val scope = rememberCoroutineScope()
    val leaderboard = remember { mutableStateListOf<Contributor>() }
    
    LaunchedEffect(Unit) {
        scope.launch {
            val data = communityService.getLeaderboard()
            leaderboard.clear()
            leaderboard.addAll(data)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("贡献排行榜") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceElevated,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 榜单头部
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = OppoOrange)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏆 贡献排行榜", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DeepSpace)
                    Text("每周一0点更新", fontSize = 14.sp, color = DeepSpace.copy(alpha = 0.8f))
                }
            }
            
            // 榜单列表
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(leaderboard) { contributor ->
                    LeaderboardItem(contributor = contributor)
                }
            }
        }
    }
}

@Composable
fun LeaderboardItem(contributor: Contributor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when (contributor.rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> SurfaceHover
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                contributor.rank.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (contributor.rank <= 3) DeepSpace else TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 头像
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OppoOrange.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("👤", fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 信息
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(contributor.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (contributor.isMaster) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(OppoOrange)
                            .padding(2.dp 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("大师", fontSize = 10.sp, color = DeepSpace, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                "${contributor.contributionCount} 个预设 · ${contributor.likes} 点赞",
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
        
        // 点赞数
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(contributor.likes.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("获赞", fontSize = 10.sp, color = TextTertiary)
        }
    }
}
