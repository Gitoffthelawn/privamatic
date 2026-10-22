package com.techtrest.privamatic

import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator.ScoreRating

/**
 * How the widget draws a stored opacity, and its fixed colours.
 * Measurements behind every value: docs/specs/widget-pack.md.
 */
object WidgetStyle {

    /**
     * Lowest card opacity at which all widget text reaches 4.5:1 and the shield 3:1 on any
     * wallpaper, in both themes. Below it the card blends through the text's own tone.
     */
    const val MIN_CARD_OPACITY = 76f

    const val MAX_OPACITY = 100f

    /** 0 = no background (text on the wallpaper); otherwise [MIN_CARD_OPACITY]..[MAX_OPACITY]. */
    const val NO_BACKGROUND = 0f

    // Score and change tints. Light values sit on the light card, dark values on the dark card,
    // the legacy (API 26–30) card and the wallpaper in the no-background style.
    const val GREEN_LIGHT = 0xFF004225.toInt()
    const val GREEN_DARK = 0xFF93D5AA.toInt()
    /** The app's Fair teal (#3B6470) darkened so it passes on the light card at the minimum. */
    const val TEAL_LIGHT = 0xFF224C58.toInt()
    const val TEAL_DARK = 0xFFA3CDDB.toInt()
    const val RED_LIGHT = 0xFF93000A.toInt()
    const val RED_DARK = 0xFFFFB4AB.toInt()

    /**
     * The opacity the widget draws with. Stored data is never rewritten: a value that rounds
     * to 0 % is no background, anything else below the minimum draws as the minimum.
     */
    fun effectiveOpacity(stored: Float): Float = when {
        stored.isNaN() -> MAX_OPACITY
        stored < 0.5f -> NO_BACKGROUND
        stored < MIN_CARD_OPACITY -> MIN_CARD_OPACITY
        stored > MAX_OPACITY -> MAX_OPACITY
        else -> stored
    }

    /** Slider positions between 0 and the minimum snap to the nearer of the two. */
    fun snapSliderValue(value: Float): Float = when {
        value < MIN_CARD_OPACITY / 2 -> NO_BACKGROUND
        value < MIN_CARD_OPACITY -> MIN_CARD_OPACITY
        else -> value.coerceAtMost(MAX_OPACITY)
    }

    /** Score tint as (light, dark), matching the app's ScoreCard hues. */
    fun scoreColors(rating: ScoreRating): Pair<Int, Int> = when (rating) {
        ScoreRating.EXCELLENT, ScoreRating.GOOD -> GREEN_LIGHT to GREEN_DARK
        ScoreRating.FAIR -> TEAL_LIGHT to TEAL_DARK
        ScoreRating.POOR, ScoreRating.CRITICAL -> RED_LIGHT to RED_DARK
    }

    /** Change indicator tint as (light, dark): green for a rise, red for a drop. */
    fun deltaColors(isIncrease: Boolean): Pair<Int, Int> =
        if (isIncrease) GREEN_LIGHT to GREEN_DARK else RED_LIGHT to RED_DARK
}
