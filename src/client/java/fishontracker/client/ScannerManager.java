package fishontracker.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;

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

    private static final Map<String, List<ItemStack>> capturedContainersMap = new LinkedHashMap<>();
    private static final Set<String> scannedContainers = new HashSet<>();
    private static String lastScannedContainerSignature = "";

    private static final Pattern PRESET_PATTERN = Pattern.compile("preset.*?(\\d+)", Pattern.CASE_INSENSITIVE);

    public static void startSession() {
        Minecraft client = Minecraft.getInstance();

        if (isScanning) {
            if (client.player != null) {
                client.player.sendSystemMessage(
                    Component.literal("§2§l[FOT] §8» §e⚠ Scan session is already running! Use §6/fot scan stop §eto finish.")
                );
            }
            return;
        }

        isScanning = true;
        capturedContainersMap.clear();
        scannedContainers.clear();
        lastScannedContainerSignature = "";

        if (client.player != null) {
            client.player.sendSystemMessage(
                Component.literal("§2§l[FOT] §8» §a✔ Scanning session started! Open chests to track items.")
            );
        }
    }

public static void recordContainer(String containerId, List<ItemStack> items) {
        if (!isScanning || items.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();

        if (hasRodTitlesItem(items)) {
            if (client.gui != null) {
                client.gui.setOverlayMessage(
                    Component.literal("§2§l[FOT] §8» §c✖ Skipped: Rod Menu"),
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
            
            String rangeLabel = extractPresetRangeLabel(items);
                displayTitle = cat + " Presets" + (rangeLabel.isEmpty() ? "" : " (" + rangeLabel + ")");
        } else {
            displayTitle = ItemFilter.cleanText(containerId);
            if (displayTitle.isEmpty()) {
                displayTitle = "Menu";
            }
        }

        if (!isPresetMenu && ItemFilter.isBlacklistedContainerTitle(containerId)) {
            if (client.gui != null) {
                client.gui.setOverlayMessage(
                    Component.literal("§2§l[FOT] §8» §c✖ Skipped: " + displayTitle),
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
                        Component.literal("§2§l[FOT] §8» §c✖ Skipped: " + displayTitle + " (Cosmetic/Hat detected)"),
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
                        Component.literal("§2§l[FOT] §8» §c✖ Skipped: " + displayTitle + " (Blacklisted item detected!)"),
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
                        Component.literal("§2§l[FOT] §8» §e⚠ Already scanned: " + displayTitle),
                        false
                    );
                }
            }
            return;
        }

        lastScannedContainerSignature = containerSignature;
        scannedContainers.add(containerSignature);

        List<ItemStack> containerItemsList = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (isPresetControlButton(stack) || isRodTitlesButton(stack)) continue;

            containerItemsList.add(stack.copy());
        }

        capturedContainersMap.put(containerSignature, containerItemsList);

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
                Component.literal("§2§l[FOT] §8» §a✔ Captured: " + displayTitle + " (" + totalItemsCount + " " + itemWord + ")"),
                false
            );
        }
    }

    private static String extractPresetRangeLabel(List<ItemStack> items) {
        List<Integer> nums = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            String cleanName = ItemFilter.cleanText(ItemFilter.getItemEffectiveName(stack));
            Matcher matcher = PRESET_PATTERN.matcher(cleanName);
            if (matcher.find()) {
                try {
                    int num = Integer.parseInt(matcher.group(1));
                    if (!nums.contains(num)) nums.add(num);
                } catch (NumberFormatException ignored) {}
            }
        }
        if (nums.isEmpty()) return "";
        nums.sort(Integer::compareTo);
        if (nums.size() == 1) return String.valueOf(nums.get(0));
        return nums.get(0) + "-" + nums.get(nums.size() - 1);
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
            String combinedText = (cleanName + " " + loreBuilder.toString()).toLowerCase();

            // Jeśli przycisk proponuje zmianę na Armor Presets -> znaczy, że jesteśmy w Rod Presets!
            if (combinedText.contains("switch to armor presets")) {
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

        return "presets:" + type + ":" + startRange + "-" + endRange;
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

    private static BlockPos getCanonicalBlockPos(Minecraft client, BlockPos pos) {
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

        if (client.hitResult instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            if (client.level != null && client.level.getBlockState(pos).hasBlockEntity()) {
                BlockPos canonicalPos = getCanonicalBlockPos(client, pos);
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
                    Component.literal("§2§l[FOT] §8» §e⚠ No scan session is currently running! Use §2/fot scan start §eto begin.")
                );
            }
            return;
        }

        isScanning = false;
        if (client.player == null) return;

        List<Map<String, Object>> exportedList = new ArrayList<>();
        Map<String, Integer> mergedItems = new LinkedHashMap<>();
        Map<String, Map<String, Object>> mergedCustomItems = new LinkedHashMap<>();

        int totalQuantitiesSum = 0;

        List<ItemStack> allCapturedItems = new ArrayList<>();
        for (List<ItemStack> containerList : capturedContainersMap.values()) {
            allCapturedItems.addAll(containerList);
        }

        for (ItemStack stack : allCapturedItems) {
            if (stack.isEmpty()) continue;
            if (!ItemFilter.isAllowed(stack)) continue;

            if (isPresetControlButton(stack) || isRodTitlesButton(stack)) {
                continue;
            }

            if (RodPartParser.isRodPart(stack)) {
                Map<String, Object> rodData = RodPartParser.parseRodPart(stack);
                String id = (String) rodData.get("id");
                int qty = stack.getCount();

                if (!mergedCustomItems.containsKey(id)) {
                    mergedCustomItems.put(id, rodData);
                    totalQuantitiesSum += qty;
                }
                continue;
            }

            if (ArmorParser.isArmor(stack)) {
                Map<String, Object> armorData = ArmorParser.parseArmor(stack);
                String id = (String) armorData.get("id");
                int qty = stack.getCount();

                if (!mergedCustomItems.containsKey(id)) {
                    mergedCustomItems.put(id, armorData);
                    totalQuantitiesSum += qty;
                }
                continue;
            }

            if (SuperchargeParser.isSupercharge(stack)) {
                Map<String, Object> superchargeData = SuperchargeParser.parseSupercharge(stack);
                String id = (String) superchargeData.get("id");
                int qty = stack.getCount();

                if (!mergedCustomItems.containsKey(id)) {
                    mergedCustomItems.put(id, superchargeData);
                    totalQuantitiesSum += qty;
                }
                continue;
            }

            if (PetParser.isPet(stack)) {
                PetParser.PetParseResult result = PetParser.parsePet(stack);
                String id = (String) result.petData.get("id");
                int qty = stack.getCount();

                if (!mergedCustomItems.containsKey(id)) {
                    mergedCustomItems.put(id, result.petData);
                    totalQuantitiesSum += qty;

                    for (Map<String, Object> extraItem : result.attachedItems) {
                        String extraId = (String) extraItem.get("id");
                        if (extraId != null) {
                            mergedItems.put(extraId, mergedItems.getOrDefault(extraId, 0) + 1);
                            totalQuantitiesSum += 1;
                        }
                    }
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

        exportedList.addAll(mergedCustomItems.values());

        mergedItems.forEach((id, qty) -> {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("id", id);
            itemMap.put("qty", qty);
            exportedList.add(itemMap);
        });

        long currentCash = ScoreboardReader.getWalletBalance();
        String username = PlayerDataFetcher.getUsername();
        int playerLevel = PlayerDataFetcher.getPlayerLevel();

        Map<String, Object> finalExport = new LinkedHashMap<>();
        finalExport.put("version", "1.1");
        finalExport.put("username", username);
        finalExport.put("level", playerLevel);
        finalExport.put("cash", currentCash);
        finalExport.put("inventory", exportedList);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(finalExport);

        client.keyboardHandler.setClipboard(json);
        saveToFile(json);

        int finalContainersCount = capturedContainersMap.size();

        String containerWord = finalContainersCount == 1 ? "container" : "containers";
        String itemWord = totalQuantitiesSum == 1 ? "item" : "items";
        int totalUniqueEntries = mergedCustomItems.size() + mergedItems.size();

        client.player.sendSystemMessage(
            Component.literal("§2§l[FOT] §8» §a✔ Scan complete! Recorded " 
                + finalContainersCount + " " + containerWord + " (" + totalQuantitiesSum + " " + itemWord 
                + ", " + totalUniqueEntries + " unique). JSON copied to clipboard & saved to file!")
        );

        capturedContainersMap.clear();
        scannedContainers.clear();
        lastScannedContainerSignature = "";
    }

    public static String normalizeToId(String displayName) {
        if (displayName == null) return "unknown";
        
        String normalized = BaitParser.normalizeFancyText(displayName);

        return normalized
            .trim()
            .toLowerCase()
            .replace("\u0027", "_")
            .replace("'", "_")
            .replace("’", "_")
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