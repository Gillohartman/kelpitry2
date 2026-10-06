package com.greenmod;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Freelook (look around the player) and Freecam (detached camera). */
public final class FreeView {
	public enum Mode { NONE, FREELOOK, FREECAM }

	public static Mode mode = Mode.NONE;
	public static float yaw, pitch;
	public static double x, y, z, px, py, pz;
	public static double dist = 4.0;

	private static boolean fwd, back, left, right, up, down;
	private static CameraType savedType;

	private FreeView() {}

	public static boolean active() { return mode != Mode.NONE; }
	public static boolean freecam() { return mode == Mode.FREECAM; }

	/** Start of every client tick: capture movement keys for freecam and keep the player still. */
	public static void startTick(Minecraft mc) {
		fwd = back = left = right = up = down = false;
		if (mode != Mode.FREECAM || mc.screen != null) return;
		var o = mc.options;
		fwd = o.keyUp.isDown();
		back = o.keyDown.isDown();
		left = o.keyLeft.isDown();
		right = o.keyRight.isDown();
		up = o.keyJump.isDown();
		down = o.keyShift.isDown();
		o.keyUp.setDown(false);
		o.keyDown.setDown(false);
		o.keyLeft.setDown(false);
		o.keyRight.setDown(false);
		o.keyJump.setDown(false);
		o.keyShift.setDown(false);
		o.keySprint.setDown(false);
		o.keyAttack.setDown(false);
		o.keyUse.setDown(false);
	}

	/** End of every client tick: switch modes and move the freecam. */
	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			mode = Mode.NONE;
			return;
		}
		boolean wantCam = Modules.FREECAM.isActive();
		boolean wantLook = !wantCam && Modules.FREELOOK.isActive() && mc.screen == null && Keys.down(Modules.FL_KEY.key);

		if (wantCam) {
			if (mode != Mode.FREECAM) enter(mc, Mode.FREECAM);
		} else if (wantLook) {
			if (mode != Mode.FREELOOK) enter(mc, Mode.FREELOOK);
		} else if (mode != Mode.NONE) {
			exit(mc);
		}

		if (mode == Mode.FREECAM) {
			px = x; py = y; pz = z;
			double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
			double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
			double rx = -Math.cos(yr), rz = -Math.sin(yr);
			double mx = fx * ((fwd ? 1 : 0) - (back ? 1 : 0)) + rx * ((right ? 1 : 0) - (left ? 1 : 0));
			double my = fy * ((fwd ? 1 : 0) - (back ? 1 : 0)) + ((up ? 1 : 0) - (down ? 1 : 0));
			double mz = fz * ((fwd ? 1 : 0) - (back ? 1 : 0)) + rz * ((right ? 1 : 0) - (left ? 1 : 0));
			double len = Math.sqrt(mx * mx + my * my + mz * mz);
			if (len > 1e-6) {
				double s = Modules.FC_SPEED.value / Math.max(1.0, len);
				x += mx * s; y += my * s; z += mz * s;
			}
		}
		if (mode == Mode.FREELOOK) dist = Modules.FL_DIST.value;
	}

	private static void enter(Minecraft mc, Mode m) {
		LocalPlayer p = mc.player;
		mode = m;
		yaw = p.getYRot();
		pitch = p.getXRot();
		Vec3 eye = p.getEyePosition(1.0f);
		x = px = eye.x; y = py = eye.y; z = pz = eye.z;
		if (m == Mode.FREELOOK) {
			savedType = mc.options.getCameraType();
			dist = Modules.FL_DIST.value;
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
	}

	private static void exit(Minecraft mc) {
		if (mode == Mode.FREELOOK && savedType != null) mc.options.setCameraType(savedType);
		savedType = null;
		mode = Mode.NONE;
	}

	/** Called from the player turn mixin. Returns true if the mouse movement was consumed. */
	public static boolean onTurn(double dyaw, double dpitch) {
		if (mode == Mode.NONE) return false;
		yaw += (float) (dyaw * 0.15);
		pitch = Math.max(-90f, Math.min(90f, pitch + (float) (dpitch * 0.15)));
		return true;
	}

	/** Scroll: freecam speed or freelook distance. */
	public static boolean onScroll(double dy) {
		if (mode == Mode.FREECAM && Modules.FC_SCROLL.value) {
			Modules.FC_SPEED.value = clamp(Modules.FC_SPEED.value + dy * 0.1, Modules.FC_SPEED.min, Modules.FC_SPEED.max);
			Notifications.push(String.format("Freecam speed %.1f", Modules.FC_SPEED.value));
			return true;
		}
		if (mode == Mode.FREELOOK && Modules.FL_SCROLL.value) {
			Modules.FL_DIST.value = clamp(Modules.FL_DIST.value - dy * 0.5, Modules.FL_DIST.min, Modules.FL_DIST.max);
			dist = Modules.FL_DIST.value;
			return true;
		}
		return false;
	}

	private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

	public static Vec3 cameraPos(float pt) {
		if (mode == Mode.FREECAM) {
			return new Vec3(px + (x - px) * pt, py + (y - py) * pt, pz + (z - pz) * pt);
		}
		Minecraft mc = Minecraft.getInstance();
		Vec3 eye = mc.player.getEyePosition(pt);
		double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
		double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
		return new Vec3(eye.x - fx * dist, eye.y - fy * dist, eye.z - fz * dist);
	}
}
