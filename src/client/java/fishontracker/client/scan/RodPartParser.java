package fishontracker.client.scan;

import fishontracker.client.util.TextUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RodPartParser {

    private static final Map<String, String> REEL_TECHS = new HashMap<>();
    private static final Map<String, String> POLE_TECHS = new HashMap<>();

    static {
        REEL_TECHS.put("micro precision", "Micro Precision Gears");
        REEL_TECHS.put("duralumin", "Duralumin Gears");
        REEL_TECHS.put("cold forged aluminium", "Cold Forged Aluminium Gears");
        REEL_TECHS.put("cast aluminium", "Cast Aluminium Gears");
        REEL_TECHS.put("stainless steel", "Stainless Steel Gears");
        REEL_TECHS.put("machined brass", "Machined Brass Gears");
        REEL_TECHS.put("cast brass", "Cast Brass Gears");

        POLE_TECHS.put("toray t1100g", "Toray T1100G Blank");
        POLE_TECHS.put("toray t800", "Toray T800 Blank");
        POLE_TECHS.put("im8 graphite", "IM8 Graphite Blank");
        POLE_TECHS.put("im7 graphite", "IM7 Graphite Blank");
        POLE_TECHS.put("im6 graphite", "IM6 Graphite Blank");
        POLE_TECHS.put("graphite fiberglass", "Graphite Fiberglass Blank");
        POLE_TECHS.put("s-glass", "S-Glass Blank");
    }

    public static boolean isRodPart(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Items.FLINT)
            || stack.is(Items.BLAZE_ROD)
            || stack.is(Items.FEATHER)
            || stack.is(Items.BREEZE_ROD)
            || stack.is(Items.IRON_NUGGET);
    }

    public static Map<String, Object> parseRodPart(ItemStack stack) {
        Map<String, Object> data = new LinkedHashMap<>();

        if (stack.is(Items.BREEZE_ROD) || stack.is(Items.IRON_NUGGET)) {
            String partType = stack.is(Items.BREEZE_ROD) ? "BlankTech" : "GearTech";
            String model = parseCleanName(stack);

            String uniqueKey = "rod_" + TextUtil.toId(partType + "_" + model);

            data.put("id", uniqueKey);
            data.put("isCustomItem", true);
            data.put("customType", "rod");
            data.put("partType", partType);
            data.put("model", model);
            data.put("tech", "None");
            data.put("name", model);
            data.put("category", "Rod Parts");
            data.put("qty", stack.getCount());

            return data;
        }

        String partType = "Pole";
        if (stack.is(Items.FLINT)) {
            partType = "Reel";
        } else if (stack.is(Items.FEATHER)) {
            partType = "Line";
        }

        String model = parseCleanName(stack);

        String tech = detectTechFromLore(stack);
        if (tech == null || partType.equals("Line")) {
            tech = "None";
        }

        String finalName = model;
        if (!tech.equals("None")) {
            finalName += " [" + tech + "]";
        }

        String uniqueKey = "rod_" + TextUtil.toId(partType + "_" + model + "_" + tech);

        data.put("id", uniqueKey);
        data.put("isCustomItem", true);
        data.put("customType", "rod");
        data.put("partType", partType);
        data.put("model", model);
        data.put("tech", tech);
        data.put("name", finalName);
        data.put("category", "Rod Parts");
        data.put("qty", stack.getCount());

        return data;
    }

    private static String detectTechFromLore(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        StringBuilder fullLoreBuilder = new StringBuilder();
        for (Component line : lore.lines()) {
            fullLoreBuilder.append(line.getString()).append(" ");
        }

        String searchableLore = cleanText(fullLoreBuilder.toString()).toLowerCase();

        if (stack.is(Items.FLINT)) {
            for (Map.Entry<String, String> entry : REEL_TECHS.entrySet()) {
                if (searchableLore.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        } else if (stack.is(Items.BLAZE_ROD)) {
            for (Map.Entry<String, String> entry : POLE_TECHS.entrySet()) {
                if (searchableLore.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    private static String parseCleanName(ItemStack stack) {
        String raw = ItemFilter.getItemEffectiveName(stack);
        String cleaned = cleanText(raw);

        for (String officialName : POLE_TECHS.values()) {
            if (officialName.equalsIgnoreCase(cleaned)) {
                return officialName;
            }
        }
        for (String officialName : REEL_TECHS.values()) {
            if (officialName.equalsIgnoreCase(cleaned)) {
                return officialName;
            }
        }

        return capitalizeTitle(cleaned);
    }

    private static String cleanText(String text) {
        return TextUtil.stripFormatting(text);
    }

    private static String capitalizeTitle(String input) {
        if (input == null || input.isEmpty()) return "";

        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;

            if (word.contains("-")) {
                String[] subParts = word.split("-");
                StringBuilder hyphenWord = new StringBuilder();
                for (int i = 0; i < subParts.length; i++) {
                    hyphenWord.append(formatSingleWord(subParts[i]));
                    if (i < subParts.length - 1) {
                        hyphenWord.append("-");
                    }
                }
                result.append(hyphenWord).append(" ");
            } else {
                result.append(formatSingleWord(word)).append(" ");
            }
        }

        return result.toString().trim();
    }

    private static String formatSingleWord(String word) {
        if (word == null || word.isEmpty()) return "";

        if (word.matches("(?i)^(i|ii|iii|iv|v|vi|vii|viii|ix|x)$")) {
            return word.toUpperCase();
        }

        if (word.matches("(?i)^(lx|rx|mx|sx|dx|tx|ex|fx|gx|im\\d*|t\\d+[a-z]*)$")) {
            return word.toUpperCase();
        }

        if (word.length() >= 2 && word.matches("^[A-Z0-9]+$")) {
            return word;
        }

        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}