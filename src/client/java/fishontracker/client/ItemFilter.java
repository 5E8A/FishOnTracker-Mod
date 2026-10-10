package fishontracker.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemFilter {

    private static final Set<Item> ALLOWED_ITEMS = new HashSet<>();
    private static final Set<String> BLACKLISTED_NAMES = new HashSet<>();


    private static final List<String> BLACKLISTED_CONTAINER_TITLES = List.of(
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
    }

    private static void blacklist(String name) {
        BLACKLISTED_NAMES.add(name.trim().toLowerCase());
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

        if (!ALLOWED_ITEMS.contains(stack.getItem())) return false;

        if (hasBlacklistedName(stack)) return false;

        if (isCosmeticItem(stack)) return false;

        return true;
    }

    public static boolean isCosmeticItem(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return false;

        StringBuilder fullLoreBuilder = new StringBuilder();
        for (Component line : lore.lines()) {
            fullLoreBuilder.append(line.getString()).append(" ");
        }

        String cleanLore = cleanText(fullLoreBuilder.toString());

        return cleanLore.contains("hat cosmetic")
            || cleanLore.contains("cosmetic")
            || cleanLore.contains("click to view hats");
    }

    public static String cleanText(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        String normalized = normalizeCustomFont(raw);

        return BaitParser.normalizeFancyText(normalized)
            .toLowerCase()
            .replaceAll("§[0-9a-fk-or]", "")
            .replaceAll("&[0-9a-fk-or]", "")
            .replaceAll("[\uE000-\uF8FF]", "")
            .replace('\u00A0', ' ') 
            .replace('\u200B', ' ') 
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static String normalizeCustomFont(String text) {
        if (text == null) return "";
        return text
            .replace('ᴀ', 'a').replace('ʙ', 'b').replace('ᴄ', 'c').replace('ᴅ', 'd')
            .replace('ᴇ', 'e').replace('ғ', 'f').replace('ɢ', 'g').replace('ʜ', 'h')
            .replace('ɪ', 'i').replace('ᴊ', 'j').replace('ᴋ', 'k').replace('ʟ', 'l')
            .replace('ᴍ', 'm').replace('ɴ', 'n').replace('ᴏ', 'o').replace('ᴘ', 'p')
            .replace('ǫ', 'q').replace('ʀ', 'r').replace('s', 's').replace('ᴛ', 't')
            .replace('ᴜ', 'u').replace('ᴠ', 'v').replace('ᴡ', 'w').replace('x', 'x')
            .replace('ʏ', 'y').replace('ᴢ', 'z');
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