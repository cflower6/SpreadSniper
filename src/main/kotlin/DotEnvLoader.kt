import io.github.cdimascio.dotenv.dotenv

object DotenvLoader {
    fun load(directory: String = "src/main/kotlin") {
        val dotenv = dotenv {
            ignoreIfMalformed = true
            ignoreIfMissing = false
            this.directory = directory
        }

        dotenv.entries().forEach { entry ->
            System.setProperty(entry.key, entry.value)
        }

        println("✅ .env loaded: ${dotenv.entries().size} variables")
    }
}