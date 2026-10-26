package fishontracker.client.scan;

import fishontracker.Fishontracker;
import fishontracker.client.util.TextUtil;

import java.util.List;

/**
 * Shared rarity detection used by every item parser. Detection runs in three stages:
 *
 * <ol>
 *   <li>private-use font glyphs (e.g. {@code } = Mythical),</li>
 *   <li>rarity words in the lore text,</li>
 *   <li>legacy colour codes (only for parsers that historically used this fallback).</li>
 * </ol>
 *
 * <p>When no stage matches, a warning is logged instead of silently returning a wrong
 * rarity — an unknown rarity produces an "unknown" id which the website can flag,
 * while a guessed rarity would be priced incorrectly.</p>
 */
public final class RarityParser {

    /** Ordered from highest to lowest rarity; matched per lore line. */
    private static final String[] GLYPHS = {"\uf037", "\uf036", "\uf035", "\uf034", "\uf033"};
    private static final String[] GLYPH_RARITIES = {"Mythical", "Legendary", "Epic", "Rare", "Common"};

    private static final String[][] TEXT_KEYWORDS = {
        {"mythic", "Mythical"},
        {"legendary", "Legendary"},
        {"epic", "Epic"},
        {"rare", "Rare"},
        {"common", "Common"}
    };

    private static final String[][] COLOR_CODES = {
        {"§d", "Mythical"},
        {"§6", "Legendary"},
        {"§5", "Epic"},
        {"§9", "Rare"},
        {"§b", "Rare"},
        {"§f", "Common"},
        {"§7", "Common"},
        {"§a", "Common"}
    };

    private RarityParser() {
    }

    /**
     * @param rawLines       raw lore lines (formatting codes still present, glyphs untouched)
     * @param fallback       value returned when nothing matches; should be {@code "Unknown"}
     *                       (or lowercase {@code "unknown"} for id-style parsers)
     * @param textLineLimit  how many lore lines to scan for rarity words;
     *                       {@code <= 0} means "all lines"
     * @param colorFallback  whether the legacy colour-code stage is enabled
     */
    public static String parse(List<String> rawLines, String fallback, int textLineLimit, boolean colorFallback) {
        String rarity = detect(rawLines, textLineLimit, colorFallback);
        if (rarity == null) {
            Fishontracker.LOGGER.warn(
                "Could not detect rarity from item lore (using fallback '{}'). Lore lines: {}",
                fallback, rawLines
            );
            return fallback;
        }

        // Parsers that build ids directly (chummers, bait packages) use lowercase rarity
        // words; parsers that build display names use capitalized ones. The fallback's
        // casing defines the convention for the calling parser.
        if (fallback.equals(fallback.toLowerCase())) {
            return rarity.toLowerCase();
        }
        return rarity;
    }

    /** Returns the canonical (capitalized) rarity, or {@code null} when nothing matched. */
    private static String detect(List<String> rawLines, int textLineLimit, boolean colorFallback) {
        if (rawLines == null) return null;

        // Stage 1: font glyphs.
        for (String raw : rawLines) {
            if (raw == null) continue;
            for (int i = 0; i < GLYPHS.length; i++) {
                if (raw.contains(GLYPHS[i])) return GLYPH_RARITIES[i];
            }
        }

        // Stage 2: rarity words in the lore text.
        int limit = (textLineLimit <= 0) ? rawLines.size() : Math.min(textLineLimit, rawLines.size());
        for (int i = 0; i < limit; i++) {
            String clean = TextUtil.stripFormatting(rawLines.get(i));
            if (clean.isEmpty()) continue;
            for (String[] keyword : TEXT_KEYWORDS) {
                if (clean.contains(keyword[0])) return keyword[1];
            }
        }

        // Stage 3: legacy colour codes.
        if (colorFallback) {
            for (String raw : rawLines) {
                if (raw == null) continue;
                for (String[] code : COLOR_CODES) {
                    if (raw.contains(code[0])) return code[1];
                }
            }
        }

        return null;
    }
}
