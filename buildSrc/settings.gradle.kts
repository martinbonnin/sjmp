pluginManagement {
    this.resolutionStrategy {
        eachPlugin {
            /**
             * org.jetbrains.kotlin.plugin.sam.with.receiver only support plugin markers starting with version 1.7
             * put all resolution for kotlin plugins here
             */
            when (requested.id.id) {
                "org.jetbrains.kotlin.jvm" -> useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.32")
                "org.jetbrains.kotlin.plugin.sam.with.receiver" -> useModule("org.jetbrains.kotlin:kotlin-sam-with-receiver:1.5.32")
            }
        }
    }
}