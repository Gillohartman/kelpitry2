package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/** RESTRICTED: move everything out of (steal) or into (dump) the open chest with shift-clicks. */
public final class ChestTools {
	private static boolean prevSteal, prevDump;

	private ChestTools() {}

	public static boolean available(Minecraft mc) {
		AbstractContainerMenu m = mc.player == null ? null : mc.player.containerMenu;
		return Modules.CHEST_TOOLS.isActive() && (m instanceof ChestMenu || m instanceof ShulkerBoxMenu) && m.slots.size() > 36;
	}

	public static void tick(Minecraft mc) {
		boolean s = Keys.down(Modules.CT_STEAL_KEY.key), d = Keys.down(Modules.CT_DUMP_KEY.key);
		if (available(mc)) {
			if (s && !prevSteal) steal(mc);
			if (d && !prevDump) dump(mc);
		}
		prevSteal = s;
		prevDump = d;
	}

	public static void steal(Minecraft mc) { move(mc, true); }
	public static void dump(Minecraft mc) { move(mc, false); }

	private static void move(Minecraft mc, boolean fromChest) {
		if (!available(mc)) return;
		LocalPlayer p = mc.player;
		MultiPlayerGameMode gm = mc.gameMode;
		if (gm == null) return;
		AbstractContainerMenu menu = p.containerMenu;
		int size = menu.slots.size();
		int from = fromChest ? 0 : size - 36;
		int to = fromChest ? size - 36 : size;
		for (int i = from; i < to; i++) {
			if (!menu.slots.get(i).getItem().isEmpty()) InventorySorter.quickMove(gm, menu.containerId, i, p);
		}
	}
}
