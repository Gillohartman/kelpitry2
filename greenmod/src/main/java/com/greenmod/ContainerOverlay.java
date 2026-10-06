package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

/** Drawn on top of every container screen: tint, totem glow, sort button, shulker preview. */
public final class ContainerOverlay {
	private static boolean prevLeft;

	private ContainerOverlay() {}

	public static void render(GuiGraphicsExtractor g, int mouseX, int mouseY, int leftPos, int topPos,
							  int imageW, int imageH, AbstractContainerMenu menu, Slot hovered, Screen screen) {
		Minecraft mc = Minecraft.getInstance();
		nextLayer(g);

		double tint = Modules.HC_INV_OP.value / 100.0;
		if (Modules.HUD_COLORS.isActive() && tint > 0) {
			Ui.rr(g, leftPos, topPos, imageW, imageH, 4, Ui.a(Modules.HC_INV.rgb, tint));
		}

		if (Modules.TOTEM_GLOW.isActive() && Modules.TG_INV.value) {
			for (Slot s : menu.slots) {
				if (s.getItem().is(Items.TOTEM_OF_UNDYING)) TotemFeatures.glow(g, leftPos + s.x, topPos + s.y);
			}
		}

		boolean left = Keys.mouse(0);
		boolean click = left && !prevLeft;
		prevLeft = left;

		if (Modules.SORT.isActive() && Modules.SORT_BUTTON.value && !(screen instanceof CreativeModeInventoryScreen)) {
			int bw = 34, bh = 12;
			int bx = leftPos + imageW - bw - 4;
			int by = topPos - bh - 3;
			boolean hv = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
			Ui.rr(g, bx, by, bw, bh, 4, Ui.a(hv ? Ui.mix(0x111418, Ui.accent(), 0.35) : 0x111418, 0.9));
			String t = "Sort";
			g.text(mc.font, t, bx + (bw - mc.font.width(t)) / 2, by + 2, Ui.opaque(Ui.accent()));
			if (hv && click) InventorySorter.sort();
		}

		if (ChestTools.available(mc)) {
			int by = topPos - 15;
			int bx = leftPos + imageW - 38 - 40;
			String[] labels = {"Dump", "Steal"};
			for (int i = 0; i < 2; i++) {
				int x = bx - i * 40;
				boolean hv = mouseX >= x && mouseX < x + 36 && mouseY >= by && mouseY < by + 12;
				Ui.rr(g, x, by, 36, 12, 4, Ui.a(hv ? Ui.mix(0x111418, Ui.accent(), 0.35) : 0x111418, 0.9));
				g.text(mc.font, labels[i], x + (36 - mc.font.width(labels[i])) / 2, by + 2, Ui.opaque(Ui.accent()));
				if (hv && click) { if (i == 0) ChestTools.dump(mc); else ChestTools.steal(mc); }
			}
		}

		nextLayer(g);
		ShulkerPreview.render(g, mouseX, mouseY, hovered);
		MapPreview.render(g, mouseX, mouseY, hovered);
	}

	/** Items are drawn in a later pass than fills; start a new layer (if this version has one) so overlays stay on top. */
	private static void nextLayer(GuiGraphicsExtractor g) {
		try {
			g.getClass().getMethod("nextStratum").invoke(g);
		} catch (Throwable ignored) {
		}
	}
}
