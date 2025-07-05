import io.github.cdimascio.dotenv.dotenv

object DotenvLoader {
    fun load(directory: String = "src/main/kotlin") {
        // ✅ Load .env only if NOT in Railway
        val isProd = System.getenv("RAILWAY_ENVIRONMENT") != null

        if (!isProd) {
            println("🛠 Loading local .env file")

            val dotenv = dotenv {
                ignoreIfMalformed = true
                ignoreIfMissing = false
                this.directory = directory
            }

            dotenv.entries().forEach { entry ->
                System.setProperty(entry.key, entry.value)
            }

            println("✅ .env loaded: ${dotenv.entries().size} variables")
        } else {
            println("🚀 Running in Railway (cloud env vars only)")
        }
    }
}