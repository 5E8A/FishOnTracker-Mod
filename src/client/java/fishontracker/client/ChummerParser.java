package fishontracker.client;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

public class ChummerParser {

    public static boolean isChummerItem(ItemStack stack) {
        if (stack.isEmpty()) return false;

        String name = BaitParser.normalizeFancyText(stack.getHoverName().getString()).toLowerCase();
        return name.contains("chummer");
    }


    public static String parseRarity(ItemStack stack) {
        ItemLore loreComponent = stack.get(DataComponents.LORE);
        if (loreComponent == null || loreComponent.lines().isEmpty()) {
            return "common";
        }

        List<Component> lines = loreComponent.lines();


        for (Component lineComp : lines) {
            String raw = lineComp.getString();
            if (raw.contains("\uf037")) return "mythical";
            if (raw.contains("\uf036")) return "legendary";
            if (raw.contains("\uf035")) return "epic";
            if (raw.contains("\uf034")) return "rare";
            if (raw.contains("\uf033")) return "common";
        }


        for (Component lineComp : lines) {
            String clean = BaitParser.normalizeFancyText(lineComp.getString()).toLowerCase();
            

            if (clean.contains("mythic")) return "mythical";
            if (clean.contains("legendary")) return "legendary";
            if (clean.contains("epic")) return "epic";
            if (clean.contains("rare")) return "rare";
            if (clean.contains("common")) return "common";
        }


        for (Component lineComp : lines) {
            String raw = lineComp.getString();
            if (raw.contains("§d")) return "mythical";
            if (raw.contains("§6")) return "legendary";
            if (raw.contains("§5")) return "epic";
            if (raw.contains("§9") || raw.contains("§b")) return "rare";
            if (raw.contains("§f") || raw.contains("§7") || raw.contains("§a")) return "common";
        }

        return "common";
    }

    public static String getChummerId(ItemStack stack) {
        String rarity = parseRarity(stack);
        return rarity + "_chummer";
    }
}