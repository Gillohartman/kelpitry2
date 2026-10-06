package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Kelp Client badge on the Minecraft main menu. */
public final class Branding {
	private Branding() {}

	public static void draw(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		String title = "Kelp Client";
		String sub = "v" + GreenModClient.VERSION + "  -  made by ocxh";
		int w = Math.max(8 + 14 + 8 + mc.font.width(title) + 8, 34 + (int) (mc.font.width(sub) * 0.75f) + 8), h = 28;
		int x = 8, y = 8;

		Ui.rr(g, x - 1, y - 1, w + 2, h + 2, 8, Ui.a(Ui.accent(), 0.35));
		Ui.rr(g, x, y, w, h, 7, Ui.a(0x111418, 0.9));
		Ui.rr(g, x + 5, y + 6, 16, 16, 5, Ui.a(Ui.accent(), 0.25));
		int kx = x + 9, ky = y + 10;
		g.fill(kx, ky, kx + 8, ky + 8, 0xFF2F4A24);
		g.fill(kx, ky, kx + 8, ky + 1, 0xFF5E8A45);
		g.fill(kx + 1, ky + 1, kx + 2, ky + 8, 0xFF4A7A36);
		g.fill(kx + 4, ky + 1, kx + 5, ky + 8, 0xFF4A7A36);
		g.fill(kx + 6, ky + 1, kx + 7, ky + 8, 0xFF3C6A2C);

		g.text(mc.font, title, x + 28, y + 5, Ui.opaque(Ui.accent()));
		Ui.small(g, mc.font, sub, x + 28, y + 17, 0xAAAAAA, 0.75f);
	}
}
