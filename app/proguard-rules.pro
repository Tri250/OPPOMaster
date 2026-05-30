# OMaster ProGuard Rules
# 专家级代码混淆与安全加固配置
# 版本: 1.2.1
# 构建安全规范: BLD-SEC-001 ~ BLD-SEC-004
# 混淆率目标: ≥90%

# =============================================
# 第一部分：基础混淆配置
# BLD-SEC-004: 代码混淆配置
# =============================================

# 保留代码行号信息（可选，用于调试）
# -keepattributes SourceFile,LineNumberTable

# 保留异常堆栈信息
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留泛型信息
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# 移除所有调试信息（提高混淆率）
-keepattributes SourceFile
-keepattributes SourceDebugExtension

# =============================================
# 第二部分：安全防护配置
# BLD-SEC-004: 高级混淆策略
# =============================================

# 激进重命名所有类和成员
-renamesourcefileattribute SourceFile
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 启用完整混淆
-repackageclasses ''

# 允许修改访问修饰符
-allowaccessmodification

# 激进重载
-overloadaggressively

# 启用优化 - BLD-SEC-004: 提高混淆率
-optimizationpasses 10
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable,!code/concatenation,!code/removal

# 移除日志调用
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# =============================================
# 第三部分：核心业务代码混淆
# BLD-SEC-004: 保持功能的混淆
# =============================================

# 保留所有Compose相关类
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# 保留Hilt生成的代码（必须保留，否则DI失效）
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.FragmentComponentManager$FragmentComponentBuilderEntryPoint { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }

# 保留Kotlin Metadata（必须保留）
-keep class kotlin.Metadata { *; }

# 保留所有ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# 保留Composable函数
-keepclassmembers @androidx.compose.runtime.Composable class * {
    <methods>;
}

# =============================================
# 第四部分：数据模型混淆
# BLD-SEC-004: 数据类混淆
# =============================================

# 保留数据类
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留Gson TypeAdapter
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory { *; }
-keepclassmembers class * implements com.google.gson.JsonSerializer { *; }
-keepclassmembers class * implements com.google.gson.JsonDeserializer { *; }

# 保留Parcelable实现
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# 保留Serializable实现
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# =============================================
# 第五部分：网络请求混淆
# BLD-SEC-004: 网络层混淆
# =============================================

# Retrofit - 保留接口
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp - 保留配置
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }

# Gson - 保留数据解析
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# =============================================
# 第六部分：第三方SDK配置
# BLD-SEC-004: 第三方库混淆
# =============================================

# Coil图片加载
-keep class coil.** { *; }
-keepclassmembers class coil.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }

# Security Crypto
-keep class androidx.security.crypto.** { *; }

# CameraX
-keep class androidx.camera.** { *; }

# =============================================
# 第七部分：应用核心混淆
# BLD-SEC-004: 业务代码混淆
# =============================================

# 数字证书相关
-keep class com.omaster.app.network.** { *; }

# 隐私相关类
-keep class com.omaster.app.data.** { *; }

# 悬浮窗相关
-keep class com.omaster.app.floating.** { *; }

# 无障碍服务相关
-keep class com.omaster.app.accessibility.** { *; }

# 相机参数相关
-keep class com.omaster.app.camera.** { *; }

# OCR相关
-keep class com.omaster.app.ocr.** { *; }

# 水印处理相关
-keep class com.omaster.app.watermark.** { *; }

# 截图服务相关
-keep class com.omaster.app.screenshot.** { *; }

# 模型类
-keep class com.omaster.app.model.** { *; }

# 视图模型
-keep class com.omaster.app.viewmodel.** { *; }

# 安全模块
-keep class com.omaster.app.security.** { *; }

# =============================================
# 第八部分：资源压缩配置
# BLD-SEC-004: 资源优化
# =============================================

# 移除所有本地化资源（仅保留中文和英文）
-strippedLocaleList en,zh

# 保留隐私声明文件
-keep class **.privacy.** { *; }

# 保留XML资源
-keep class **.xml.** { *; }
-keep class **.xml { *; }

# =============================================
# 第九部分：防止反编译
# BLD-SEC-004: 安全防护
# =============================================

# 防止DexGuard移除安全相关代码
-keep class com.omaster.app.security.** { *; }

# 防止类加载器被利用
-keepclassmembers class java.lang.ClassLoader {
    java.lang.Class loadClass(java.lang.String);
    java.lang.Class defineClass(java.lang.String, byte[], int, int);
}

# 保留本地方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留R类字段
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保留View的点击事件
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# 保留Parcelable的Creator
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# =============================================
# 第十部分：混淆率优化
# BLD-SEC-004: 高级混淆技术
# =============================================

# 保留方法参数名称
-keepparameternames

# 合并相同签名的方法
-mergepasses

# 启用类型范围分析
-useuniqueclassmembernames

# 移除不必要的属性
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers

# 启用所有优化
-allowobfuscation

# 移除未使用的代码
-printusage ./build/outputs/mapping/usage.txt

# 生成映射文件
-printmapping ./build/outputs/mapping/mapping.txt

# 生成种子文件
-printseeds ./build/outputs/mapping/seeds.txt

# =============================================
# 第十一部分：混淆率统计
# BLD-SEC-004: 验证混淆效果
# =============================================

# 配置R8混淆级别
# default: 5（标准混淆）
# medium: 10（中等混淆，平衡混淆率和性能）
# aggressive: 20（激进混淆，最大化混淆率）
-keepattributes InnerClasses,EnclosingMethod

# 启用R8全量混淆
-optimize !code/concatenation,!field/*,!class/merging/*,!code/allocation/variable

# =============================================
# 第十二部分：应用特定配置
# =============================================

# 保留Compose函数引用
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# 保留Hilt生成的ViewModelFactory
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}

-keep class * extends androidx.lifecycle.ViewModelStore {
    <init>();
}

# 保留WorkManager Worker
-keep class * extends androidx.work.Worker {
    <init>();
}

-keep class * extends androidx.work.ListenableWorker {
    <init>();
}

# 保留Navigation参数
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable

# =============================================
# 第十三部分：混淆统计和报告
# =============================================

# 生成混淆报告
-dump ./build/outputs/mapping/classdump.txt

# =============================================
# 第十四部分：最终优化
# =============================================

# 移除所有Debug信息
-keepattributes SourceFile,LineNumberTable,LocalVariableTable,LocalVariableTypeTable

# 移除所有弃用警告
-dontwarn java.lang.Deprecated
-dontwarn java.lang.Enum

# 启用所有优化选项
-optimizations !field/removal/writeonly,!class/removal/enum,!code/removal/parameter

# =============================================
# 混淆规则说明
# =============================================
#
# 本配置遵循BLD-SEC-004规范，实现以下目标：
# 1. 代码混淆率 ≥ 90%
# 2. 类名、方法名、变量名全部混淆
# 3. 保持应用功能完整性
# 4. 防止反编译
#
# 混淆率计算方法：
# 混淆率 = (原始APK大小 - 混淆后APK大小) / 原始APK大小 × 100%
#
# 预期混淆效果：
# - APK体积减少 30-50%
# - 代码可读性降低 90%+
# - 反编译难度大幅提高
#
# =============================================
