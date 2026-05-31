#!/bin/bash

# 修复 SceneDetectionScreen.kt
sed -i 's/import androidx.compose.foundation.lazy.items/import androidx.compose.foundation.lazy.items\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll/' app/src/main/java/com/omaster/app/ui/screens/SceneDetectionScreen.kt

# 添加FileProvider导入
sed -i '/^package com.omaster.app.ui.screens$/a\
import android.content.Context\
import androidx.core.content.FileProvider' app/src/main/java/com/omaster/app/ui/screens/SceneDetectionScreen.kt

# 添加rememberCoroutineScope
sed -i 's/import androidx.compose.runtime.LaunchedEffect/import androidx.compose.runtime.LaunchedEffect\nimport androidx.compose.runtime.rememberCoroutineScope/' app/src/main/java/com/omaster/app/ui/screens/SceneDetectionScreen.kt

# 添加PaddingValues导入
sed -i '/^import androidx.compose.material3.TopAppBarDefaults$/a\
import androidx.compose.foundation.layout.PaddingValues' app/src/main/java/com/omaster/app/ui/screens/SceneDetectionScreen.kt

echo "Fixes applied successfully"
