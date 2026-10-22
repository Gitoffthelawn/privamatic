package com.techtrest.privamatic

import com.techtrest.privamatic.WidgetStyle.MIN_CARD_OPACITY
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator.ScoreRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Widget opacity clamp, slider snap and colour choices (docs/specs/widget-pack.md).
 *
 * Contrast uses the model from docs/decisions/widget-readability.md: Material tones as neutral
 * greys, blended in gamma-encoded sRGB over the card at the given opacity. White and black are
 * the worst wallpapers for every colour here; the photo patch is the paper's grey rock.
 */
class WidgetStyleTest {

    // --- clamp on read -------------------------------------------------------------------

    @Test
    fun effectiveOpacity_zeroAndRoundsToZero_areNoBackground() {
        listOf(0f, 0.49f, -5f).forEach {
            assertEquals("stored $it", WidgetStyle.NO_BACKGROUND, WidgetStyle.effectiveOpacity(it))
        }
    }

    @Test
    fun effectiveOpacity_belowMinimum_drawsAsMinimum() {
        listOf(0.5f, 1f, 40f, MIN_CARD_OPACITY - 0.01f).forEach {
            assertEquals("stored $it", MIN_CARD_OPACITY, WidgetStyle.effectiveOpacity(it))
        }
    }

    @Test
    fun effectiveOpacity_minimumAndAbove_unchanged() {
        listOf(MIN_CARD_OPACITY, 87.3f, 100f).forEach {
            assertEquals("stored $it", it, WidgetStyle.effectiveOpacity(it))
        }
    }

    @Test
    fun effectiveOpacity_outOfRange_isSafe() {
        assertEquals(100f, WidgetStyle.effectiveOpacity(150f))
        assertEquals(100f, WidgetStyle.effectiveOpacity(Float.NaN))
    }

    // --- settings screen: "Show background" switch + slider (minimum..100) ---------------

    @Test
    fun settingsFor_noBackground_switchOffSliderAt100() {
        listOf(0f, 0.3f).forEach {
            assertEquals("stored $it", WidgetStyle.Settings(false, 100f), WidgetStyle.settingsFor(it))
        }
    }

    @Test
    fun settingsFor_belowMinimum_switchOnSliderAtMinimum() {
        listOf(1f, 40f, 75f).forEach {
            assertEquals("stored $it", WidgetStyle.Settings(true, MIN_CARD_OPACITY), WidgetStyle.settingsFor(it))
        }
    }

    @Test
    fun settingsFor_cardValues_shownAsStored() {
        assertEquals(WidgetStyle.Settings(true, 88.5f), WidgetStyle.settingsFor(88.5f))
        assertEquals(WidgetStyle.Settings(true, 100f), WidgetStyle.settingsFor(100f))
    }

    @Test
    fun storedOpacity_switchOff_stores0WhateverTheSlider() {
        listOf(MIN_CARD_OPACITY, 90f, 100f).forEach {
            assertEquals(0f, WidgetStyle.storedOpacity(WidgetStyle.Settings(false, it)))
        }
    }

    @Test
    fun storedOpacity_switchOn_storesTheSliderWithinRange() {
        assertEquals(MIN_CARD_OPACITY, WidgetStyle.storedOpacity(WidgetStyle.Settings(true, MIN_CARD_OPACITY)))
        assertEquals(90f, WidgetStyle.storedOpacity(WidgetStyle.Settings(true, 90f)))
        assertEquals(MIN_CARD_OPACITY, WidgetStyle.storedOpacity(WidgetStyle.Settings(true, 10f)))
        assertEquals(100f, WidgetStyle.storedOpacity(WidgetStyle.Settings(true, 120f)))
    }

    /** Opening the screen and saving unchanged must draw the same widget as before. */
    @Test
    fun openAndSave_drawsTheSameWidget() {
        listOf(-1f, 0f, 0.3f, 0.5f, 1f, 40f, 75.9f, 76f, 88.5f, 100f, 150f, Float.NaN).forEach {
            val saved = WidgetStyle.storedOpacity(WidgetStyle.settingsFor(it))
            assertEquals("stored $it", WidgetStyle.effectiveOpacity(it), WidgetStyle.effectiveOpacity(saved))
        }
    }

    // --- no-background text colour from the wallpaper hint --------------------------------

    @Test
    fun darkText_onlyWithTheHintFromApi31() {
        val dark = WidgetStyle.HINT_SUPPORTS_DARK_TEXT
        assertTrue(WidgetStyle.noBackgroundUsesDarkText(31, dark))
        assertTrue(WidgetStyle.noBackgroundUsesDarkText(37, dark or 2)) // with HINT_SUPPORTS_DARK_THEME
        assertFalse(WidgetStyle.noBackgroundUsesDarkText(37, 2))
        assertFalse(WidgetStyle.noBackgroundUsesDarkText(37, 0))
        assertFalse(WidgetStyle.noBackgroundUsesDarkText(37, null))
        assertFalse(WidgetStyle.noBackgroundUsesDarkText(30, dark))
    }

    // --- colours -------------------------------------------------------------------------

    private data class Pair3(val name: String, val fg: DoubleArray, val card: DoubleArray, val target: Double)

    private val lightCard = tone(95.0)
    private val darkCard = tone(10.0)

    private fun cases(): List<Pair3> {
        val light = listOf(
            "primary T10" to tone(10.0), "secondary T20" to tone(20.0),
            "green" to argb(WidgetStyle.GREEN_LIGHT), "teal" to argb(WidgetStyle.TEAL_LIGHT),
            "red" to argb(WidgetStyle.RED_LIGHT)
        ).map { Pair3("light ${it.first}", it.second, lightCard, 4.5) } +
            Pair3("light shield T30", tone(30.0), lightCard, 3.0)
        val dark = listOf(
            "primary T95" to tone(95.0), "secondary T90" to tone(90.0),
            "green" to argb(WidgetStyle.GREEN_DARK), "teal" to argb(WidgetStyle.TEAL_DARK),
            "red" to argb(WidgetStyle.RED_DARK)
        ).map { Pair3("dark ${it.first}", it.second, darkCard, 4.5) } +
            Pair3("dark shield T80", tone(80.0), darkCard, 3.0)
        return light + dark
    }

    @Test
    fun everyCardColour_passesFromTheMinimumUp() {
        for (c in cases()) {
            for (step in MIN_CARD_OPACITY.toInt()..100) {
                val worst = worstContrast(c.fg, c.card, step / 100.0)
                assertTrue("${c.name} at $step: ${"%.2f".format(worst)} < ${c.target}", worst >= c.target)
            }
        }
    }

    @Test
    fun minimum_isTheLowestPassingStep() {
        val below = (MIN_CARD_OPACITY - 1) / 100.0
        assertTrue(
            "every colour still passes at ${MIN_CARD_OPACITY - 1} %, so the minimum could be lower",
            cases().any { worstContrast(it.fg, it.card, below) < it.target }
        )
    }

    @Test
    fun scoreTint_followsTheRating() {
        assertEquals(WidgetStyle.GREEN_LIGHT to WidgetStyle.GREEN_DARK, WidgetStyle.scoreColors(ScoreRating.EXCELLENT))
        assertEquals(WidgetStyle.GREEN_LIGHT to WidgetStyle.GREEN_DARK, WidgetStyle.scoreColors(ScoreRating.GOOD))
        assertEquals(WidgetStyle.TEAL_LIGHT to WidgetStyle.TEAL_DARK, WidgetStyle.scoreColors(ScoreRating.FAIR))
        assertEquals(WidgetStyle.RED_LIGHT to WidgetStyle.RED_DARK, WidgetStyle.scoreColors(ScoreRating.POOR))
        assertEquals(WidgetStyle.RED_LIGHT to WidgetStyle.RED_DARK, WidgetStyle.scoreColors(ScoreRating.CRITICAL))
    }

    /** The old dark-theme "down" was olive (hue ~60°) or lavender (~270°) depending on the wallpaper. */
    @Test
    fun changeColours_areRedAndGreenInBothThemes() {
        val (upLight, upDark) = WidgetStyle.deltaColors(isIncrease = true)
        val (downLight, downDark) = WidgetStyle.deltaColors(isIncrease = false)
        for (c in listOf(upLight, upDark)) {
            val h = hue(c)
            assertTrue("up hue $h", h in 100.0..170.0)
        }
        for (c in listOf(downLight, downDark)) {
            val h = hue(c)
            assertTrue("down hue $h", h >= 345.0 || h <= 15.0)
        }
    }

    // --- layouts -------------------------------------------------------------------------

    private val sharedIds = listOf(
        "widget_root", "widget_device_name", "widget_os_name", "widget_divider",
        "widget_score_value", "widget_score_delta", "widget_score_rating"
    )

    @Test
    fun cardLayouts_haveEveryViewTheProviderTouches() {
        listOf("layout", "layout-v31", "layout-night-v31").forEach { dir ->
            val ids = layoutIds("src/main/res/$dir/widget_privacy.xml")
            (sharedIds + "widget_background").forEach { assertTrue("$dir lacks $it", it in ids) }
        }
    }

    /** The provider sends no alpha call in this style; a stray one would fail the whole widget. */
    @Test
    fun noBackgroundLayouts_haveNoBackgroundView() {
        listOf("widget_privacy_no_background", "widget_privacy_no_background_dark_text").forEach { name ->
            val ids = layoutIds("src/main/res/layout/$name.xml")
            sharedIds.forEach { assertTrue("$name lacks $it", it in ids) }
            assertFalse("$name has widget_background", ids.contains("widget_background"))
        }
    }

    // --- helpers -------------------------------------------------------------------------

    private fun layoutIds(path: String): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(File(path))
        val all = doc.getElementsByTagName("*")
        return (0 until all.length).mapNotNull {
            (all.item(it) as org.w3c.dom.Element)
                .getAttributeNS("http://schemas.android.com/apk/res/android", "id")
                .removePrefix("@+id/").ifEmpty { null }
        }.toSet()
    }

    private fun tone(t: Double): DoubleArray {
        val y = if (t > 8) ((t + 16) / 116).pow(3) else t / 903.3
        val s = if (y <= 0.0031308) 12.92 * y else 1.055 * y.pow(1 / 2.4) - 0.055
        return doubleArrayOf(s, s, s)
    }

    private fun argb(c: Int) = doubleArrayOf(
        ((c shr 16) and 0xFF) / 255.0, ((c shr 8) and 0xFF) / 255.0, (c and 0xFF) / 255.0
    )

    private fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun lum(rgb: DoubleArray) = 0.2126 * lin(rgb[0]) + 0.7152 * lin(rgb[1]) + 0.0722 * lin(rgb[2])

    private fun ratio(a: Double, b: Double) = (max(a, b) + 0.05) / (min(a, b) + 0.05)

    private val wallpapers = listOf(
        doubleArrayOf(1.0, 1.0, 1.0),
        doubleArrayOf(0.0, 0.0, 0.0),
        argb(0xFF839099.toInt()) // the paper's photo patch
    )

    private fun worstContrast(fg: DoubleArray, card: DoubleArray, alpha: Double): Double =
        wallpapers.minOf { wp ->
            val bg = DoubleArray(3) { alpha * card[it] + (1 - alpha) * wp[it] }
            ratio(lum(fg), lum(bg))
        }

    private fun hue(c: Int): Double {
        val (r, g, b) = argb(c).toList()
        val h = Math.toDegrees(atan2(sqrt(3.0) * (g - b), 2 * r - g - b))
        return if (h < 0) h + 360 else h
    }
}
