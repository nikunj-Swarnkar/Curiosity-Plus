package com.curiosity.client;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.uku3lig.ukulib.config.ConfigManager;
import net.uku3lig.ukulib.utils.Ukutils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import com.curiosity.client.compat.ModCompat;
import com.curiosity.client.config.CuriosityHudConfig;
import com.curiosity.client.mixin.InventoryMenuAccessor;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
public final class ArmorHud {
    public static final String MOD_ID = CuriosityPlusClient.MOD_ID;

    @Getter
    private static final ConfigManager<CuriosityHudConfig> manager = ConfigManager.createDefault(CuriosityHudConfig.class, MOD_ID);

    public static final int STEP = 20;
    public static final int SIZE = 22;
    public static final int HOTBAR_OFFSET = 98;
    public static final int OFFHAND_OFFSET = 29;
    public static final int ATTACK_INDICATOR_OFFSET = 23;
    public static final int WARNING_SIZE = 8;

    public static final SoundEvent ARMOR_BREAKING_SOUND = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MOD_ID, "armor_breaking"));

    public static final EquipmentSlot[] SLOT_IDS = InventoryMenuAccessor.getSLOT_IDS();
    public static final EquipmentSlot[] ALL_SLOTS = ArrayUtils.addAll(SLOT_IDS, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);

    private static final List<ItemStack> lastStacks = new ArrayList<>(Collections.nCopies(ALL_SLOTS.length, ItemStack.EMPTY));

    @Getter
    @Setter
    private static ModCompat modCompat = new ModCompat.NoOpModCompat();

    @Nullable
    public static Player getCameraPlayer() {
        return Minecraft.getInstance().getCameraEntity() instanceof Player player ? player : null;
    }

    public static Optional<Rect2i> getWidgetRect(GuiGraphicsExtractor graphics, Player player) {
        CuriosityHudConfig config = manager.getConfig();
        List<ItemStack> armorItems = getArmorItems(player);

        if (armorItems.isEmpty()) {
            return Optional.empty();
        }

        final int sideMultiplier, sideOffsetMultiplier;
        if ((config.getAnchor() == CuriosityHudConfig.Anchor.HOTBAR && config.getSide() == CuriosityHudConfig.Side.LEFT)
                || (config.getAnchor() != CuriosityHudConfig.Anchor.HOTBAR && config.getSide() == CuriosityHudConfig.Side.RIGHT)) {
            sideMultiplier = -1;
            sideOffsetMultiplier = -1;
        } else {
            sideMultiplier = 1;
            sideOffsetMultiplier = 0;
        }

        final int addedHotbarOffset = switch (config.getOffhandSlotBehavior()) {
            case ALWAYS_IGNORE -> 0;
            case ALWAYS_LEAVE_SPACE ->
                    player.getMainArm() == config.getSide().asArm() ? ATTACK_INDICATOR_OFFSET : OFFHAND_OFFSET;
            case ADHERE -> {
                if (player.getMainArm() == config.getSide().asArm()) {
                    if (Minecraft.getInstance().options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR &&
                            player.getAttackStrengthScale(0) < 1) {
                        yield ATTACK_INDICATOR_OFFSET;
                    }
                } else if (!player.getOffhandItem().isEmpty()) {
                    yield OFFHAND_OFFSET;
                }

                yield 0;
            }
        };

        final int textureWidth = SIZE + ((armorItems.size() - 1) * STEP);
        final int widgetWidth = config.getOrientation() == CuriosityHudConfig.Orientation.VERTICAL ? SIZE : textureWidth;
        final int widgetHeight = config.getOrientation() == CuriosityHudConfig.Orientation.VERTICAL ? textureWidth : SIZE;

        final int armorWidgetX = config.getOffsetX() * sideMultiplier + switch (config.getAnchor()) {
            case TOP_CENTER -> (graphics.guiWidth() - widgetWidth) / 2;
            case TOP, VERT_CENTER, BOTTOM -> (widgetWidth - graphics.guiWidth()) * sideOffsetMultiplier;
            case HOTBAR ->
                    graphics.guiWidth() / 2 + ((HOTBAR_OFFSET + addedHotbarOffset) * sideMultiplier) + (widgetWidth * sideOffsetMultiplier);
        };

        final int armorWidgetY = switch (config.getAnchor()) {
            case BOTTOM, HOTBAR ->
                    graphics.guiHeight() - widgetHeight - config.getOffsetY() - modCompat.screenSafeArea();
            case TOP, TOP_CENTER -> config.getOffsetY();
            case VERT_CENTER -> (graphics.guiHeight() - widgetHeight) / 2 + config.getOffsetY();
        };

        return Optional.of(new Rect2i(armorWidgetX, armorWidgetY, widgetWidth, widgetHeight));
    }

    public static Optional<Rect2i> getEffectiveWidgetRect(GuiGraphicsExtractor graphics, Player player) {
        CuriosityHudConfig config = manager.getConfig();

        return getWidgetRect(graphics, player).map(rect -> {
            if (config.getOrientation() == CuriosityHudConfig.Orientation.HORIZONTAL) {
                int additionalHeight = 0;
                int lineHeight = Minecraft.getInstance().font.lineHeight;

                if (config.isWarningShown()) {
                    additionalHeight += WARNING_SIZE + 2 + (config.getWarningBobIntensity() / 2);
                }

                if (config.getDurabilityDisplay() != CuriosityHudConfig.DurabilityDisplay.BAR) {
                    additionalHeight += lineHeight;
                }

                if (config.isShowAverageDurability()) {
                    additionalHeight += lineHeight + 2;
                }

                if (getElytraFlightTimeText(player).isPresent()) {
                    additionalHeight += lineHeight + 2;
                }

                rect.setHeight(rect.getHeight() + additionalHeight);
                if (!config.getAnchor().isTop()) {
                    rect.setY(rect.getY() - additionalHeight);
                }
            }

            return rect;
        });
    }


    public static List<ItemStack> getArmorItems(Player player) {
        CuriosityHudConfig config = manager.getConfig();
        Stream<ItemStack> items = Arrays.stream(SLOT_IDS).map(player::getItemBySlot);
        items = switch (config.getWidgetShown()) {
            case ALWAYS -> items;
            case IF_ANY_PRESENT -> {
                List<ItemStack> itemList = items.toList();
                yield itemList.stream().allMatch(ItemStack::isEmpty) ? Stream.of() : itemList.stream();
            }
            case NOT_EMPTY -> items.filter(s -> !s.isEmpty());
            case DAMAGED_PIECES -> items.filter(ArmorHud::shouldShowWarning);
        };

        List<ItemStack> result = new ArrayList<>(items.toList());

        if (config.isShowHandDurability()) {
            ItemStack mainHand = player.getMainHandItem();
            if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
                if (config.getWidgetShown() != CuriosityHudConfig.WidgetShown.DAMAGED_PIECES || shouldShowWarning(mainHand)) {
                    result.add(mainHand);
                }
            }

            boolean vanillaOffhandAdjacent = config.getAnchor() == CuriosityHudConfig.Anchor.HOTBAR;
            ItemStack offHand = player.getOffhandItem();
            if (!vanillaOffhandAdjacent && !offHand.isEmpty() && offHand.isDamageableItem()) {
                if (config.getWidgetShown() != CuriosityHudConfig.WidgetShown.DAMAGED_PIECES || shouldShowWarning(offHand)) {
                    result.add(offHand);
                }
            }
        }

        return result;
    }

    public static OptionalDouble getAverageArmorDurability(Player player) {
        return Arrays.stream(SLOT_IDS)
                .map(player::getItemBySlot)
                .filter(s -> !s.isEmpty() && s.isDamageableItem() && s.getMaxDamage() > 0)
                .mapToDouble(s -> 1.0 - ((double) s.getDamageValue() / s.getMaxDamage()))
                .average();
    }

    public static Optional<String> getElytraFlightTimeText(Player player) {
        CuriosityHudConfig config = manager.getConfig();
        if (!config.isShowElytraFlightTime()) {
            return Optional.empty();
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA) || !player.isFallFlying()) {
            return Optional.empty();
        }

        int unbreakingLevel = chest.getEnchantments().entrySet().stream()
                .filter(entry -> entry.getKey().is(Enchantments.UNBREAKING))
                .mapToInt(Object2IntMap.Entry::getIntValue)
                .findFirst()
                .orElse(0);

        int remainingDurability = Math.max(0, chest.getMaxDamage() - chest.getDamageValue());
        int totalSeconds = remainingDurability * (1 + unbreakingLevel);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return Optional.of(String.format("Fly: %dm %02ds", minutes, seconds));
    }

    public static boolean shouldPlayBreakSound(Player player) {
        for (int i = 0; i < ALL_SLOTS.length; i++) {
            EquipmentSlot slot = ALL_SLOTS[i];
            ItemStack current = player.getItemBySlot(slot);
            ItemStack last = lastStacks.set(i, current);

            boolean shouldPlaySound = last.getDamageValue() != current.getDamageValue() && shouldShowWarning(current);
            boolean isMainHandAndDifferentThanLast = slot == EquipmentSlot.MAINHAND && !last.is(current.getItem());
            boolean isArmor = slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND;
            boolean shouldPlayMainHand = slot == EquipmentSlot.MAINHAND && manager.getConfig().isMainHandDurability();
            boolean shouldPlayOffHand = slot == EquipmentSlot.OFFHAND && manager.getConfig().isOffHandDurability();
            if (shouldPlaySound && !isMainHandAndDifferentThanLast && (isArmor || shouldPlayMainHand || shouldPlayOffHand)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Always returns compact numeric or percentage durability for individual slots so they never overlap.
     */
    public static String getDurabilityText(ItemStack stack) {
        return switch (manager.getConfig().getDurabilityDisplay()) {
            case NUMERIC -> String.valueOf(stack.getMaxDamage() - stack.getDamageValue());
            case PERCENTAGE -> {
                if (stack.getDamageValue() == 0) yield "";
                double percentage = 1.0 - ((double) stack.getDamageValue() / stack.getMaxDamage());
                yield (int) Math.floor(percentage * 100) + "%";
            }
            case BAR -> throw new IllegalStateException("cannot display durability text when display is set to BAR");
        };
    }

    public static boolean shouldShowWarning(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;

        final int damage = stack.getDamageValue();
        final int maxDamage = stack.getMaxDamage();
        double percentage = 1.0 - ((double) damage / maxDamage);

        return percentage <= manager.getConfig().getMinDurabilityPercentage()
                || maxDamage - damage <= manager.getConfig().getMinDurabilityValue();
    }

    public static void onInitialize() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "key"));

        Ukutils.registerToggleBind(
                new KeyMapping("armorhud.keybind.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_F6, category),
                () -> manager.getConfig().isEnabled(),
                b -> manager.getConfig().setEnabled(b),
                Component.translatable("armorhud.keybind.toggle.msg")
        );

        Ukutils.registerToggleBind(
                new KeyMapping("key.curiosity.toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_F7, category),
                () -> manager.getConfig().isCompassEnabled(),
                b -> manager.getConfig().setCompassEnabled(b),
                Component.translatable("key.curiosity.toggle.msg")
        );
    }
}