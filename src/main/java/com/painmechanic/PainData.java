package com.painmechanic;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

/**
 * 服务端疼痛值存储（内存 + 世界文件持久化）。
 */
public final class PainData {
	private static final Map<UUID, Float> PAIN = new HashMap<>();
	private static Path saveFile;
	private static boolean dirty;
	private static int saveTicks;

	private PainData() {
	}

	public static void onServerStart(MinecraftServer server) {
		PAIN.clear();
		saveFile = server.getSavePath(WorldSavePath.ROOT).resolve("pain_mechanic.dat");
		dirty = false;
		saveTicks = 0;
		load();
	}

	public static void onServerStop(MinecraftServer server) {
		save();
		PAIN.clear();
		saveFile = null;
		dirty = false;
		saveTicks = 0;
	}

	/** 定期保存，避免服务器崩溃时只依赖 SERVER_STOPPING。 */
	public static void tick() {
		if (!dirty || ++saveTicks < 600) {
			return;
		}
		saveTicks = 0;
		save();
	}

	public static float get(UUID id) {
		return PAIN.getOrDefault(id, 0f);
	}

	public static void add(UUID id, float delta) {
		if (delta == 0f) {
			return;
		}
		float next = Math.max(0f, get(id) + delta);
		set(id, next);
	}

	public static void set(UUID id, float value) {
		float v = Math.max(0f, value);
		if (v <= 0f) {
			if (PAIN.remove(id) != null) {
				dirty = true;
			}
		} else {
			Float previous = PAIN.put(id, v);
			if (previous == null || Float.compare(previous, v) != 0) {
				dirty = true;
			}
		}
	}

	private static void load() {
		if (saveFile == null || !Files.exists(saveFile)) {
			return;
		}
		try {
			NbtCompound root = NbtIo.readCompressed(saveFile, NbtSizeTracker.ofUnlimitedBytes());
			NbtList list = root.getList("players").orElseGet(NbtList::new);
			for (int i = 0; i < list.size(); i++) {
				NbtCompound entry = list.getCompound(i).orElse(null);
				if (entry == null) {
					continue;
				}
				String uuidString = entry.getString("uuid", "");
				float pain = entry.getFloat("pain", 0f);
				try {
					UUID uuid = UUID.fromString(uuidString);
					if (pain > 0f) {
						PAIN.put(uuid, pain);
					}
				} catch (IllegalArgumentException ignored) {
					// 忽略损坏条目
				}
			}
		} catch (IOException | RuntimeException e) {
			PainMechanic.LOGGER.warn("[Simple Pain] 读取疼痛数据失败", e);
		}
	}

	private static void save() {
		if (saveFile == null) {
			return;
		}
		Path tempFile = saveFile.resolveSibling(saveFile.getFileName() + ".tmp");
		try {
			NbtCompound root = new NbtCompound();
			NbtList list = new NbtList();
			PAIN.forEach((uuid, pain) -> {
				NbtCompound entry = new NbtCompound();
				entry.putString("uuid", uuid.toString());
				entry.putFloat("pain", pain);
				list.add(entry);
			});
			root.put("players", list);
			Files.createDirectories(saveFile.getParent());
			NbtIo.writeCompressed(root, tempFile);
			try {
				Files.move(tempFile, saveFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tempFile, saveFile, StandardCopyOption.REPLACE_EXISTING);
			}
			dirty = false;
		} catch (IOException e) {
			PainMechanic.LOGGER.warn("[Simple Pain] 保存疼痛数据失败", e);
		} finally {
			try {
				Files.deleteIfExists(tempFile);
			} catch (IOException ignored) {
			}
		}
	}
}
