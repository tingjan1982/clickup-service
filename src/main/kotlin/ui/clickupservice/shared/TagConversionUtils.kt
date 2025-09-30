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
        "ptd" to "PTD",
        "rbm" to "RBM",
        "rbhp" to "RBHP",
        "super" to "SUPER",
        "bab" to "BAB",
        "lpjp" to "LPJP",
        "lpjp-carpark" to "LPJP",
        "personal" to "PER",
        "faircloth" to "CFC",
        "cfc" to "CFC"
    )

    private val REVERSE_TAGS_MAP: Map<String, String> = TAGS_MAP.entries.associate { (k, v) -> v to k }

    fun convertTag(tag: String): String {
        return TAGS_MAP[tag] ?: throw Exception("tag is not found: $tag")
    }

    fun reverseLookupTag(entity: String): String {
        return REVERSE_TAGS_MAP[entity] ?: throw Exception("entity is not found: $entity")
    }
}