package com.xiaoming.hunterwildcard.client;

import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;

import java.util.Arrays;

public final class HunterWildcardClientText {
    private HunterWildcardClientText() {
    }

    public static String translate(String spec) {
        if (spec == null || spec.isBlank()) {
            return "";
        }

        if (!spec.startsWith(HunterWildcardText.KEY_PREFIX)) {
            return spec;
        }

        String[] parts = spec.split(HunterWildcardText.SPEC_SEPARATOR, -1);
        Object[] args = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length))
                .map(HunterWildcardClientText::translateArgument)
                .toArray();
        return I18n.translate(parts[0], args);
    }

    public static Text text(String spec) {
        if (spec == null || spec.isBlank()) {
            return Text.empty();
        }

        if (!spec.startsWith(HunterWildcardText.KEY_PREFIX)) {
            return Text.literal(spec);
        }

        String[] parts = spec.split(HunterWildcardText.SPEC_SEPARATOR, -1);
        Object[] args = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length))
                .map(HunterWildcardClientText::textArgument)
                .toArray();
        return Text.translatable(parts[0], args);
    }

    private static Object translateArgument(String value) {
        return value != null && value.startsWith(HunterWildcardText.KEY_PREFIX) ? translate(value) : value;
    }

    private static Object textArgument(String value) {
        return value != null && value.startsWith(HunterWildcardText.KEY_PREFIX) ? text(value) : value;
    }
}
