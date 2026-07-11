package com.dailyshop;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

public final class SoundResolver {

    private SoundResolver() {
    }

    public static Sound resolve(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        try {
            Object value = Sound.class.getField(configured.trim().toUpperCase(Locale.ROOT)).get(null);
            if (value instanceof Sound sound) {
                return sound;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to a namespaced registry key.
        }
        {
            String value = configured.trim().toLowerCase(Locale.ROOT);
            NamespacedKey key = value.contains(":")
                    ? NamespacedKey.fromString(value)
                    : NamespacedKey.minecraft(value);
            return key == null ? null : Registry.SOUNDS.get(key);
        }
    }
}
