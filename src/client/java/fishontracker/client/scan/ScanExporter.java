package fishontracker.client.scan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fishontracker.Fishontracker;
import fishontracker.client.util.ChatUtil;
import fishontracker.client.util.TextUtil;
import fishontracker.client.config.TrackerConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates every captured container into a single export (clipboard + JSON file).
 *
 * <p>Split out of {@link ScannerManager} so the scanning session logic and the export
 * logic stay small and separate. Quantity merging goes through {@link #addCustom} /
 * {@link #addSimple}, which read quantities as {@link Number} instead of blindly casting
 * to {@link Integer}.</p>
 */
final class ScanExporter {

    private final Map<String, List<ItemStack>> containers;
    private final Map<String, Map<String, Object>> customById = new LinkedHashMap<>();
    private final Map<String, Integer> simpleById = new LinkedHashMap<>();
    private int totalQuantity = 0;

    private ScanExporter(Map<String, List<ItemStack>> containers) {
        this.containers = containers;
    }

    /** Exports the given captured containers and clears nothing (caller owns the map). */
    static void export(Map<String, List<ItemStack>> containers) {
        new ScanExporter(containers).run();
    }

    private void run() {
        aggregate();

        List<Map<String, Object>> exportedList = new ArrayList<>(customById.values());
        simpleById.forEach((id, qty) -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", id);
            entry.put("qty", qty);
            exportedList.add(entry);
        });

        Map<String, Object> finalExport = new LinkedHashMap<>();
        finalExport.put("version", "1.1");
        finalExport.put("username", profileValue(PlayerDataFetcher.getUsername(), ""));
        finalExport.put("level", profileValue(PlayerDataFetcher.getPlayerLevel(), 0));
        finalExport.put("cash", profileValue(ScoreboardReader.getWalletBalance(), 0));
        finalExport.put("inventory", exportedList);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(finalExport);

        Minecraft client = Minecraft.getInstance();
        client.keyboardHandler.setClipboard(json);
        saveToFile(json);

        int containerCount = containers.size();
        int uniqueEntries = customById.size() + simpleById.size();
        ChatUtil.chat("§a✔ Scan complete! Recorded "
            + containerCount + " " + (containerCount == 1 ? "container" : "containers")
            + " (" + totalQuantity + " " + (totalQuantity == 1 ? "item" : "items")
            + ", " + uniqueEntries + " unique). JSON copied to clipboard & saved to file!");
    }

    private void aggregate() {
        for (List<ItemStack> containerItems : containers.values()) {
            for (ItemStack stack : containerItems) {
                if (stack == null || stack.isEmpty()) continue;
                if (!ItemFilter.isAllowed(stack)) continue;
                if (ScannerManager.isPresetControlButton(stack) || ScannerManager.isRodTitlesButton(stack)) continue;
                aggregateStack(stack);
            }
        }
    }

    private void aggregateStack(ItemStack stack) {
        // --- STANDALONE PET COSMETICS (unequipped skins / trails) ---
        // Must run before the pet check: their names contain "pet" ("Leatherback Pet Skin").
        String cosmeticId = PetParser.petCosmeticId(stack);
        if (cosmeticId != null) {
            addSimple(cosmeticId, stack.getCount());
            return;
        }

        // --- ARMOR ---
        if (ArmorParser.isArmor(stack)) {
            addCustom(ArmorParser.parseArmor(stack), stack.getCount());
            return;
        }

        // --- PETS ---
        if (PetParser.isPet(stack)) {
            PetParser.PetParseResult result = PetParser.parsePet(stack);
            addCustom(result.petData, stack.getCount());
            for (Map<String, Object> attached : result.attachedItems) {
                Object id = attached.get("id");
                if (id == null) continue;
                addSimple(String.valueOf(id), quantity(attached));
            }
            return;
        }

        // --- ROD PARTS ---
        if (RodPartParser.isRodPart(stack)) {
            addCustom(RodPartParser.parseRodPart(stack), stack.getCount());
            return;
        }

        // --- SUPERCHARGE ---
        if (SuperchargeParser.isSupercharge(stack)) {
            addCustom(SuperchargeParser.parseSupercharge(stack), stack.getCount());
            return;
        }

        // --- SIMPLE (id + quantity only) ITEMS ---
        if (BaitPackageParser.isBaitPackage(stack)) {
            addSimple(BaitPackageParser.getBaitPackageId(stack), stack.getCount());
        } else if (BaitParser.isBaitItem(stack)) {
            String rawName = stack.getHoverName().getString();
            String color = BaitParser.parseBaitColor(stack);
            if (color != null && !color.isEmpty()) {
                rawName += " " + color;
            }
            addSimple(TextUtil.toId(rawName), BaitParser.parseBaitQuantity(stack));
        } else if (ChummerParser.isChummerItem(stack)) {
            addSimple(ChummerParser.getChummerId(stack), stack.getCount());
        } else if (FishParser.isFishItem(stack)) {
            Map<String, Object> fishEntry = FishParser.parseFish(stack);
            if (fishEntry != null) {
                addSimple(String.valueOf(fishEntry.get("id")), stack.getCount());
            } else {
                addSimple(TextUtil.toId(ItemFilter.getItemEffectiveName(stack)), stack.getCount());
            }
        } else {
            addSimple(TextUtil.toId(ItemFilter.getItemEffectiveName(stack)), stack.getCount());
        }
    }

    /** Merges a fully-described custom item (armor, pet, rod part, ...) by its id. */
    private void addCustom(Map<String, Object> data, int count) {
        String id = String.valueOf(data.get("id"));
        Map<String, Object> existing = customById.get(id);
        if (existing != null) {
            existing.put("qty", quantity(existing) + count);
        } else {
            data.put("qty", count);
            customById.put(id, data);
        }
        totalQuantity += count;
    }

    /** Merges a simple id/qty entry. */
    private void addSimple(String id, int count) {
        simpleById.merge(id, count, Integer::sum);
        totalQuantity += count;
    }

    /** Reads a "qty" field safely — numbers may arrive as Integer, Long or Double. */
    private static int quantity(Map<String, Object> entry) {
        Object qty = entry.get("qty");
        return (qty instanceof Number number) ? number.intValue() : 1;
    }

    /**
     * The website expects the profile fields to always be present, so when profile data is
     * disabled they are exported as empty values instead of being omitted.
     */
    private static Object profileValue(Object value, Object emptyValue) {
        return TrackerConfig.INSTANCE.includeProfileData ? value : emptyValue;
    }

    private static void saveToFile(String jsonContent) {
        try {
            Path exportPath = FabricLoader.getInstance().getConfigDir().resolve("fishontracker/exports");
            File dir = exportPath.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy_HH-mm-ss"));
            File file = new File(dir, "fot_eq_" + timestamp + ".json");

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonContent);
            }
        } catch (Exception e) {
            Fishontracker.LOGGER.error("Failed to write scan export file", e);
        }
    }
}
