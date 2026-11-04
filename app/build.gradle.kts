plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.clinicavidaplus"
    compileSdk = 36

    defaultConfig {
        // -Ploja = versão da Google Play (pacote próprio; "com.example" é recusado pela Play).
        applicationId = if (findProperty("loja") != null) "br.com.consultas" else "com.example.clinicavidaplus"
        minSdk = 26 // SDK do Jitsi exige Android 8+
        targetSdk = 36
        // No GitHub Actions: versionCode crescente e o canal (release) que o app consulta.
        val codigoCi = (findProperty("versionCodeCi") as String?)?.toIntOrNull()
        versionCode = codigoCi ?: 1
        versionName = if (codigoCi != null) "1.0.$codigoCi" else "1.0"
        val canal = (findProperty("canalAtualizacao") as String?) ?: ""
        buildConfigField("String", "CANAL_ATUALIZACAO", "\"$canal\"")

        // Só processadores de celular (o Jitsi traz libs nativas pesadas por arquitetura).
        // -PtodasAbis libera x86_64 (emulador do workflow de fotos das telas).
        if (findProperty("todasAbis") == null) {
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Chave de upload da Play Store (só no CI: vem dos GitHub Secrets).
    signingConfigs {
        create("upload") {
            val arquivo = System.getenv("UPLOAD_KEYSTORE_PATH")
            if (arquivo != null) {
                storeFile = file(arquivo)
                storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (System.getenv("UPLOAD_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("upload")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        lintConfig = file("../lint.xml")
        abortOnError = true
    }

    // Necessário para o JavaMail funcionar no Android
    packaging {
        resources {
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // JavaMail para envio de e-mail
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    // Videochamada das consultas (Jitsi Meet)
    implementation("org.jitsi.react:jitsi-meet-sdk:13.3.0")

    // Banco local (ROOM): cópia do cadastro do paciente no aparelho.
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Geolocalização: mapa OpenStreetMap (sem API Key) + GPS do usuário.
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}