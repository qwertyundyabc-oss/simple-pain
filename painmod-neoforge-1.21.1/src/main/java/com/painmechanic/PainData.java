package com.painmechanic;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

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
		saveFile = server.getWorldPath(LevelResource.ROOT).resolve("pain_mechanic.dat");
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
			CompoundTag root = NbtIo.readCompressed(saveFile, NbtAccounter.unlimitedHeap());
			ListTag list = root.getList("players", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag entry = list.getCompound(i);
				if (entry == null) {
					continue;
				}
				String uuidString = entry.getString("uuid");
				float pain = entry.getFloat("pain");
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
			CompoundTag root = new CompoundTag();
			ListTag list = new ListTag();
			PAIN.forEach((uuid, pain) -> {
				CompoundTag entry = new CompoundTag();
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
