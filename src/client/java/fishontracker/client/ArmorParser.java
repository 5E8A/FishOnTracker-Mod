package fishontracker.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArmorParser {

    private static final Pattern QUALITY_PATTERN = Pattern.compile("\\[(\\d+(?:\\.\\d+)?)%\\]");

    public static boolean isArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Items.LEATHER_CHESTPLATE)
            || stack.is(Items.LEATHER_LEGGINGS)
            || stack.is(Items.LEATHER_BOOTS);
    }

    public static Map<String, Object> parseArmor(ItemStack stack) {
        Map<String, Object> data = new LinkedHashMap<>();

        String cleanItemName = parseCleanName(stack.getHoverName().getString());


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


        data.put("id", "armor_" + System.currentTimeMillis());
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
        return "Savanna";
    }

    private static String resolvePiece(ItemStack stack, String cleanTitle) {
        String lower = cleanTitle.toLowerCase();
        if (lower.contains("chestplate") || stack.is(Items.LEATHER_CHESTPLATE)) return "Chestplate";
        if (lower.contains("leggings") || stack.is(Items.LEATHER_LEGGINGS)) return "Leggings";
        if (lower.contains("boots") || stack.is(Items.LEATHER_BOOTS)) return "Boots";
        return "Chestplate";
    }

    private static String parseRarity(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return "Common";


        for (Component lineComp : lore.lines()) {
            String raw = lineComp.getString();
            if (raw.contains("\uf037")) return "Mythical";
            if (raw.contains("\uf036")) return "Legendary";
            if (raw.contains("\uf035")) return "Epic";
            if (raw.contains("\uf034")) return "Rare";
            if (raw.contains("\uf033")) return "Common";
        }


        for (int i = 0; i < Math.min(3, lore.lines().size()); i++) {
            String line = cleanText(lore.lines().get(i).getString()).toLowerCase();
            if (line.contains("mythical")) return "Mythical";
            if (line.contains("legendary")) return "Legendary";
            if (line.contains("epic")) return "Epic";
            if (line.contains("rare")) return "Rare";
            if (line.contains("common")) return "Common";
        }

        return "Common";
    }

    private static int parseRating(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return 100;

        for (Component line : lore.lines()) {
            String clean = cleanText(line.getString());
            Matcher matcher = QUALITY_PATTERN.matcher(clean);
            if (matcher.find()) {
                try {
                    return Math.round(Float.parseFloat(matcher.group(1)));
                } catch (Exception ignored) {}
            }
        }
        return 100;
    }

    private static String parseInfusionKey(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return "none";

        StringBuilder fullLore = new StringBuilder();
        for (Component line : lore.lines()) {
            fullLore.append(line.getString()).append(" ");
        }

        String text = cleanText(fullLore.toString()).toLowerCase();
        if (text.contains("albino infusion")) return "albino";
        if (text.contains("melanistic infusion")) return "melanistic";
        if (text.contains("trophy infusion")) return "trophy";

        return "none";
    }

    private static String resolveInfusionDisplay(String key) {
        if ("albino".equals(key)) return "Albino Infusion";
        if ("melanistic".equals(key)) return "Melanistic Infusion";
        if ("trophy".equals(key)) return "Trophy Infusion";
        return "";
    }

    private static String parseCleanName(String raw) {
        return BaitParser.normalizeFancyText(raw)
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("&[0-9a-fk-or]", "")
            .replaceAll("[\uE000-\uF8FF]", "")
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