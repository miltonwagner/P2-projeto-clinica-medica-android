plugins {
    alias(libs.plugins.android.application)
    // Plugin do Google Services necessário para o Firebase
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.psyconet"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.psyconet"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Dependências nativas do seu projeto
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // --- DEPENDÊNCIAS DO FIREBASE ---
    // Importa o Firebase BoM (gerencia as versões compatíveis automaticamente)
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))

    // Biblioteca do Cloud Firestore para salvar e ler os agendamentos
    implementation("com.google.firebase:firebase-firestore")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}