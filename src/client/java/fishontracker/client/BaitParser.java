package fishontracker.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaitParser {

    private static final Pattern USES_FRACTION = Pattern.compile("uses:\\s*(\\d+)/\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMAINING_USES = Pattern.compile("remaining\\s*uses:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STOCK = Pattern.compile("stock:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLOR_PATTERN = Pattern.compile("color:\\s*([a-zA-Z0-9_\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERIC_DIGIT = Pattern.compile("(\\d+)");


    public static boolean isBaitItem(ItemStack stack) {
        List<String> lore = getCleanLore(stack);
        for (String line : lore) {
            String normalized = normalizeFancyText(line);
            if (normalized.contains("bait item") 
                || normalized.contains("advanced lure item") 
                || normalized.contains("standard lure item")
                || normalized.contains("lure item")) {
                return true;
            }
        }
        return false;
    }

    public static int parseBaitQuantity(ItemStack stack) {
        List<String> lore = getCleanLore(stack);

        for (String line : lore) {
            String norm = normalizeFancyText(line);

            Matcher mFraction = USES_FRACTION.matcher(norm);
            if (mFraction.find()) {
                return Integer.parseInt(mFraction.group(1));
            }

            Matcher mRemaining = REMAINING_USES.matcher(norm);
            if (mRemaining.find()) {
                return Integer.parseInt(mRemaining.group(1));
            }

            Matcher mStock = STOCK.matcher(norm);
            if (mStock.find()) {
                return Integer.parseInt(mStock.group(1));
            }
        }

        if (lore.size() >= 4) {
            String line4 = normalizeFancyText(lore.get(3));
            Matcher mAnyDigit = GENERIC_DIGIT.matcher(line4);
            if (mAnyDigit.find()) {
                return Integer.parseInt(mAnyDigit.group(1));
            }
        }

        return stack.getCount();
    }

    public static String parseBaitColor(ItemStack stack) {
        List<String> lore = getCleanLore(stack);
        for (String line : lore) {
            String norm = normalizeFancyText(line);
            Matcher matcher = COLOR_PATTERN.matcher(norm);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    public static String normalizeFancyText(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            switch (ch) {
                case 'ᴀ': ch = 'a'; break;
                case 'ʙ': ch = 'b'; break;
                case 'ᴄ': ch = 'c'; break;
                case 'ᴅ': ch = 'd'; break;
                case 'ᴇ': ch = 'e'; break;
                case 'ꜰ': ch = 'f'; break;
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
                case 'ʀ': ch = 'r'; break;
                case 'ꜱ': ch = 's'; break;
                case 'ᴛ': ch = 't'; break;
                case 'ᴜ': ch = 'u'; break;
                case 'ᴠ': ch = 'v'; break;
                case 'ᴡ': ch = 'w'; break;
                case 'x': ch = 'x'; break;
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

    public static List<String> getCleanLore(ItemStack stack) {
        ItemLore loreComponent = stack.get(DataComponents.LORE);
        if (loreComponent == null) return List.of();

        return loreComponent.lines().stream()
            .map(Component::getString)
            .map(line -> line
                .replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("&[0-9a-fk-or]", "")
                .replaceAll("[\uE000-\uF8FF]", "") 
                .trim()
            )
            .toList();
    }
}