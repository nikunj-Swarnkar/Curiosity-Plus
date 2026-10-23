package com.curiosity.client.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.curiosity.client.ArmorHud;
import com.curiosity.client.config.CuriosityHudConfig;

import java.util.Optional;

@Mixin(BossHealthOverlay.class)
public class MixinBossHealthOverlay {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void calculateOffset(GuiGraphicsExtractor graphics, CallbackInfo ci, @Share("offset") LocalIntRef offsetRef) {
        CuriosityHudConfig config = ArmorHud.getManager().getConfig();
        if (!config.isEnabled() || !config.isPushBossbars() || config.getAnchor() != CuriosityHudConfig.Anchor.TOP_CENTER)
            return;

        Player player = ArmorHud.getCameraPlayer();
        if (player == null) return;
        Optional<Rect2i> rect = ArmorHud.getEffectiveWidgetRect(graphics, player);
        if (rect.isEmpty()) return;

        offsetRef.set(rect.get().getY() + rect.get().getHeight());
    }

    @ModifyVariable(method = "extractRenderState", at = @At("STORE"), name = "yOffset")
    public int pushBossBars(int yOffset, @Share("offset") LocalIntRef offsetRef) {
        return yOffset + offsetRef.get();
    }
}
