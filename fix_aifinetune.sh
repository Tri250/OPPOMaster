#!/bin/bash

# 添加rememberScrollState导入
sed -i 's/import androidx.compose.foundation.layout.padding/import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll/' app/src/main/java/com/omaster/app/ui/screens/AiFineTuneScreen.kt

# 添加rememberCoroutineScope
sed -i 's/import androidx.compose.runtime.getValue/import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.rememberCoroutineScope/' app/src/main/java/com/omaster/app/ui/screens/AiFineTuneScreen.kt

# 添加remember
sed -i 's/import androidx.compose.runtime.Composable/import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.remember/' app/src/main/java/com/omaster/app/ui/screens/AiFineTuneScreen.kt

# 添加missing相关的导入（如果缺失）
sed -i '/^import androidx.compose.foundation.shape.RoundedCornerShape$/a\
import androidx.compose.material3.AlertDialog\
import androidx.compose.material3.TextButton' app/src/main/java/com/omaster/app/ui/screens/AiFineTuneScreen.kt

# 添加ColorOSLightBackground常量导入（如果缺失）
sed -i 's/import com.omaster.app.ui.theme.ColorOSBlack/import com.omaster.app.ui.theme.ColorOSBlack\nimport com.omaster.app.ui.theme.ColorOSLightBackground/' app/src/main/java/com/omaster/app/ui/screens/AiFineTuneScreen.kt

echo "AiFineTuneScreen fixes applied"
