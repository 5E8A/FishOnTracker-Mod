package fishontracker.client.mixin;

import fishontracker.client.config.TrackerConfig;
import fishontracker.client.scan.PetParser;
import fishontracker.client.util.TextUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void fishontracker$shortenTooltip(Item.TooltipContext context,
                                             Player player,
                                             net.minecraft.world.item.TooltipFlag flag,
                                             CallbackInfoReturnable<List<Component>> cir) {
        if (!TrackerConfig.INSTANCE.shorterArmorTooltips
                && !TrackerConfig.INSTANCE.shorterFishTooltips
                && !TrackerConfig.INSTANCE.shorterPetTooltips) return;

        ItemStack stack = (ItemStack) (Object) this;
        boolean isArmor = TrackerConfig.INSTANCE.shorterArmorTooltips && (stack.is(net.minecraft.world.item.Items.LEATHER_CHESTPLATE) || stack.is(net.minecraft.world.item.Items.LEATHER_LEGGINGS) || stack.is(net.minecraft.world.item.Items.LEATHER_BOOTS));
        boolean isFish = TrackerConfig.INSTANCE.shorterFishTooltips && (stack.is(net.minecraft.world.item.Items.COD) || stack.is(net.minecraft.world.item.Items.WHITE_DYE) || stack.is(net.minecraft.world.item.Items.BLACK_DYE) || stack.is(net.minecraft.world.item.Items.GOLD_INGOT) || stack.is(net.minecraft.world.item.Items.DRIED_KELP) || stack.is(net.minecraft.world.item.Items.PRISMARINE_SHARD) || stack.is(net.minecraft.world.item.Items.BONE) || stack.is(net.minecraft.world.item.Items.ROTTEN_FLESH));
        boolean isPet = TrackerConfig.INSTANCE.shorterPetTooltips && stack.is(net.minecraft.world.item.Items.PLAYER_HEAD) && (PetParser.isPet(stack) || isPetByLore(stack));
        if (!isArmor && !isFish && !isPet) return;

        List<Component> lines = cir.getReturnValue();
        if (lines == null || lines.isEmpty()) return;

        List<Component> filtered = new ArrayList<>(lines);

        // --- Armor ---
        if (isArmor) {
            int idx = -1;
            for (int i = 0; i < filtered.size(); i++) {
                String norm = TextUtil.normalizeFancyText(filtered.get(i).getString()).toLowerCase();
                if (norm.contains("right click to roll bonuses")) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                int start = idx;
                if (idx > 0) {
                    String prevStripped = stripPrivate(filtered.get(idx - 1).getString());
                    if (prevStripped.isEmpty()) {
                        start = idx - 1;
                    }
                }
                int end = idx;
                if (idx + 1 < filtered.size()) {
                    end = idx + 1;
                }
                filtered.subList(start, end + 1).clear();
            }
        }

        // --- Fish ---
        if (isFish) {
            filtered.removeIf(line -> {
                String stripped = stripPrivate(line.getString());
                if (stripped.isEmpty()) return false;
                return TextUtil.normalizeFancyText(line.getString()).toLowerCase().contains("ecosystem");
            });
            for (int i = 0; i < filtered.size() - 1; i++) {
                String raw = filtered.get(i).getString();
                String stripped = stripPrivate(raw);
                boolean isRarityStarLine = stripped.length() <= 3
                    && raw.chars().anyMatch(c -> c >= 0xE000 && c <= 0xF8FF);
                if (isRarityStarLine) {
                    String nextStripped = stripPrivate(filtered.get(i + 1).getString());
                    if (!nextStripped.isEmpty()) {
                        filtered.remove(i + 1);
                    }
                    break;
                }
            }
        }

        // --- Pet ---
        if (isPet) {
            for (int i = 0; i < filtered.size() - 1; i++) {
                String raw = filtered.get(i).getString();
                String stripped = stripPrivate(raw);
                boolean isRarityStarLine = stripped.length() <= 3
                    && raw.chars().anyMatch(c -> c >= 0xE000 && c <= 0xF8FF);
                if (isRarityStarLine) {
                    String nextStripped = stripPrivate(filtered.get(i + 1).getString());
                    if (!nextStripped.isEmpty()) {
                        filtered.remove(i + 1);
                    }
                    break;
                }
            }

            java.util.List<Integer> toRemove = new java.util.ArrayList<>();
            for (int i = 0; i < filtered.size(); i++) {
                String stripped = stripPrivate(filtered.get(i).getString());
                if (stripped.isEmpty()) continue;
                String norm = TextUtil.normalizeFancyText(filtered.get(i).getString()).toLowerCase();
                if ((norm.contains("equipped skin:") && norm.contains("default"))
                 || (norm.contains("equipped item:") && norm.contains("empty"))
                 || (norm.contains("trail item:") && norm.contains("empty"))) {
                    toRemove.add(i);
                    if (i > 0 && stripPrivate(filtered.get(i - 1).getString()).isEmpty()) {
                        toRemove.add(i - 1);
                    }
                    if (i + 1 < filtered.size() && stripPrivate(filtered.get(i + 1).getString()).isEmpty()) {
                        toRemove.add(i + 1);
                    }
                }
            }
            java.util.List<Integer> sorted = new java.util.ArrayList<>(toRemove);
            java.util.Collections.sort(sorted, java.util.Collections.reverseOrder());
            for (int i : sorted) {
                filtered.remove(i);
            }

            // Simplify stat lines: (+X/+X) -> (+X) when current == max.
            for (int i = 0; i < filtered.size(); i++) {
                Component simplified = simplifyPetStatLine(filtered.get(i));
                if (simplified != null) {
                    filtered.set(i, simplified);
                }
            }
        }

        cir.setReturnValue(filtered);
    }

    private static boolean isPetByLore(ItemStack stack) {
        String name = stack.getHoverName().getString().toLowerCase();
        if (name.contains("pet") && !name.contains("amulet") && !name.contains("skin") && !name.contains("trail")) return true;
        net.minecraft.world.item.component.ItemLore lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore == null) return false;
        for (net.minecraft.network.chat.Component line : lore.lines()) {
            String norm = TextUtil.normalizeFancyText(line.getString()).toLowerCase();
            if (norm.contains("pet level")) return true;
        }
        return false;
    }

    private static Component simplifyPetStatLine(Component line) {
        List<Component> siblings = line.getSiblings();
        if (siblings.isEmpty()) return null;
        List<Component> result = new ArrayList<>();
        boolean changed = false;
        boolean skipNext = false;
        for (int i = 0; i < siblings.size(); i++) {
            if (skipNext) { skipNext = false; continue; }
            Component s = siblings.get(i);
            String text = s.getString().trim();
            if (text.equals("/") && i > 0 && i + 1 < siblings.size()) {
                String prevText = siblings.get(i - 1).getString().trim();
                String nextText = siblings.get(i + 1).getString().trim();
                if (prevText.equals(nextText) && !prevText.isEmpty()) {
                    // Skip "/" and the duplicate number.
                    skipNext = true;
                    changed = true;
                    continue;
                }
            }
            result.add(s);
        }
        if (!changed) return null;
        net.minecraft.network.chat.MutableComponent out = net.minecraft.network.chat.Component.empty();
        out.setStyle(line.getStyle());
        for (Component s : result) {
            out.append(s);
        }
        return out;
    }

    private static String stripPrivate(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 0xE000 && ch <= 0xF8FF) continue;
            if (ch >= 0xAC00 && ch <= 0xD7AF) continue;
            if (Character.isWhitespace(ch)) continue;
            sb.append(ch);
        }
        return sb.toString();
    }
}
