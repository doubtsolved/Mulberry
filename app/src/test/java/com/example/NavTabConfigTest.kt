package com.example

import com.example.ui.navigation.DEFAULT_TAB_CONFIGS
import com.example.ui.navigation.MulberryTab
import com.example.ui.navigation.NavTabConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTabConfigTest {

    @Test
    fun defaultTabConfigs_containsAllTabsInDefaultOrder() {
        assertEquals(3, DEFAULT_TAB_CONFIGS.size)
        assertEquals(MulberryTab.DESK, DEFAULT_TAB_CONFIGS[0].tab)
        assertEquals(MulberryTab.LIBRARY, DEFAULT_TAB_CONFIGS[1].tab)
        assertEquals(MulberryTab.SETTINGS, DEFAULT_TAB_CONFIGS[2].tab)
        assertTrue(DEFAULT_TAB_CONFIGS.all { it.isVisible })
    }

    @Test
    fun reorderingTabs_maintainsAllElements() {
        val original = DEFAULT_TAB_CONFIGS.toMutableList()
        val moved = original.removeAt(0) // remove DESK
        original.add(moved) // move to end

        assertEquals(3, original.size)
        assertEquals(MulberryTab.LIBRARY, original[0].tab)
        assertEquals(MulberryTab.SETTINGS, original[1].tab)
        assertEquals(MulberryTab.DESK, original[2].tab)
    }

    @Test
    fun serializationAndDeserialization_preservesOrderAndVisibility() {
        val configs = listOf(
            NavTabConfig(MulberryTab.DESK, isVisible = true),
            NavTabConfig(MulberryTab.LIBRARY, isVisible = true),
            NavTabConfig(MulberryTab.SETTINGS, isVisible = false)
        )

        val serialized = configs.joinToString(",") { "${it.tab.name}:${it.isVisible}" }
        assertEquals("DESK:true,LIBRARY:true,SETTINGS:false", serialized)

        val deserialized = serialized.split(",").mapNotNull { part ->
            val seg = part.split(":")
            val tab = MulberryTab.entries.firstOrNull { it.name.equals(seg[0], ignoreCase = true) }
            if (tab != null) NavTabConfig(tab, seg[1].toBooleanStrictOrNull() ?: true) else null
        }

        assertEquals(3, deserialized.size)
        assertEquals(MulberryTab.DESK, deserialized[0].tab)
        assertTrue(deserialized[0].isVisible)
        assertEquals(MulberryTab.SETTINGS, deserialized[2].tab)
        assertFalse(deserialized[2].isVisible)
    }
}
