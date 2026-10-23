package com.curiosity.client.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.uku3lig.ukulib.config.option.StringTranslatable;

@Getter
@AllArgsConstructor
public enum HudShowMode implements StringTranslatable {
    ALWAYS("always", "curiosity.config.compass.showMode.always"),
    INVENTORY("inventory", "curiosity.config.compass.showMode.inventory"),
    HAND("hand", "curiosity.config.compass.showMode.hand");

    private final String name;
    private final String translationKey;
}