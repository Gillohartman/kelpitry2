package com.greenmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Profiles live in config/kelpclient/profiles/NAME.json, state in config/kelpclient/state.json. */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static String profile = "default";
	public static long playSeconds;

	private Config() {}

	private static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve("kelpclient");
	}

	private static Path profilesDir() {
		return dir().resolve("profiles");
	}

	private static Path profileFile(String name) {
		return profilesDir().resolve(name + ".json");
	}

	private static String clean(String name) {
		String n = name.trim().replaceAll("[^A-Za-z0-9 _-]", "");
		return n.isEmpty() ? "default" : n;
	}

	public static void load() {
		try {
			Files.createDirectories(profilesDir());
			Path st = dir().resolve("state.json");
			if (Files.exists(st)) {
				JsonObject o = JsonParser.parseString(Files.readString(st)).getAsJsonObject();
				if (o.has("profile")) profile = clean(o.get("profile").getAsString());
				if (o.has("playSeconds")) playSeconds = o.get("playSeconds").getAsLong();
			}
			loadProfile(profile);
		} catch (Exception ignored) {
		}
	}

	public static void save() {
		try {
			Files.createDirectories(profilesDir());
			JsonObject o = new JsonObject();
			o.addProperty("profile", profile);
			o.addProperty("playSeconds", playSeconds);
			Files.writeString(dir().resolve("state.json"), GSON.toJson(o));
			saveProfile(profile);
		} catch (Exception ignored) {
		}
	}

	public static List<String> profiles() {
		List<String> out = new ArrayList<>();
		try (Stream<Path> s = Files.list(profilesDir())) {
			s.forEach(p -> {
				String n = p.getFileName().toString();
				if (n.endsWith(".json")) out.add(n.substring(0, n.length() - 5));
			});
		} catch (Exception ignored) {
		}
		if (!out.contains(profile)) out.add(profile);
		java.util.Collections.sort(out);
		return out;
	}

	public static void createProfile(String name) {
		profile = clean(name);
		save();
	}

	public static void switchProfile(String name) {
		save();
		profile = clean(name);
		loadProfile(profile);
		save();
	}

	public static void deleteProfile(String name) {
		try {
			if (name.equals(profile)) return;
			Files.deleteIfExists(profileFile(name));
		} catch (Exception ignored) {
		}
	}

	private static void saveProfile(String name) throws Exception {
		JsonObject root = new JsonObject();
		JsonObject mods = new JsonObject();
		for (Module m : Modules.ALL) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", m.isEnabled());
			o.addProperty("key", m.key);
			JsonObject so = new JsonObject();
			for (Setting s : m.settings) {
				if (s instanceof Setting.Bool b) so.addProperty(s.name, b.value);
				else if (s instanceof Setting.Num n) so.addProperty(s.name, n.value);
				else if (s instanceof Setting.Color c) so.addProperty(s.name, c.rgb);
				else if (s instanceof Setting.Key k) so.addProperty(s.name, k.key);
				else if (s instanceof Setting.Choice ch) so.addProperty(s.name, ch.index);
				else if (s instanceof Setting.Text t) so.addProperty(s.name, t.value);
				else if (s instanceof Setting.Items it) {
					JsonArray arr = new JsonArray();
					for (String v : it.values) arr.add(v);
					so.add(s.name, arr);
				} else if (s instanceof Setting.BlockColors bc) {
					JsonArray arr = new JsonArray();
					for (Setting.BlockColors.Entry en : bc.values) {
						JsonObject eo = new JsonObject();
						eo.addProperty("id", en.id);
						eo.addProperty("rgb", en.rgb);
						eo.addProperty("on", en.on);
						arr.add(eo);
					}
					so.add(s.name, arr);
				}
			}
			o.add("settings", so);
			mods.add(m.id, o);
		}
		root.add("modules", mods);

		JsonArray macros = new JsonArray();
		for (Macros.Macro m : Macros.LIST) {
			JsonObject o = new JsonObject();
			o.addProperty("name", m.name);
			o.addProperty("key", m.key);
			o.addProperty("enabled", m.enabled);
			JsonArray steps = new JsonArray();
			for (Macros.Step s : m.steps) {
				JsonObject so = new JsonObject();
				so.addProperty("text", s.text);
				so.addProperty("delay", s.delayMs);
				steps.add(so);
			}
			o.add("steps", steps);
			macros.add(o);
		}
		root.add("macros", macros);
		Files.writeString(profileFile(name), GSON.toJson(root));
	}

	private static void loadProfile(String name) {
		try {
			Path p = profileFile(name);
			if (!Files.exists(p)) return;
			JsonObject root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
			JsonObject mods = root.getAsJsonObject("modules");
			if (mods != null) {
				for (Module m : Modules.ALL) {
					JsonObject o = mods.getAsJsonObject(m.id);
					if (o == null) continue;
					if (o.has("enabled") && !m.noToggle) m.loadEnabled(o.get("enabled").getAsBoolean());
					if (o.has("key")) m.key = o.get("key").getAsInt();
					JsonObject so = o.getAsJsonObject("settings");
					if (so == null) continue;
					for (Setting s : m.settings) {
						if (!so.has(s.name)) continue;
						JsonElement e = so.get(s.name);
						if (s instanceof Setting.Bool b) b.value = e.getAsBoolean();
						else if (s instanceof Setting.Num n) n.value = Math.max(n.min, Math.min(n.max, e.getAsDouble()));
						else if (s instanceof Setting.Color c) c.rgb = e.getAsInt();
						else if (s instanceof Setting.Key k) k.key = e.getAsInt();
						else if (s instanceof Setting.Choice ch) ch.index = Math.floorMod(e.getAsInt(), ch.options.length);
						else if (s instanceof Setting.Text t) t.value = e.getAsString();
						else if (s instanceof Setting.Items it) {
							it.values.clear();
							for (JsonElement v : e.getAsJsonArray()) it.values.add(v.getAsString());
						} else if (s instanceof Setting.BlockColors bc) {
							bc.values.clear();
							for (JsonElement v : e.getAsJsonArray()) {
								JsonObject eo = v.getAsJsonObject();
								bc.values.add(new Setting.BlockColors.Entry(eo.get("id").getAsString(), eo.get("rgb").getAsInt(), eo.get("on").getAsBoolean()));
							}
						}
					}
				}
			}
			Macros.LIST.clear();
			JsonArray macros = root.getAsJsonArray("macros");
			if (macros != null) {
				for (JsonElement me : macros) {
					JsonObject o = me.getAsJsonObject();
					Macros.Macro m = new Macros.Macro();
					m.name = o.get("name").getAsString();
					m.key = o.get("key").getAsInt();
					m.enabled = o.get("enabled").getAsBoolean();
					for (JsonElement se : o.getAsJsonArray("steps")) {
						JsonObject so = se.getAsJsonObject();
						m.steps.add(new Macros.Step(so.get("text").getAsString(), so.get("delay").getAsInt()));
					}
					Macros.LIST.add(m);
				}
			}
		} catch (Exception ignored) {
		}
	}
}
