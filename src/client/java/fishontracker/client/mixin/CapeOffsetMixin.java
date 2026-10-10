package fishontracker.client.mixin;

import fishontracker.client.config.TrackerConfig;
import fishontracker.client.util.FoeCapeFix;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapeLayer.class)
public class CapeOffsetMixin {

    @Redirect(
        method = "submit",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;chestEquipment:Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack fishontracker$fixCapeOffset(AvatarRenderState state) {
        if (TrackerConfig.INSTANCE.forceCapeFix && FoeCapeFix.isArmorHidden()) {
            return ItemStack.EMPTY;
        }
        return state.chestEquipment;
    }
}