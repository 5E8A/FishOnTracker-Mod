package fishontracker.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScannerManager {
    public static boolean isScanning = false;
    private static int containersCount = 0;
    private static final List<ItemStack> capturedItems = new ArrayList<>();

    private static final Set<String> scannedContainers = new HashSet<>();
    private static String lastScannedContainerSignature = "";

    private static final Pattern PRESET_PATTERN = Pattern.compile("preset.*?(\\d+)", Pattern.CASE_INSENSITIVE);

    public static void startSession() {
        Minecraft client = Minecraft.getInstance();

        if (isScanning) {
            if (client.player != null) {
                client.player.sendSystemMessage(
                    Component.literal("§e⚠ [FOT] Scan session is already running! Use §6/fot scan stop §eto finish.")
                );
            }
            return;
        }

        isScanning = true;
        containersCount = 0;
        capturedItems.clear();
        scannedContainers.clear();
        lastScannedContainerSignature = "";

        if (client.player != null) {
            client.player.sendSystemMessage(
                Component.literal("§a✔ [FOT] Scanning session started! Open chests to track items.")
            );
        }
    }

    public static void recordContainer(String containerId, List<ItemStack> items) {
        if (!isScanning || items.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();

        if (hasRodTitlesItem(items)) {
            if (client.gui != null) {
                client.gui.setOverlayMessage(
                    Component.literal("§c✖ [FOT] Skipped: Rod Menu"),
                    false
                );
            }
            return;
        }

        String presetSignature = resolvePresetSignature(items);
        boolean isPresetMenu = presetSignature != null;

        String displayTitle;
        if (isPresetMenu) {
            String[] parts = presetSignature.split(":");
            String cat = parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
            displayTitle = cat + " Presets (" + parts[2] + ")";
        } else {
            displayTitle = ItemFilter.cleanText(containerId);
            if (displayTitle.isEmpty()) {
                displayTitle = "Menu";
            }
        }

        if (!isPresetMenu && ItemFilter.isBlacklistedContainerTitle(containerId)) {
            if (client.gui != null) {
                client.gui.setOverlayMessage(
                    Component.literal("§c✖ [FOT] Skipped: " + displayTitle),
                    false
                );
            }
            return;
        }

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (ItemFilter.isCosmeticItem(stack)) {
                if (client.gui != null) {
                    client.gui.setOverlayMessage(
                        Component.literal("§c✖ [FOT] Skipped: " + displayTitle + " (Cosmetic/Hat detected)"),
                        false
                    );
                }
                return;
            }
        }

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;

            if (isPresetControlButton(stack) || isRodTitlesButton(stack)) {
                continue;
            }

            if (ItemFilter.hasBlacklistedName(stack)) {
                if (client.gui != null) {
                    client.gui.setOverlayMessage(
                        Component.literal("§c✖ [FOT] Skipped: " + displayTitle + " (Blacklisted item detected!)"),
                        false
                    );
                }
                return;
            }
        }

        String containerSignature = isPresetMenu ? presetSignature : buildContainerKey(containerId);

        if (scannedContainers.contains(containerSignature)) {
            if (!containerSignature.equals(lastScannedContainerSignature)) {
                if (client.gui != null) {
                    client.gui.setOverlayMessage(
                        Component.literal("§e⚠ [FOT] Already scanned: " + displayTitle),
                        false
                    );
                }
            }
            return;
        }

        lastScannedContainerSignature = containerSignature;
        scannedContainers.add(containerSignature);

        containersCount++;

        // Dodajemy przedmioty z weryfikacją duplikatów NBT/Stack
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (isPresetControlButton(stack) || isRodTitlesButton(stack)) continue;

            boolean isAlreadyCaptured = false;
            for (ItemStack captured : capturedItems) {
                if (ItemStack.matches(captured, stack)) {
                    isAlreadyCaptured = true;
                    break;
                }
            }

            if (!isAlreadyCaptured) {
                capturedItems.add(stack.copy());
            }
        }

        if (client.gui != null) {
            int totalItemsCount = 0;
            for (ItemStack stack : items) {
                if (!ItemFilter.isAllowed(stack)) continue;
                if (isPresetControlButton(stack) || isRodTitlesButton(stack)) continue;

                if (RodPartParser.isRodPart(stack)) {
                    totalItemsCount += stack.getCount();
                } else if (ArmorParser.isArmor(stack)) {
                    totalItemsCount += stack.getCount();
                } else if (SuperchargeParser.isSupercharge(stack)) {
                    totalItemsCount += stack.getCount();
                } else if (BaitPackageParser.isBaitPackage(stack)) {
                    totalItemsCount += stack.getCount();
                } else if (BaitParser.isBaitItem(stack)) {
                    totalItemsCount += BaitParser.parseBaitQuantity(stack);
                } else if (PetParser.isPet(stack)) {
                    totalItemsCount += stack.getCount();
                } else if (FishParser.isFishItem(stack)) {
                    totalItemsCount += stack.getCount();
                } else {
                    totalItemsCount += stack.getCount();
                }
            }

            String itemWord = totalItemsCount == 1 ? "item" : "items";
            client.gui.setOverlayMessage(
                Component.literal("§a✔ [FOT] Captured: " + displayTitle + " (" + totalItemsCount + " " + itemWord + ")"),
                false
            );
        }
    }

    private static String resolvePresetSignature(List<ItemStack> items) {
        boolean isRod = false;
        int minPresetNum = Integer.MAX_VALUE;
        boolean hasAnyPreset = false;

        StringBuilder itemsHash = new StringBuilder();

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;

            String effectiveName = ItemFilter.getItemEffectiveName(stack);
            String cleanName = ItemFilter.cleanText(effectiveName);

            StringBuilder loreBuilder = new StringBuilder();
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) {
                    loreBuilder.append(ItemFilter.cleanText(line.getString())).append(" ");
                }
            }
            String combinedText = cleanName + " " + loreBuilder.toString();

            if (combinedText.contains("switch to armor presets") || combinedText.contains("armor presets")) {
                isRod = true;
            }

            Matcher matcher = PRESET_PATTERN.matcher(cleanName);
            if (matcher.find()) {
                hasAnyPreset = true;
                try {
                    int num = Integer.parseInt(matcher.group(1));
                    if (num < minPresetNum) {
                        minPresetNum = num;
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (!isPresetControlButton(stack) && !isRodTitlesButton(stack)) {
                itemsHash.append(cleanName).append(":").append(stack.getCount()).append(";");
            }
        }

        if (!hasAnyPreset) {
            return null;
        }

        String type = isRod ? "rod" : "armor";
        int pageGroup = (minPresetNum != Integer.MAX_VALUE) ? ((minPresetNum - 1) / 5) + 1 : 1;
        int startRange = (pageGroup - 1) * 5 + 1;
        int endRange = pageGroup * 5;

        return "presets:" + type + ":" + startRange + "-" + endRange + ":" + itemsHash.toString().hashCode();
    }

    private static boolean isPresetControlButton(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String cleanEffective = ItemFilter.cleanText(ItemFilter.getItemEffectiveName(stack));
        if (cleanEffective.contains("switch to") || cleanEffective.contains("preset")) {
            return true;
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                String cleanLine = ItemFilter.cleanText(line.getString());
                if (cleanLine.contains("switch to") || cleanLine.contains("preset")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRodTitlesButton(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String cleanEffective = ItemFilter.cleanText(ItemFilter.getItemEffectiveName(stack));
        return cleanEffective.contains("rod title");
    }

    private static boolean hasRodTitlesItem(List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (isRodTitlesButton(stack)) {
                return true;
            }
        }
        return false;
    }

    private static net.minecraft.core.BlockPos getCanonicalBlockPos(Minecraft client, net.minecraft.core.BlockPos pos) {
        if (client.level == null) return pos;

        BlockState state = client.level.getBlockState(pos);

        if (state.getBlock() instanceof ChestBlock) {
            if (state.hasProperty(ChestBlock.TYPE)) {
                ChestType type = state.getValue(ChestBlock.TYPE);

                if (type == ChestType.RIGHT) {
                    return pos.relative(ChestBlock.getConnectedDirection(state));
                }
            }
        }

        return pos;
    }

    private static String buildContainerKey(String containerId) {
        Minecraft client = Minecraft.getInstance();

        String cleanTitle = ItemFilter.cleanText(containerId);

        if (cleanTitle.contains("vault") || cleanTitle.contains("personal") || cleanTitle.matches(".*\\b\\d+\\b.*")) {
            return "vault:" + cleanTitle;
        }

        if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
            if (client.level != null && client.level.getBlockState(pos).hasBlockEntity()) {
                net.minecraft.core.BlockPos canonicalPos = getCanonicalBlockPos(client, pos);
                return "block:" + canonicalPos.getX() + "_" + canonicalPos.getY() + "_" + canonicalPos.getZ();
            }
        }

        return "menu:" + cleanTitle;
    }

    public static void stopSession() {
        Minecraft client = Minecraft.getInstance();

        if (!isScanning) {
            if (client.player != null) {
                client.player.sendSystemMessage(
                    Component.literal("§e⚠ [FOT] No scan session is currently running! Use §6/fot scan start §eto begin.")
                );
            }
            return;
        }

        isScanning = false;
        if (client.player == null) return;

        List<Map<String, Object>> exportedList = new ArrayList<>();
        Map<String, Integer> mergedItems = new LinkedHashMap<>();
        int totalQuantitiesSum = 0;
        int customItemsCount = 0;

        for (ItemStack stack : capturedItems) {
            if (stack.isEmpty()) continue;
            if (!ItemFilter.isAllowed(stack)) continue;

            if (isPresetControlButton(stack) || isRodTitlesButton(stack)) {
                continue;
            }

            if (RodPartParser.isRodPart(stack)) {
                Map<String, Object> rodData = RodPartParser.parseRodPart(stack);
                exportedList.add(rodData);
                totalQuantitiesSum += stack.getCount();
                customItemsCount++;
                continue;
            }

            if (ArmorParser.isArmor(stack)) {
                Map<String, Object> armorData = ArmorParser.parseArmor(stack);
                exportedList.add(armorData);
                totalQuantitiesSum += stack.getCount();
                customItemsCount++;
                continue;
            }

            if (SuperchargeParser.isSupercharge(stack)) {
                Map<String, Object> superchargeData = SuperchargeParser.parseSupercharge(stack);
                exportedList.add(superchargeData);
                totalQuantitiesSum += stack.getCount();
                customItemsCount++;
                continue;
            }

            if (PetParser.isPet(stack)) {
                PetParser.PetParseResult result = PetParser.parsePet(stack);
                exportedList.add(result.petData);
                totalQuantitiesSum += stack.getCount();
                customItemsCount++;

                for (Map<String, Object> extraItem : result.attachedItems) {
                    exportedList.add(extraItem);
                    totalQuantitiesSum += 1;
                }
                continue;
            }

            if (FishParser.isFishItem(stack)) {
                Map<String, Object> fishEntry = FishParser.parseFish(stack);
                if (fishEntry != null) {
                    String fishId = (String) fishEntry.get("id");
                    int count = stack.getCount();
                    mergedItems.put(fishId, mergedItems.getOrDefault(fishId, 0) + count);
                    totalQuantitiesSum += count;
                    continue;
                }
            }

            String itemId;
            int count;

            if (BaitPackageParser.isBaitPackage(stack)) {
                count = stack.getCount();
                itemId = BaitPackageParser.getBaitPackageId(stack);
            }
            else if (BaitParser.isBaitItem(stack)) {
                count = BaitParser.parseBaitQuantity(stack);
                String rawName = stack.getHoverName().getString();
                String color = BaitParser.parseBaitColor(stack);
                if (color != null && !color.isEmpty()) {
                    rawName += " " + color;
                }
                itemId = normalizeToId(rawName);
            } 
            else if (ChummerParser.isChummerItem(stack)) {
                count = stack.getCount();
                itemId = ChummerParser.getChummerId(stack);
            } 
            else {
                count = stack.getCount();
                itemId = normalizeToId(ItemFilter.getItemEffectiveName(stack));
            }

            mergedItems.put(itemId, mergedItems.getOrDefault(itemId, 0) + count);
            totalQuantitiesSum += count;
        }

        mergedItems.forEach((id, qty) -> {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("id", id);
            itemMap.put("qty", qty);
            exportedList.add(itemMap);
        });

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(exportedList);

        client.keyboardHandler.setClipboard(json);
        saveToFile(json);

        String containerWord = containersCount == 1 ? "container" : "containers";
        String itemWord = totalQuantitiesSum == 1 ? "item" : "items";
        int totalUniqueEntries = mergedItems.size() + customItemsCount;

        client.player.sendSystemMessage(
            Component.literal("§6[FOT] Scan complete! Recorded " 
                + containersCount + " " + containerWord + " (" + totalQuantitiesSum + " " + itemWord 
                + ", " + totalUniqueEntries + " unique). JSON copied to clipboard & saved to file!")
        );

        scannedContainers.clear();
        lastScannedContainerSignature = "";
    }

    public static String normalizeToId(String displayName) {
        if (displayName == null) return "unknown";
        
        String normalized = BaitParser.normalizeFancyText(displayName);

        return normalized
            .trim()
            .toLowerCase()
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("&[0-9a-fk-or]", "")
            .replaceAll("[\uE000-\uF8FF]", "")
            .replaceAll("™", "")
            .replaceAll("\u2122", "")
            .replaceAll("[\\s\\-]+", "_")
            .replaceAll("[^a-z0-9_]", "");
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
            e.printStackTrace();
        }
    }
}