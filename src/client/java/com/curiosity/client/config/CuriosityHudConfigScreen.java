package com.curiosity.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.uku3lig.ukulib.config.ConfigManager;
import net.uku3lig.ukulib.config.option.CyclingOption;
import net.uku3lig.ukulib.config.option.SliderOption;
import net.uku3lig.ukulib.config.option.TypedInputOption;
import net.uku3lig.ukulib.config.option.WidgetCreator;
import net.uku3lig.ukulib.config.screen.TabbedConfigScreen;
import org.jspecify.annotations.NonNull;
import com.curiosity.client.ArmorHud;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CuriosityHudConfigScreen extends TabbedConfigScreen<CuriosityHudConfig> {
    public CuriosityHudConfigScreen(Screen parent) {
        super("armorhud.config", parent, ArmorHud.getManager());
    }

    @Override
    protected Tab[] getTabs(CuriosityHudConfig config) {
        return new Tab[] {new StyleTab(), new PositionTab(), new WarningTab(), new CompassTab()};
    }

    private static Optional<Integer> getInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public abstract static class ScrollableTab<T extends Serializable> extends GridLayoutTab {
        private final ConfigManager<T> manager;
        private OptionScrollList list;

        protected ScrollableTab(String translationKey, ConfigManager<T> manager) {
            super(Component.translatable(translationKey));
            this.manager = manager;
        }

        protected abstract WidgetCreator[] getWidgets(T config);

        @Override
        public void visitChildren(@NonNull Consumer<AbstractWidget> consumer) {
            if (this.list == null) {
                Minecraft mc = Minecraft.getInstance();
                this.list = new OptionScrollList(mc, 0, 0, 0, 24);
                for (WidgetCreator creator : getWidgets(manager.getConfig())) {
                    this.list.addOption(creator.createWidget(210, 20));
                }
            }
            consumer.accept(this.list);
        }

        @Override
        public void doLayout(@NonNull ScreenRectangle tabArea) {
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int top = Math.max(tabArea.top(), 24);
            int bottom = Math.min(tabArea.bottom(), screenHeight - 36);
            int height = Math.max(32, bottom - top);

            if (this.list != null) {
                this.list.updateSizeAndPosition(tabArea.width(), height, top);
                this.list.setX(tabArea.left());
            }
        }

        private static class OptionScrollList extends ContainerObjectSelectionList<OptionScrollList.WidgetEntry> {
            public OptionScrollList(Minecraft mc, int width, int height, int y, int itemHeight) {
                super(mc, width, height, y, itemHeight);
                this.centerListVertically = false;
            }

            public void addOption(AbstractWidget widget) {
                this.addEntry(new WidgetEntry(widget));
            }

            @Override
            public int getRowWidth() {
                return 220;
            }

            private static class WidgetEntry extends ContainerObjectSelectionList.Entry<WidgetEntry> {
                private final AbstractWidget widget;

                public WidgetEntry(AbstractWidget widget) {
                    this.widget = widget;
                }

                @Override
                public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
                    this.widget.setX(this.getRectangle().left() + (this.getRectangle().width() - this.widget.getWidth()) / 2);
                    this.widget.setY(this.getRectangle().top() + (this.getRectangle().height() - this.widget.getHeight()) / 2);
                    this.widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
                }

                @Override
                public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
                    return false;
                }

                @Override
                public @NonNull List<? extends GuiEventListener> children() {
                    return List.of(this.widget);
                }

                @Override
                public @NonNull List<? extends NarratableEntry> narratables() {
                    return List.of(this.widget);
                }
            }
        }
    }

    public static class CompassTab extends ScrollableTab<CuriosityHudConfig> {
        protected CompassTab() {
            super("armorhud.config.compass", ArmorHud.getManager());
        }

        @Override
        protected WidgetCreator[] getWidgets(CuriosityHudConfig config) {
            return new WidgetCreator[] {
                    CyclingOption.ofBoolean("curiosity.config.compass.enabled", config.isCompassEnabled(), config::setCompassEnabled),
                    CyclingOption.ofTranslatableEnum("curiosity.config.compass.showMode", HudShowMode.class, config.getHudShowMode(), config::setHudShowMode),
                    CyclingOption.ofBoolean("curiosity.config.compass.redNorth", config.isRedNorth(), config::setRedNorth),
                    CyclingOption.ofBoolean("curiosity.config.compass.coords", config.isShowCoordinates(), config::setShowCoordinates),
                    CyclingOption.ofBoolean("curiosity.config.compass.dayCounter", config.isShowDayCounter(), config::setShowDayCounter)
            };
        }
    }

    public static class StyleTab extends ScrollableTab<CuriosityHudConfig> {
        protected StyleTab() {
            super("armorhud.config.style", ArmorHud.getManager());
        }

        @Override
        protected WidgetCreator[] getWidgets(CuriosityHudConfig config) {
            return new WidgetCreator[] {
                    CyclingOption.ofBoolean("armorhud.option.enabled", config.isEnabled(), config::setEnabled),
                    CyclingOption.ofTranslatableEnum("armorhud.option.style", CuriosityHudConfig.Style.class, config.getStyle(), config::setStyle),
                    CyclingOption.ofTranslatableEnum("armorhud.option.widgetShown", CuriosityHudConfig.WidgetShown.class, config.getWidgetShown(), config::setWidgetShown),
                    CyclingOption.ofTranslatableEnum("armorhud.option.offhandSlotBehavior", CuriosityHudConfig.OffhandSlotBehavior.class, config.getOffhandSlotBehavior(), config::setOffhandSlotBehavior),
                    CyclingOption.ofTranslatableEnum("armorhud.option.durabilityDisplay", CuriosityHudConfig.DurabilityDisplay.class, config.getDurabilityDisplay(), config::setDurabilityDisplay),
                    CyclingOption.ofBoolean("armorhud.option.reversed", config.isReversed(), config::setReversed),
                    CyclingOption.ofBoolean("armorhud.option.showIcons", config.isIconsShown(), config::setIconsShown),
                    CyclingOption.ofBoolean("armorhud.option.offhandDura", config.isOffHandDurability(), config::setOffHandDurability),
                    CyclingOption.ofBoolean("armorhud.option.mainHandDura", config.isMainHandDurability(), config::setMainHandDurability),
                    CyclingOption.ofBoolean("curiosity.config.armor.showHands", config.isShowHandDurability(), config::setShowHandDurability),
                    CyclingOption.ofBoolean("curiosity.config.armor.mendingDot", config.isShowMendingDot(), config::setShowMendingDot),
                    CyclingOption.ofBoolean("curiosity.config.armor.avgDurability", config.isShowAverageDurability(), config::setShowAverageDurability),
                    CyclingOption.ofBoolean("curiosity.config.armor.elytraTimer", config.isShowElytraFlightTime(), config::setShowElytraFlightTime)
            };
        }
    }

    public static class PositionTab extends ScrollableTab<CuriosityHudConfig> {
        protected PositionTab() {
            super("armorhud.config.position", ArmorHud.getManager());
        }

        @Override
        protected WidgetCreator[] getWidgets(CuriosityHudConfig config) {
            return new WidgetCreator[] {
                    CyclingOption.ofTranslatableEnum("armorhud.option.anchor", CuriosityHudConfig.Anchor.class, config.getAnchor(), config::setAnchor),
                    CyclingOption.ofTranslatableEnum("armorhud.option.side", CuriosityHudConfig.Side.class, config.getSide(), config::setSide),
                    CyclingOption.ofTranslatableEnum("armorhud.option.orientation", CuriosityHudConfig.Orientation.class, config.getOrientation(), config::setOrientation),
                    CyclingOption.ofBoolean("armorhud.option.pushBossbars", config.isPushBossbars(), config::setPushBossbars),
                    CyclingOption.ofBoolean("armorhud.option.pushIcons", config.isPushStatusEffectIcons(), config::setPushStatusEffectIcons),
                    CyclingOption.ofBoolean("armorhud.option.pushSubtitles", config.isPushSubtitles(), config::setPushSubtitles),
                    new TypedInputOption<>("armorhud.option.offsetX", String.valueOf(config.getOffsetX()), config::setOffsetX, CuriosityHudConfigScreen::getInt),
                    new TypedInputOption<>("armorhud.option.offsetY", String.valueOf(config.getOffsetY()), config::setOffsetY, CuriosityHudConfigScreen::getInt),
            };
        }
    }

    public static class WarningTab extends ScrollableTab<CuriosityHudConfig> {
        protected WarningTab() {
            super("armorhud.config.warning", ArmorHud.getManager());
        }

        @Override
        protected WidgetCreator[] getWidgets(CuriosityHudConfig config) {
            return new WidgetCreator[] {
                    CyclingOption.ofBoolean("armorhud.option.playBreakSound", config.isPlayBreakSound(), config::setPlayBreakSound),
                    CyclingOption.ofBoolean("armorhud.option.showWarning", config.isWarningShown(), config::setWarningShown),
                    CyclingOption.ofBoolean("curiosity.config.armor.pulseLowDura", config.isPulseLowDurability(), config::setPulseLowDurability),
                    new TypedInputOption<>("armorhud.option.iconBobIntensity", String.valueOf(config.getWarningBobIntensity()), config::setWarningBobIntensity, CuriosityHudConfigScreen::getInt),
                    new TypedInputOption<>("armorhud.option.minDuraValue", String.valueOf(config.getMinDurabilityValue()), config::setMinDurabilityValue, CuriosityHudConfigScreen::getInt),
                    new SliderOption("armorhud.option.minDuraPercent", config.getMinDurabilityPercentage(), config::setMinDurabilityPercentage, SliderOption.PERCENT_VALUE_TO_TEXT),
            };
        }
    }
}