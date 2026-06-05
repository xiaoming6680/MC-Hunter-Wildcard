package com.xiaoming.hunterwildcard.client.screen;

import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.ConfigSnapshot;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.DebugAction;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.GameAction;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.SyncConfigPayload;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.TeamAction;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HunterWildcardConfigScreen extends Screen {
    private final List<Label> labels = new ArrayList<>();
    private final List<Box> boxes = new ArrayList<>();
    private final Map<NumberField, TextFieldWidget> numberFields = new EnumMap<>(NumberField.class);

    private Page currentPage = Page.GAME;
    private SyncConfigPayload serverSync;
    private ConfigSnapshot editableConfig;
    private boolean canManage;
    private boolean requested;
    private int refreshTicks;
    private int contentScroll;
    private int pageContentHeight;
    private String statusMessage = "正在请求服务器数据...";

    public HunterWildcardConfigScreen() {
        super(Text.literal("猎人外卡"));
    }

    public static void receiveSync(SyncConfigPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HunterWildcardConfigScreen screen) {
            screen.applySync(payload);
        }
    }

    @Override
    protected void init() {
        labels.clear();
        boxes.clear();
        numberFields.clear();

        if (!isDebugPageEnabled() && currentPage == Page.DEBUG) {
            currentPage = Page.GAME;
        }

        Layout layout = layout();
        contentScroll = Math.min(contentScroll, maxContentScroll(layout));
        pageContentHeight = layout.contentHeight();
        buildNavigation(layout);
        labels.add(new Label(currentPage.label, layout.contentX(), layout.panelY() + 14, 0xFFFFFFFF, true));

        switch (currentPage) {
            case GAME -> buildGamePage(layout);
            case TEAM -> buildTeamPage(layout);
            case CONFIG -> buildConfigPage(layout);
            case DEBUG -> buildDebugPage(layout);
        }

        buildFooter(layout);

        if (!requested) {
            requested = true;
            requestConfig();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Layout layout = layout();
        context.fill(0, 0, width, height, 0x88000000);
        context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(), 0xD0161B22);
        context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.navWidth(), layout.panelY() + layout.panelHeight(), 0xE01E252D);
        context.fill(layout.panelX() + layout.navWidth(), layout.panelY(), layout.panelX() + layout.navWidth() + 1, layout.panelY() + layout.panelHeight(), 0xFF35404B);

        context.drawText(textRenderer, Text.literal("猎人外卡"), layout.panelX() + 12, layout.panelY() + 13, 0xFFFFFFFF, true);

        for (Box box : boxes) {
            context.fill(box.x, box.y, box.x + box.width, box.y + box.height, box.color);
            context.fill(box.x, box.y, box.x + box.width, box.y + 1, box.borderColor);
            context.fill(box.x, box.y + box.height - 1, box.x + box.width, box.y + box.height, box.borderColor);
            context.fill(box.x, box.y, box.x + 1, box.y + box.height, box.borderColor);
            context.fill(box.x + box.width - 1, box.y, box.x + box.width, box.y + box.height, box.borderColor);
        }

        for (Label label : labels) {
            context.drawText(textRenderer, Text.literal(trim(label.text, label.maxWidth(layout))), label.x, label.y, label.color, label.shadow);
        }

        String footerMessage = trim(statusMessage, Math.max(80, layout.contentWidth()));
        context.drawText(textRenderer, Text.literal(footerMessage), layout.contentX(), layout.panelY() + layout.panelHeight() - 14, 0xFFFFD966, false);
        renderScrollBar(context, layout);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = layout();
        if (!isInsideContent(layout, mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        int maxScroll = maxContentScroll(layout);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (currentPage == Page.CONFIG && !applyVisibleInputs(false)) {
            return true;
        }

        int delta = (int) Math.round(verticalAmount * 22.0D);
        if (delta == 0) {
            delta = verticalAmount > 0 ? 22 : -22;
        }
        contentScroll = clamp(contentScroll - delta, 0, maxScroll);
        clearAndInit();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        if (currentPage == Page.CONFIG) {
            return;
        }

        refreshTicks++;
        if (refreshTicks >= 20) {
            refreshTicks = 0;
            requestConfig(false);
        }
    }

    private void buildNavigation(Layout layout) {
        int x = layout.panelX() + 10;
        int y = layout.panelY() + 48;
        for (Page page : visiblePages()) {
            StyledButtonWidget button = new StyledButtonWidget(
                    x,
                    y,
                    layout.navWidth() - 20,
                    24,
                    page.label,
                    "",
                    widget -> switchPage(page),
                    page == currentPage ? ButtonVariant.SELECTED : ButtonVariant.NORMAL
            );
            button.active = true;
            addDrawableChild(button);
            y += 30;
        }
    }

    private void buildGamePage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.contentWidth();
        addBox(x, layout.contentY(), w, Math.min(118, layout.contentHeight()));

        if (serverSync == null) {
            addContentLabel(layout, "等待服务器同步。", x + 12, y + 14, 0xFFC9D4DE);
            markContentBottom(layout, y + 40);
            return;
        }

        addContentLabel(layout, "当前阶段: " + stateName(serverSync.gameState()), x + 12, y + 12, 0xFFFFFFFF);
        addContentLabel(layout, "猎人: " + serverSync.hunterCount() + " 人", x + 12, y + 32, 0xFFC9D4DE);
        addContentLabel(layout, "逃亡者: " + serverSync.runnerCount() + " 人", x + 12, y + 52, 0xFFC9D4DE);
        addContentLabel(layout, "当前外卡: " + serverSync.activeWildcard(), x + 12, y + 72, 0xFFC9D4DE);
        addContentLabel(layout, "你的权限: " + (serverSync.canManage() ? "OP" : "普通玩家"), x + 12, y + 92, serverSync.canManage() ? 0xFF77E287 : 0xFFFFC2C8);

        if (serverSync.canManage() && serverSync.gameState() == GameState.WAITING) {
            addContentButton(layout, x, y + 134, 110, 28, "开始游戏", "", widget -> sendGameAction(GameAction.START_GAME), ButtonVariant.NORMAL, true);
            markContentBottom(layout, y + 172);
        } else {
            markContentBottom(layout, y + 118);
        }
    }

    private void buildTeamPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.contentWidth();
        addBox(x, layout.contentY(), w, Math.min(96, layout.contentHeight()));

        if (serverSync == null) {
            addContentLabel(layout, "等待服务器同步。", x + 12, y + 14, 0xFFC9D4DE);
            markContentBottom(layout, y + 40);
            return;
        }

        addContentLabel(layout, "我的队伍: " + serverSync.playerRole(), x + 12, y + 12, 0xFFFFFFFF);
        addContentLabel(layout, "猎人: " + serverSync.hunterCount() + " 人", x + 12, y + 34, 0xFFC9D4DE);
        addContentLabel(layout, "逃亡者: " + serverSync.runnerCount() + " 人", x + 12, y + 54, 0xFFC9D4DE);

        int buttonY = y + 116;
        int buttonWidth = Math.max(82, Math.min(112, (w - 18) / 3));
        addContentButton(layout, x, buttonY, buttonWidth, 26, "加入猎人", "", widget -> sendTeamAction(TeamAction.JOIN_HUNTER), ButtonVariant.NORMAL, true);
        addContentButton(layout, x + buttonWidth + 9, buttonY, buttonWidth, 26, "加入逃亡者", "", widget -> sendTeamAction(TeamAction.JOIN_RUNNER), ButtonVariant.NORMAL, true);
        addContentButton(layout, x + (buttonWidth + 9) * 2, buttonY, buttonWidth, 26, "离开队伍", "", widget -> sendTeamAction(TeamAction.LEAVE), ButtonVariant.DANGER, serverSync.playerInTeam());
        markContentBottom(layout, buttonY + 34);
    }

    private void buildConfigPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.contentWidth();
        addBox(x, layout.contentY(), w, layout.contentHeight());

        if (editableConfig == null) {
            addContentLabel(layout, "等待配置同步。", x + 12, y + 14, 0xFFC9D4DE);
            markContentBottom(layout, y + 40);
            return;
        }

        addContentLabel(layout, canManage ? "配置模式: 可修改" : "配置模式: 只读", x + 12, y + 10, canManage ? 0xFF77E287 : 0xFFFFC2C8);

        int gap = 16;
        int columnWidth = Math.max(180, (w - 24 - gap) / 2);
        int leftX = x + 12;
        int rightX = leftX + columnWidth + gap;
        int rowY = y + 28;
        int rowGap = Math.max(17, Math.min(20, (layout.contentHeight() - 66) / Math.max(1, NumberField.values().length)));
        int fieldHeight = rowGap <= 18 ? 16 : 18;

        addContentLabel(layout, "时间配置", leftX, rowY, 0xFFFFFFFF, true);
        int numberY = rowY + 18;
        for (NumberField field : NumberField.values()) {
            addContentNumberField(layout, field, leftX, numberY, columnWidth, fieldHeight);
            numberY += rowGap;
        }

        addContentLabel(layout, "外卡开关", rightX, rowY, 0xFFFFFFFF, true);
        int toggleAreaWidth = Math.min(columnWidth, w - (rightX - x) - 12);
        int toggleBottom = rowY + 18;
        if (toggleAreaWidth >= 300) {
            int toggleColumnGap = 8;
            int toggleWidth = (toggleAreaWidth - toggleColumnGap) / 2;
            int toggleY = rowY + 18;
            ToggleField[] fields = ToggleField.values();
            for (int i = 0; i < fields.length; i++) {
                int column = i / 4;
                int row = i % 4;
                int fieldY = toggleY + row * 24;
                addContentToggleField(layout, fields[i], rightX + column * (toggleWidth + toggleColumnGap), fieldY, toggleWidth, 20);
                toggleBottom = Math.max(toggleBottom, fieldY + 20);
            }
        } else {
            int toggleHeight = 16;
            int toggleGap = Math.max(17, Math.min(19, (layout.contentHeight() - 66) / Math.max(1, ToggleField.values().length)));
            int toggleY = rowY + 18;
            for (ToggleField field : ToggleField.values()) {
                addContentToggleField(layout, field, rightX, toggleY, toggleAreaWidth, toggleHeight);
                toggleBottom = toggleY + toggleHeight;
                toggleY += toggleGap;
            }
        }
        markContentBottom(layout, Math.max(numberY, toggleBottom) + 12);
    }

    private void buildDebugPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.contentWidth();
        addBox(x, layout.contentY(), w, layout.contentHeight());

        if (!isDebugPageEnabled()) {
            addContentLabel(layout, "调试页未开启。", x + 12, y + 14, 0xFFFFC2C8);
            markContentBottom(layout, y + 40);
            return;
        }

        addContentLabel(layout, "调试操作", x + 12, y + 12, 0xFFFFFFFF, true);
        addContentLabel(layout, canManage ? "这些操作会发送到服务端执行。" : "只有 OP 可以执行调试操作。", x + 12, y + 32, canManage ? 0xFFC9D4DE : 0xFFFFC2C8);

        int buttonY = y + 58;
        int buttonWidth = Math.max(100, Math.min(136, (w - 36) / 2));
        addContentButton(layout, x + 12, buttonY, buttonWidth, 28, "开始游戏", "", widget -> sendDebugAction(DebugAction.START_GAME), ButtonVariant.NORMAL, canManage && serverSync != null && serverSync.gameState() == GameState.WAITING);
        addContentButton(layout, x + 24 + buttonWidth, buttonY, buttonWidth, 28, "停止游戏", "", widget -> sendDebugAction(DebugAction.STOP_GAME), ButtonVariant.DANGER, canManage && isGameActive());
        addContentButton(layout, x + 12, buttonY + 36, buttonWidth, 28, "随机外卡", "", widget -> sendDebugAction(DebugAction.ROLL_WILDCARD), ButtonVariant.NORMAL, canManage && serverSync != null && serverSync.gameState() == GameState.RUNNING);
        addContentButton(layout, x + 24 + buttonWidth, buttonY + 36, buttonWidth, 28, "停止外卡", "", widget -> sendDebugAction(DebugAction.STOP_WILDCARD), ButtonVariant.DANGER, canManage && serverSync != null && serverSync.activeWildcardRunning());

        int testTitleY = buttonY + 84;
        addContentLabel(layout, "单独测试外卡", x + 12, testTitleY, 0xFFFFFFFF, true);
        int columns = w >= 360 ? 2 : 1;
        int testGap = 10;
        int testButtonWidth = Math.max(110, (w - 24 - (columns - 1) * testGap) / columns);
        int testButtonY = testTitleY + 18;
        ToggleField[] fields = ToggleField.values();
        int testBottom = testButtonY;
        for (int i = 0; i < fields.length; i++) {
            ToggleField field = fields[i];
            int column = i % columns;
            int row = i / columns;
            int fieldX = x + 12 + column * (testButtonWidth + testGap);
            int fieldY = testButtonY + row * 32;
            boolean enabled = editableConfig != null && getToggle(editableConfig, field);
            addContentButton(
                    layout,
                    fieldX,
                    fieldY,
                    testButtonWidth,
                    26,
                    enabled ? "测试 " + field.label : field.label + " 已关闭",
                    "",
                    widget -> sendTestWildcard(field),
                    ButtonVariant.NORMAL,
                    canManage && enabled
            );
            testBottom = Math.max(testBottom, fieldY + 26);
        }
        markContentBottom(layout, testBottom + 12);
    }

    private void buildFooter(Layout layout) {
        int y = layout.panelY() + layout.panelHeight() - 32;
        if (currentPage == Page.CONFIG) {
            int buttonWidth = Math.max(76, Math.min(108, (layout.contentWidth() - 16) / 3));
            int x = layout.contentX() + Math.max(0, layout.contentWidth() - (buttonWidth * 3 + 16));
            addButton(x, y, buttonWidth, 24, "保存配置", "", widget -> saveConfig(), ButtonVariant.NORMAL, canManage && editableConfig != null);
            addButton(x + buttonWidth + 8, y, buttonWidth, 24, "重新加载", "", widget -> reloadConfig(), ButtonVariant.NORMAL, canManage);
            addButton(x + (buttonWidth + 8) * 2, y, buttonWidth, 24, "关闭", "", widget -> close(), ButtonVariant.NORMAL, true);
            return;
        }

        addButton(layout.contentX() + layout.contentWidth() - 86, y, 86, 24, "关闭", "", widget -> close(), ButtonVariant.NORMAL, true);
    }

    private void addNumberField(NumberField field, int x, int y, int width, int fieldHeight) {
        int fieldWidth = 52;
        int fieldX = x + width - fieldWidth - 18;
        int labelY = y + Math.max(3, (fieldHeight - textRenderer.fontHeight) / 2);
        labels.add(new Label(field.label, x, labelY, 0xFFC9D4DE, false, Math.max(50, fieldX - x - 8)));
        labels.add(new Label("秒", fieldX + fieldWidth + 5, labelY, 0xFFC9D4DE));

        TextFieldWidget textField = new TextFieldWidget(textRenderer, fieldX, y, fieldWidth, fieldHeight, Text.literal(field.label));
        textField.setMaxLength(7);
        textField.setText(Integer.toString(getNumber(editableConfig, field)));
        textField.setEditable(canManage);
        textField.active = canManage;
        numberFields.put(field, textField);
        addDrawableChild(textField);
    }

    private void addToggleField(ToggleField field, int x, int y, int width, int height) {
        boolean enabled = getToggle(editableConfig, field);
        addButton(
                x,
                y,
                width,
                height,
                field.label + ": " + (enabled ? "开启" : "关闭"),
                "",
                widget -> toggleField(field),
                enabled ? ButtonVariant.TOGGLE_ON : ButtonVariant.TOGGLE_OFF,
                canManage
        );
    }

    private void addContentLabel(Layout layout, String text, int x, int y, int color) {
        addContentLabel(layout, text, x, y, color, false);
    }

    private void addContentLabel(Layout layout, String text, int x, int y, int color, boolean shadow) {
        if (isVisibleInContent(layout, y, textRenderer.fontHeight)) {
            labels.add(new Label(text, x, y, color, shadow));
        }
    }

    private void addContentNumberField(Layout layout, NumberField field, int x, int y, int width, int fieldHeight) {
        if (isVisibleInContent(layout, y, fieldHeight)) {
            addNumberField(field, x, y, width, fieldHeight);
        }
    }

    private void addContentToggleField(Layout layout, ToggleField field, int x, int y, int width, int height) {
        if (isVisibleInContent(layout, y, height)) {
            addToggleField(field, x, y, width, height);
        }
    }

    private void addContentButton(Layout layout, int x, int y, int width, int height, String title, String description, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled) {
        if (isVisibleInContent(layout, y, height)) {
            addButton(x, y, width, height, title, description, action, variant, enabled);
        }
    }

    private void addButton(int x, int y, int width, int height, String title, String description, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled) {
        StyledButtonWidget button = new StyledButtonWidget(x, y, width, height, title, description, action, variant);
        button.active = enabled;
        addDrawableChild(button);
    }

    private void addBox(int x, int y, int width, int height) {
        boxes.add(new Box(x, y, width, height, 0x88303A46, 0xFF4C5A66));
    }

    private int pageTop(Layout layout) {
        return layout.contentY() - contentScroll;
    }

    private void markContentBottom(Layout layout, int screenBottomY) {
        pageContentHeight = Math.max(pageContentHeight, screenBottomY - pageTop(layout));
    }

    private boolean isVisibleInContent(Layout layout, int y, int height) {
        return y >= layout.contentY() && y + height <= layout.contentBottom();
    }

    private boolean isInsideContent(Layout layout, double mouseX, double mouseY) {
        return mouseX >= layout.contentX()
                && mouseX <= layout.contentX() + layout.contentWidth()
                && mouseY >= layout.contentY()
                && mouseY <= layout.contentBottom();
    }

    private int maxContentScroll(Layout layout) {
        return Math.max(0, pageContentHeight - layout.contentHeight());
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void renderScrollBar(DrawContext context, Layout layout) {
        int maxScroll = maxContentScroll(layout);
        if (maxScroll <= 0) {
            return;
        }

        int trackX = layout.contentX() + layout.contentWidth() - 4;
        int trackTop = layout.contentY() + 2;
        int trackHeight = Math.max(16, layout.contentHeight() - 4);
        int thumbHeight = Math.max(18, trackHeight * layout.contentHeight() / Math.max(layout.contentHeight(), pageContentHeight));
        int thumbY = trackTop + (trackHeight - thumbHeight) * contentScroll / maxScroll;
        context.fill(trackX, trackTop, trackX + 2, trackTop + trackHeight, 0x664C5A66);
        context.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xCC7FC2FF);
    }

    private void switchPage(Page page) {
        if (page == currentPage) {
            return;
        }

        if (!applyVisibleInputs(false)) {
            return;
        }

        currentPage = page;
        contentScroll = 0;
        clearAndInit();
    }

    private void toggleField(ToggleField field) {
        if (!canManage || editableConfig == null || !applyVisibleInputs(true)) {
            return;
        }

        editableConfig = setToggle(editableConfig, field, !getToggle(editableConfig, field));
        clearAndInit();
    }

    private boolean applyVisibleInputs(boolean showErrors) {
        if (editableConfig == null) {
            return true;
        }

        ConfigSnapshot updated = editableConfig;
        for (Map.Entry<NumberField, TextFieldWidget> entry : numberFields.entrySet()) {
            String raw = entry.getValue().getText().trim();
            int value;
            try {
                value = Integer.parseInt(raw);
            } catch (NumberFormatException exception) {
                statusMessage = showErrors
                        ? entry.getKey().label + " 必须是整数。"
                        : "请先修正当前页的数字输入。";
                return false;
            }

            if (value <= 0) {
                statusMessage = entry.getKey().label + " 必须大于 0。";
                return false;
            }

            updated = setNumber(updated, entry.getKey(), value);
        }

        editableConfig = updated;
        return true;
    }

    private void saveConfig() {
        if (!canManage) {
            statusMessage = "只有 OP 可以保存配置。";
            return;
        }

        if (!applyVisibleInputs(true) || editableConfig == null) {
            return;
        }

        sendPayload(new HunterWildcardPackets.UpdateConfigPayload(editableConfig), HunterWildcardPackets.C2S_UPDATE_CONFIG, "已提交保存请求。");
    }

    private void reloadConfig() {
        if (!canManage) {
            statusMessage = "只有 OP 可以重新加载配置。";
            return;
        }

        sendPayload(new HunterWildcardPackets.ReloadConfigPayload(), HunterWildcardPackets.C2S_RELOAD_CONFIG, "已提交重新加载请求。");
    }

    private void requestConfig() {
        requestConfig(true);
    }

    private void requestConfig(boolean updateMessage) {
        sendPayload(new HunterWildcardPackets.RequestConfigPayload(), HunterWildcardPackets.C2S_REQUEST_CONFIG, "正在等待服务器同步...", updateMessage);
    }

    private void sendDebugAction(DebugAction action) {
        if (!canManage) {
            statusMessage = "只有 OP 可以执行调试操作。";
            return;
        }

        if (!isDebugPageEnabled()) {
            statusMessage = "请先使用 /hw ts true 打开调试页。";
            return;
        }

        sendPayload(new HunterWildcardPackets.DebugActionPayload(action), HunterWildcardPackets.C2S_DEBUG_ACTION, "已提交调试操作。");
    }

    private void sendTestWildcard(ToggleField field) {
        if (!canManage) {
            statusMessage = "只有 OP 可以测试外卡。";
            return;
        }

        if (!isDebugPageEnabled()) {
            statusMessage = "请先使用 /hw ts true 打开调试页。";
            return;
        }

        sendPayload(new HunterWildcardPackets.TestWildcardPayload(field.label), HunterWildcardPackets.C2S_TEST_WILDCARD, "已提交外卡测试: " + field.label);
    }

    private void sendTeamAction(TeamAction action) {
        sendPayload(new HunterWildcardPackets.TeamActionPayload(action), HunterWildcardPackets.C2S_TEAM_ACTION, "已提交队伍操作。");
    }

    private void sendGameAction(GameAction action) {
        sendPayload(new HunterWildcardPackets.GameActionPayload(action), HunterWildcardPackets.C2S_GAME_ACTION, "已提交游戏操作。");
    }

    private void sendPayload(CustomPayload payload, CustomPayload.Id<?> id, String successMessage) {
        sendPayload(payload, id, successMessage, true);
    }

    private void sendPayload(CustomPayload payload, CustomPayload.Id<?> id, String successMessage, boolean updateMessage) {
        if (!canSend(id)) {
            if (updateMessage) {
                statusMessage = "当前服务器未启用猎人外卡同步。";
            }
            return;
        }

        try {
            ClientPlayNetworking.send(payload);
            if (updateMessage) {
                statusMessage = successMessage;
            }
        } catch (IllegalStateException exception) {
            if (updateMessage) {
                statusMessage = "当前未连接到服务器。";
            }
        }
    }

    private boolean canSend(CustomPayload.Id<?> id) {
        try {
            return ClientPlayNetworking.canSend(id);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return false;
        }
    }

    private void applySync(SyncConfigPayload payload) {
        serverSync = payload;
        editableConfig = payload.config();
        canManage = payload.canManage();
        if (!payload.debugPageEnabled() && currentPage == Page.DEBUG) {
            currentPage = Page.GAME;
        }
        statusMessage = "已同步服务器数据。";
        clearAndInit();
    }

    private boolean isDebugPageEnabled() {
        return serverSync != null && serverSync.debugPageEnabled();
    }

    private boolean isGameActive() {
        return serverSync != null && serverSync.gameState() != GameState.WAITING;
    }

    private List<Page> visiblePages() {
        List<Page> pages = new ArrayList<>();
        pages.add(Page.GAME);
        pages.add(Page.TEAM);
        pages.add(Page.CONFIG);
        if (isDebugPageEnabled()) {
            pages.add(Page.DEBUG);
        }
        return pages;
    }

    private int getNumber(ConfigSnapshot config, NumberField field) {
        return switch (field) {
            case PREPARING_SECONDS -> config.preparingSeconds();
            case ENDING_SECONDS -> config.endingSeconds();
            case COMPASS_UPDATE_SECONDS -> config.compassUpdateSeconds();
            case HUNTER_RESPAWN_SECONDS -> config.hunterRespawnSeconds();
            case WILDCARD_INTERVAL_SECONDS -> config.wildcardIntervalSeconds();
            case WILDCARD_DURATION_SECONDS -> config.wildcardDurationSeconds();
            case ACTION_BAR_INTERVAL_SECONDS -> config.actionBarIntervalSeconds();
            case HUNTER_RADAR_INTERVAL_SECONDS -> config.hunterRadarIntervalSeconds();
            case SUPPLY_DROP_INTERVAL_SECONDS -> config.supplyDropIntervalSeconds();
        };
    }

    private ConfigSnapshot setNumber(ConfigSnapshot config, NumberField field, int value) {
        return new ConfigSnapshot(
                field == NumberField.PREPARING_SECONDS ? value : config.preparingSeconds(),
                field == NumberField.ENDING_SECONDS ? value : config.endingSeconds(),
                field == NumberField.COMPASS_UPDATE_SECONDS ? value : config.compassUpdateSeconds(),
                field == NumberField.HUNTER_RESPAWN_SECONDS ? value : config.hunterRespawnSeconds(),
                field == NumberField.WILDCARD_INTERVAL_SECONDS ? value : config.wildcardIntervalSeconds(),
                field == NumberField.WILDCARD_DURATION_SECONDS ? value : config.wildcardDurationSeconds(),
                field == NumberField.ACTION_BAR_INTERVAL_SECONDS ? value : config.actionBarIntervalSeconds(),
                field == NumberField.HUNTER_RADAR_INTERVAL_SECONDS ? value : config.hunterRadarIntervalSeconds(),
                field == NumberField.SUPPLY_DROP_INTERVAL_SECONDS ? value : config.supplyDropIntervalSeconds(),
                config.enableSpeedRush(),
                config.enableFeatherweight(),
                config.enableGlowing(),
                config.enableNightHunt(),
                config.enableExplosiveDeath(),
                config.enableSupplyDrop(),
                config.enableHunterRadar(),
                config.enableCompassChaos()
        );
    }

    private boolean getToggle(ConfigSnapshot config, ToggleField field) {
        return switch (field) {
            case SPEED_RUSH -> config.enableSpeedRush();
            case FEATHERWEIGHT -> config.enableFeatherweight();
            case GLOWING -> config.enableGlowing();
            case NIGHT_HUNT -> config.enableNightHunt();
            case EXPLOSIVE_DEATH -> config.enableExplosiveDeath();
            case SUPPLY_DROP -> config.enableSupplyDrop();
            case HUNTER_RADAR -> config.enableHunterRadar();
            case COMPASS_CHAOS -> config.enableCompassChaos();
        };
    }

    private ConfigSnapshot setToggle(ConfigSnapshot config, ToggleField field, boolean value) {
        return new ConfigSnapshot(
                config.preparingSeconds(),
                config.endingSeconds(),
                config.compassUpdateSeconds(),
                config.hunterRespawnSeconds(),
                config.wildcardIntervalSeconds(),
                config.wildcardDurationSeconds(),
                config.actionBarIntervalSeconds(),
                config.hunterRadarIntervalSeconds(),
                config.supplyDropIntervalSeconds(),
                field == ToggleField.SPEED_RUSH ? value : config.enableSpeedRush(),
                field == ToggleField.FEATHERWEIGHT ? value : config.enableFeatherweight(),
                field == ToggleField.GLOWING ? value : config.enableGlowing(),
                field == ToggleField.NIGHT_HUNT ? value : config.enableNightHunt(),
                field == ToggleField.EXPLOSIVE_DEATH ? value : config.enableExplosiveDeath(),
                field == ToggleField.SUPPLY_DROP ? value : config.enableSupplyDrop(),
                field == ToggleField.HUNTER_RADAR ? value : config.enableHunterRadar(),
                field == ToggleField.COMPASS_CHAOS ? value : config.enableCompassChaos()
        );
    }

    private String stateName(GameState state) {
        return switch (state) {
            case WAITING -> "等待中";
            case PREPARING -> "准备中";
            case RUNNING -> "运行中";
            case ENDING -> "结算中";
        };
    }

    private String trim(String text, int width) {
        return textRenderer.trimToWidth(text, Math.max(10, width));
    }

    private Layout layout() {
        int panelWidth = Math.min(720, Math.max(320, width - 24));
        if (panelWidth > width - 8) {
            panelWidth = Math.max(220, width - 8);
        }

        int panelHeight = Math.min(390, Math.max(240, height - 24));
        if (panelHeight > height - 8) {
            panelHeight = Math.max(200, height - 8);
        }

        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int navWidth = panelWidth < 520 ? 96 : 120;
        int contentX = panelX + navWidth + 18;
        int contentY = panelY + 48;
        int contentWidth = panelX + panelWidth - contentX - 14;
        int contentBottom = panelY + panelHeight - 66;
        return new Layout(panelX, panelY, panelWidth, panelHeight, navWidth, contentX, contentY, contentWidth, contentBottom);
    }

    private enum Page {
        GAME("游戏"),
        TEAM("队伍"),
        CONFIG("配置"),
        DEBUG("调试");

        private final String label;

        Page(String label) {
            this.label = label;
        }
    }

    private enum NumberField {
        PREPARING_SECONDS("准备时间"),
        ENDING_SECONDS("结算时间"),
        COMPASS_UPDATE_SECONDS("指南针刷新"),
        HUNTER_RESPAWN_SECONDS("猎人复活"),
        WILDCARD_INTERVAL_SECONDS("外卡间隔"),
        WILDCARD_DURATION_SECONDS("外卡持续"),
        ACTION_BAR_INTERVAL_SECONDS("状态栏刷新"),
        HUNTER_RADAR_INTERVAL_SECONDS("雷达播报"),
        SUPPLY_DROP_INTERVAL_SECONDS("空投间隔");

        private final String label;

        NumberField(String label) {
            this.label = label;
        }
    }

    private enum ToggleField {
        SPEED_RUSH("疾速追猎"),
        FEATHERWEIGHT("轻盈之身"),
        GLOWING("全员发光"),
        NIGHT_HUNT("暗夜追猎"),
        EXPLOSIVE_DEATH("死亡爆炸"),
        SUPPLY_DROP("补给空投"),
        HUNTER_RADAR("猎人雷达"),
        COMPASS_CHAOS("指南针干扰");

        private final String label;

        ToggleField(String label) {
            this.label = label;
        }
    }

    private enum ButtonVariant {
        NORMAL,
        SELECTED,
        DANGER,
        TOGGLE_ON,
        TOGGLE_OFF,
        DISABLED
    }

    private record Layout(int panelX, int panelY, int panelWidth, int panelHeight, int navWidth, int contentX, int contentY, int contentWidth, int contentBottom) {
        int contentHeight() {
            return contentBottom - contentY;
        }
    }

    private record Label(String text, int x, int y, int color, boolean shadow, int width) {
        Label(String text, int x, int y, int color) {
            this(text, x, y, color, false, 0);
        }

        Label(String text, int x, int y, int color, boolean shadow) {
            this(text, x, y, color, shadow, 0);
        }

        int maxWidth(Layout layout) {
            return width > 0 ? width : Math.max(20, layout.panelX() + layout.panelWidth() - x - 12);
        }
    }

    private record Box(int x, int y, int width, int height, int color, int borderColor) {
    }

    private class StyledButtonWidget extends ButtonWidget {
        private final String description;
        private final ButtonVariant variant;

        StyledButtonWidget(int x, int y, int width, int height, String title, String description, ButtonWidget.PressAction action, ButtonVariant variant) {
            super(x, y, width, height, net.minecraft.text.Text.literal(title), action, DEFAULT_NARRATION_SUPPLIER);
            this.description = description == null ? "" : description;
            this.variant = variant == null ? ButtonVariant.NORMAL : variant;
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            ButtonVariant renderedVariant = active || variant == ButtonVariant.SELECTED ? variant : ButtonVariant.DISABLED;
            Palette palette = palette(renderedVariant, isHovered());
            int x = getX();
            int y = getY();
            int width = getWidth();
            int height = getHeight();

            context.fill(x, y, x + width, y + height, palette.background);
            context.fill(x, y, x + width, y + 1, palette.border);
            context.fill(x, y + height - 1, x + width, y + height, palette.border);
            context.fill(x, y, x + 1, y + height, palette.border);
            context.fill(x + width - 1, y, x + width, y + height, palette.border);
            if (renderedVariant != ButtonVariant.NORMAL) {
                context.fill(x + 2, y + 2, x + 5, y + height - 2, palette.accent);
            }

            String title = trim(getMessage().getString(), width - 12);
            if (!description.isBlank() && height >= 34) {
                int textX = renderedVariant == ButtonVariant.NORMAL ? x + 8 : x + 11;
                context.drawText(textRenderer, net.minecraft.text.Text.literal(title), textX, y + 7, palette.titleColor, true);
                context.drawText(textRenderer, net.minecraft.text.Text.literal(trim(description, width - 20)), textX, y + 23, palette.descriptionColor, false);
                return;
            }

            int titleX = x + Math.max(4, (width - textRenderer.getWidth(title)) / 2);
            int titleY = y + Math.max(4, (height - textRenderer.fontHeight) / 2);
            context.drawText(textRenderer, net.minecraft.text.Text.literal(title), titleX, titleY, palette.titleColor, true);
        }

        @Override
        protected void drawLabel(net.minecraft.client.font.DrawnTextConsumer textConsumer) {
        }

        private Palette palette(ButtonVariant variant, boolean hovered) {
            return switch (variant) {
                case SELECTED -> new Palette(0xAA345B78, 0xFF7FC2FF, 0xFF7FC2FF, 0xFFFFFFFF, 0xFFD7ECFF);
                case DANGER -> new Palette(hovered ? 0xAA6D3434 : 0x8845292F, hovered ? 0xFFFF8A8A : 0xFFD76474, 0xFFFF8A8A, 0xFFFFFFFF, 0xFFFFC2C8);
                case TOGGLE_ON -> new Palette(hovered ? 0xAA2E5C49 : 0x88324B3F, hovered ? 0xFF77E287 : 0xFF55B978, 0xFF77E287, 0xFFFFFFFF, 0xFFD7F8E1);
                case TOGGLE_OFF -> new Palette(hovered ? 0xAA6D3434 : 0x8845292F, hovered ? 0xFFFF8A8A : 0xFFD76474, 0xFFFF8A8A, 0xFFFFFFFF, 0xFFFFC2C8);
                case DISABLED -> new Palette(0x66303A46, 0xFF59636C, 0xFF59636C, 0xFF9FAAB4, 0xFF9FAAB4);
                default -> new Palette(hovered ? 0xAA3E5570 : 0x88303A46, hovered ? 0xFF74B6FF : 0xFF4C5A66, 0xFF74B6FF, 0xFFFFFFFF, 0xFFC9D4DE);
            };
        }
    }

    private record Palette(int background, int border, int accent, int titleColor, int descriptionColor) {
    }
}
