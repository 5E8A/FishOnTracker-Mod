package fishontracker.client.scan;

import fishontracker.Fishontracker;
import fishontracker.client.util.TextUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorParser {

    private static final Pattern QUALITY_PATTERN = Pattern.compile("\\[(\\d+(?:\\.\\d+)?)%\\]");

    /** Rarity words are only searched in the first lore lines of armor. */
    private static final int RARITY_TEXT_LINE_LIMIT = 3;

    public static boolean isArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Items.LEATHER_CHESTPLATE)
            || stack.is(Items.LEATHER_LEGGINGS)
            || stack.is(Items.LEATHER_BOOTS);
    }

    public static Map<String, Object> parseArmor(ItemStack stack) {
        Map<String, Object> data = new LinkedHashMap<>();

        String cleanItemName = TextUtil.stripFormatting(stack.getHoverName().getString());

        String climate = resolveClimate(cleanItemName);
        String piece = resolvePiece(stack, cleanItemName);

        String rarity = parseRarity(stack);
        int rating = parseRating(stack);

        String infusionKey = parseInfusionKey(stack);
        String infusionDisplay = resolveInfusionDisplay(infusionKey);

        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(rarity).append(" ").append(climate).append(" ").append(piece);
        nameBuilder.append(" (").append(rating).append("%)");
        if (!infusionDisplay.isEmpty()) {
            nameBuilder.append(" [").append(infusionDisplay).append("]");
        }

        String uniqueKey = "armor_" + TextUtil.toId(climate + "_" + piece + "_" + rarity + "_" + rating + "_" + infusionKey);

        data.put("id", uniqueKey);
        data.put("isCustomItem", true);
        data.put("customType", "armor");
        data.put("climate", climate);
        data.put("piece", piece);
        data.put("rarity", rarity);
        data.put("rating", rating);
        data.put("infusion", infusionKey);
        data.put("name", nameBuilder.toString());
        data.put("category", "Armor");
        data.put("qty", stack.getCount());

        return data;
    }

    private static String resolveClimate(String cleanTitle) {
        String lower = cleanTitle.toLowerCase();
        if (lower.contains("subtropical")) return "Subtropical";
        if (lower.contains("subarctic")) return "Subarctic";
        if (lower.contains("semi-arid") || lower.contains("semi arid")) return "Semi-Arid";
        if (lower.contains("savanna")) return "Savanna";
        if (lower.contains("continental")) return "Continental";
        if (lower.contains("rainforest")) return "Rainforest";
        if (lower.contains("mediterranean")) return "Mediterranean";
        if (lower.contains("oceanic")) return "Oceanic";
        if (lower.contains("arid")) return "Arid";
        if (lower.contains("monsoon")) return "Monsoon";

        // Never guess a climate: a wrong guess silently prices the item as if it belonged
        // to another climate. "Unknown" is flagged by the website instead.
        Fishontracker.LOGGER.warn("Unrecognized armor climate in '{}' — marking as Unknown", cleanTitle);
        return "Unknown";
    }

    private static String resolvePiece(ItemStack stack, String cleanTitle) {
        String lower = cleanTitle.toLowerCase();
        if (lower.contains("chestplate") || stack.is(Items.LEATHER_CHESTPLATE)) return "Chestplate";
        if (lower.contains("leggings") || stack.is(Items.LEATHER_LEGGINGS)) return "Leggings";
        if (lower.contains("boots") || stack.is(Items.LEATHER_BOOTS)) return "Boots";
        return "Chestplate";
    }

    private static String parseRarity(ItemStack stack) {
        List<String> rawLines = readLoreLines(stack);
        return RarityParser.parse(rawLines, "Unknown", RARITY_TEXT_LINE_LIMIT, false);
    }

    private static int parseRating(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return 100;

        for (Component line : lore.lines()) {
            String clean = TextUtil.stripFormatting(line.getString());
            Matcher matcher = QUALITY_PATTERN.matcher(clean);
            if (matcher.find()) {
                try {
                    return Math.round(Float.parseFloat(matcher.group(1)));
                } catch (Exception ignored) {}
            }
        }

        Fishontracker.LOGGER.warn(
            "No [x%] quality rating found for '{}' — assuming 100%",
            stack.getHoverName().getString()
        );
        return 100;
    }

    private static String parseInfusionKey(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return "none";

        StringBuilder fullLore = new StringBuilder();
        for (Component line : lore.lines()) {
            fullLore.append(line.getString()).append(" ");
        }

        String text = TextUtil.stripFormatting(fullLore.toString());
        if (text.contains("albino infusion")) return "albino";
        if (text.contains("melanistic infusion")) return "melanistic";
        if (text.contains("trophy infusion")) return "trophy";

        return "none";
    }

    private static String resolveInfusionDisplay(String key) {
        return switch (key) {
            case "albino" -> "Albino Infusion";
            case "melanistic" -> "Melanistic Infusion";
            case "trophy" -> "Trophy Infusion";
            default -> "";
        };
    }

    private static List<String> readLoreLines(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return List.of();

        List<String> lines = new ArrayList<>(lore.lines().size());
        for (Component line : lore.lines()) {
            lines.add(line.getString());
        }
        return lines;
    }
}
