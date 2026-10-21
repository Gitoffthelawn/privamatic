package com.techtrest.privamatic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * No app data may leave the device through backup or device-to-device transfer
 * (Oliver, 2026-10-21; docs/specs/backup-exclusions.md).
 *
 * The platform walks each domain as its own tree, and an exclude only prunes an exact
 * path, so every domain needs its own `path="."` exclude. `root` does not cover the others.
 * Unit tests run with the module directory as working directory.
 */
class BackupRulesTest {

    private val allDomains = setOf(
        "root", "file", "database", "sharedpref", "external",
        "device_root", "device_file", "device_database", "device_sharedpref"
    )
    private val androidNs = "http://schemas.android.com/apk/res/android"

    private fun parse(path: String): Element =
        DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File(path))
            .documentElement

    private fun children(parent: Element, tag: String): List<Element> {
        val nodes = parent.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun section(root: Element, tag: String): Element =
        children(root, tag).single()

    /** Domain → path for every `<exclude>`; fails on any `<include>`. */
    private fun excludes(section: Element): Map<String, String> {
        assertTrue("<${section.tagName}> must include nothing", children(section, "include").isEmpty())
        return children(section, "exclude").associate { it.getAttribute("domain") to it.getAttribute("path") }
    }

    private fun assertExcludesEverything(section: Element) {
        val excludes = excludes(section)
        assertEquals("<${section.tagName}> exclude count", allDomains.size, children(section, "exclude").size)
        assertEquals("<${section.tagName}> domains", allDomains, excludes.keys)
        excludes.forEach { (domain, path) -> assertEquals("<${section.tagName}> $domain path", ".", path) }
    }

    private val extractionRules get() = parse("src/main/res/xml/data_extraction_rules.xml")
    private val legacyRules get() = parse("src/main/res/xml/backup_rules.xml")

    @Test
    fun `cloud backup excludes every domain`() {
        assertExcludesEverything(section(extractionRules, "cloud-backup"))
    }

    @Test
    fun `device transfer excludes every domain`() {
        assertExcludesEverything(section(extractionRules, "device-transfer"))
    }

    @Test
    fun `device transfer mirrors cloud backup`() {
        val rules = extractionRules
        assertEquals(excludes(section(rules, "cloud-backup")), excludes(section(rules, "device-transfer")))
    }

    @Test
    fun `no other transfer mode is configured`() {
        // A <cross-platform-transfer> section would need its own excludes (Android 16 QPR2+).
        val nodes = extractionRules.childNodes
        val tags = (0 until nodes.length)
            .map { nodes.item(it) }
            .filterIsInstance<Element>()
            .map { it.tagName }
        assertEquals(listOf("cloud-backup", "device-transfer"), tags)
    }

    @Test
    fun `legacy rules for Android 11 and lower exclude every domain`() {
        assertExcludesEverything(legacyRules)
    }

    @Test
    fun `manifest disables backup and points at both rule files`() {
        val application = children(parse("src/main/AndroidManifest.xml"), "application").single()
        assertEquals("false", application.getAttributeNS(androidNs, "allowBackup"))
        assertEquals("@xml/data_extraction_rules", application.getAttributeNS(androidNs, "dataExtractionRules"))
        assertEquals("@xml/backup_rules", application.getAttributeNS(androidNs, "fullBackupContent"))
    }
}
