package com.curiosity.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import com.curiosity.client.config.CuriosityHudConfig;
import com.curiosity.client.mixin.HudAccessor;
import com.curiosity.client.mixin.InventoryMenuAccessor;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import static com.curiosity.client.ArmorHud.*;

public class ArmorHudRenderer {
    private static final Identifier HOTBAR_SPRITE = HudAccessor.getHOTBAR_SPRITE();
    private static final Identifier HOTBAR_OFFHAND_LEFT_SPRITE = HudAccessor.getHOTBAR_OFFHAND_LEFT_SPRITE();

    public static final Identifier WARNING_TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "warn.png");

    private static int getPulsingRedColor() {
        float pulse = (float) (Math.sin(Util.getMillis() / 150.0) * 0.5 + 0.5);
        int alpha = (int) (100 + (155 * pulse)) << 24;
        return alpha | 0xFF3333;
    }

    private static void drawClampedText(GuiGraphicsExtractor graphics, Font font, String text, int targetCenterX, int y, int color) {
        int textWidth = font.width(text);
        int drawX = targetCenterX - (textWidth / 2);
        int minX = 2;
        int maxX = Math.max(minX, graphics.guiWidth() - textWidth - 2);
        drawX = Mth.clamp(drawX, minX, maxX);
        graphics.text(font, text, drawX, y, color);
    }

    public static void drawArmorHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        CuriosityHudConfig config = ArmorHud.getManager().getConfig();
        if (!config.isEnabled()) return;

        Player player = ArmorHud.getCameraPlayer();
        if (player == null) return;

        final Optional<Rect2i> rect = ArmorHud.getWidgetRect(graphics, player);
        if (rect.isEmpty()) return;

        List<ItemStack> armorItems = ArmorHud.getArmorItems(player);
        if (config.isReversed()) {
            armorItems = armorItems.reversed();
        }

        final int textureWidth = SIZE + ((armorItems.size() - 1) * STEP);

        graphics.pose().pushMatrix();
        graphics.pose().translate(rect.get().getX(), rect.get().getY());

        if (config.getOrientation() == CuriosityHudConfig.Orientation.VERTICAL) {
            graphics.pose().rotate(Mth.HALF_PI).translate(0, -SIZE);
        }

        int color = ARGB.white(ArmorHud.getModCompat().hudOpacity());
        HudAccessor hud = (HudAccessor) Minecraft.getInstance().gui.hud;

        switch (config.getStyle()) {
            case HOTBAR -> {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 0, 0, 0, textureWidth - 3, SIZE, color);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 182 - 3, 0, textureWidth - 3, 0, 3, SIZE, color);
            }
            case ROUNDED_CORNERS -> {
                if (armorItems.size() > 1) {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, 3, SIZE, color);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 3, 0, 3, 0, textureWidth - 6, SIZE, color);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, SIZE - 3, 1, textureWidth - 3, 0, 3, SIZE, color);
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE, SIZE, color);
                }
            }
            case ROUNDED -> {
                if (armorItems.size() > 1) {
                    int borderWidth = (SIZE - STEP) / 2;
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE - borderWidth, SIZE, color);
                    for (int i = 1; i < armorItems.size() - 1; i++) {
                        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, borderWidth, 1, borderWidth + i * STEP, 0, STEP, SIZE, color);
                    }
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 1, 1, textureWidth - STEP - borderWidth, 0, SIZE - borderWidth, SIZE, color);
                } else {
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, 29, 24, 0, 1, 0, 0, SIZE, SIZE, color);
                }
            }
            case NONE -> {
            }
        }
        graphics.pose().popMatrix();

        Font font = Minecraft.getInstance().font;

        int boxX = rect.get().getX();
        int boxY = rect.get().getY();
        int boxW = rect.get().getWidth();
        int boxH = rect.get().getHeight();
        int centerX = boxX + (boxW / 2);

        int baseOffset = 0;
        if (config.getOrientation() == CuriosityHudConfig.Orientation.HORIZONTAL) {
            if (config.getDurabilityDisplay() != CuriosityHudConfig.DurabilityDisplay.BAR) {
                baseOffset += font.lineHeight + 1;
            }
            if (config.isWarningShown()) {
                baseOffset += WARNING_SIZE + 2;
            }
        }

        int currentLineOffset = baseOffset;

        if (config.isShowAverageDurability()) {
            OptionalDouble avgOpt = ArmorHud.getAverageArmorDurability(player);
            if (avgOpt.isPresent()) {
                float ratio = (float) Mth.clamp(avgOpt.getAsDouble(), 0.0, 1.0);
                int avgColor = ARGB.opaque(Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F));
                String avgText = "Avg: " + (int) Math.round(ratio * 100) + "%";

                int textY = config.getAnchor().isTop()
                        ? (boxY + boxH + currentLineOffset + 2)
                        : (boxY - currentLineOffset - font.lineHeight - 2);

                drawClampedText(graphics, font, avgText, centerX, textY, avgColor);
                currentLineOffset += font.lineHeight + 2;
            }
        }

        Optional<String> elytraTimeOpt = ArmorHud.getElytraFlightTimeText(player);
        if (elytraTimeOpt.isPresent()) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            int flyColor = (config.isPulseLowDurability() && ArmorHud.shouldShowWarning(chest))
                    ? getPulsingRedColor()
                    : ARGB.opaque(chest.getBarColor());

            int textY = config.getAnchor().isTop()
                    ? (boxY + boxH + currentLineOffset + 2)
                    : (boxY - currentLineOffset - font.lineHeight - 2);

            drawClampedText(graphics, font, elytraTimeOpt.get(), centerX, textY, flyColor);
        }

        for (int i = 0; i < armorItems.size(); i++) {
            ItemStack stack = armorItems.get(i);
            int x = rect.get().getX();
            int y = rect.get().getY();

            switch (config.getOrientation()) {
                case HORIZONTAL -> x += (STEP * i);
                case VERTICAL -> y += (STEP * i);
            }

            if (config.isIconsShown() && config.getWidgetShown().shouldDrawEmptySlots() && stack.isEmpty()) {
                int slotIndex = config.isReversed() ? (armorItems.size() - 1 - i) : i;
                if (slotIndex >= 0 && slotIndex < SLOT_IDS.length) {
                    Identifier identifier = InventoryMenuAccessor.getTEXTURE_EMPTY_SLOTS().get(SLOT_IDS[slotIndex]);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, x + 3, y + 3, 16, 16);
                }
            }

            hud.armorhud$extractSlot(graphics, x + 3, y + 3, tickCounter, player, stack, i + 1);

            if (config.isShowMendingDot() && !stack.isEmpty() && stack.isEnchanted()
                    && stack.getEnchantments().keySet().stream().anyMatch(e -> e.is(Enchantments.MENDING))) {
                graphics.fill(x + 1, y + 1, x + 3, y + 3, 0xFF00FF66);
            }

            CuriosityHudConfig.Side extrasSide = config.getAnchor() == CuriosityHudConfig.Anchor.HOTBAR ? config.getSide() : config.getSide().getOpposite();

            if (config.getAnchor().isTop() && config.getOrientation() == CuriosityHudConfig.Orientation.HORIZONTAL) {
                y += SIZE;
            } else if (extrasSide == CuriosityHudConfig.Side.RIGHT && config.getOrientation() == CuriosityHudConfig.Orientation.VERTICAL) {
                x += SIZE;
            }

            if (config.getDurabilityDisplay() != CuriosityHudConfig.DurabilityDisplay.BAR && !stack.isEmpty() && stack.isDamageableItem()) {
                int textColor;
                if (config.isPulseLowDurability() && ArmorHud.shouldShowWarning(stack)) {
                    textColor = getPulsingRedColor();
                } else {
                    textColor = ARGB.opaque(stack.getBarColor());
                }

                String dura = ArmorHud.getDurabilityText(stack);
                int textHeight = font.lineHeight;

                if (!dura.isEmpty()) {
                    if (config.getOrientation() == CuriosityHudConfig.Orientation.HORIZONTAL) {
                        if (!config.getAnchor().isTop()) y -= textHeight;
                        graphics.centeredText(font, dura, x + (SIZE / 2), y, textColor);
                        if (config.getAnchor().isTop()) y += textHeight;
                    } else {
                        int textWidth = font.width(dura) + 2;
                        int textY = (SIZE - textHeight) / 2;

                        if (extrasSide == CuriosityHudConfig.Side.LEFT) x -= textWidth;
                        graphics.text(font, dura, x + 1, y + textY, textColor);
                        if (extrasSide == CuriosityHudConfig.Side.RIGHT) x += textWidth;
                    }
                }
            }

            if (config.isWarningShown() && ArmorHud.shouldShowWarning(stack)) {
                if (config.getWarningBobIntensity() != 0) {
                    int intensity = config.getWarningBobIntensity();
                    y += (int) (hud.getRandom().nextInt(intensity) - Math.ceil(intensity / 2F));
                }

                if (config.getOrientation() == CuriosityHudConfig.Orientation.HORIZONTAL) {
                    if (!config.getAnchor().isTop()) y -= WARNING_SIZE + 2;

                    int warnX = (SIZE - WARNING_SIZE) / 2;
                    graphics.blit(RenderPipelines.GUI_TEXTURED, WARNING_TEXTURE, x + warnX, y + 1, 0, 0, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE);
                } else {
                    if (extrasSide == CuriosityHudConfig.Side.LEFT) x -= WARNING_SIZE + 2;

                    int warnY = (SIZE - WARNING_SIZE) / 2;
                    graphics.blit(RenderPipelines.GUI_TEXTURED, WARNING_TEXTURE, x + 1, y + warnY, 0, 0, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE);
                }
            }
        }
    }

    public static void renderMainAndOffhand(GuiGraphicsExtractor graphics) {
        CuriosityHudConfig config = getManager().getConfig();
        if (!config.isEnabled()) return;

        Player player = getCameraPlayer();
        if (player == null) return;

        Font font = Minecraft.getInstance().font;
        HudAccessor hud = (HudAccessor) Minecraft.getInstance().gui.hud;

        ItemStack offhand = player.getOffhandItem();
        if (offhand.isDamageableItem() && config.isOffHandDurability()) {
            int x = player.getMainArm() == HumanoidArm.RIGHT
                    ? (graphics.guiWidth() / 2) - 91 - 29
                    : (graphics.guiWidth() / 2) + 91 + 7;
            int y = graphics.guiHeight() - SIZE;

            if (config.getDurabilityDisplay() != CuriosityHudConfig.DurabilityDisplay.BAR) {
                String dura = ArmorHud.getDurabilityText(offhand);
                y -= font.lineHeight;
                int textColor = (config.isPulseLowDurability() && shouldShowWarning(offhand))
                        ? getPulsingRedColor()
                        : ARGB.opaque(offhand.getBarColor());
                graphics.centeredText(font, dura, x + (SIZE / 2), y, textColor);
            }

            if (config.isWarningShown() && shouldShowWarning(offhand)) {
                if (config.getWarningBobIntensity() != 0) {
                    int intensity = config.getWarningBobIntensity();
                    y += (int) (hud.getRandom().nextInt(intensity) - Math.ceil(intensity / 2F));
                }

                y -= WARNING_SIZE + 2;
                int warnX = (SIZE - WARNING_SIZE) / 2;
                graphics.blit(RenderPipelines.GUI_TEXTURED, WARNING_TEXTURE, x + warnX, y + 1, 0, 0, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE, WARNING_SIZE);
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!config.isShowHandDurability() && mainHand.isDamageableItem() && config.isMainHandDurability() && config.getDurabilityDisplay() != CuriosityHudConfig.DurabilityDisplay.BAR) {
            int x = (graphics.guiWidth() / 2) - 91 + player.getInventory().getSelectedSlot() * 20 + (SIZE / 2);
            int y = graphics.guiHeight() - SIZE - 1;

            String dura = ArmorHud.getDurabilityText(mainHand);
            int textX = x - (font.width(dura) / 2);
            int textColor = (config.isPulseLowDurability() && shouldShowWarning(mainHand))
                    ? getPulsingRedColor()
                    : ARGB.opaque(mainHand.getBarColor());

            graphics.text(font, dura, textX + 1, y, ARGB.black(255), false);
            graphics.text(font, dura, textX - 1, y, ARGB.black(255), false);
            graphics.text(font, dura, textX, y + 1, ARGB.black(255), false);
            graphics.text(font, dura, textX, y - 1, ARGB.black(255), false);
            graphics.text(font, dura, textX, y, textColor, false);
        }
    }
}