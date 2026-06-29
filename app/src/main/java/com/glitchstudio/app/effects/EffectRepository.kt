package com.glitchstudio.app.effects

/**
 * The full catalogue of creative effects, grouped by [EffectCategory].
 * Aggregated from the per-category source files in this package.
 */
object EffectRepository {

    val all: List<ShaderEffect> =
        colorEffects +
            lightEffects +
            stylizeEffects +
            distortEffects +
            glitchEffects +
            blurEffects +
            textureEffects +
            retroEffects

    private val byId: Map<String, ShaderEffect> = all.associateBy { it.id }

    /** Categories that actually contain at least one effect, in declared order. */
    val categories: List<EffectCategory> =
        EffectCategory.entries.filter { cat -> all.any { it.category == cat } }

    fun inCategory(category: EffectCategory): List<ShaderEffect> =
        all.filter { it.category == category }

    fun byId(id: String): ShaderEffect? = byId[id]

    val count: Int get() = all.size

    val firstEffect: ShaderEffect get() = all.first()
}
