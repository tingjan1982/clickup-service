package ui.clickupservice.shared

object TagConversionUtils {

    private val TAGS_MAP: Map<String, String> = mapOf(
        "ui-harbour" to "UI",
        "cf-bribie" to "CF",
        "cf2-gympie" to "CF2",
        "cf3-bundaberg" to "CF3",
        "cf6-tannum" to "CF6",
        "cf7-townsville" to "CF7",
        "phkd" to "PHKD",
        "phka" to "PHKA",
        "rbm" to "RBM",
        "rbhp" to "RBHP",
        "super" to "SUPER",
        "bab" to "BAB",
        "lpjp" to "LPJP",
        "personal" to "PER",
        "faircloth" to "CFC",
        "cfc" to "CFC"
    )

    fun convertTag(tag: String): String {
        return TAGS_MAP[tag] ?: throw Exception("tag is not found: $tag")
    }
}