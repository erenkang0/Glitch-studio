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
        addAll(glitchEffects)
        addAll(retroEffects)
        addAll(distortEffects)
        addAll(stylizeEffects)
        addAll(colorEffects)
        addAll(lightEffects)
        addAll(patternEffects)
    }

    /** Number of creative effects, excluding the pass-through. */
    val creativeCount: Int get() = all.size - 1

    fun byId(id: String): Effect = all.firstOrNull { it.id == id } ?: original

    fun inCategory(category: String): List<Effect> = all.filter { it.category == category }
}
