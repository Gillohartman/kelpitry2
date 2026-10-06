package com.greenmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

/** RESTRICTED movement/building helpers. All of them check isActive(), so they never run on public servers. */
public final class ActionModules {
	private static int scaffoldCooldown;
	private static Field rightClickDelay;
	private static boolean searchedField;

	private ActionModules() {}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null || mc.level == null) return;
		tridentBoost(p);
		fastSprint(mc, p);
		scaffold(mc, p);
		fastPlace(mc);
	}

	private static void tridentBoost(LocalPlayer p) {
		if (!Modules.TRIDENT_BOOST.isActive() || !p.isAutoSpinAttack()) return;
		double k = 1.0 + (Modules.TB_POWER.value - 1.0) * 0.08;
		Vec3 v = p.getDeltaMovement();
		p.setDeltaMovement(v.x * k, v.y * k, v.z * k);
	}

	private static void fastSprint(Minecraft mc, LocalPlayer p) {
		if (!Modules.FAST_SPRINT.isActive() || mc.screen != null || FreeView.freecam() || Modules.FLIGHT.isActive()) return;
		if (!p.isSprinting() || !p.onGround()) return;
		double fwd = 0, side = 0;
		if (mc.options.keyUp.isDown()) fwd += 1;
		if (mc.options.keyDown.isDown()) fwd -= 1;
		if (mc.options.keyLeft.isDown()) side += 1;
		if (mc.options.keyRight.isDown()) side -= 1;
		if (fwd == 0 && side == 0) return;
		double yaw = Math.toRadians(p.getYRot());
		double len = Math.max(1.0, Math.sqrt(fwd * fwd + side * side));
		double sp = Modules.FS_SPEED.value / len;
		Vec3 v = p.getDeltaMovement();
		p.setDeltaMovement((-Math.sin(yaw) * fwd + Math.cos(yaw) * side) * sp, v.y, (Math.cos(yaw) * fwd + Math.sin(yaw) * side) * sp);
	}

	private static void scaffold(Minecraft mc, LocalPlayer p) {
		if (!Modules.SCAFFOLD.isActive() || mc.screen != null || mc.gameMode == null) return;
		if (scaffoldCooldown-- > 0) return;
		ClientLevel level = mc.level;
		BlockPos below = p.blockPosition().below();
		if (!level.getBlockState(below).canBeReplaced()) return;

		int slot = -1;
		int sel = p.getInventory().getSelectedSlot();
		if (p.getInventory().getItem(sel).getItem() instanceof BlockItem) slot = sel;
		else for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).getItem() instanceof BlockItem) { slot = i; break; }
		if (slot < 0) return;

		for (Direction d : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			BlockPos n = below.relative(d);
			if (level.getBlockState(n).canBeReplaced()) continue;
			Direction face = d.getOpposite();
			Vec3 hitPos = Vec3.atCenterOf(n).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
			int prev = p.getInventory().getSelectedSlot();
			p.getInventory().setSelectedSlot(slot);
			mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(hitPos, face, n, false));
			p.swing(InteractionHand.MAIN_HAND);
			p.getInventory().setSelectedSlot(prev);
			scaffoldCooldown = Modules.SC_DELAY.i();
			return;
		}
	}

	private static void fastPlace(Minecraft mc) {
		if (!Modules.FAST_PLACE.isActive()) return;
		try {
			if (!searchedField) {
				searchedField = true;
				rightClickDelay = Minecraft.class.getDeclaredField("rightClickDelay");
				rightClickDelay.setAccessible(true);
			}
			if (rightClickDelay != null) rightClickDelay.setInt(mc, 0);
		} catch (Throwable ignored) {
		}
	}
}
