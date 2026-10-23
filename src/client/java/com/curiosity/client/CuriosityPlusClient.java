package com.curiosity.client;


import com.curiosity.client.config.CompassConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class CuriosityPlusClient implements ClientModInitializer {

	public static Logger LOGGER = LogManager.getLogger();

	public static final String MOD_ID = "curiosity";
	public static final String MOD_NAME = "Curiosity Plus";

	public static final Identifier hudIdentifier = Identifier.parse(MOD_ID + ":hud");

	@Override
	public void onInitializeClient() {
		log(Level.INFO, "Initializing");

		ArmorHud.onInitialize();
		CompassConfig.readConfigFile();

		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, hudIdentifier, new CompassHud());

	}

	public static void log(Level level, String message) {
		LOGGER.log(level, "[" + MOD_NAME + "] " + message);
	}
}