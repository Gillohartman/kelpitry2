package com.greenmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class GreenModClient implements ClientModInitializer {
	public static final String MOD_ID = "kelpclient";
	public static final String VERSION = "1.2.0";

	private static boolean prevOpen;
	private static boolean prevSort;
	private static Double savedGamma;
	private static int tickCount;

	@Override
	public void onInitializeClient() {
		Config.load();
		CrosshairPixels.load();

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "overlay"), HitboxHud::render);

		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, delta) -> {
			if (Modules.CROSSHAIR.isActive()) HitboxHud.crosshair(graphics);
			else original.extractRenderState(graphics, delta);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original -> (graphics, delta) -> {
			if (Modules.HUD_COLORS.isActive()) HudColors.health(graphics, Minecraft.getInstance());
			else original.extractRenderState(graphics, delta);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, original -> (graphics, delta) -> {
			if (Modules.HUD_COLORS.isActive()) HudColors.food(graphics, Minecraft.getInstance());
			else original.extractRenderState(graphics, delta);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.INFO_BAR, original -> (graphics, delta) -> {
			if (Modules.HUD_COLORS.isActive() && Modules.HC_XP.value) HudColors.xpBar(graphics, Minecraft.getInstance());
			else original.extractRenderState(graphics, delta);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, original -> (graphics, delta) -> {
			if (Modules.HUD_COLORS.isActive() && Modules.HC_XP.value) HudColors.level(graphics, Minecraft.getInstance());
			else original.extractRenderState(graphics, delta);
		});

		// freecam needs the movement keys cleared BEFORE the player is ticked
		ClientTickEvents.START_CLIENT_TICK.register(FreeView::startTick);
		ClientTickEvents.END_CLIENT_TICK.register(GreenModClient::tick);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			ZoomFeature.restore();
			FovFeature.restore(client);
			if (savedGamma != null) {
				client.options.gamma().set(Math.min(1.0, savedGamma));
				savedGamma = null;
			}
			DiscordRpc.shutdown();
			ChunkFinder.save();
			Waypoints.save();
			CrosshairPixels.save();
			Config.save();
		});
	}

	private static void tick(Minecraft mc) {
		// these have to run even on menu screens
		AutoReconnect.tick(mc);

		if (mc.player == null || mc.level == null) return;

		// 1) safety first: decide what may run before anything else does
		ServerSafety.tick(mc);

		// playtime
		if (++tickCount % 20 == 0) Config.playSeconds++;
		if (tickCount % 6000 == 0) Config.save();

		// open GUI with Right Shift
		boolean rs = Keys.down(GLFW.GLFW_KEY_RIGHT_SHIFT);
		if (rs && !prevOpen && mc.screen == null) mc.setScreen(new GreenGui());
		prevOpen = rs;

		// module toggle keys (setEnabled enforces the safety lock)
		for (Module m : Modules.ALL) {
			if (m.key < 0 || m.noToggle) continue;
			boolean d = Keys.down(m.key);
			if (d && !m.lastDown && mc.screen == null) m.toggle();
			m.lastDown = d;
		}

		// inventory sort key (container screens only)
		boolean sd = Keys.down(Modules.SORT_KEY.key);
		if (Modules.SORT.isActive() && sd && !prevSort && mc.screen instanceof AbstractContainerScreen<?>) {
			if (!Modules.SORT_CTRL.value || Keys.ctrl()) InventorySorter.sort();
		}
		prevSort = sd;

		if (Modules.REFILL.isActive()) InventorySorter.refillTick(mc);

		if (Modules.NO_BOB.isActive() && mc.options.bobView().get()) mc.options.bobView().set(false);

		fullbright(mc);
		FovFeature.tick(mc);
		ZoomFeature.update();
		FreeView.tick(mc);
		MovementFeatures.tick(mc);
		ActionModules.tick(mc);
		AutoTotem.tick(mc);
		AutoXp.tick(mc);
		AutoCrafter.tick(mc);
		ChestTools.tick(mc);
		AimAssist.tick(mc);
		BlockEsp.tick(mc);
		ChunkFinder.tick(mc);
		Waypoints.tick(mc);
		RedstoneTweaks.tick(mc);
		BeaconInfo.tick(mc);
		Xray.tick(mc);
		ChatSettings.tick(mc);
		OnlineHub.tick(mc);
		CopyName.tick(mc);
		SpotifyPlayer.tick(mc);
		Macros.tick(mc);
		AutoHome.tick(mc);
		DiscordRpc.tick(mc);

		if (Modules.SPRINT.isActive() && mc.screen == null && mc.options.keyUp.isDown()
				&& !mc.player.isShiftKeyDown() && !mc.player.isUsingItem()) {
			mc.options.keySprint.setDown(true);
		}
	}

	private static void fullbright(Minecraft mc) {
		OptionInstance<Double> opt = mc.options.gamma();
		if (Modules.FULLBRIGHT.isActive()) {
			if (savedGamma == null) savedGamma = Math.min(1.0, opt.get());
			if (opt.get() < 15.0) GreenMod.forceOption(opt, 16.0);
		} else if (savedGamma != null && !Xray.active()) {
			opt.set(Math.min(1.0, savedGamma));
			savedGamma = null;
		}
	}
}
