package fishontracker.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextUtilTest {

    // --- normalizeFancyText ---

    @Test
    void normalizeFancyTextReturnsEmptyForNull() {
        assertEquals("", TextUtil.normalizeFancyText(null));
    }

    @Test
    void normalizeFancyTextConvertsSmallCaps() {
        assertEquals("rare", TextUtil.normalizeFancyText("ʀᴀʀᴇ"));
        assertEquals("common", TextUtil.normalizeFancyText("ᴄᴏᴍᴍᴏɴ"));
    }

    @Test
    void normalizeFancyTextConvertsAlternateGlyphVariants() {
        // Two different code points exist for 'f' and 'q' depending on the font used.
        assertEquals("fq", TextUtil.normalizeFancyText("ꜰǫ"));
        assertEquals("fq", TextUtil.normalizeFancyText("ғǫ"));
    }

    @Test
    void normalizeFancyTextConvertsFullWidthCharacters() {
        assertEquals("supercharge", TextUtil.normalizeFancyText("ＳＵＰＥＲＣＨＡＲＧＥ"));
    }

    @Test
    void normalizeFancyTextLowercases() {
        assertEquals("bait item", TextUtil.normalizeFancyText("Bait Item"));
    }

    // --- stripFormatting ---

    @Test
    void stripFormattingRemovesColorCodes() {
        assertEquals("rare rod", TextUtil.stripFormatting("§cRare §lRod"));
        assertEquals("bait", TextUtil.stripFormatting("&6Bait"));
    }

    @Test
    void stripFormattingRemovesPrivateUseFontGlyphs() {
        assertEquals("legendary", TextUtil.stripFormatting("\uf036legendary"));
    }

    @Test
    void stripFormattingRemovesTrademarkAndSpecialSpaces() {
        assertEquals("rod", TextUtil.stripFormatting("Rod™"));
        assertEquals("a b", TextUtil.stripFormatting("a\u00A0b"));
        assertEquals("a b", TextUtil.stripFormatting("a\u200Bb"));
    }

    @Test
    void stripFormattingCollapsesWhitespaceAndLowercases() {
        assertEquals("fish   merchant".replaceAll("\\s+", " "),
            TextUtil.stripFormatting("  §bFish    Merchant  "));
    }

    @Test
    void stripFormattingReturnsEmptyForNull() {
        assertEquals("", TextUtil.stripFormatting(null));
    }

    // --- normalize ---

    @Test
    void normalizeStripsPunctuationToLowercaseWords() {
        assertEquals("fishing quest 1", TextUtil.normalize("Fishing Quest #1"));
        assertEquals("500x credits", TextUtil.normalize("§b500x credits!"));
        assertEquals("rod part trade ins", TextUtil.normalize("Rod Part Trade-Ins"));
    }

    @Test
    void normalizeHandlesNull() {
        assertEquals("", TextUtil.normalize(null));
    }

    // --- toId ---

    @Test
    void toIdBuildsStableIdentifiers() {
        assertEquals("developers_rod", TextUtil.toId("Developer's Rod"));
        assertEquals("semi_arid_chestplate", TextUtil.toId("Semi-Arid Chestplate"));
        assertEquals("legendary_sword", TextUtil.toId("§6Legendary Sword™"));
    }

    @Test
    void toIdKeepsUnderscoresAndDigits() {
        assertEquals("toray_t1100g_blank", TextUtil.toId("Toray T1100G Blank"));
    }

    @Test
    void toIdReturnsUnknownForNull() {
        assertEquals("unknown", TextUtil.toId(null));
    }
}
