package com.xiaoming.hunterwildcard.client.screen.widget;

import com.xiaoming.hunterwildcard.client.HunterWildcardClientText;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DropdownWidget extends ButtonWidget {
    private static final String ARROW_CLOSED = "▼";
    private static final String ARROW_OPEN = "▲";

    private final TextRenderer textRenderer;
    private final String label;
    private final List<Option> options;
    private final Consumer<String> valueConsumer;
    private final Runnable closeOthers;
    private String value;
    private boolean expanded;
    private boolean openUp;

    public DropdownWidget(
            TextRenderer textRenderer,
            int x,
            int y,
            int width,
            int height,
            String label,
            List<Option> options,
            String value,
            boolean enabled,
            Consumer<String> valueConsumer,
            Runnable closeOthers
    ) {
        super(x, y, width, height, net.minecraft.text.Text.empty(), button -> {
        }, DEFAULT_NARRATION_SUPPLIER);
        this.textRenderer = textRenderer;
        this.label = Objects.requireNonNull(label);
        this.options = List.copyOf(options);
        this.valueConsumer = Objects.requireNonNull(valueConsumer);
        this.closeOthers = closeOthers == null ? () -> {
        } : closeOthers;
        this.value = value;
        this.active = enabled;
        updateMessage();
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void close() {
        expanded = false;
        updateMessage();
    }

    public void setOpenUp(boolean openUp) {
        this.openUp = openUp;
    }

    public boolean containsPoint(double mouseX, double mouseY) {
        return isInside(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY)
                || (expanded && optionIndexAt(mouseX, mouseY) >= 0);
    }

    public void renderOverlay(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!visible || !expanded) {
            return;
        }

        int optionHeight = optionHeight();
        int menuX = getX();
        int menuWidth = getWidth();
        int menuHeight = optionHeight * options.size();
        int menuY = menuY(menuHeight);
        context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, 0xF0222A33);
        context.fill(menuX, menuY, menuX + menuWidth, menuY + 1, 0xFF7FC2FF);
        context.fill(menuX, menuY + menuHeight - 1, menuX + menuWidth, menuY + menuHeight, 0xFF4C5A66);
        context.fill(menuX, menuY, menuX + 1, menuY + menuHeight, 0xFF4C5A66);
        context.fill(menuX + menuWidth - 1, menuY, menuX + menuWidth, menuY + menuHeight, 0xFF4C5A66);

        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int optionY = menuY + i * optionHeight;
            boolean hovered = isInside(menuX, optionY, menuWidth, optionHeight, mouseX, mouseY);
            boolean selected = option.value().equals(selectedOption().value());
            if (hovered || selected) {
                context.fill(menuX + 2, optionY + 1, menuX + menuWidth - 2, optionY + optionHeight - 1, hovered ? 0xAA3E5570 : 0x66345B78);
            }

            int color = selected ? 0xFF7FC2FF : 0xFFFFFFFF;
            String text = textRenderer.trimToWidth(tr(option.displayName()), menuWidth - 14);
            context.drawText(textRenderer, net.minecraft.text.Text.literal(text), menuX + 7, optionY + Math.max(4, (optionHeight - textRenderer.fontHeight) / 2), color, false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (!visible || button != 0) {
            return false;
        }

        if (isInside(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY)) {
            if (!active) {
                close();
                return true;
            }

            boolean wasExpanded = expanded;
            closeOthers.run();
            expanded = !wasExpanded;
            updateMessage();
            return true;
        }

        int optionIndex = optionIndexAt(mouseX, mouseY);
        if (expanded && optionIndex >= 0) {
            if (!active) {
                close();
                return true;
            }

            Option option = options.get(optionIndex);
            value = option.value();
            close();
            valueConsumer.accept(value);
            return true;
        }

        if (expanded) {
            close();
        }
        return false;
    }

    @Override
    protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean enabled = active;
        boolean hovered = isHovered();
        Palette palette = enabled
                ? new Palette(hovered || expanded ? 0xAA3E5570 : 0x88303A46, hovered || expanded ? 0xFF74B6FF : 0xFF4C5A66, 0xFFFFFFFF)
                : new Palette(0x66303A46, 0xFF59636C, 0xFF9FAAB4);
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        context.fill(x, y, x + width, y + height, palette.background);
        context.fill(x, y, x + width, y + 1, palette.border);
        context.fill(x, y + height - 1, x + width, y + height, palette.border);
        context.fill(x, y, x + 1, y + height, palette.border);
        context.fill(x + width - 1, y, x + width, y + height, palette.border);

        String arrow = expanded ? ARROW_OPEN : ARROW_CLOSED;
        int arrowWidth = textRenderer.getWidth(arrow);
        String title = textRenderer.trimToWidth(titleText(), width - arrowWidth - 18);
        int titleX = x + 7;
        int titleY = y + Math.max(4, (height - textRenderer.fontHeight) / 2);
        context.drawText(textRenderer, net.minecraft.text.Text.literal(title), titleX, titleY, palette.text, true);
        context.drawText(textRenderer, net.minecraft.text.Text.literal(arrow), x + width - arrowWidth - 7, titleY, palette.text, true);
    }

    @Override
    protected void drawLabel(DrawnTextConsumer textConsumer) {
    }

    private void updateMessage() {
        setMessage(net.minecraft.text.Text.literal(title()));
    }

    private String title() {
        return titleText() + " " + (expanded ? ARROW_OPEN : ARROW_CLOSED);
    }

    private String titleText() {
        return label.isBlank()
                ? tr(selectedOption().displayName())
                : tr(HunterWildcardText.spec("screen.dropdown.title", tr(label), tr(selectedOption().displayName())));
    }

    private Option selectedOption() {
        for (Option option : options) {
            if (option.value().equals(value)) {
                return option;
            }
        }

        return options.isEmpty() ? new Option("", "") : options.get(0);
    }

    private int optionIndexAt(double mouseX, double mouseY) {
        int menuX = getX();
        int optionHeight = optionHeight();
        int menuY = menuY(optionHeight * options.size());
        if (!isInside(menuX, menuY, getWidth(), optionHeight * options.size(), mouseX, mouseY)) {
            return -1;
        }

        int index = (int) ((mouseY - menuY) / optionHeight);
        return index >= 0 && index < options.size() ? index : -1;
    }

    private int optionHeight() {
        return Math.max(18, getHeight());
    }

    private int menuY(int menuHeight) {
        return openUp ? getY() - menuHeight - 1 : getY() + getHeight() + 1;
    }

    private static String tr(String spec) {
        return HunterWildcardClientText.translate(spec);
    }

    private static boolean isInside(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record Palette(int background, int border, int text) {
    }

    public record Option(String value, String displayName) {
    }
}
