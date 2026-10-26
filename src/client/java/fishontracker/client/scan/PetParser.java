package fishontracker.client.scan;

import fishontracker.Fishontracker;
import fishontracker.client.util.TextUtil;
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

    private static final String EQUIPPED_ITEM_PREFIX = "equipped item:";
    private static final String EQUIPPED_SKIN_PREFIX = "equipped skin:";
    private static final String EQUIPPED_TRAIL_PREFIX = "equipped trail:";
    private static final String PET_SKIN_SUFFIX = "_pet";
    private static final String PET_TRAIL_SUFFIX = "_pet_trail";
    private static final String PET_SKIN_NAME_SUFFIX = "_skin";

    /** Standalone pet skins/trails carry this marker in their lore. */
    private static final String PET_COSMETIC_MARKER = "pet cosmetic item";
    private static final String PET_TRAIL_MARKER = "pet trail";
    private static final String PET_SKIN_MARKER = "pet skin";

    public enum CosmeticKind {
        TRAIL,
        SKIN
    }

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
        // "Leatherback Pet Skin" / "Cherry Blossom Pet Trail" contain "pet" but are not pets.
        if (isPetCosmetic(stack)) return false;

        String name = TextUtil.stripFormatting(stack.getHoverName().getString());
        if (name.contains("pet")) return true;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component c : lore.lines()) {
                String norm = TextUtil.stripFormatting(c.getString());
                if (norm.contains("pet level") || norm.contains("while in any")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True for standalone pet cosmetics (unequipped skins and trails). */
    public static boolean isPetCosmetic(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        for (Component line : lore.lines()) {
            if (TextUtil.stripFormatting(line.getString()).contains(PET_COSMETIC_MARKER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Classifies a standalone pet cosmetic. The name is checked first ("Cherry Blossom
     * Pet Trail"), then the lore, because e.g. the trail lore explains that only one
     * pet trail can be active. Unknown cosmetics fall back to a skin plus a warning.
     */
    public static CosmeticKind classifyPetCosmetic(ItemStack stack) {
        if (!isPetCosmetic(stack)) return null;

        String name = TextUtil.stripFormatting(stack.getHoverName().getString());
        List<String> loreLines = new ArrayList<>();

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                loreLines.add(TextUtil.stripFormatting(line.getString()));
            }
        }

        return classifyPetCosmetic(name, loreLines);
    }

    static CosmeticKind classifyPetCosmetic(String normalizedName, List<String> normalizedLore) {
        String name = normalizedName == null ? "" : normalizedName;
        List<String> lore = normalizedLore == null ? List.of() : normalizedLore;

        if (name.contains(PET_TRAIL_MARKER)) return CosmeticKind.TRAIL;
        if (name.contains(PET_SKIN_MARKER)) return CosmeticKind.SKIN;
        if (containsAny(lore, PET_TRAIL_MARKER)) return CosmeticKind.TRAIL;
        if (containsAny(lore, PET_SKIN_MARKER)) return CosmeticKind.SKIN;

        Fishontracker.LOGGER.warn("Could not classify pet cosmetic '{}' - treating it as a skin", name);
        return CosmeticKind.SKIN;
    }

    /**
     * Id used by the website for a standalone pet cosmetic, or {@code null} when the
     * stack is not one. Trails keep their full name, skins lose the "skin" suffix so
     * that equipped and standalone skins share the same id.
     */
    public static String petCosmeticId(ItemStack stack) {
        CosmeticKind kind = classifyPetCosmetic(stack);
        if (kind == null) return null;

        String rawName = stack.getHoverName().getString();
        String id = kind == CosmeticKind.TRAIL ? petTrailId(rawName) : petSkinId(rawName);
        return id.isEmpty() ? null : id;
    }

    /** {@code "Cherry Blossom Pet Trail" -> "cherry_blossom_pet_trail"}. */
    static String petTrailId(String rawName) {
        String id = toItemId(rawName == null ? "" : rawName);
        if (id.isEmpty()) return "";
        if (!id.endsWith(PET_TRAIL_SUFFIX)) {
            id = id + PET_TRAIL_SUFFIX;
        }
        return id;
    }

    /** {@code "Leatherback Pet Skin" -> "leatherback_pet"}. */
    static String petSkinId(String rawName) {
        String id = toItemId(rawName == null ? "" : rawName);
        if (id.isEmpty()) return "";

        if (id.endsWith(PET_SKIN_NAME_SUFFIX)) {
            String base = id.substring(0, id.length() - PET_SKIN_NAME_SUFFIX.length());
            if (!base.isEmpty()) id = base;
        }
        if (!id.endsWith(PET_SKIN_SUFFIX)) {
            id = id + PET_SKIN_SUFFIX;
        }
        return id;
    }

    private static boolean containsAny(List<String> lines, String needle) {
        for (String line : lines) {
            if (line != null && line.contains(needle)) return true;
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
                rawLines.add(TextUtil.stripFormatting(c.getString()));
            }
        }

        String rarity = parseRarity(stack);
        String rawName = TextUtil.stripFormatting(stack.getHoverName().getString());
        String species = resolveSpecies(rawLines, rawName);

        ParsedRating parsedRating = parseRatingAndStats(rawLines, rarity);
        int rating = parsedRating.rating;
        String specialStat = parsedRating.specialStat;

        String equippedItemKey = "";
        String equippedSkinKey = "";
        String equippedTrailKey = "";

        for (String line : rawLines) {
            String norm = TextUtil.stripFormatting(line);

            if (norm.contains(EQUIPPED_SKIN_PREFIX)) {
                String skinName = stripPrefix(norm, EQUIPPED_SKIN_PREFIX);
                if (!isDefaultOrEmpty(skinName)) {
                    String skinId = petSkinId(skinName);
                    equippedSkinKey = skinId;

                    attachedItems.add(attachmentEntry(skinId, stack.getCount()));
                }
            }

            if (norm.contains(EQUIPPED_ITEM_PREFIX)) {
                String itemName = stripPrefix(norm, EQUIPPED_ITEM_PREFIX);
                if (!isDefaultOrEmpty(itemName)) {
                    String itemId = toItemId(itemName);
                    equippedItemKey = itemId;

                    attachedItems.add(attachmentEntry(itemId, stack.getCount()));
                }
            }

            String trailId = equippedTrailId(norm);
            if (trailId != null) {
                equippedTrailKey = trailId;
                attachedItems.add(attachmentEntry(trailId, stack.getCount()));
            }
        }

        String tagStr = "";
        if (rating < 100) {
            if ("luck".equals(specialStat)) tagStr = " [100% Luck]";
            else if ("scale".equals(specialStat)) tagStr = " [100% Scale]";
        }

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(species).append("_").append(rarity).append("_").append(rating).append("_").append(specialStat);
        if (!equippedItemKey.isEmpty()) {
            keyBuilder.append("_item_").append(equippedItemKey);
        }
        if (!equippedSkinKey.isEmpty()) {
            keyBuilder.append("_skin_").append(equippedSkinKey);
        }
        if (!equippedTrailKey.isEmpty()) {
            keyBuilder.append("_trail_").append(equippedTrailKey);
        }

        String uniqueKey = "pet_" + TextUtil.toId(keyBuilder.toString());

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
            String norm = TextUtil.stripFormatting(line);
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
        if (lore == null || lore.lines().isEmpty()) {
            return RarityParser.parse(null, "Unknown", -1, false);
        }

        // Rarity glyphs live in the private-use area, so the raw (unmodified) lines are
        // required here — they must not be stripped before glyph detection.
        List<String> rawLines = new ArrayList<>(lore.lines().size());
        for (Component lineComp : lore.lines()) {
            rawLines.add(lineComp.getString());
        }
        return RarityParser.parse(rawLines, "Unknown", -1, false);
    }

    private static ParsedRating parseRatingAndStats(List<String> rawLines, String rarity) {
        List<Double> luckPercents = new ArrayList<>();
        List<Double> scalePercents = new ArrayList<>();
        List<Double> allPercents = new ArrayList<>();

        double maxRarityStat = getMaxStatForRarity(rarity);

        for (String rawLine : rawLines) {
            String norm = TextUtil.stripFormatting(rawLine);

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

    private static String stripPrefix(String line, String prefix) {
        int index = line.indexOf(prefix);
        if (index < 0) return "";
        return line.substring(index + prefix.length()).trim();
    }

    private static Map<String, Object> attachmentEntry(String id, int qty) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("qty", qty);
        return entry;
    }

    /**
     * Extracts the id of a pet trail from an already normalized
     * {@code "equipped trail: <name>"} lore line. Returns {@code null} when the line is
     * not a trail line, or when the trail is default/empty/none.
     */
    static String equippedTrailId(String normalizedLine) {
        if (normalizedLine == null || !normalizedLine.contains(EQUIPPED_TRAIL_PREFIX)) return null;

        String trailName = stripPrefix(normalizedLine, EQUIPPED_TRAIL_PREFIX);
        if (isDefaultOrEmpty(trailName)) return null;

        return petTrailId(trailName);
    }

    private static String toItemId(String rawName) {
        return rawName.toLowerCase()
            .replace("'", "")
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
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