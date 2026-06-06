// 文件说明： settings.gradle.kts
// 作用： 定义工程包含的模块、插件仓库以及 Gradle 的整体装配入口。
// 备注：该注释用于快速说明配置文件在构建、混淆或工程组织中的职责。
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RedBook"
include(":app")
 
