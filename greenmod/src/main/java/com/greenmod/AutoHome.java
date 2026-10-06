package com.greenmod;

import net.minecraft.client.Minecraft;

/**
 * One key, four steps, six commands:
 * press 1 sends commands 1+2, press 2 sends 3+4, press 3 sends 5, press 4 sends 6.
 * A second key sets the counter back to step 1.
 * In singleplayer and on allowed servers the commands go out instantly. Everywhere else they are spaced one second
 * apart, like macros, so this can never be used to flood a public server.
 */
public final class AutoHome {
	private static final int[][] STEPS = {{0, 1}, {2, 3}, {4}, {5}};
	private static int step;
	private static boolean prevRun, prevReset;

	private AutoHome() {}

	public static int step() { return step; }

	public static void tick(Minecraft mc) {
		if (mc.player == null) return;
		boolean run = Keys.down(Modules.AH_KEY.key);
		boolean reset = Keys.down(Modules.AH_RESET_KEY.key);
		boolean usable = Modules.AUTO_HOME.isActive() && mc.screen == null;

		if (usable && reset && !prevReset) {
			step = 0;
			Notifications.push("Auto Set Home: back to step 1.");
		}
		if (usable && run && !prevRun) runStep(mc);
		prevRun = run;
		prevReset = reset;
	}

	private static void runStep(Minecraft mc) {
		if (step >= STEPS.length) {
			if (!Modules.AH_WRAP.value) {
				Notifications.push("Auto Set Home: all 4 steps done. Press the reset key.");
				return;
			}
			step = 0;
		}
		boolean fast = ServerSafety.allowsRestricted();
		long delay = 0;
		int sent = 0;
		for (int idx : STEPS[step]) {
			String cmd = Modules.AH_CMD[idx].value;
			if (cmd == null || cmd.isBlank()) continue;
			if (fast) Macros.sendNow(mc, cmd);
			else { delay += 1000; Macros.enqueue(cmd, delay); }
			sent++;
		}
		if (Modules.AH_NOTIFY.value) {
			Notifications.push("Auto Set Home: step " + (step + 1) + "/4" + (sent == 0 ? " (no commands set)" : " sent " + sent));
		}
		step++;
	}
}
