package fishontracker.client.scan;

import fishontracker.Fishontracker;
import fishontracker.client.util.TextUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SuperchargeParser {

    public static boolean isSupercharge(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String effectiveName = ItemFilter.getItemEffectiveName(stack);
        String clean = ItemFilter.cleanText(effectiveName);

        if (clean.contains("supercharge") || effectiveName.contains("⚡")) {
            return true;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            String firstLineClean = ItemFilter.cleanText(lore.lines().get(0).getString());
            return firstLineClean.contains("supercharge") || lore.lines().get(0).getString().contains("⚡");
        }

        return false;
    }

    public static Map<String, Object> parseSupercharge(ItemStack stack) {
        Map<String, Object> data = new LinkedHashMap<>();

        String cleanItemName = parseCleanName(ItemFilter.getItemEffectiveName(stack));
        String climate = resolveClimate(cleanItemName);
        String rarity = parseRarity(stack);

        String displayName = rarity + " " + climate + " Supercharge";
        String uniqueKey = "supercharge_" + TextUtil.toId(climate + "_" + rarity);

        data.put("id", uniqueKey);
        data.put("isCustomItem", true);
        data.put("customType", "supercharge");
        data.put("climate", climate);
        data.put("rarity", rarity);
        data.put("name", displayName);
        data.put("category", "Supercharges");
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

        // Never guess a climate — an unknown value is flagged instead of silently priced
        // as a different climate.
        Fishontracker.LOGGER.warn("Unrecognized supercharge climate in '{}' — marking as Unknown", cleanTitle);
        return "Unknown";
    }

    private static String parseRarity(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) {
            return RarityParser.parse(null, "Unknown", -1, false);
        }

        // Glyphs are private-use characters: pass the raw lines, unmodified.
        List<String> rawLines = new ArrayList<>(lore.lines().size());
        for (Component lineComp : lore.lines()) {
            rawLines.add(lineComp.getString());
        }
        return RarityParser.parse(rawLines, "Unknown", -1, false);
    }

    private static String parseCleanName(String raw) {
        return TextUtil.stripFormatting(raw).replace("⚡", "");
    }

}