package fishontracker.client.scan;

import fishontracker.client.util.TextUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaitParser {

    private static final Pattern USES_FRACTION = Pattern.compile("uses:\\s*(\\d+)/\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMAINING_USES = Pattern.compile("remaining\\s*uses:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STOCK = Pattern.compile("stock:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLOR_PATTERN = Pattern.compile("color:\\s*([a-zA-Z0-9_\\s]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERIC_DIGIT = Pattern.compile("(\\d+)");


    public static boolean isBaitItem(ItemStack stack) {
        List<String> lore = getCleanLore(stack);
        for (String line : lore) {
            String normalized = TextUtil.stripFormatting(line);
            if (normalized.contains("bait item") 
                || normalized.contains("advanced lure item") 
                || normalized.contains("standard lure item")
                || normalized.contains("lure item")) {
                return true;
            }
        }
        return false;
    }

    public static int parseBaitQuantity(ItemStack stack) {
        List<String> lore = getCleanLore(stack);

        for (String line : lore) {
            String norm = TextUtil.stripFormatting(line);

            Matcher mFraction = USES_FRACTION.matcher(norm);
            if (mFraction.find()) {
                return Integer.parseInt(mFraction.group(1));
            }

            Matcher mRemaining = REMAINING_USES.matcher(norm);
            if (mRemaining.find()) {
                return Integer.parseInt(mRemaining.group(1));
            }

            Matcher mStock = STOCK.matcher(norm);
            if (mStock.find()) {
                return Integer.parseInt(mStock.group(1));
            }
        }

        if (lore.size() >= 4) {
            String line4 = TextUtil.stripFormatting(lore.get(3));
            Matcher mAnyDigit = GENERIC_DIGIT.matcher(line4);
            if (mAnyDigit.find()) {
                return Integer.parseInt(mAnyDigit.group(1));
            }
        }

        return stack.getCount();
    }

    public static String parseBaitColor(ItemStack stack) {
        List<String> lore = getCleanLore(stack);
        for (String line : lore) {
            String norm = TextUtil.stripFormatting(line);
            Matcher matcher = COLOR_PATTERN.matcher(norm);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    public static List<String> getCleanLore(ItemStack stack) {
        ItemLore loreComponent = stack.get(DataComponents.LORE);
        if (loreComponent == null) return List.of();

        return loreComponent.lines().stream()
            .map(Component::getString)
            .map(line -> line
                .replaceAll("§[0-9a-fk-or]", "")
                .replaceAll("&[0-9a-fk-or]", "")
                .replaceAll("[\uE000-\uF8FF]", "") 
                .trim()
            )
            .toList();
    }
}