package fishontracker.client;

import fishontracker.client.util.TextUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps an equipped pet amulet to the star texture drawn on top of the pet icon.
 *
 * <p>FishOnMC Extras draws one and the same yellow star for every equipped pet item.
 * This class supplies a colour per amulet instead, using the icons shipped with the mod.
 * The mapping is fixed in code and cannot be edited at runtime.</p>
 */
public final class AmuletStarTextures {

    private static final String NAMESPACE = "fishontracker";
    private static final String TEXTURE_DIR = "textures/gui/amulet_stars/";

    /** Lore line on a pet that lists the equipped amulet. */
    private static final String EQUIPPED_ITEM_PREFIX = "equipped item:";

    /** Amulet display name -> texture file name. */
    private static final Map<String, String> MAPPING = new LinkedHashMap<>();

    /** Cached texture ids keyed by the normalized amulet name. */
    private static final Map<String, Identifier> TEXTURE_CACHE = new LinkedHashMap<>();

    static {
        map("Prospecting Amulet", "prospecting_icon.png");
        map("Wade's Amulet", "wades_icon.png");
        map("Midas Amulet", "midas_icon.png");
        map("Onyx Amulet", "onyx_icon.png");
        map("Ivory Amulet", "ivory_icon.png");
        map("Fabled Amulet", "fabled_icon.png");
        map("Bigfoot's Necklace", "bigfoot_necklace_icon.png");
        map("Bigfoot's Amulet", "bigfoot_amulet_icon.png");
        map("Moon Amulet", "moon_icon.png");
        map("Cryptid Amulet", "cryptid_icon.png");
        map("Titan's Graceful Necklace", "titans_icon.png");
    }

    private AmuletStarTextures() {
    }

    private static void map(String amuletName, String fileName) {
        MAPPING.put(TextUtil.normalize(amuletName), fileName);
    }

    /**
     * Extracts the equipped amulet name from a pet's lore, or {@code null} when the pet has
     * no amulet (or only a skin/trail) equipped.
     */
    public static String getEquippedAmulet(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) return null;

        List<Component> lines = lore.lines();
        for (Component line : lines) {
            String norm = TextUtil.stripFormatting(line.getString());
            int index = norm.indexOf(EQUIPPED_ITEM_PREFIX);
            if (index < 0) continue;

            String name = norm.substring(index + EQUIPPED_ITEM_PREFIX.length()).trim();
            if (name.isEmpty()) continue;
            if (name.equals("default") || name.equals("empty") || name.equals("none")) continue;

            return name;
        }
        return null;
    }

    /**
     * Texture to draw for the given pet, or {@code null} when the pet has no amulet or the
     * amulet has no custom texture (in which case FishOnMC Extras draws its own star).
     */
    public static Identifier getStarTexture(ItemStack stack) {
        String amulet = getEquippedAmulet(stack);
        return amulet == null ? null : getStarTextureForAmulet(amulet);
    }

    /** See {@link #getStarTexture(ItemStack)}. Accepts the raw amulet display name. */
    public static Identifier getStarTextureForAmulet(String amuletName) {
        String key = TextUtil.normalize(amuletName);
        if (key.isEmpty()) return null;

        Identifier cached = TEXTURE_CACHE.get(key);
        if (cached != null) return cached;

        String fileName = MAPPING.get(key);
        if (fileName == null) return null;

        Identifier texture = Identifier.fromNamespaceAndPath(NAMESPACE, TEXTURE_DIR + fileName);
        TEXTURE_CACHE.put(key, texture);
        return texture;
    }

    /** Amulet display name -> texture file name, as currently configured. */
    static Map<String, String> currentMapping() {
        return Map.copyOf(MAPPING);
    }
}
