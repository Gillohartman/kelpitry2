package com.greenmod;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small drawing helpers: rounded rectangles, small text, colours. */
public final class Ui {
	private Ui() {}

	public static int a(int rgb, double opacity) {
		int al = (int) Math.round(Math.max(0, Math.min(1, opacity)) * 255);
		return (al << 24) | (rgb & 0xFFFFFF);
	}

	public static int opaque(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }

	public static int mix(int c1, int c2, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
		int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
		int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
		return (r << 16) | (g << 8) | b;
	}

	public static int hsv(float h, float s, float v) {
		h = h - (float) Math.floor(h);
		float h6 = h * 6f;
		int i = (int) h6;
		float f = h6 - i;
		float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
		float r, g, b;
		switch (i % 6) {
			case 0: r = v; g = t; b = p; break;
			case 1: r = q; g = v; b = p; break;
			case 2: r = p; g = v; b = t; break;
			case 3: r = p; g = q; b = v; break;
			case 4: r = t; g = p; b = v; break;
			default: r = v; g = p; b = q; break;
		}
		return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}

	public static float[] toHsv(int rgb) {
		float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
		float d = max - min, h;
		if (d == 0) h = 0;
		else if (max == r) h = ((g - b) / d) % 6f;
		else if (max == g) h = (b - r) / d + 2f;
		else h = (r - g) / d + 4f;
		h /= 6f;
		if (h < 0) h += 1f;
		return new float[] {h, max == 0 ? 0 : d / max, max};
	}

	/** Filled rounded rectangle. */
	public static void rr(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int c) {
		if (w <= 0 || h <= 0) return;
		r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
		if (r == 0) { g.fill(x, y, x + w, y + h, c); return; }
		g.fill(x, y + r, x + w, y + h - r, c);
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			int inset = r - (int) Math.floor(Math.sqrt(r * r - dy * dy));
			g.fill(x + inset, y + i, x + w - inset, y + i + 1, c);
			g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, c);
		}
	}

	public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int rgb) {
		g.text(f, s, x, y, opaque(rgb));
	}

	public static void small(GuiGraphicsExtractor g, Font f, String s, float x, float y, int rgb, float scale) {
		var p = g.pose();
		p.pushMatrix();
		p.translate(x, y);
		p.scale(scale, scale);
		g.text(f, s, 0, 0, opaque(rgb));
		p.popMatrix();
	}

	/** Small padlock icon, about 7x9. */
	public static void lock(GuiGraphicsExtractor g, int x, int y, int rgb) {
		int c = opaque(rgb);
		g.fill(x + 1, y, x + 6, y + 1, c);
		g.fill(x + 1, y + 1, x + 2, y + 4, c);
		g.fill(x + 5, y + 1, x + 6, y + 4, c);
		rr(g, x, y + 4, 7, 5, 1, c);
	}

	/** Straight line made of 1-pixel-wide runs (Bresenham style, no gaps). */
	public static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
		int dx = x2 - x1, dy = y2 - y1;
		if (Math.abs(dy) >= Math.abs(dx)) {
			if (dy == 0) { g.fill(x1, y1, x1 + 1, y1 + 1, color); return; }
			int step = dy > 0 ? 1 : -1;
			int n = Math.abs(dy);
			for (int i = 0; i <= n; i++) {
				int y = y1 + i * step;
				double t0 = Math.max(0, i - 0.5) / n, t1 = Math.min(n, i + 0.5) / n;
				int xa = (int) Math.round(x1 + dx * t0), xb = (int) Math.round(x1 + dx * t1);
				g.fill(Math.min(xa, xb), y, Math.max(xa, xb) + 1, y + 1, color);
			}
		} else {
			int step = dx > 0 ? 1 : -1;
			int n = Math.abs(dx);
			for (int i = 0; i <= n; i++) {
				int x = x1 + i * step;
				double t0 = Math.max(0, i - 0.5) / n, t1 = Math.min(n, i + 0.5) / n;
				int ya = (int) Math.round(y1 + dy * t0), yb = (int) Math.round(y1 + dy * t1);
				g.fill(x, Math.min(ya, yb), x + 1, Math.max(ya, yb) + 1, color);
			}
		}
	}

	public static int accent() { return Modules.GUI_ACCENT.rgb; }
}
