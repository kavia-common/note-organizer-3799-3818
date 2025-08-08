androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))
        implementation("com.google.android.material:material:1.10.0")
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation("androidx.cardview:cardview:1.0.0")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    }
}
