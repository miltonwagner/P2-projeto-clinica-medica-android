plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.clinicavidaplus"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.clinicavidaplus"
        minSdk = 26 // SDK do Jitsi exige Android 8+
        targetSdk = 36
        // No GitHub Actions: versionCode crescente e o canal (release) que o app consulta.
        val codigoCi = (findProperty("versionCodeCi") as String?)?.toIntOrNull()
        versionCode = codigoCi ?: 1
        versionName = if (codigoCi != null) "1.0.$codigoCi" else "1.0"
        val canal = (findProperty("canalAtualizacao") as String?) ?: ""
        buildConfigField("String", "CANAL_ATUALIZACAO", "\"$canal\"")

        // Só processadores de celular (o Jitsi traz libs nativas pesadas por arquitetura).
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}