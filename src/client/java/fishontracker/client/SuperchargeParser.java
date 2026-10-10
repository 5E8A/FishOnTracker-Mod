package fishontracker.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.LinkedHashMap;
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

        data.put("id", "supercharge_" + System.currentTimeMillis());
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
        return "Subtropical";
    }

    private static String parseRarity(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return "Epic";

        for (Component lineComp : lore.lines()) {
            String raw = lineComp.getString();
            if (raw.contains("\uf037")) return "Mythical";
            if (raw.contains("\uf036")) return "Legendary";
            if (raw.contains("\uf035")) return "Epic";
        }

        for (Component lineComp : lore.lines()) {
            String clean = cleanText(lineComp.getString()).toLowerCase();
            if (clean.contains("mythical") || clean.contains("mythic")) return "Mythical";
            if (clean.contains("legendary")) return "Legendary";
            if (clean.contains("epic")) return "Epic";
        }

        return "Epic";
    }

    private static String parseCleanName(String raw) {
        return BaitParser.normalizeFancyText(raw)
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("&[0-9a-fk-or]", "")
            .replaceAll("[\uE000-\uF8FF]", "")
            .replaceAll("⚡", "")
            .trim();
    }

    private static String cleanText(String raw) {
        if (raw == null) return "";
        return raw
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("&[0-9a-fk-or]", "")
            .replaceAll("[\uE000-\uF8FF]", "")
            .replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }
}