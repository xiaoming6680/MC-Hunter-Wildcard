package com.xiaoming.hunterwildcard.util;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Arrays;

public final class HunterWildcardText {
    public static final String KEY_PREFIX = HunterWildcardMod.MOD_ID + ".";
    public static final String SPEC_SEPARATOR = "\u001F";

    private HunterWildcardText() {
    }

    public static String key(String path) {
        return KEY_PREFIX + path;
    }

    public static MutableText translatable(String path, Object... args) {
        return Text.translatable(key(path), args);
    }

    public static String spec(String path, Object... args) {
        String key = key(path);
        if (args == null || args.length == 0) {
            return key;
        }

        StringBuilder builder = new StringBuilder(key);
        for (Object arg : args) {
            builder.append(SPEC_SEPARATOR).append(arg == null ? "" : arg);
        }
        return builder.toString();
    }

    public static Text fromSpec(String spec) {
        if (spec == null || spec.isBlank()) {
            return Text.empty();
        }

        if (!spec.startsWith(KEY_PREFIX)) {
            return Text.literal(spec);
        }

        String[] parts = spec.split(SPEC_SEPARATOR, -1);
        Object[] args = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length))
                .map(HunterWildcardText::argumentFromSpec)
                .toArray();
        return Text.translatable(parts[0], args);
    }

    public static MutableText prefixed(Text message) {
        return Text.empty()
                .append(translatable("msg.prefix"))
                .append(" ")
                .append(message);
    }

    public static MutableText prefixedSpec(String spec) {
        return prefixed(fromSpec(spec));
    }

    public static String wildcardId(String classSimpleName) {
        String baseName = classSimpleName.endsWith("Rule")
                ? classSimpleName.substring(0, classSimpleName.length() - "Rule".length())
                : classSimpleName;
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < baseName.length(); i++) {
            char c = baseName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                id.append('_');
            }
            id.append(Character.toLowerCase(c));
        }
        return id.toString();
    }

    public static String wildcardNameKey(String wildcardId) {
        return key("wildcard." + wildcardId + ".name");
    }

    public static String wildcardDescriptionKey(String wildcardId) {
        return key("wildcard." + wildcardId + ".description");
    }

    public static MutableText wildcardName(String wildcardId) {
        return Text.translatable(wildcardNameKey(wildcardId));
    }

    private static Object argumentFromSpec(String value) {
        return value != null && value.startsWith(KEY_PREFIX) ? Text.translatable(value) : value;
    }
}
