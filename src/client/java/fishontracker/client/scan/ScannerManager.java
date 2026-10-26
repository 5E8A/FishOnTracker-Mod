package fishontracker.client.scan;

import fishontracker.client.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owns the scan session: which containers were captured, which were already scanned,
 * and the start/stop flow of {@code /fot scan}. Building the actual export is done by
 * {@link ScanExporter}.
 */
public class ScannerManager {

    private static boolean isScanning = false;

    private static final Map<String, List<ItemStack>> capturedContainersMap = new LinkedHashMap<>();
    private static final Set<String> scannedContainers = new HashSet<>();
    private static String lastScannedContainerSignature = "";

    private static final Pattern PRESET_PATTERN = Pattern.compile("preset.*?(\\d+)", Pattern.CASE_INSENSITIVE);

    public static boolean isScanning() {
        return isScanning;
    }

    public static void startSession() {
        if (isScanning) {
            ChatUtil.chat("§e⚠ Scan session is already running! Use §6/fot scan stop §eto finish.");
            return;
        }

        isScanning = true;
        clearState();

        ChatUtil.chat("§a✔ Scanning session started! Open chests to track items.");
    }

    public static void recordContainer(String containerId, List<ItemStack> items) {
        if (!isScanning || items.isEmpty()) return;

        if (hasRodTitlesItem(items)) {
            ChatUtil.overlay("§c✖ Skipped: Rod Menu");
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
            ChatUtil.overlay("§c✖ Skipped: " + displayTitle);
            return;
        }

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (ItemFilter.isCosmeticItem(stack)) {
                ChatUtil.overlay("§c✖ Skipped: " + displayTitle + " (Cosmetic/Hat detected)");
                return;
            }
        }

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;
            if (isPresetControlButton(stack) || isRodTitlesButton(stack)) continue;

            if (ItemFilter.hasBlacklistedName(stack)) {
                ChatUtil.overlay("§c✖ Skipped: " + displayTitle + " (Blacklisted item detected!)");
                return;
            }
        }

        String containerSignature = isPresetMenu ? presetSignature : buildContainerKey(containerId);

        if (scannedContainers.contains(containerSignature)) {
            if (!containerSignature.equals(lastScannedContainerSignature)) {
                ChatUtil.overlay("§e⚠ Already scanned: " + displayTitle);
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

        int totalItemsCount = 0;
        for (ItemStack stack : items) {
            if (!ItemFilter.isAllowed(stack)) continue;
            if (isPresetControlButton(stack) || isRodTitlesButton(stack)) continue;

            totalItemsCount += quantityOf(stack);
        }

        String itemWord = totalItemsCount == 1 ? "item" : "items";
        ChatUtil.overlay("§a✔ Captured: " + displayTitle + " (" + totalItemsCount + " " + itemWord + ")");
    }

    /** Baits report their remaining uses rather than the stack size. */
    private static int quantityOf(ItemStack stack) {
        if (BaitParser.isBaitItem(stack)) {
            return BaitParser.parseBaitQuantity(stack);
        }
        return stack.getCount();
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

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) continue;

            String cleanName = ItemFilter.cleanText(ItemFilter.getItemEffectiveName(stack));

            StringBuilder loreBuilder = new StringBuilder();
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null) {
                for (Component line : lore.lines()) {
                    loreBuilder.append(ItemFilter.cleanText(line.getString())).append(" ");
                }
            }
            String combinedText = (cleanName + " " + loreBuilder).toLowerCase();

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
        }

        if (!hasAnyPreset) {
            return null;
        }

        String type = isRod ? "rod" : "armor";
        int firstPreset = (minPresetNum != Integer.MAX_VALUE) ? minPresetNum : 1;
        return "presets:" + type + ":" + PresetPages.pageRange(firstPreset);
    }

    static boolean isPresetControlButton(ItemStack stack) {
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

    static boolean isRodTitlesButton(ItemStack stack) {
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
        if (!isScanning) {
            ChatUtil.chat("§e⚠ No scan session is currently running! Use §2/fot scan start §eto begin.");
            return;
        }

        isScanning = false;
        ScanExporter.export(capturedContainersMap);
        clearState();
    }

    private static void clearState() {
        capturedContainersMap.clear();
        scannedContainers.clear();
        lastScannedContainerSignature = "";
    }
}
