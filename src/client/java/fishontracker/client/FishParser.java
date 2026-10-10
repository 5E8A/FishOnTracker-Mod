package fishontracker.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.LinkedHashMap;
import java.util.Map;

public class FishParser {


    private static final String SPECIAL_RARITY_GLYPH = "\uF072";

    public static boolean isFishItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        return stack.is(Items.COD)
            || stack.is(Items.WHITE_DYE)
            || stack.is(Items.BLACK_DYE)
            || stack.is(Items.GOLD_INGOT)
            || stack.is(Items.DRIED_KELP)
            || stack.is(Items.PRISMARINE_SHARD)
            || stack.is(Items.BONE)
            || stack.is(Items.ROTTEN_FLESH);
    }

    public static Map<String, Object> parseFish(ItemStack stack) {
        if (!isFishItem(stack)) return null;

        String fishId = resolveFishId(stack);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", fishId);
        data.put("qty", stack.getCount());

        return data;
    }

    private static String resolveFishId(ItemStack stack) {

        if (stack.is(Items.WHITE_DYE)) {
            return "albino_fish";
        }

        if (stack.is(Items.BLACK_DYE)) {
            return "melanistic_fish";
        }

        if (stack.is(Items.GOLD_INGOT)) {
            return "trophy_fish";
        }

        if (stack.is(Items.DRIED_KELP)) {
            return "fabled_fish";
        }

        if (stack.is(Items.PRISMARINE_SHARD)) {
            return "alternate_fish";
        }

        if (stack.is(Items.BONE)) {
            return "spooky_fish";
        }

        if (stack.is(Items.ROTTEN_FLESH)) {
            return "zombie_fish";
        }

        if (stack.is(Items.COD)) {
            if (hasSpecialGlyphInFirstLine(stack)) {
                return "special_fish";
            }
            return "normal_fish";
        }

        return "normal_fish";
    }

    private static boolean hasSpecialGlyphInFirstLine(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null || lore.lines().isEmpty()) return false;

        String firstLineRaw = lore.lines().get(0).getString();
        return firstLineRaw.contains(SPECIAL_RARITY_GLYPH) || firstLineRaw.contains("");
    }
}