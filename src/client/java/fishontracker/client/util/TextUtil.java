package fishontracker.client.util;

/**
 * Shared text processing helpers used by every item parser.
 *
 * <p>Previously each parser carried its own copy of "strip formatting codes" logic with
 * slightly different behaviour. All of that lives here now so that a server-side text or
 * font change only has to be fixed in one place.</p>
 *
 * <p>This class intentionally has no Minecraft dependencies so it can be unit tested.</p>
 */
public final class TextUtil {

    private TextUtil() {
    }

    /**
     * Converts small-caps / full-width characters to plain ASCII and lowercases the result.
     * Returns an empty string for {@code null}.
     */
    public static String normalizeFancyText(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            switch (ch) {
                case 'ᴀ': ch = 'a'; break;
                case 'ʙ': ch = 'b'; break;
                case 'ᴄ': ch = 'c'; break;
                case 'ᴅ': ch = 'd'; break;
                case 'ᴇ': ch = 'e'; break;
                case 'ꜰ': ch = 'f'; break;
                case 'ғ': ch = 'f'; break;
                case 'ɢ': ch = 'g'; break;
                case 'ʜ': ch = 'h'; break;
                case 'ɪ': ch = 'i'; break;
                case 'ᴊ': ch = 'j'; break;
                case 'ᴋ': ch = 'k'; break;
                case 'ʟ': ch = 'l'; break;
                case 'ᴍ': ch = 'm'; break;
                case 'ɴ': ch = 'n'; break;
                case 'ᴏ': ch = 'o'; break;
                case 'ᴘ': ch = 'p'; break;
                case 'ꞯ': ch = 'q'; break;
                case 'ǫ': ch = 'q'; break;
                case 'ʀ': ch = 'r'; break;
                case 'ꜱ': ch = 's'; break;
                case 'ᴛ': ch = 't'; break;
                case 'ᴜ': ch = 'u'; break;
                case 'ᴠ': ch = 'v'; break;
                case 'ᴡ': ch = 'w'; break;
                case 'ʏ': ch = 'y'; break;
                case 'ᴢ': ch = 'z'; break;
                default:
                    if (ch >= 0xFF01 && ch <= 0xFF5E) {
                        ch = (char) (ch - 0xFEE0);
                    }
                    break;
            }
            sb.append(Character.toLowerCase(ch));
        }

        return sb.toString();
    }

    /**
     * Normalizes fancy text and strips every formatting code the game/server uses:
     * {@code §}/{@code &} colour codes, private-use font glyphs, trademark signs,
     * non-breaking and zero-width spaces. Result is lowercased and whitespace-collapsed.
     */
    public static String stripFormatting(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        return normalizeFancyText(raw)
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("&[0-9a-fk-or]", "")
            .replaceAll("[\uE000-\uF8FF]", "")
            .replace("™", "")
            .replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Aggressively normalizes a string to lowercase letters, digits and single spaces.
     * Used for comparing display/container titles against blacklists.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        return stripFormatting(raw)
            .toLowerCase()
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Turns a display name into a stable, URL/id-safe identifier
     * (e.g. {@code "Developer's Rod™"} → {@code "developer_s_rod"}).
     * Returns {@code "unknown"} for {@code null}.
     */
    public static String toId(String displayName) {
        if (displayName == null) return "unknown";

        return normalizeFancyText(displayName)
            .trim()
            .toLowerCase()
            .replace("'", "")
            .replace("’", "_")
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("&[0-9a-fk-or]", "")
            .replaceAll("[\uE000-\uF8FF]", "")
            .replace("™", "")
            .replaceAll("[\\s\\-]+", "_")
            .replaceAll("[^a-z0-9_]", "");
    }
}
