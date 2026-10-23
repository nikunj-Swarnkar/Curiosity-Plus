package com.curiosity.client.mixin;

import com.curiosity.client.ArmorHudRenderer;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.curiosity.client.config.CuriosityHudConfig;

import java.util.Optional;

import static com.curiosity.client.ArmorHud.*;

@Mixin(Hud.class)
public class MixinHud {
    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    public void renderArmorHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Profiler.get().push(MOD_ID);
        ArmorHudRenderer.drawArmorHud(graphics, deltaTracker);
        Profiler.get().pop();
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At("TAIL"))
    public void renderHands(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ArmorHudRenderer.renderMainAndOffhand(graphics);
    }

    @Inject(method = "extractEffects", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    public void calculateStatusEffectIconsOffset(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci, @Share("shift") LocalIntRef shiftRef) {
        CuriosityHudConfig config = getManager().getConfig();
        if (!config.isEnabled() || !config.isPushStatusEffectIcons() || config.getAnchor() != CuriosityHudConfig.Anchor.TOP
                || config.getSide() != CuriosityHudConfig.Side.RIGHT) return;

        Player player = getCameraPlayer();
        if (player == null) return;

        Optional<Rect2i> rect = getEffectiveWidgetRect(graphics, player);
        if (rect.isEmpty()) return;

        shiftRef.set(rect.get().getY() + rect.get().getHeight());
    }

    @ModifyVariable(method = "extractEffects", at = @At(value = "STORE"), name = "y")
    public int statusEffectIconsOffset(int y, @Share("shift") LocalIntRef shiftRef) {
        return y + shiftRef.get();
    }
}