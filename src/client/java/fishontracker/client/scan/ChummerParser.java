package fishontracker.client.scan;

import fishontracker.client.util.TextUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public class ChummerParser {

    public static boolean isChummerItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        String name = TextUtil.stripFormatting(stack.getHoverName().getString());
        return name.contains("chummer");
    }

    public static String parseRarity(ItemStack stack) {
        ItemLore loreComponent = stack.get(DataComponents.LORE);
        if (loreComponent == null || loreComponent.lines().isEmpty()) {
            return RarityParser.parse(null, "unknown", -1, true);
        }

        // Raw lines: rarity glyphs are private-use characters and must survive untouched.
        List<Component> lines = loreComponent.lines();
        List<String> rawLines = new ArrayList<>(lines.size());
        for (Component lineComp : lines) {
            rawLines.add(lineComp.getString());
        }
        return RarityParser.parse(rawLines, "unknown", -1, true);
    }

    public static String getChummerId(ItemStack stack) {
        String rarity = parseRarity(stack);
        return rarity + "_chummer";
    }
}
