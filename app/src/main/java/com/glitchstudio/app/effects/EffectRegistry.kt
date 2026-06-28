package com.glitchstudio.app.effects

/**
 * Central catalogue of every effect available in the app. The first entry is a
 * neutral pass-through ("Original"); the rest are grouped by category.
 */
object EffectRegistry {

    val original = Effect(
        id = "original", name = "Original", category = "Basic",
        body = """
        vec4 process(vec2 uv) {
            return vec4(tex(uv), 1.0);
        }"""
    )

    val all: List<Effect> = buildList {
        add(original)
        addAll(glitchEffects); addAll(glitchEffects2)
        addAll(retroEffects); addAll(retroEffects2)
        addAll(distortEffects); addAll(distortEffects2)
        addAll(stylizeEffects); addAll(stylizeEffects2)
        addAll(colorEffects); addAll(colorEffects2)
        addAll(lightEffects); addAll(lightEffects2)
        addAll(patternEffects); addAll(patternEffects2)
        addAll(artEffects)
        addAll(textureEffects)
        addAll(sciFiEffects)
    }

    /** Number of creative effects, excluding the pass-through. */
    val creativeCount: Int get() = all.size - 1

    fun byId(id: String): Effect = all.firstOrNull { it.id == id } ?: original

    fun inCategory(category: String): List<Effect> = all.filter { it.category == category }
}
