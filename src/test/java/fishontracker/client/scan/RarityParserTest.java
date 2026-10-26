package fishontracker.client.scan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RarityParserTest {

    @Test
    void detectsRarityFromFontGlyphs() {
        assertEquals("Mythical", RarityParser.parse(List.of("\uf037something"), "Unknown", -1, false));
        assertEquals("Legendary", RarityParser.parse(List.of("x", "\uf036"), "Unknown", -1, false));
        assertEquals("Epic", RarityParser.parse(List.of("\uf035"), "Unknown", -1, false));
        assertEquals("Rare", RarityParser.parse(List.of("\uf034"), "Unknown", -1, false));
        assertEquals("Common", RarityParser.parse(List.of("\uf033"), "Unknown", -1, false));
    }

    @Test
    void detectsRarityFromTextWhenGlyphsAreMissing() {
        assertEquals("Legendary", RarityParser.parse(List.of("a legendary companion"), "Unknown", -1, false));
        assertEquals("Mythical", RarityParser.parse(List.of("a Mythical companion"), "Unknown", -1, false));
    }

    @Test
    void glyphStageRunsBeforeTextStage() {
        // Line 1 has the Mythical glyph, line 2 says "common" — glyph must win.
        assertEquals("Mythical", RarityParser.parse(List.of("\uf037", "very common drop"), "Unknown", -1, false));
    }

    @Test
    void textLineLimitRestrictsScanning() {
        // "rare" only appears on line 3, which is outside the limit of 2.
        assertEquals("Unknown", RarityParser.parse(List.of("line one", "line two", "a rare thing"), "Unknown", 2, false));
        assertEquals("Rare", RarityParser.parse(List.of("line one", "line two", "a rare thing"), "Unknown", 3, false));
    }

    @Test
    void colorFallbackOnlyRunsWhenEnabled() {
        List<String> lines = List.of("§dno words here");
        // Lowercase fallback → lowercase result (id-style convention).
        assertEquals("mythical", RarityParser.parse(lines, "unknown", -1, true));
        // Color stage disabled → nothing detected.
        assertEquals("Unknown", RarityParser.parse(lines, "Unknown", -1, false));
    }

    @Test
    void returnsFallbackWhenNothingMatches() {
        assertEquals("Unknown", RarityParser.parse(List.of("no rarity info"), "Unknown", -1, false));
        assertEquals("unknown", RarityParser.parse(null, "unknown", -1, true));
        assertEquals("Unknown", RarityParser.parse(List.of(), "Unknown", -1, false));
    }

    @Test
    void idStyleParsersGetLowercaseRarityInIds() {
        // Chummer/bait-package ids concatenate the rarity directly (no toId()), so their
        // lowercase fallback convention must apply to detected rarities too.
        assertEquals("mythical", RarityParser.parse(List.of("\uf037"), "unknown", -1, true));
        assertEquals("legendary", RarityParser.parse(List.of("a legendary drop"), "unknown", -1, false));
        assertEquals("rare", RarityParser.parse(List.of("§9"), "unknown", -1, true));

        // Display-name parsers keep the capitalized convention.
        assertEquals("Mythical", RarityParser.parse(List.of("\uf037"), "Unknown", -1, false));
    }
}