// 文件说明： build.gradle.kts
// 作用： 定义整个工程共享的顶层 Gradle 构建配置。
// 备注：该注释用于快速说明配置文件在构建、混淆或工程组织中的职责。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}
