package fishontracker.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PetParser {

    private static final Map<String, String> LOCATION_TO_SPECIES = new LinkedHashMap<>();

    static {
        LOCATION_TO_SPECIES.put("cypress lake", "Bullfrog");
        LOCATION_TO_SPECIES.put("kenai river", "Bear");
        LOCATION_TO_SPECIES.put("lake biwa", "Fox");
        LOCATION_TO_SPECIES.put("murray river", "Kangaroo");
        LOCATION_TO_SPECIES.put("everglades", "Marsh Rabbit");
        LOCATION_TO_SPECIES.put("toledo bend reservoir", "Duck");
        LOCATION_TO_SPECIES.put("great lakes", "Eagle");
        LOCATION_TO_SPECIES.put("danube river", "Wolf");
        LOCATION_TO_SPECIES.put("oil rig", "Pelican");
        LOCATION_TO_SPECIES.put("amazon river", "Capybara");
        LOCATION_TO_SPECIES.put("key west", "Sea Turtle");
        LOCATION_TO_SPECIES.put("lofoten islands", "Sheep");
        LOCATION_TO_SPECIES.put("mediterranean sea", "Lynx");
        LOCATION_TO_SPECIES.put("cairns", "Koala");
        LOCATION_TO_SPECIES.put("cape cod", "Shark");
        LOCATION_TO_SPECIES.put("hawaii", "Dolphin");
        LOCATION_TO_SPECIES.put("nile river", "Hippo");
    }

    private static final List<String> BASE_SPECIES_FALLBACK = Arrays.asList(
        "Bullfrog", "Bear", "Fox", "Kangaroo", "Marsh Rabbit", "Duck",
        "Sea Turtle", "Eagle", "Wolf", "Pelican", "Capybara", "Lynx",
        "Shark", "Hippo", "Dolphin", "Sheep", "Koala"
    );

    private static final Pattern STAT_PAIR_PATTERN = Pattern.compile("\\(\\s*\\+?\\d+\\s*[/⁄∕|]\\s*\\+?(\\d+)\\s*\\)");
    private static final Pattern PCT_PATTERN = Pattern.compile("\\(\\s*([0-9]+(?:\\.[0-9]+)?)\\s*%\\s*\\)");

    public static class PetParseResult {
        public final Map<String, Object> petData;
        public final List<Map<String, Object>> attachedItems;

        public PetParseResult(Map<String, Object> petData, List<Map<String, Object>> attachedItems) {
            this.petData = petData;
            this.attachedItems = attachedItems;
        }
    }

    public static boolean isPet(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String name = cleanText(stack.getHoverName().getString()).toLowerCase();
        if (name.contains("pet")) return true;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component c : lore.lines()) {
                String norm = BaitParser.normalizeFancyText(cleanText(c.getString()));
                if (norm.contains("pet level") || norm.contains("while in any")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static PetParseResult parsePet(ItemStack stack) {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> attachedItems = new ArrayList<>();

        List<String> rawLines = new ArrayList<>();
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component c : lore.lines()) {
                rawLines.add(cleanText(c.getString()));
            }
        }

        String rarity = parseRarity(stack);
        String rawName = cleanText(stack.getHoverName().getString());
        String species = resolveSpecies(rawLines, rawName);

        ParsedRating parsedRating = parseRatingAndStats(rawLines, rarity);
        int rating = parsedRating.rating;
        String specialStat = parsedRating.specialStat;

        String tagStr = "";
        if (rating < 100) {
            if ("luck".equals(specialStat)) tagStr = " [100% Luck]";
            else if ("scale".equals(specialStat)) tagStr = " [100% Scale]";
        }

        for (String line : rawLines) {
            String norm = BaitParser.normalizeFancyText(line);

            if (norm.contains("equipped skin:")) {
                String skinName = norm.substring(norm.indexOf("equipped skin:") + 14).trim();
                if (!isDefaultOrEmpty(skinName)) {
                    String skinId = toItemId(skinName);

                    if (!skinId.endsWith("_pet")) {
                        skinId = skinId + "_pet";
                    }

                    Map<String, Object> skinEntry = new LinkedHashMap<>();
                    skinEntry.put("id", skinId);
                    skinEntry.put("qty", 1);
                    attachedItems.add(skinEntry);
                }
            }

            if (norm.contains("equipped item:")) {
                String itemName = norm.substring(norm.indexOf("equipped item:") + 14).trim();
                if (!isDefaultOrEmpty(itemName)) {
                    Map<String, Object> itemEntry = new LinkedHashMap<>();
                    itemEntry.put("id", toItemId(itemName));
                    itemEntry.put("qty", 1);
                    attachedItems.add(itemEntry);
                }
            }
        }

        String uniqueKey = "pet_" + ScannerManager.normalizeToId(species + "_" + rarity + "_" + rating + "_" + specialStat);

        data.put("id", uniqueKey);
        data.put("isCustomItem", true);
        data.put("customType", "pet");
        data.put("species", species);
        data.put("rarity", rarity);
        data.put("rating", rating);
        data.put("specialStat", specialStat);
        data.put("name", rarity + " " + species + " (" + rating + "%)" + tagStr);
        data.put("category", "Pets");
        data.put("qty", stack.getCount());

        return new PetParseResult(data, attachedItems);
    }

    private static String resolveSpecies(List<String> rawLines, String rawName) {
        for (String line : rawLines) {
            String norm = BaitParser.normalizeFancyText(line);
            if (norm.contains("while at")) {
                for (Map.Entry<String, String> entry : LOCATION_TO_SPECIES.entrySet()) {
                    if (norm.contains(entry.getKey())) {
                        return entry.getValue();
                    }
                }
            }
        }

        for (String sp : BASE_SPECIES_FALLBACK) {
            if (rawName.toLowerCase().contains(sp.toLowerCase())) {
                return sp;
            }
        }

        return "Unknown";
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

        for (Component lineComp : lore.lines()) {
            String norm = BaitParser.normalizeFancyText(cleanText(lineComp.getString()));
            if (norm.contains("mythical")) return "Mythical";
            if (norm.contains("legendary")) return "Legendary";
            if (norm.contains("epic")) return "Epic";
            if (norm.contains("rare")) return "Rare";
            if (norm.contains("common")) return "Common";
        }

        return "Common";
    }

    private static ParsedRating parseRatingAndStats(List<String> rawLines, String rarity) {
        List<Double> luckPercents = new ArrayList<>();
        List<Double> scalePercents = new ArrayList<>();
        List<Double> allPercents = new ArrayList<>();

        double maxRarityStat = getMaxStatForRarity(rarity);

        for (String rawLine : rawLines) {
            String norm = BaitParser.normalizeFancyText(rawLine);

            boolean isLuck = norm.contains("luck");
            boolean isScale = norm.contains("scale");

            if (!isLuck && !isScale) continue;

            Double pct = null;

            Matcher mPct = PCT_PATTERN.matcher(norm);
            if (mPct.find()) {
                try {
                    pct = Double.parseDouble(mPct.group(1));
                } catch (Exception ignored) {}
            }

            if (pct == null) {
                Matcher mPair = STAT_PAIR_PATTERN.matcher(norm);
                if (mPair.find()) {
                    try {
                        double statVal = Double.parseDouble(mPair.group(1));
                        pct = (statVal / maxRarityStat) * 100.0;
                    } catch (Exception ignored) {}
                }
            }

            if (pct != null) {
                pct = Math.max(20.0, Math.min(100.0, pct));
                allPercents.add(pct);

                if (isLuck) luckPercents.add(pct);
                if (isScale) scalePercents.add(pct);
            }
        }

        if (allPercents.isEmpty()) {
            return new ParsedRating(0, "none");
        }

        double sum = 0.0;
        for (double p : allPercents) {
            sum += p;
        }
        double avg = sum / allPercents.size();

        int finalRating = (int) Math.floor(avg);

        String specialStat = "none";
        boolean allLuck100 = luckPercents.size() >= 2 && luckPercents.stream().allMatch(p -> p >= 99.5);
        boolean allScale100 = scalePercents.size() >= 2 && scalePercents.stream().allMatch(p -> p >= 99.5);

        if (allLuck100) {
            specialStat = "luck";
        } else if (allScale100) {
            specialStat = "scale";
        }

        return new ParsedRating(finalRating, specialStat);
    }

    private static double getMaxStatForRarity(String rarity) {
        switch (rarity.toLowerCase()) {
            case "mythical": return 188.0;
            case "legendary": return 125.0;
            case "epic": return 75.0;
            case "rare": return 50.0;
            case "common":
            default: return 25.0;
        }
    }

    private static boolean isDefaultOrEmpty(String val) {
        if (val == null) return true;
        String clean = val.trim().toLowerCase();
        return clean.isEmpty() || clean.equals("default") || clean.equals("empty") || clean.equals("none");
    }

    private static String toItemId(String rawName) {
        return rawName.toLowerCase()
            .replace("'", "")
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    private static String cleanText(String raw) {
        if (raw == null) return "";
        return raw
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("&[0-9a-fk-or]", "")
            .replace('\u00A0', ' ')
            .replace('\u200B', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static class ParsedRating {
        final int rating;
        final String specialStat;

        ParsedRating(int rating, String specialStat) {
            this.rating = rating;
            this.specialStat = specialStat;
        }
    }
}