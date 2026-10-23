package com.curiosity.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.curiosity.client.ArmorHud;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "tick", at = @At("RETURN"))
    public void playBreakSound(CallbackInfo ci) {
        Player player = ArmorHud.getCameraPlayer();
        if (ArmorHud.getManager().getConfig().isPlayBreakSound() && player != null && ArmorHud.shouldPlayBreakSound(player)) {
            player.makeSound(ArmorHud.ARMOR_BREAKING_SOUND);
        }
    }
}