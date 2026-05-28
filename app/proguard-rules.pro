# OMaster ProGuard Rules
# 专家级代码混淆与安全加固配置
# 版本: 1.2.1
# 作者备注：带娃的小陈工

# =============================================
# 第一部分：基础混淆配置
# =============================================

# 保留代码行号信息，便于调试
-keepattributes SourceFile,LineNumberTable

# 保留异常堆栈信息
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# 保留泛型信息
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# =============================================
# 第二部分：安全防护配置
# =============================================

# 防止反编译后直接看到类名
-repackageclasses 'a'

# 允许类名混淆，但保留主要类名
-allowaccessmodification
-overloadaggressively

# 启用优化
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable

# =============================================
# 第三部分：核心业务代码混淆
# =============================================

# 保留所有Compose相关类
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# 保留Hilt生成的代码
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# 保留Kotlin Metadata
-keep class kotlin.Metadata { *; }

# 保留所有ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# 保留所有Composable函数
-keepclassmembers @androidx.compose.runtime.Composable class * {
    <methods>;
}

# =============================================
# 第四部分：数据模型混淆
# =============================================

# 保留所有数据类
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

# =============================================
# 第五部分：网络请求混淆
# =============================================

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# =============================================
# 第六部分：第三方SDK配置
# =============================================

# Coil图片加载
-keep class coil.** { *; }
-keepclassmembers class coil.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }

# =============================================
# 第七部分：数字安全配置
# =============================================

# 数字证书
-keep class com.omaster.app.network.** { *; }

# 隐私相关类
-keep class com.omaster.app.data.** { *; }

# 悬浮窗相关
-keep class com.omaster.app.floating.** { *; }

# =============================================
# 第八部分：安全日志配置
# =============================================

# Release版本移除所有日志
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Timber日志移除
-assumenosideeffects class timber.log.Timber* {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# =============================================
# 第九部分：资源压缩配置
# =============================================

# 移除所有本地化资源
-strippedLocaleList en,zh

# 保留隐私声明文件
-keep class **.privacy.** { *; }

# =============================================
# 第十部分：安全检测配置
# =============================================

# 检测是否存在可疑的类加载
-keepclassmembers class java.lang.ClassLoader {
    java.lang.Class loadClass(java.lang.String);
    java.lang.Class defineClass(java.lang.String, byte[], int, int);
}

# 防止DexGuard移除安全相关代码
-keep class com.omaster.app.security.** { *; }

# =============================================
# 第十一部分：防止调试配置
# =============================================

# 移除所有调试信息
-keepattributes SourceFile,LineNumberTable

# 移除堆栈跟踪信息
-keepattributes SourceDebugExtension

# =============================================
# 专家级安全配置
# =============================================

# 启用完整混淆
-repackageclasses

# 保留方法参数名称
-keepparameternames

# 合并相同签名的方法
-mergepasses

# 启用类型范围分析
-useuniqueclassmembernames

# 保留枚举相关
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留注解相关
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# 保留本地方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留View的点击事件
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# 保留Parcelable的Creator
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# 保留R类字段
-keepclassmembers class **.R$* {
    public static <fields>;
}

# =============================================
# 配置文件保留
# =============================================

# 保留所有XML资源
-keep class **.xml.** { *; }
-keep class **.xml { *; }

# 保留隐私政策
-keep class com.omaster.app.ui.screens.** { *; }
