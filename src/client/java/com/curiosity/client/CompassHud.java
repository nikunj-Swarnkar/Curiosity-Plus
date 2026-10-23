package com.curiosity.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.curiosity.client.config.CuriosityHudConfig;
import com.curiosity.client.mixin.BossHealthOverlayAccessor;
import com.curiosity.client.mixin.EquipmentAccessor;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
public class CompassHud implements HudElement {

    private static final String compass_text_simple = "S . . . . . . . SW . . . . . . . W . . . . . . . NW . . . . . . . N . . . . . . . NE . . . . . . . E . . . . . . . SE . . . . . . . ";
    private static final String compass_text_triple = compass_text_simple + compass_text_simple + compass_text_simple;
    private static final int oneSideLength = 25;

    private static final Set<Integer> DEFAULT_COMPASS_ITEMS = Set.of(Item.getId(Items.COMPASS), Item.getId(Items.RECOVERY_COMPASS));

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        CuriosityHudConfig config = ArmorHud.getManager().getConfig();

        Minecraft client = Minecraft.getInstance();
        if (client.gui.hud.isHidden()) {
            return;
        }

        LocalPlayer p = client.player;
        if (p == null) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        Font textRenderer = client.gui.hud.getFont();

        int bossBarCount = ((BossHealthOverlayAccessor) client.gui.hud.getBossOverlay()).getEvents().size();
        int currentTopY = 3 + bossBarCount * 19;

        boolean canShowCompass = config.isCompassEnabled() && checkCompassCondition(p, config);

        if (canShowCompass) {
            float modYaw = (p.yHeadRot % 360.0f + 360.0f) % 360.0f;
            String rawCompassText = displayedText(modYaw);
            int compassWidthInPixels = textRenderer.width(rawCompassText);

            String renderCompassText = config.isRedNorth() ? colorizedNorth(rawCompassText) : rawCompassText;
            int compassPosX = screenWidth / 2 - compassWidthInPixels / 2;

            graphics.text(textRenderer, renderCompassText, compassPosX, currentTopY, 0xFFFFFFFF);
            currentTopY += 11;

            if (config.isShowCoordinates()) {
                BlockPos pos = p.blockPosition();
                String coordsText = String.format("X: %d  Y: %d  Z: %d", pos.getX(), pos.getY(), pos.getZ());
                int coordsWidth = textRenderer.width(coordsText);
                int coordsPosX = screenWidth / 2 - coordsWidth / 2;

                graphics.text(textRenderer, coordsText, coordsPosX, currentTopY, 0xFFFFFFFF);
                currentTopY += 11;
            }
        }

        if (config.isShowDayCounter() && client.level != null) {
            long day = client.level.getOverworldClockTime() / 24000L;
            String dayText = "Day: " + day;
            int dayWidth = textRenderer.width(dayText);
            int dayPosX = screenWidth / 2 - dayWidth / 2;

            graphics.text(textRenderer, dayText, dayPosX, currentTopY, 0xFFFFFFFF);
        }
    }

    private static boolean checkCompassCondition(LocalPlayer p, CuriosityHudConfig config) {
        return switch (config.getHudShowMode()) {
            case ALWAYS -> true;
            case INVENTORY -> {
                Set<Integer> compassIds = new HashSet<>(DEFAULT_COMPASS_ITEMS);
                Set<Integer> inventory = Stream.concat(
                                p.getInventory().getNonEquipmentItems().stream(),
                                Inventory.EQUIPMENT_SLOT_MAPPING.values().stream()
                                        .map(slot -> ((EquipmentAccessor) p.getInventory()).getEquipment().get(slot))
                        )
                        .map(ItemStack::getItem)
                        .map(Item::getId)
                        .collect(Collectors.toSet());

                compassIds.retainAll(inventory);
                yield !compassIds.isEmpty();
            }
            case HAND -> DEFAULT_COMPASS_ITEMS.contains(Item.getId(p.getOffhandItem().getItem()))
                    || DEFAULT_COMPASS_ITEMS.contains(Item.getId(p.getMainHandItem().getItem()));
        };
    }

    private static String colorizedNorth(String text) {
        String result = text.replace(" N ", " §cN§r ");
        if (result.startsWith("N ")) {
            result = "§cN§r " + result.substring(2);
        }
        if (result.endsWith(" N")) {
            result = result.substring(0, result.length() - 2) + " §cN§r";
        }
        return result;
    }

    private static String displayedText(float yaw) {
        int textAnchor = Math.round((yaw / 360.0f) * compass_text_simple.length()) + compass_text_simple.length();
        return compass_text_triple.substring(textAnchor - oneSideLength, textAnchor + oneSideLength);
    }
}