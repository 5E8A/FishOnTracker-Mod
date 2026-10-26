package fishontracker.client.scan;

import fishontracker.Fishontracker;
import fishontracker.client.util.TextUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ItemFilter {

    private static final Set<Item> ALLOWED_ITEMS = new HashSet<>();
    private static final Set<String> BLACKLISTED_NAMES = new LinkedHashSet<>();
    private static final Set<String> BLACKLISTED_CONTAINER_TITLES = new LinkedHashSet<>();

    /**
     * Both blacklists are editable at runtime so a server-side text change can be fixed
     * without recompiling the mod:
     *   config/fishontracker/blacklist_items.txt
     *   config/fishontracker/blacklist_containers.txt
     * If a file does not exist it is created with the built-in defaults; if it exists it
     * fully replaces them (delete the file to restore defaults).
     */
    private static final String ITEM_BLACKLIST_FILE = "blacklist_items.txt";
    private static final String CONTAINER_BLACKLIST_FILE = "blacklist_containers.txt";

    private static final List<String> DEFAULT_CONTAINER_TITLES = List.of(
        "menu",
        "identifier",
        "forge",
        "scrapper",
        "calibrator",
        "fish merchant"
    );

    static {
        // ALLOWED ITEMS
        ALLOWED_ITEMS.add(Items.FLINT);
        ALLOWED_ITEMS.add(Items.BLAZE_ROD);
        ALLOWED_ITEMS.add(Items.FEATHER);
        ALLOWED_ITEMS.add(Items.PLAYER_HEAD);
        ALLOWED_ITEMS.add(Items.LEATHER_BOOTS);
        ALLOWED_ITEMS.add(Items.LEATHER_LEGGINGS);
        ALLOWED_ITEMS.add(Items.LEATHER_CHESTPLATE);
        ALLOWED_ITEMS.add(Items.SLIME_BALL);
        ALLOWED_ITEMS.add(Items.COOKED_COD);
        ALLOWED_ITEMS.add(Items.STICK);
        ALLOWED_ITEMS.add(Items.COD);
        ALLOWED_ITEMS.add(Items.BREEZE_ROD);
        ALLOWED_ITEMS.add(Items.IRON_NUGGET);
        ALLOWED_ITEMS.add(Items.BAKED_POTATO);
        ALLOWED_ITEMS.add(Items.BONE);
        ALLOWED_ITEMS.add(Items.PRISMARINE_SHARD);
        ALLOWED_ITEMS.add(Items.LIGHT_BLUE_DYE);
        ALLOWED_ITEMS.add(Items.WHITE_DYE);
        ALLOWED_ITEMS.add(Items.BLACK_DYE);
        ALLOWED_ITEMS.add(Items.GOLD_INGOT);
        ALLOWED_ITEMS.add(Items.DRIED_KELP);
        ALLOWED_ITEMS.add(Items.ROTTEN_FLESH);
        ALLOWED_ITEMS.add(Items.BLAZE_POWDER);

        // BLACKLISTED ITEM NAMES
        blacklist("Current Listings");
        blacklist("Crafting Components");
        blacklist("Fish Compendium");
        blacklist("Store Reroll");
        blacklist("Climate Armor Sets");
        blacklist("Filter");
        blacklist("Species Collections");
        blacklist("Basses Collection");
        blacklist("HATS");
        blacklist("Crew Info");
        blacklist("Event Leaderboards");
        blacklist("Tournament Rewards");
        blacklist("Power Breakdown");
        blacklist("Cypress Lake");
        blacklist("Albino Collection");
        blacklist("STONE MASON");
        blacklist("Categories");
        blacklist("Sort Listings");
        blacklist("Information");
        blacklist("Ambient Sounds");
        blacklist("500x credits");
        blacklist("Calibrator");
        blacklist("Instance 1");
        blacklist("IDENTIFIER");
        blacklist("Armor Forge");
        blacklist("ABANDON");
        blacklist("Fishing Quest #1");
        blacklist("EASY");
        blacklist("Adult");
        blacklist("Rod Part Trade Ins");
        blacklist("Return");
        blacklist("Empty Reconstruction Slot");
        blacklist("Empty Manufacturing Slot");
        blacklist("Daily Missions");
        blacklist("Rod Titles");

        BLACKLISTED_CONTAINER_TITLES.addAll(DEFAULT_CONTAINER_TITLES);

        loadExternalBlacklists();
    }

    private static void blacklist(String name) {
        // Normalize exactly like the titles/items we compare against, otherwise entries
        // containing punctuation (e.g. "Fishing Quest #1") can never match.
        BLACKLISTED_NAMES.add(TextUtil.normalize(name));
    }

    private static void loadExternalBlacklists() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("fishontracker");
        loadOrWriteList(configDir.resolve(ITEM_BLACKLIST_FILE), BLACKLISTED_NAMES);
        loadOrWriteList(configDir.resolve(CONTAINER_BLACKLIST_FILE), BLACKLISTED_CONTAINER_TITLES);
    }

    private static void loadOrWriteList(Path file, Set<String> target) {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try (var writer = Files.newBufferedWriter(file)) {
                    writer.write("# One entry per line. Lines starting with '#' are ignored.\n");
                    writer.write("# Entries are matched against normalized (lowercase, no punctuation) text.\n");
                    writer.write("# Delete this file to restore the mod's built-in defaults.\n");
                    for (String entry : target) {
                        writer.write(entry + "\n");
                    }
                }
                return;
            }

            List<String> lines = Files.readAllLines(file);
            target.clear();
            int loaded = 0;
            for (String line : lines) {
                String entry = line.trim();
                if (entry.isEmpty() || entry.startsWith("#")) continue;
                String normalized = TextUtil.normalize(entry);
                if (normalized.isEmpty()) continue;
                if (target.add(normalized)) loaded++;
            }
            Fishontracker.LOGGER.info("Loaded {} entries from {}", loaded, file.getFileName());
        } catch (Exception e) {
            Fishontracker.LOGGER.error("Failed to read blacklist file {}", file, e);
        }
    }

    public static String getItemEffectiveName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";

        boolean hasCustomName = stack.has(DataComponents.CUSTOM_NAME);
        String hoverName = stack.getHoverName().getString();
        String cleanHover = cleanText(hoverName);

        if (!hasCustomName || cleanHover.isEmpty()) {
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                for (Component line : lore.lines()) {
                    String lineClean = cleanText(line.getString());
                    if (!lineClean.isEmpty()) {
                        return line.getString();
                    }
                }
            }
        }

        return hoverName;
    }

    public static boolean isAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        // Standalone pet skins/trails are custom server items on an unknown base item,
        // so they are recognised by their lore marker instead of the base item list.
        if (!ALLOWED_ITEMS.contains(stack.getItem()) && !PetParser.isPetCosmetic(stack)) return false;

        if (hasBlacklistedName(stack)) return false;

        if (isCosmeticItem(stack)) return false;

        return true;
    }

    /**
     * Hat cosmetics live in the store menu and must never be scanned. Pet cosmetics are
     * matched separately — their lore also says "cosmetic", so matching that word here
     * would cause every container holding a pet skin/trail to be skipped.
     */
    public static boolean isCosmeticItem(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        StringBuilder fullLoreBuilder = new StringBuilder();
        for (Component line : lore.lines()) {
            fullLoreBuilder.append(line.getString()).append(" ");
        }

        String cleanLore = cleanText(fullLoreBuilder.toString());

        return cleanLore.contains("hat cosmetic")
            || cleanLore.contains("click to view hats");
    }

    public static String cleanText(String raw) {
        return TextUtil.normalize(raw);
    }

    public static boolean hasBlacklistedName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String cleanTitle = cleanText(getItemEffectiveName(stack));
        if (cleanTitle.isEmpty()) return false;

        for (String blacklisted : BLACKLISTED_NAMES) {
            if (cleanTitle.contains(blacklisted)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlacklistedContainerTitle(String containerTitle) {
        if (containerTitle == null || containerTitle.isEmpty()) return false;
        String cleanTitle = cleanText(containerTitle);

        for (String blacklisted : BLACKLISTED_CONTAINER_TITLES) {
            if (cleanTitle.contains(blacklisted)) {
                return true;
            }
        }
        return false;
    }
}
