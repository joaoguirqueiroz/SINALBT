plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "br.com.vertice81.sinalbt"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.vertice81.sinalbt"
        minSdk = 26
        targetSdk = 35
        // O GitHub Actions incrementa o versionCode e publica uma versão nova a cada envio.
        // Localmente, o projeto mantém uma versão estável para desenvolvimento.
        versionCode = 15000 + (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 0)
        versionName = System.getenv("SINAL_VERSION_NAME") ?: "1.5.0"
        buildConfigField("String", "UPDATES_REPO", "\"joaoguirqueiroz/SINALBT\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Para versões distribuídas: todas devem usar a MESMA chave de assinatura.
    // Nenhum segredo é incorporado ao código-fonte.
    val releaseStore = System.getenv("SINAL_SIGNING_STORE")
    val releasePass = System.getenv("SINAL_SIGNING_STORE_PASSWORD")
    val releaseAlias = System.getenv("SINAL_SIGNING_KEY_ALIAS")
    val releaseKeyPass = System.getenv("SINAL_SIGNING_KEY_PASSWORD")
    val signingReady = listOf(releaseStore, releasePass, releaseAlias, releaseKeyPass).all { !it.isNullOrBlank() }
    if (signingReady) {
        signingConfigs {
            create("distribution") {
                storeFile = file(releaseStore!!)
                storePassword = releasePass!!
                keyAlias = releaseAlias!!
                keyPassword = releaseKeyPass!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (signingReady) signingConfig = signingConfigs.getByName("distribution")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
