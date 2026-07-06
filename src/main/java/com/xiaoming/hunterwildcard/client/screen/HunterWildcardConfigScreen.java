package com.xiaoming.hunterwildcard.client.screen;

import com.xiaoming.hunterwildcard.client.hud.WildcardIcons;
import com.xiaoming.hunterwildcard.client.screen.widget.DropdownWidget;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.game.HunterVictoryType;
import com.xiaoming.hunterwildcard.game.RunnerVictoryType;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.ConfigSnapshot;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.DebugAction;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.GameAction;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.OperationResultPayload;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.SyncConfigPayload;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets.TeamAction;
import com.xiaoming.hunterwildcard.respawn.RespawnMode;
import com.xiaoming.hunterwildcard.respawn.RunnerTeamLossMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class HunterWildcardConfigScreen extends Screen {
    private static final int CARD_PADDING_X = 10;
    private static final int CARD_PADDING_TOP = 10;
    private static final int CARD_PADDING_BOTTOM = 8;
    private static final int CARD_TITLE_HEIGHT = 13;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 3;
    private static final int CARD_GAP = 8;
    private static final int COMPACT_CARD_GAP = 12;
    private static final int SMALL_CARD_MAX_WIDTH = 300;
    private static final int MEDIUM_CARD_MAX_WIDTH = 320;
    private static final int LARGE_CARD_MAX_WIDTH = 420;
    private static final int STATUS_BLOCK_MAX_WIDTH = 160;
    private static final int WILDCARD_TOGGLE_TARGET_WIDTH = 220;
    private static final int WILDCARD_TOGGLE_MAX_WIDTH = 260;
    private static final int WILDCARD_TOGGLE_HEIGHT = 32;
    private static final int TWO_COLUMN_GAP = COMPACT_CARD_GAP;
    private static final int TWO_COLUMN_MIN_WIDTH = 460;
    private static final int LABEL_WIDTH = 112;
    private static final int CONTROL_WIDTH = 104;
    private static final int DROPDOWN_WIDTH = 180;
    private static final int UNIT_WIDTH = 24;
    private static final int CONTROL_HEIGHT = 18;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_GAP = 6;
    private static final int HINT_HEIGHT = 12;
    private static final int SCROLL_STEP = 32;
    private static final int SCROLL_BAR_RESERVE = 12;
    private static final int REALTIME_REFRESH_TICKS = 5;
    private static final int CONFIG_IDLE_REFRESH_TICKS = 10;
    private static final long TOAST_FADE_IN_MS = 140L;
    private static final long TOAST_HOLD_MS = 1800L;
    private static final long TOAST_FADE_OUT_MS = 220L;
    private static ConfigSnapshot cachedEditableConfig;

    private final List<Label> labels = new ArrayList<>();
    private final List<WrappedLabel> wrappedLabels = new ArrayList<>();
    private final List<Box> boxes = new ArrayList<>();
    private final List<Icon> icons = new ArrayList<>();
    private final Map<NumberField, TextFieldWidget> numberFields = new EnumMap<>(NumberField.class);
    private final Map<StringField, TextFieldWidget> stringFields = new EnumMap<>(StringField.class);
    private final Map<DropdownField, DropdownWidget> dropdownFields = new EnumMap<>(DropdownField.class);
    private final Map<Page, Float> pageScrollOffsets = new EnumMap<>(Page.class);
    private final Map<Page, Float> pageTargetScrollOffsets = new EnumMap<>(Page.class);

    private Page currentPage = Page.GAME;
    private SyncConfigPayload serverSync;
    private ConfigSnapshot editableConfig;
    private boolean canManage;
    private boolean requested;
    private int refreshTicks;
    private float scrollOffset;
    private float targetScrollOffset;
    private float maxScroll;
    private float navScroll;
    private float maxNavScroll;
    private int renderedScroll;
    private int pageContentHeight;
    private String statusMessage = "正在请求服务器数据...";
    private StatusKind statusKind = StatusKind.INFO;
    private String toastMessage = "";
    private StatusKind toastKind = StatusKind.INFO;
    private long toastStartTimeMs;
    private long toastDurationMs;
    private String hoverTooltip = "";
    private int hoverTooltipX;
    private int hoverTooltipY;
    private boolean hasSyncedOnce;
    private boolean manualReloadRequested;
    private boolean manualSaveRequested;
    private StyledButtonWidget saveButton;
    private ToggleField selectedWildcardSettings;

    public HunterWildcardConfigScreen() {
        super(Text.literal("猎人外卡"));
    }

    public static void receiveSync(SyncConfigPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HunterWildcardConfigScreen screen) {
            screen.applySync(payload);
        }
    }

    public static void receiveOperationResult(OperationResultPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HunterWildcardConfigScreen screen) {
            if (payload.success()) {
                cachedEditableConfig = null;
                screen.showToast(payload.message(), StatusKind.SUCCESS);
            } else {
                screen.showToast(payload.message(), StatusKind.ERROR);
            }
        }
    }

    public static void closeFromServer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HunterWildcardConfigScreen) {
            client.setScreen(null);
        }
    }

    @Override
    protected void init() {
        labels.clear();
        wrappedLabels.clear();
        boxes.clear();
        icons.clear();
        numberFields.clear();
        stringFields.clear();
        dropdownFields.clear();
        saveButton = null;

        Layout layout = layout();
        clampScroll(layout);
        renderedScroll = Math.round(scrollOffset);
        int buildScroll = renderedScroll;
        pageContentHeight = layout.viewportHeight();
        ensureVisiblePage();
        buildNavigation(layout);

        switch (currentPage) {
            case GAME -> buildGamePage(layout);
            case TEAM -> buildTeamPage(layout);
            case BASIC -> buildBasicPage(layout);
            case VICTORY -> buildVictoryPage(layout);
            case RESPAWN -> buildRespawnPage(layout);
            case WILDCARD -> buildWildcardPage(layout);
            case DEBUG -> buildDebugPage(layout);
        }

        updateMaxScroll(layout);
        if (renderedScroll != buildScroll) {
            clearAndInit();
            return;
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
        if (updateSmoothScroll(layout, delta)) {
            clearAndInit();
            layout = layout();
        }

        context.fill(0, 0, width, height, 0x88000000);
        context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.panelWidth(), layout.panelY() + layout.panelHeight(), 0xD0161B22);
        context.fill(layout.panelX(), layout.panelY(), layout.panelX() + layout.navWidth(), layout.panelY() + layout.panelHeight(), 0xE01E252D);
        context.fill(layout.panelX() + layout.navWidth(), layout.panelY(), layout.panelX() + layout.navWidth() + 1, layout.panelY() + layout.panelHeight(), 0xFF35404B);

        context.drawText(textRenderer, Text.literal("猎人外卡"), layout.panelX() + 12, layout.panelY() + 13, 0xFFFFFFFF, true);
        context.drawText(textRenderer, Text.literal(currentPage.label), layout.contentX(), layout.panelY() + 14, 0xFFFFFFFF, true);
        context.drawText(textRenderer, Text.literal(currentPage.description), layout.contentX(), layout.panelY() + 29, 0xFF9FAAB4, false);

        context.enableScissor(layout.contentX(), layout.viewportTop(), layout.contentX() + layout.usableContentWidth(), layout.viewportBottom());
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
        for (WrappedLabel label : wrappedLabels) {
            renderWrappedLabel(context, layout, label);
        }
        for (Icon icon : icons) {
            context.drawItem(icon.stack, icon.x, icon.y);
        }
        context.disableScissor();

        renderFooterStatus(context, layout);
        renderScrollBar(context, layout);
        renderNavigationScrollBar(context, layout);

        hoverTooltip = "";
        if (saveButton != null) {
            saveButton.active = canSaveConfig();
        }
        super.render(context, mouseX, mouseY, delta);
        renderToast(context, layout, delta);
        renderDropdownOverlays(context, mouseX, mouseY, delta);
        renderHoverTooltip(context);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        for (DropdownWidget dropdown : dropdownFields.values()) {
            if (dropdown.isExpanded() && dropdown.containsPoint(mouseX, mouseY)) {
                if (dropdown.mouseClicked(click, doubled)) {
                    return true;
                }
            }
        }

        for (DropdownWidget dropdown : dropdownFields.values()) {
            if (dropdown.containsPoint(mouseX, mouseY)) {
                if (dropdown.mouseClicked(click, doubled)) {
                    return true;
                }
            }
        }

        closeDropdowns();
        boolean handled = super.mouseClicked(click, doubled);
        if (handled && isInsideContent(layout(), mouseX, mouseY)) {
            ensureFocusedInputVisible(layout());
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Layout layout = layout();
        if (isInsideNavigation(layout, mouseX, mouseY)) {
            updateNavigationScroll(layout);
            if (maxNavScroll > 0.5F) {
                float delta = (float) (verticalAmount * SCROLL_STEP);
                if (Math.abs(delta) < 0.5F) {
                    delta = verticalAmount > 0 ? SCROLL_STEP : -SCROLL_STEP;
                }
                navScroll = clamp(navScroll - delta, 0.0F, maxNavScroll);
                clearAndInit();
                return true;
            }
        }

        if (!isInsideContent(layout, mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        updateMaxScroll(layout);
        if (maxScroll <= 0.5F) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (isConfigEditPage() && !applyVisibleInputs(false)) {
            return true;
        }

        closeDropdowns();
        float delta = (float) (verticalAmount * SCROLL_STEP);
        if (Math.abs(delta) < 0.5F) {
            delta = verticalAmount > 0 ? SCROLL_STEP : -SCROLL_STEP;
        }
        targetScrollOffset = clamp(targetScrollOffset - delta, 0.0F, maxScroll);
        rememberCurrentScroll();
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (isConfigEditPage() && editableConfig != null) {
            if (!applyVisibleInputs(true)) {
                return;
            }
            cachedEditableConfig = editableConfig;
        }

        closeDropdowns();
        super.close();
    }

    @Override
    public void tick() {
        int refreshInterval = refreshIntervalTicks();
        if (refreshInterval <= 0) {
            refreshTicks = 0;
            return;
        }

        refreshTicks++;
        if (refreshTicks >= refreshInterval) {
            refreshTicks = 0;
            requestConfig(false);
        }
    }

    private int refreshIntervalTicks() {
        if (isRealtimeStatusPage()) {
            return REALTIME_REFRESH_TICKS;
        }

        if (isConfigEditPage()) {
            return hasUnsavedChanges() || hasVisibleInputChanges() || hasFocusedTextField() || hasExpandedDropdown() ? -1 : CONFIG_IDLE_REFRESH_TICKS;
        }

        return CONFIG_IDLE_REFRESH_TICKS;
    }

    private boolean isRealtimeStatusPage() {
        return currentPage == Page.GAME || currentPage == Page.TEAM || currentPage == Page.DEBUG;
    }

    private void buildNavigation(Layout layout) {
        int x = layout.panelX() + 10;
        int navTop = navigationTop(layout);
        int navBottom = navigationBottom(layout);
        int buttonHeight = 24;
        int buttonGap = 6;
        updateNavigationScroll(layout);
        int y = navTop - Math.round(navScroll);
        for (Page page : visiblePages()) {
            if (y + buttonHeight < navTop || y > navBottom) {
                y += buttonHeight + buttonGap;
                continue;
            }

            StyledButtonWidget button = new StyledButtonWidget(
                    x,
                    y,
                    layout.navWidth() - 20,
                    buttonHeight,
                    page.label,
                    "",
                    widget -> switchPage(page),
                    page == currentPage ? ButtonVariant.SELECTED : ButtonVariant.NORMAL
            );
            button.active = true;
            addDrawableChild(button);
            y += buttonHeight + buttonGap;
        }
    }

    private void buildGamePage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (serverSync == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, "游戏状态");
            card.hint("等待服务器同步。");
            markContentBottom(layout, card.finish());
            return;
        }

        String startTooltip = startGameTooltip(serverSync.gameState() == GameState.WAITING);
        int currentY = addStatusPills(layout, x, y, w, List.of(
                new StatusBlock("身份", serverSync.playerRole(), serverSync.playerInTeam() ? 0xFFFFFFFF : 0xFFFFD966),
                new StatusBlock("权限", serverSync.canManage() ? "OP" : "普通", serverSync.canManage() ? 0xFF77E287 : 0xFFC9D4DE),
                new StatusBlock("猎人", serverSync.hunterCount() + "人", serverSync.hunterCount() > 0 ? 0xFF77E287 : 0xFFFFD966),
                new StatusBlock("逃亡者", serverSync.runnerCount() + "人", serverSync.runnerCount() > 0 ? 0xFF77E287 : 0xFFFFD966),
                new StatusBlock("外卡", compactWildcardDisplayName(), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE)
        ));

        currentY = addTwoColumnCards(
                layout,
                x,
                currentY,
                w,
                MEDIUM_CARD_MAX_WIDTH,
                "当前对局",
                card -> {
                    card.info("状态", serverSync.gameState() == GameState.WAITING ? "等待中" : "已开始", stateColor(serverSync.gameState()));
                    card.info("我的队伍", serverSync.playerRole(), serverSync.playerInTeam() ? 0xFFFFFFFF : 0xFFFFD966);
                    card.info("开始条件", startConditionDisplay(startTooltip), startTooltip.isBlank() ? 0xFF77E287 : 0xFFFFD966);
                },
                "外卡状态",
                card -> {
                    card.info("当前外卡", wildcardDisplayName(), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE);
                    card.info("下次触发", formatSeconds(serverSync.nextWildcardSeconds()), 0xFF7FC2FF);
                    card.info("运行剩余", formatSeconds(serverSync.activeWildcardRemainingSeconds()), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE);
                }
        );

        boolean canStart = startTooltip.isBlank();
        int buttonWidth = Math.min(240, Math.max(160, w - CARD_PADDING_X * 2));
        int buttonX = x + Math.max(0, (w - buttonWidth) / 2);
        addContentButton(
                layout,
                buttonX,
                currentY,
                buttonWidth,
                BUTTON_HEIGHT,
                "开始游戏",
                startTooltip,
                widget -> sendGameAction(GameAction.START_GAME),
                ButtonVariant.PRIMARY,
                canStart
        );
        markContentBottom(layout, currentY + BUTTON_HEIGHT + CARD_GAP);
    }

    private void buildTeamPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (serverSync == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, "队伍管理");
            card.hint("等待服务器同步。");
            markContentBottom(layout, card.finish());
            return;
        }

        markContentBottom(layout, addTeamManagementCard(layout, x, y, w));
    }

    private void buildBasicPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (editableConfig == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, "基础规则");
            card.hint("等待配置同步。");
            markContentBottom(layout, card.finish());
            return;
        }

        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                "游戏流程",
                card -> {
                    card.number(NumberField.PREPARING_SECONDS);
                    card.number(NumberField.ENDING_SECONDS);
                },
                "显示刷新",
                card -> {
                    card.number(NumberField.COMPASS_UPDATE_SECONDS);
                    card.number(NumberField.ACTION_BAR_INTERVAL_SECONDS);
                    card.hint("控制指南针和底部状态提示刷新节奏。");
                }
        );
        bottom = addTwoColumnCards(
                layout,
                x,
                bottom,
                w,
                "世界与边界",
                card -> {
                    boolean boundaryEnabled = editableConfig.hunterPrepareBoundaryEnabled();
                    card.booleanField(BooleanField.HUNTER_PREPARE_BOUNDARY_ENABLED);
                    card.number(NumberField.HUNTER_PREPARE_BOUNDARY_RADIUS, canEditConfig() && boundaryEnabled);
                    card.number(NumberField.HUNTER_PREPARE_BOUNDARY_WARN_DISTANCE, canEditConfig() && boundaryEnabled);
                    if (!boundaryEnabled) {
                        card.hint("关闭时半径和警告距离不会生效。");
                    }
                },
                "死亡掉落",
                card -> {
                    card.booleanField(BooleanField.RUNNER_DEATH_NO_DROPS);
                    card.booleanField(BooleanField.HUNTER_DEATH_NO_DROPS);
                    card.hint("两个开关互相独立。追猎指南针始终不会从猎人死亡掉落中掉出。");
                }
        );

        markContentBottom(layout, bottom);
    }

    private void buildVictoryPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (editableConfig == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, "胜利规则");
            card.hint("等待配置同步。");
            markContentBottom(layout, card.finish());
            return;
        }

        RunnerVictoryType victoryType = RunnerVictoryType.fromConfig(editableConfig.runnerVictoryType(), RunnerVictoryType.DRAGON);
        HunterVictoryType hunterVictoryType = HunterVictoryType.fromConfig(editableConfig.hunterVictoryType(), HunterVictoryType.RUNNERS_OUT);
        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                360,
                260,
                "逃亡者胜利方式",
                card -> buildRunnerVictoryCard(card, victoryType),
                "猎人胜利方式",
                card -> buildHunterVictoryCard(card, hunterVictoryType)
        );
        markContentBottom(layout, bottom);
    }

    private void buildRespawnPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (editableConfig == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, "生命复活");
            card.hint("等待配置同步。");
            markContentBottom(layout, card.finish());
            return;
        }

        RespawnMode hunterMode = RespawnMode.fromConfig(editableConfig.hunterRespawnMode(), RespawnMode.INFINITE);
        boolean killCountMode = isHunterKillCountMode();
        RespawnMode runnerMode = killCountMode ? RespawnMode.INFINITE : RespawnMode.fromConfig(editableConfig.runnerRespawnMode(), RespawnMode.LIMITED_LIVES);
        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                LARGE_CARD_MAX_WIDTH,
                "猎人生命 / 复活",
                card -> {
                    card.dropdown(DropdownField.HUNTER_RESPAWN_MODE);
                    card.number(NumberField.HUNTER_LIVES, canEditConfig() && hunterMode == RespawnMode.LIMITED_LIVES);
                    card.number(NumberField.HUNTER_RESPAWN_SECONDS, canEditConfig() && hunterMode != RespawnMode.NO_RESPAWN);
                    String hunterHint = respawnHint(hunterMode, "猎人");
                    if (!hunterHint.isBlank()) {
                        card.hint(hunterHint);
                    }
                },
                "逃亡者生命 / 复活",
                card -> {
                    if (killCountMode) {
                        card.info("逃亡者复活模式", "无限复活（锁定）", 0xFF7FC2FF);
                        card.number(NumberField.RUNNER_LIVES, false);
                        card.number(NumberField.RUNNER_RESPAWN_SECONDS, canEditConfig());
                        card.hint("击杀数模式下逃亡者固定无限复活。");
                    } else {
                        card.dropdown(DropdownField.RUNNER_RESPAWN_MODE);
                        card.number(NumberField.RUNNER_LIVES, canEditConfig() && runnerMode == RespawnMode.LIMITED_LIVES);
                        card.number(NumberField.RUNNER_RESPAWN_SECONDS, canEditConfig() && runnerMode != RespawnMode.NO_RESPAWN);
                        String runnerHint = respawnHint(runnerMode, "逃亡者");
                        if (!runnerHint.isBlank()) {
                            card.hint(runnerHint);
                        }
                    }
                }
        );
        markContentBottom(layout, bottom);
    }

    private void buildWildcardPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        if (editableConfig == null) {
            CardBuilder card = addCard(layout, x, layout.contentY(), w, "外卡规则");
            card.hint("等待配置同步。");
            markContentBottom(layout, card.finish());
            return;
        }

        int bottom = addTwoColumnCards(
                layout,
                x,
                y,
                w,
                MEDIUM_CARD_MAX_WIDTH,
                "外卡总设置",
                card -> {
                    card.numberPair(NumberField.WILDCARD_INTERVAL_SECONDS, NumberField.WILDCARD_DURATION_SECONDS);
                    card.info("已启用外卡", enabledWildcardCount(editableConfig) + " / " + ToggleField.values().length, enabledWildcardCount(editableConfig) > 0 ? 0xFF77E287 : 0xFFC9D4DE);
                },
                "外卡触发状态",
                card -> {
                    if (serverSync == null) {
                        card.hint("等待服务器同步。");
                    } else {
                        card.info("当前外卡", wildcardDisplayName(), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE);
                        card.info("下一次触发", formatSeconds(serverSync.nextWildcardSeconds()), 0xFFC9D4DE);
                        card.info("运行剩余", formatSeconds(serverSync.activeWildcardRemainingSeconds()), serverSync.activeWildcardRunning() ? 0xFF7FC2FF : 0xFFC9D4DE);
                    }
                }
        );
        if (selectedWildcardSettings != null && hasWildcardSettings(selectedWildcardSettings)) {
            bottom = addWildcardSettingsCard(layout, x, bottom, w, selectedWildcardSettings);
        } else {
            selectedWildcardSettings = null;
            bottom = addWildcardToggleMatrixCard(layout, x, bottom, w);
        }
        markContentBottom(layout, bottom);
    }

    private void buildRunnerVictoryCard(CardBuilder card, RunnerVictoryType victoryType) {
        card.dropdown(DropdownField.RUNNER_VICTORY_TYPE);
        switch (victoryType) {
            case DRAGON -> card.hint("当前目标：击败末影龙。");
            case SURVIVE_TIME -> card.number(NumberField.SURVIVE_TIME_SECONDS);
            case REACH_LOCATION -> {
                card.dropdown(DropdownField.TARGET_DIMENSION);
                card.coordinates(NumberField.TARGET_X, NumberField.TARGET_Y, NumberField.TARGET_Z);
                card.number(NumberField.TARGET_RADIUS);
                card.buttonGrid(List.of(new ButtonSpec(
                        "设定当前位置",
                        widget -> setTargetToCurrentLocation(),
                        ButtonVariant.NORMAL,
                        canEditConfig() && editableConfig != null && client != null && client.player != null && client.world != null,
                        "使用你当前所在维度和方块坐标作为逃亡者目标。"
                )), 1, 132);
            }
            case COLLECT_ITEM -> {
                card.string(StringField.TARGET_ITEM_ID);
                card.number(NumberField.TARGET_ITEM_COUNT);
            }
        }
    }

    private void buildHunterVictoryCard(CardBuilder card, HunterVictoryType hunterVictoryType) {
        card.dropdown(DropdownField.HUNTER_VICTORY_TYPE);
        if (hunterVictoryType == HunterVictoryType.RUNNER_KILL_COUNT) {
            card.number(NumberField.HUNTER_RUNNER_KILL_TARGET);
            card.hint("逃亡者会锁定为无限复活。");
        } else {
            card.dropdown(DropdownField.RUNNER_TEAM_LOSS_MODE);
        }
    }

    private String respawnHint(RespawnMode mode, String roleName) {
        return switch (mode) {
            case INFINITE -> roleName + "无限复活时生命数无效。";
            case NO_RESPAWN -> roleName + "不复活时生命数和复活时间无效。";
            case LIMITED_LIVES -> "";
        };
    }

    private void buildDebugPage(Layout layout) {
        int x = layout.contentX();
        int y = pageTop(layout);
        int w = layout.usableContentWidth();

        CardBuilder actionCard = addCard(layout, x, y, w, "对局控制");
        actionCard.hint(canManage ? "这些操作会发送到服务端执行。" : "只有 OP 可以执行调试操作。");
        actionCard.buttonGrid(List.of(
                new ButtonSpec("停止游戏", widget -> sendDebugAction(DebugAction.STOP_GAME), ButtonVariant.DANGER, canManage && isGameActive(), "结束当前对局并进入结算流程。"),
                new ButtonSpec("随机外卡", widget -> sendDebugAction(DebugAction.ROLL_WILDCARD), ButtonVariant.NORMAL, canManage && serverSync != null && serverSync.gameState() == GameState.RUNNING, "立即按服务端规则抽取一个可用外卡。"),
                new ButtonSpec("停止外卡", widget -> sendDebugAction(DebugAction.STOP_WILDCARD), ButtonVariant.DANGER, canManage && serverSync != null && serverSync.activeWildcardRunning(), "强制结束当前正在运行的外卡。")
        ), w >= 900 ? 3 : 2, 150);
        int currentY = actionCard.finish() + CARD_GAP;

        CardBuilder testCard = addCard(layout, x, currentY, w, "单独测试外卡");
        List<ButtonSpec> testButtons = new ArrayList<>();
        for (ToggleField field : ToggleField.values()) {
            boolean enabled = editableConfig != null && getToggle(editableConfig, field);
            testButtons.add(new ButtonSpec(
                    enabled ? "测试 " + field.label : field.label + " 已关闭",
                    widget -> sendTestWildcard(field),
                    ButtonVariant.NORMAL,
                    canManage && enabled,
                    enabled ? "单独触发该外卡用于测试。" : "该外卡当前关闭，不能测试。"
            ));
        }
        testCard.buttonGrid(testButtons, w >= 900 ? 3 : 2, 150);
        markContentBottom(layout, testCard.finish());
    }

    private void buildFooter(Layout layout) {
        int y = layout.panelY() + layout.panelHeight() - 32;
        if (isConfigEditPage()) {
            int buttonWidth = Math.max(76, Math.min(108, (layout.usableContentWidth() - 16) / 3));
            int x = layout.contentX() + Math.max(0, layout.usableContentWidth() - (buttonWidth * 3 + 16));
            saveButton = addButton(x, y, buttonWidth, 24, "保存配置", "只有存在未保存配置时才能提交。", widget -> saveConfig(), ButtonVariant.PRIMARY, canSaveConfig());
            addButton(x + buttonWidth + 8, y, buttonWidth, 24, "恢复默认", "将当前页面配置恢复为默认值，保存后生效。", widget -> restoreDefaultConfig(), ButtonVariant.NORMAL, canEditConfig() && editableConfig != null);
            addButton(x + (buttonWidth + 8) * 2, y, buttonWidth, 24, "关闭", "", widget -> close(), ButtonVariant.NORMAL, true);
            return;
        }

        addButton(layout.contentX() + layout.usableContentWidth() - 86, y, 86, 24, "关闭", "", widget -> close(), ButtonVariant.NORMAL, true);
    }

    private CardBuilder addCard(Layout layout, int x, int y, int width, String title) {
        return new CardBuilder(layout, x, y, width, title);
    }

    private int addTwoColumnCards(
            Layout layout,
            int x,
            int y,
            int width,
            String leftTitle,
            CardBody leftBody,
            String rightTitle,
            CardBody rightBody
    ) {
        return addTwoColumnCards(layout, x, y, width, MEDIUM_CARD_MAX_WIDTH, leftTitle, leftBody, rightTitle, rightBody);
    }

    private int addTwoColumnCards(
            Layout layout,
            int x,
            int y,
            int width,
            int maxCardWidth,
            String leftTitle,
            CardBody leftBody,
            String rightTitle,
            CardBody rightBody
    ) {
        return addTwoColumnCards(layout, x, y, width, maxCardWidth, maxCardWidth, leftTitle, leftBody, rightTitle, rightBody);
    }

    private int addTwoColumnCards(
            Layout layout,
            int x,
            int y,
            int width,
            int leftMaxWidth,
            int rightMaxWidth,
            String leftTitle,
            CardBody leftBody,
            String rightTitle,
            CardBody rightBody
    ) {
        if (useTwoColumns(width)) {
            int availableWidth = width - TWO_COLUMN_GAP;
            int minColumnWidth = Math.min(150, Math.max(100, availableWidth / 2));
            int totalMaxWidth = Math.max(1, leftMaxWidth + rightMaxWidth);
            int leftWidth = Math.min(leftMaxWidth, Math.max(minColumnWidth, availableWidth * leftMaxWidth / totalMaxWidth));
            int rightWidth = Math.min(rightMaxWidth, availableWidth - leftWidth);
            if (rightWidth < minColumnWidth) {
                rightWidth = minColumnWidth;
                leftWidth = availableWidth - rightWidth;
            }
            if (leftWidth < minColumnWidth) {
                leftWidth = minColumnWidth;
                rightWidth = availableWidth - leftWidth;
            }

            int extraWidth = Math.max(0, availableWidth - leftWidth - rightWidth);
            int addLeft = Math.min(extraWidth, Math.max(0, leftMaxWidth - leftWidth));
            leftWidth += addLeft;
            extraWidth -= addLeft;
            rightWidth += Math.min(extraWidth, Math.max(0, rightMaxWidth - rightWidth));

            int rowWidth = leftWidth + rightWidth + TWO_COLUMN_GAP;
            int rowX = x + Math.max(0, (width - rowWidth) / 2);
            CardBuilder leftCard = addCard(layout, rowX, y, leftWidth, leftTitle);
            leftBody.build(leftCard);
            CardBuilder rightCard = addCard(layout, rowX + leftWidth + TWO_COLUMN_GAP, y, rightWidth, rightTitle);
            rightBody.build(rightCard);
            int leftBottom = leftCard.finish();
            int rightBottom = rightCard.finish();
            return Math.max(leftBottom, rightBottom) + CARD_GAP;
        }

        int cardWidth = Math.min(width, Math.max(leftMaxWidth, rightMaxWidth));
        int cardX = x + Math.max(0, (width - cardWidth) / 2);
        CardBuilder leftCard = addCard(layout, cardX, y, cardWidth, leftTitle);
        leftBody.build(leftCard);
        int nextY = leftCard.finish() + CARD_GAP;
        CardBuilder rightCard = addCard(layout, cardX, nextY, cardWidth, rightTitle);
        rightBody.build(rightCard);
        return rightCard.finish() + CARD_GAP;
    }

    private int addWildcardToggleMatrixCard(Layout layout, int x, int y, int width) {
        int columns = wildcardToggleColumns(width);
        int gap = 8;
        int targetGridWidth = columns * WILDCARD_TOGGLE_TARGET_WIDTH + (columns - 1) * gap;
        int cardWidth = Math.min(width, targetGridWidth + CARD_PADDING_X * 2);
        int cardX = x + Math.max(0, (width - cardWidth) / 2);
        CardBuilder matrixCard = addCard(layout, cardX, y, cardWidth, "外卡开关");
        addWildcardBulkButtons(layout, matrixCard, cardX, y, cardWidth);
        matrixCard.toggleGrid(ToggleField.values(), columns);
        return matrixCard.finish() + CARD_GAP;
    }

    private void addWildcardBulkButtons(Layout layout, CardBuilder card, int cardX, int cardY, int cardWidth) {
        List<ButtonSpec> buttons = List.of(
                new ButtonSpec("开启所有外卡", widget -> setAllWildcards(true), ButtonVariant.NORMAL, canEditConfig(), "开启全部外卡开关。"),
                new ButtonSpec("关闭所有外卡", widget -> setAllWildcards(false), ButtonVariant.DANGER, canEditConfig(), "关闭全部外卡开关。")
        );
        if (cardWidth >= 236) {
            int buttonWidth = 82;
            int buttonHeight = 18;
            int totalWidth = buttonWidth * buttons.size() + BUTTON_GAP * (buttons.size() - 1);
            int buttonX = cardX + cardWidth - CARD_PADDING_X - totalWidth;
            int buttonY = cardY + CARD_PADDING_TOP - 3;
            for (int i = 0; i < buttons.size(); i++) {
                ButtonSpec button = buttons.get(i);
                addContentButton(
                        layout,
                        buttonX + i * (buttonWidth + BUTTON_GAP),
                        buttonY,
                        buttonWidth,
                        buttonHeight,
                        button.title(),
                        button.tooltip(),
                        button.action(),
                        button.variant(),
                        button.enabled()
                );
            }
            card.gap(8);
            return;
        }

        card.buttonGrid(buttons, 2, 112);
    }

    private int addWildcardSettingsCard(Layout layout, int x, int y, int width, ToggleField field) {
        int cardWidth = Math.min(width, SMALL_CARD_MAX_WIDTH);
        int cardX = x + Math.max(0, (width - cardWidth) / 2);
        CardBuilder card = addCard(layout, cardX, y, cardWidth, field.label + "设置");
        switch (field) {
            case HUNTER_RADAR -> {
                card.number(NumberField.HUNTER_RADAR_INTERVAL_SECONDS, canEditConfig());
                card.hint("猎人雷达会按该间隔播报逃亡者方位。");
            }
            case SUPPLY_DROP -> {
                card.number(NumberField.SUPPLY_DROP_INTERVAL_SECONDS, canEditConfig());
                card.hint("补给空投会按该间隔落下带发光轨迹和信标光柱的好坏混合随机补给箱。");
            }
            case BLOCK_DECAY -> {
                card.number(NumberField.BLOCK_DECAY_SECONDS, canEditConfig());
                card.hint("新放置且未被排除的方块会在该时间后消失。");
            }
            case PEARL_FRENZY -> {
                card.number(NumberField.PEARL_FRENZY_MAX_PEARLS, canEditConfig());
                card.number(NumberField.PEARL_FRENZY_INTERVAL_SECONDS, canEditConfig());
                card.number(NumberField.WILDCARD_DURATION_SECONDS, canEditConfig());
                card.hint("珍珠补给不会让背包中的末影珍珠超过上限。");
            }
            case WIND_CHARGE_BRAWL -> {
                card.number(NumberField.WIND_CHARGE_BRAWL_INTERVAL_SECONDS, canEditConfig());
                card.number(NumberField.WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT, canEditConfig());
                card.hint("倍率以百分比填写，180 表示 1.8 倍。");
            }
            default -> card.hint("该外卡当前没有单独参数。");
        }
        card.button("返回外卡开关", widget -> closeWildcardSettings(), ButtonVariant.NORMAL, true, 132);
        return card.finish() + CARD_GAP;
    }

    private int addStatusBlocks(Layout layout, int x, int y, int width, List<StatusBlock> blocks) {
        int gap = 12;
        int columns;
        if (width >= 430) {
            columns = 3;
        } else if (width >= 300) {
            columns = 2;
        } else {
            columns = 1;
        }
        columns = Math.max(1, Math.min(columns, blocks.size()));

        int blockHeight = 48;
        int blockWidth = Math.min(STATUS_BLOCK_MAX_WIDTH, Math.max(80, (width - (columns - 1) * gap) / columns));
        int gridWidth = blockWidth * columns + (columns - 1) * gap;
        int gridX = x + Math.max(0, (width - gridWidth) / 2);
        for (int i = 0; i < blocks.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            addStatusBlock(layout, gridX + column * (blockWidth + gap), y + row * (blockHeight + gap), blockWidth, blockHeight, blocks.get(i));
        }

        int rows = (blocks.size() + columns - 1) / columns;
        return y + rows * blockHeight + (rows - 1) * gap + CARD_GAP;
    }

    private void addStatusBlock(Layout layout, int x, int y, int width, int height, StatusBlock block) {
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, 0x77303A46, block.color()));
        }
        if (isVisibleInContent(layout, y + 9, textRenderer.fontHeight)) {
            labels.add(new Label(block.label(), x + 8, y + 7, 0xFF9FAAB4, false, Math.max(20, width - 16)));
        }
        if (isVisibleInContent(layout, y + 27, textRenderer.fontHeight)) {
            labels.add(new Label(block.value(), x + 8, y + 27, block.color(), true, Math.max(20, width - 16)));
        }
    }

    private int addStatusPills(Layout layout, int x, int y, int width, List<StatusBlock> blocks) {
        int gap = 8;
        int rowGap = 7;
        int pillHeight = 28;
        List<Integer> pillWidths = new ArrayList<>();
        for (StatusBlock block : blocks) {
            int textWidth = textRenderer.getWidth(block.label() + "：" + block.value());
            pillWidths.add(Math.min(190, Math.max(88, textWidth + 24)));
        }
        int totalWidth = pillWidths.stream().mapToInt(Integer::intValue).sum() + gap * Math.max(0, blocks.size() - 1);
        if (blocks.size() == 5 && totalWidth > width) {
            int compactWidth = Math.max(72, (width - gap * 4) / 5);
            for (int i = 0; i < pillWidths.size(); i++) {
                pillWidths.set(i, compactWidth);
            }
        }

        int index = 0;
        int rowY = y;
        while (index < blocks.size()) {
            int rowStart = index;
            int rowWidth = 0;
            while (index < blocks.size()) {
                int nextWidth = pillWidths.get(index);
                int candidateWidth = rowWidth == 0 ? nextWidth : rowWidth + gap + nextWidth;
                if (candidateWidth > width && index > rowStart) {
                    break;
                }

                rowWidth = candidateWidth;
                index++;
            }

            int pillX = x + Math.max(0, (width - rowWidth) / 2);
            for (int i = rowStart; i < index; i++) {
                StatusBlock block = blocks.get(i);
                int pillWidth = pillWidths.get(i);
                addStatusPill(layout, pillX, rowY, pillWidth, pillHeight, block);
                pillX += pillWidth + gap;
            }

            rowY += pillHeight + rowGap;
        }

        return rowY - rowGap + CARD_GAP;
    }

    private void addStatusPill(Layout layout, int x, int y, int width, int height, StatusBlock block) {
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, 0x66303A46, block.color()));
        }

        String label = block.label() + "：";
        int labelWidth = Math.min(textRenderer.getWidth(label), Math.max(20, width / 2));
        int textY = y + Math.max(4, (height - textRenderer.fontHeight) / 2);
        if (isVisibleInContent(layout, y, height)) {
            labels.add(new Label(label, x + 8, textY, 0xFF9FAAB4, false, labelWidth));
            labels.add(new Label(block.value(), x + 8 + labelWidth, textY, block.color(), true, Math.max(20, width - labelWidth - 14)));
        }
    }

    private int addTeamManagementCard(Layout layout, int x, int y, int width) {
        boolean twoColumns = width >= TWO_COLUMN_MIN_WIDTH;
        int sectionGap = TWO_COLUMN_GAP;
        int availableSectionWidth = twoColumns ? (width - CARD_PADDING_X * 2 - sectionGap) / 2 : width - CARD_PADDING_X * 2;
        int sectionWidth = Math.min(twoColumns ? SMALL_CARD_MAX_WIDTH : MEDIUM_CARD_MAX_WIDTH, Math.max(80, availableSectionWidth));
        int sectionsWidth = twoColumns ? sectionWidth * 2 + sectionGap : sectionWidth;
        int cardWidth = Math.min(width, sectionsWidth + CARD_PADDING_X * 2);
        int cardX = x + Math.max(0, (width - cardWidth) / 2);
        int contentX = cardX + CARD_PADDING_X;
        int contentWidth = Math.max(80, cardWidth - CARD_PADDING_X * 2);
        int sectionsX = contentX + Math.max(0, (contentWidth - sectionsWidth) / 2);
        int sectionHeight = 60;
        int sectionY = y + CARD_PADDING_TOP + CARD_TITLE_HEIGHT + 4;
        int secondSectionY = twoColumns ? sectionY : sectionY + sectionHeight + sectionGap;
        int sectionsBottom = twoColumns ? sectionY + sectionHeight : secondSectionY + sectionHeight;
        int bottomRowY = sectionsBottom + 6;
        int cardHeight = bottomRowY + BUTTON_HEIGHT + CARD_PADDING_BOTTOM - y;

        drawCardBackground(layout, cardX, y, cardWidth, cardHeight);
        addCardTitle(layout, "队伍管理", contentX, y + CARD_PADDING_TOP);

        boolean isHunter = "猎人".equals(serverSync.playerRole());
        boolean isRunner = "逃亡者".equals(serverSync.playerRole());
        boolean canChangeTeam = canChangeTeam();
        String lockedTooltip = canChangeTeam ? "" : "游戏已经开始，当前不能更换或离开队伍。";
        addTeamSection(
                layout,
                sectionsX,
                sectionY,
                sectionWidth,
                sectionHeight,
                "猎人阵营",
                serverSync.hunterCount(),
                isHunter,
                0xFFD76474,
                isHunter ? "已是猎人" : "加入猎人",
                canChangeTeam ? (isHunter ? "你已经在猎人阵营。" : "加入猎人阵营。") : lockedTooltip,
                widget -> sendTeamAction(TeamAction.JOIN_HUNTER),
                canChangeTeam && !isHunter
        );
        addTeamSection(
                layout,
                twoColumns ? sectionsX + sectionWidth + sectionGap : sectionsX,
                secondSectionY,
                sectionWidth,
                sectionHeight,
                "逃亡者阵营",
                serverSync.runnerCount(),
                isRunner,
                0xFF7FC2FF,
                isRunner ? "已是逃亡者" : "加入逃亡者",
                canChangeTeam ? (isRunner ? "你已经在逃亡者阵营。" : "加入逃亡者阵营。") : lockedTooltip,
                widget -> sendTeamAction(TeamAction.JOIN_RUNNER),
                canChangeTeam && !isRunner
        );

        int leaveWidth = Math.min(260, Math.max(104, contentWidth / 4));
        int teamLabelWidth = Math.max(40, contentWidth - leaveWidth - BUTTON_GAP);
        if (isVisibleInContent(layout, bottomRowY, BUTTON_HEIGHT)) {
            labels.add(new Label("我的当前队伍：" + serverSync.playerRole(), contentX, bottomRowY + 6, 0xFFC9D4DE, false, teamLabelWidth));
        }
        addContentButton(
                layout,
                contentX + contentWidth - leaveWidth,
                bottomRowY,
                leaveWidth,
                BUTTON_HEIGHT,
                "离开队伍",
                canChangeTeam ? (serverSync.playerInTeam() ? "退出当前阵营。" : "你尚未加入队伍。") : lockedTooltip,
                widget -> sendTeamAction(TeamAction.LEAVE),
                ButtonVariant.DANGER,
                canChangeTeam && serverSync.playerInTeam()
        );
        return y + cardHeight + CARD_GAP;
    }

    private void addTeamSection(
            Layout layout,
            int x,
            int y,
            int width,
            int height,
            String title,
            int count,
            boolean current,
            int accent,
            String buttonTitle,
            String tooltip,
            ButtonWidget.PressAction action,
            boolean enabled
    ) {
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, 0x55303A46, 0xFF4C5A66));
            boxes.add(new Box(x, y, 3, height, accent, accent));
        }
        int buttonWidth = Math.min(96, Math.max(78, width / 3));
        int textWidth = Math.max(40, width - buttonWidth - 24);
        addContentLabel(layout, title, x + 10, y + 9, accent, true);
        if (isVisibleInContent(layout, y + 9, textRenderer.fontHeight)) {
            labels.add(new Label(current ? "当前阵营" : "可加入", x + Math.max(66, textRenderer.getWidth(title) + 18), y + 9, current ? 0xFF77E287 : 0xFF9FAAB4, false, Math.max(40, textWidth - 62)));
        }
        addContentLabel(layout, "当前人数：" + count + " 人", x + 10, y + 29, 0xFFC9D4DE, false);
        addContentButton(layout, x + width - buttonWidth - 8, y + height - BUTTON_HEIGHT - 7, buttonWidth, BUTTON_HEIGHT, buttonTitle, tooltip, action, ButtonVariant.NORMAL, enabled);
    }

    private void addWildcardToggleTile(Layout layout, ToggleField field, int x, int y, int width, int height) {
        boolean enabled = getToggle(editableConfig, field);
        boolean configurable = hasWildcardSettings(field);
        int borderColor = enabled ? 0xFF55B978 : 0xFF59636C;
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, enabled ? 0x55324B3F : 0x55303A46, borderColor));
        }

        int toggleWidth = Math.min(34, Math.max(30, width / 5));
        int settingsWidth = configurable ? 42 : 0;
        int settingsGap = configurable ? 4 : 0;
        int buttonsWidth = toggleWidth + settingsWidth + settingsGap;
        int infoButtonSize = 12;
        int infoGap = 3;
        int iconSize = 16;
        int iconX = x + 7;
        int iconY = y + Math.max(4, (height - iconSize) / 2);
        int labelX = iconX + iconSize + 6;
        int rightButtonsX = x + width - buttonsWidth - 5;
        int maxInfoX = rightButtonsX - infoButtonSize - 4;
        int desiredTextWidth = Math.max(8, width - buttonsWidth - (labelX - x) - infoButtonSize - infoGap - 12);
        int firstLineWidth = textRenderer.getWidth(wrapLabelText(field.label, desiredTextWidth, 2).get(0));
        int infoX = Math.min(labelX + Math.min(firstLineWidth, desiredTextWidth) + infoGap, Math.max(labelX, maxInfoX));
        int textWidth = Math.max(8, infoX - labelX - infoGap);
        if (isVisibleInContent(layout, y, height)) {
            icons.add(new Icon(WildcardIcons.iconFor(field.label), iconX, iconY));
            wrappedLabels.add(new WrappedLabel(field.label, labelX, y, height, enabled ? 0xFFFFFFFF : 0xFFC9D4DE, false, textWidth, 2));
        }
        addContentButton(
                layout,
                infoX,
                y + Math.max(0, (height - infoButtonSize) / 2),
                infoButtonSize,
                infoButtonSize,
                "i",
                wildcardInfoTooltip(field),
                widget -> {
                },
                ButtonVariant.NORMAL,
                true
        );
        if (configurable) {
            addContentButton(
                    layout,
                    x + width - buttonsWidth - 5,
                    y + Math.max(4, (height - 18) / 2),
                    settingsWidth,
                    18,
                    "设置",
                    "",
                    widget -> openWildcardSettings(field),
                    ButtonVariant.NORMAL,
                    editableConfig != null
            );
        }
        addContentButton(
                layout,
                x + width - toggleWidth - 5,
                y + Math.max(4, (height - 18) / 2),
                toggleWidth,
                18,
                enabled ? "开" : "关",
                wildcardToggleTooltip(field, enabled),
                widget -> toggleField(field),
                enabled ? ButtonVariant.TOGGLE_ON : ButtonVariant.TOGGLE_OFF,
                canEditConfig()
        );
    }

    private String wildcardToggleTooltip(ToggleField field, boolean enabled) {
        return field.label + "：" + (enabled ? "已开启" : "已关闭") + "，点击切换。";
    }

    private String wildcardInfoTooltip(ToggleField field) {
        ConfigSnapshot config = editableConfig;
        return switch (field) {
            case SPEED_RUSH -> "全体参与者获得速度 II，追逐和逃跑节奏都会变快；外卡结束后移除本效果。";
            case FEATHERWEIGHT -> "全体参与者获得跳跃提升 II 和缓降，更容易跨越地形并减少坠落威胁。";
            case GLOWING -> "全体参与者获得发光效果，隔墙也更容易被发现，潜行和躲藏会变难。";
            case NIGHT_HUNT -> "强制世界保持夜晚，猎人获得夜视；逃亡者需要在夜间环境下行动。";
            case EXPLOSIVE_DEATH -> "参与者死亡或击杀生物时，会在死亡/被击杀位置产生爆炸。";
            case SUPPLY_DROP -> "每 " + configuredSeconds(config == null ? 60 : config.supplyDropIntervalSeconds()) + " 随机选一名参与者附近投放补给箱；未开启箱子会连同内部物品一起消失。";
            case HUNTER_RADAR -> "每 " + configuredSeconds(config == null ? 20 : config.hunterRadarIntervalSeconds()) + " 向猎人播报同维度最近逃亡者距离；被探测者会收到预警。";
            case COMPASS_CHAOS -> "猎人指南针目标会随机偏离真实逃亡者位置约 30-80 格，追踪方向不再完全可靠。";
            case HUNGER_CHASE -> "参与者更易饥饿，低饥饿会短暂缓慢；进食获得速度，高级食物获得 10 秒速度 II。";
            case WEAPON_OVERHEAT -> "短时间连续攻击会积累过热；热度越高越容易获得虚弱和缓慢，热度条显示当前状态。";
            case LIGHT_LOAD -> "按护甲重量给予速度或缓慢：轻装更灵活，重甲更笨重。";
            case BLOCK_DECAY -> "新放置且未被排除的普通方块会在 " + configuredSeconds(config == null ? 10 : config.blockDecaySeconds()) + " 后消失；容器、门、床、工作台等不会腐化。";
            case PEARL_FRENZY -> "最多持有 " + configuredCount(config == null ? 4 : config.pearlFrenzyMaxPearls()) + " 个末影珍珠，每 " + configuredSeconds(config == null ? 45 : config.pearlFrenzyIntervalSeconds()) + " 补给 1 个；投掷珍珠可能触发副作用。";
            case WIND_CHARGE_BRAWL -> "每 " + configuredSeconds(config == null ? 5 : config.windChargeBrawlIntervalSeconds()) + " 补给 1 个风弹，最多 16 个；风弹反弹/爆炸倍率为 " + configuredPercent(config == null ? 180 : config.windChargeExplosionMultiplierPercent()) + "。";
            case BLOOD_RAGE -> "血量越低强化越高：90%速度 I，70%加力量 I，50%速度 II+抗性 I，30%力量/抗性 II，15%及以下再加夜视。";
            case DISABLED_WILDCARD -> "本轮外卡没有额外效果，用于让随机池中出现空白回合。";
        };
    }

    private String configuredSeconds(int seconds) {
        return Math.max(1, seconds) + " 秒";
    }

    private String configuredCount(int count) {
        return Integer.toString(Math.max(1, count));
    }

    private String configuredPercent(int percent) {
        return Math.max(1, percent) + "%";
    }

    private boolean useTwoColumns(int usableContentWidth) {
        return usableContentWidth >= TWO_COLUMN_MIN_WIDTH;
    }

    private void drawCardBackground(Layout layout, int x, int y, int width, int height) {
        if (isVisibleInContentPartial(layout, y, height)) {
            boxes.add(new Box(x, y, width, height, 0x88303A46, 0xFF4C5A66));
        }
    }

    private void drawCardBorder(Layout layout, int x, int y, int width, int height) {
        drawCardBackground(layout, x, y, width, height);
    }

    private void addCardTitle(Layout layout, String title, int x, int y) {
        addContentLabel(layout, title, x, y, 0xFFFFFFFF, true);
    }

    private void addHintText(Layout layout, String text, int x, int y, int width) {
        if (isVisibleInContent(layout, y, textRenderer.fontHeight)) {
            labels.add(new Label(text, x, y, 0xFF9FAAB4, false, width));
        }
    }

    private int addInfoRow(Layout layout, String label, String value, int x, int y, int width, int valueColor) {
        int valueX = x + Math.max(92, Math.min(LABEL_WIDTH, width / 2));
        int labelY = y + Math.max(3, (ROW_HEIGHT - textRenderer.fontHeight) / 2);
        addContentLabel(layout, label + "：", x, labelY, 0xFFC9D4DE, false);
        if (isVisibleInContent(layout, y, textRenderer.fontHeight)) {
            labels.add(new Label(value, valueX, labelY, valueColor, false, Math.max(40, width - (valueX - x))));
        }
        return nextRowY(y);
    }

    private int addInputRow(Layout layout, NumberField field, int x, int y, int width) {
        return addInputRow(layout, field, x, y, width, canManage);
    }

    private int addInputRow(Layout layout, NumberField field, int x, int y, int width, boolean editable) {
        addContentNumberField(layout, field, x, y, width, CONTROL_HEIGHT, editable);
        return nextRowY(y);
    }

    private int addInputRow(Layout layout, StringField field, int x, int y, int width) {
        addContentStringField(layout, field, x, y, width, CONTROL_HEIGHT);
        return nextRowY(y);
    }

    private int addCoordinateRow(Layout layout, NumberField xField, NumberField yField, NumberField zField, int x, int y, int width) {
        if (!isVisibleInContent(layout, y, ROW_HEIGHT)) {
            return nextRowY(y);
        }

        int labelWidth = width < 230 ? 32 : 52;
        int axisWidth = 8;
        int gap = 4;
        int availableWidth = width - labelWidth - axisWidth * 3 - gap * 5;
        int fieldWidth = Math.max(28, availableWidth / 3);
        int rowWidth = labelWidth + (axisWidth + fieldWidth) * 3 + gap * 5;
        if (rowWidth > width) {
            fieldWidth = Math.max(24, (width - labelWidth - axisWidth * 3 - gap * 5) / 3);
            rowWidth = labelWidth + (axisWidth + fieldWidth) * 3 + gap * 5;
        }

        int startX = x + Math.max(0, (width - rowWidth) / 2);
        int labelY = y + Math.max(3, (ROW_HEIGHT - textRenderer.fontHeight) / 2);
        labels.add(new Label(width < 230 ? "坐标：" : "目标坐标：", startX, labelY, 0xFFC9D4DE, false, labelWidth));

        int currentX = startX + labelWidth + gap;
        currentX = addCoordinateInput(layout, "X", xField, currentX, controlY(y), axisWidth, fieldWidth, CONTROL_HEIGHT);
        currentX = addCoordinateInput(layout, "Y", yField, currentX + gap, controlY(y), axisWidth, fieldWidth, CONTROL_HEIGHT);
        addCoordinateInput(layout, "Z", zField, currentX + gap, controlY(y), axisWidth, fieldWidth, CONTROL_HEIGHT);
        return nextRowY(y);
    }

    private int addDropdownRow(Layout layout, DropdownField field, int x, int y, int width) {
        return addDropdownRow(layout, field, x, y, width, canManage);
    }

    private int addDropdownRow(Layout layout, DropdownField field, int x, int y, int width, boolean editable) {
        int dropdownWidth = dropdownWidth(width);
        int dropdownX = x + width - dropdownWidth;
        int labelY = y + Math.max(3, (ROW_HEIGHT - textRenderer.fontHeight) / 2);
        labels.add(new Label(field.label + "：", x, labelY, 0xFFC9D4DE, false, Math.max(10, dropdownX - x - 8)));
        addContentDropdownField(layout, field, dropdownX, controlY(y), dropdownWidth, CONTROL_HEIGHT, editable);
        return nextRowY(y);
    }

    private int addToggleRow(Layout layout, BooleanField field, int x, int y, int width) {
        addContentBooleanField(layout, field, x, y, width, BUTTON_HEIGHT);
        return nextRowY(y);
    }

    private int addButtonRow(Layout layout, List<ButtonSpec> buttons, int x, int y, int width, int requestedColumns, int maxButtonWidth) {
        if (buttons.isEmpty()) {
            return y;
        }

        int columns = Math.max(1, Math.min(requestedColumns, buttons.size()));
        int availableButtonWidth = Math.max(80, (width - (columns - 1) * BUTTON_GAP) / columns);
        int buttonWidth = maxButtonWidth <= 0 ? availableButtonWidth : Math.min(maxButtonWidth, availableButtonWidth);
        int rowWidth = buttonWidth * columns + (columns - 1) * BUTTON_GAP;
        int startX = x + Math.max(0, (width - rowWidth) / 2);
        int rows = (buttons.size() + columns - 1) / columns;
        for (int i = 0; i < buttons.size(); i++) {
            ButtonSpec button = buttons.get(i);
            int column = i % columns;
            int row = i / columns;
            addContentButton(
                    layout,
                    startX + column * (buttonWidth + BUTTON_GAP),
                    y + row * (BUTTON_HEIGHT + BUTTON_GAP),
                    buttonWidth,
                    BUTTON_HEIGHT,
                    button.title(),
                    button.tooltip(),
                    button.action(),
                    button.variant(),
                    button.enabled()
            );
        }
        return y + rows * BUTTON_HEIGHT + (rows - 1) * BUTTON_GAP + ROW_GAP;
    }

    private int nextRowY(int y) {
        return y + ROW_HEIGHT + ROW_GAP;
    }

    private int controlY(int rowY) {
        return rowY + Math.max(0, (ROW_HEIGHT - CONTROL_HEIGHT) / 2);
    }

    private int buttonY(int rowY) {
        return rowY + Math.max(0, (ROW_HEIGHT - BUTTON_HEIGHT) / 2);
    }

    private int rowContentBottom(int y) {
        return y + Math.max(Math.max(ROW_HEIGHT, CONTROL_HEIGHT), BUTTON_HEIGHT);
    }

    private int contentStartY(int cardY) {
        return cardY + CARD_PADDING_TOP + CARD_TITLE_HEIGHT;
    }

    private int formLabelWidth(int width, int controlWidth, int unitSpace) {
        return Math.max(10, width - controlWidth - unitSpace - 8);
    }

    private int numberFieldWidth(NumberField field, int width, int unitSpace) {
        int labelTarget = Math.min(LABEL_WIDTH, Math.max(72, width / 2));
        int minWidth = Math.min(46, Math.max(34, width - unitSpace));
        int maxBySpace = Math.max(minWidth, width - unitSpace);
        int maxByRow = width - labelTarget - unitSpace - 8;
        int preferred = field.allowsNegative() ? 96 : CONTROL_WIDTH;
        int target = Math.min(preferred, maxBySpace);
        if (maxByRow >= minWidth) {
            target = Math.min(target, maxByRow);
        }
        return Math.max(minWidth, Math.min(128, target));
    }

    private int textFieldWidth(int width) {
        int labelTarget = Math.min(LABEL_WIDTH, Math.max(72, width / 2));
        int minWidth = Math.min(82, Math.max(52, width));
        int maxByRow = width - labelTarget - 8;
        int target = Math.min(150, width);
        if (maxByRow >= minWidth) {
            target = Math.min(target, maxByRow);
        }
        return Math.max(minWidth, target);
    }

    private int dropdownWidth(int width) {
        int labelTarget = Math.min(LABEL_WIDTH, Math.max(72, width / 2));
        int minWidth = Math.min(100, Math.max(70, width));
        int maxByRow = width - labelTarget - 8;
        int target = Math.min(DROPDOWN_WIDTH, width);
        if (maxByRow >= minWidth) {
            target = Math.min(target, maxByRow);
        }
        return Math.max(minWidth, target);
    }

    private void addNumberField(NumberField field, int x, int y, int width, int fieldHeight) {
        addNumberField(field, x, y, width, fieldHeight, canEditConfig());
    }

    private void addNumberField(NumberField field, int x, int y, int width, int fieldHeight, boolean editable) {
        boolean fieldEditable = editable && canEditConfig();
        int unitTextWidth = field.unit.isBlank() ? 0 : Math.min(UNIT_WIDTH, Math.max(18, textRenderer.getWidth(field.unit)));
        int unitSpace = field.unit.isBlank() ? 0 : unitTextWidth + 8;
        int fieldWidth = numberFieldWidth(field, width, unitSpace);
        int fieldX = x + width - fieldWidth - unitSpace;
        int labelY = y + Math.max(3, (fieldHeight - textRenderer.fontHeight) / 2);
        labels.add(new Label(field.label + "：", x, labelY, 0xFFC9D4DE, false, formLabelWidth(width, fieldWidth, unitSpace)));
        if (!field.unit.isBlank()) {
            labels.add(new Label(field.unit, fieldX + fieldWidth + 8, labelY, fieldEditable ? 0xFFC9D4DE : 0xFF7D8790));
        }

        TextFieldWidget textField = new TextFieldWidget(textRenderer, fieldX, y, fieldWidth, fieldHeight, Text.literal(field.label));
        textField.setMaxLength(11);
        textField.setText(Integer.toString(getNumber(editableConfig, field)));
        textField.setEditable(fieldEditable);
        textField.active = fieldEditable;
        numberFields.put(field, textField);
        addDrawableChild(textField);
    }

    private int addCoordinateInput(Layout layout, String axis, NumberField field, int x, int y, int axisWidth, int fieldWidth, int fieldHeight) {
        int labelY = y + Math.max(3, (fieldHeight - textRenderer.fontHeight) / 2);
        labels.add(new Label(axis, x, labelY, 0xFF9FAAB4, false, axisWidth));

        int fieldX = x + axisWidth + 2;
        TextFieldWidget textField = new TextFieldWidget(textRenderer, fieldX, y, fieldWidth, fieldHeight, Text.literal(field.label));
        textField.setMaxLength(11);
        textField.setText(Integer.toString(getNumber(editableConfig, field)));
        boolean editable = canEditConfig();
        textField.setEditable(editable);
        textField.active = editable;
        numberFields.put(field, textField);
        addDrawableChild(textField);
        return fieldX + fieldWidth;
    }

    private void addStringField(StringField field, int x, int y, int width, int fieldHeight) {
        int fieldWidth = textFieldWidth(width);
        int fieldX = x + width - fieldWidth;
        int labelY = y + Math.max(3, (fieldHeight - textRenderer.fontHeight) / 2);
        labels.add(new Label(field.label + "：", x, labelY, 0xFFC9D4DE, false, Math.max(10, fieldX - x - 8)));

        TextFieldWidget textField = new TextFieldWidget(textRenderer, fieldX, y, fieldWidth, fieldHeight, Text.literal(field.label));
        textField.setMaxLength(field.maxLength);
        textField.setText(getString(editableConfig, field));
        boolean editable = canEditConfig();
        textField.setEditable(editable);
        textField.active = editable;
        stringFields.put(field, textField);
        addDrawableChild(textField);
    }

    private void addDropdownField(DropdownField field, int x, int y, int width, int height, boolean openUp, boolean enabled) {
        boolean editable = enabled && canEditConfig();
        DropdownWidget dropdown = new DropdownWidget(
                textRenderer,
                x,
                y,
                width,
                height,
                "",
                dropdownOptions(field, getDropdownValue(editableConfig, field)),
                getDropdownValue(editableConfig, field),
                editable,
                value -> selectDropdownField(field, value),
                this::closeDropdowns
        );
        dropdown.setOpenUp(openUp);
        dropdownFields.put(field, dropdown);
        addDrawableChild(dropdown);
    }

    private List<DropdownWidget.Option> dropdownOptions(DropdownField field, String currentValue) {
        if (currentValue == null || currentValue.isBlank()) {
            return field.options;
        }

        for (DropdownWidget.Option option : field.options) {
            if (option.value().equals(currentValue)) {
                return field.options;
            }
        }

        List<DropdownWidget.Option> options = new ArrayList<>(field.options);
        options.add(option(currentValue, currentValue));
        return options;
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
                canEditConfig()
        );
    }

    private void addBooleanField(BooleanField field, int x, int y, int width, int height) {
        boolean enabled = getBoolean(editableConfig, field);
        int buttonWidth = width < 320
                ? Math.max(78, Math.min(116, width / 2))
                : Math.min(150, Math.max(CONTROL_WIDTH, width / 2));
        buttonWidth = Math.min(buttonWidth, width);
        int buttonX = x + width - buttonWidth;
        int labelY = y + Math.max(3, (height - textRenderer.fontHeight) / 2);
        labels.add(new Label(field.label + "：", x, labelY, 0xFFC9D4DE, false, Math.max(10, buttonX - x - 8)));
        addButton(
                buttonX,
                y,
                buttonWidth,
                height,
                enabled ? "开启" : "关闭",
                "",
                widget -> toggleBooleanField(field),
                enabled ? ButtonVariant.TOGGLE_ON : ButtonVariant.TOGGLE_OFF,
                canEditConfig()
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

    private void renderWrappedLabel(DrawContext context, Layout layout, WrappedLabel label) {
        int maxWidth = label.maxWidth(layout);
        List<String> lines = wrapLabelText(label.text(), maxWidth, label.maxLines());
        int textHeight = lines.size() * textRenderer.fontHeight + Math.max(0, lines.size() - 1);
        int startY = label.y() + Math.max(0, (label.height() - textHeight) / 2);
        for (int i = 0; i < lines.size(); i++) {
            context.drawText(textRenderer, Text.literal(lines.get(i)), label.x(), startY + i * (textRenderer.fontHeight + 1), label.color(), label.shadow());
        }
    }

    private List<String> wrapLabelText(String text, int maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            String line = textRenderer.trimToWidth(remaining, maxWidth);
            if (line.isBlank()) {
                line = remaining.substring(0, 1);
            }
            remaining = remaining.substring(line.length()).trim();
            if (!remaining.isEmpty() && lines.size() == maxLines - 1) {
                line = textRenderer.trimToWidth(line + "...", maxWidth);
            }
            lines.add(line);
        }
        return lines.isEmpty() ? List.of("") : lines;
    }

    private void addContentNumberField(Layout layout, NumberField field, int x, int y, int width, int fieldHeight) {
        if (isVisibleInContent(layout, y, ROW_HEIGHT)) {
            addNumberField(field, x, controlY(y), width, fieldHeight);
        }
    }

    private void addContentNumberField(Layout layout, NumberField field, int x, int y, int width, int fieldHeight, boolean editable) {
        if (isVisibleInContent(layout, y, ROW_HEIGHT)) {
            addNumberField(field, x, controlY(y), width, fieldHeight, editable);
        }
    }

    private void addContentStringField(Layout layout, StringField field, int x, int y, int width, int fieldHeight) {
        if (isVisibleInContent(layout, y, ROW_HEIGHT)) {
            addStringField(field, x, controlY(y), width, fieldHeight);
        }
    }

    private void addContentDropdownField(Layout layout, DropdownField field, int x, int y, int width, int height) {
        addContentDropdownField(layout, field, x, y, width, height, canManage);
    }

    private void addContentDropdownField(Layout layout, DropdownField field, int x, int y, int width, int height, boolean editable) {
        if (isVisibleInContent(layout, y, height)) {
            int menuHeight = Math.max(18, height) * field.options.size();
            boolean openUp = y + height + 1 + menuHeight > layout.viewportBottom()
                    && y - menuHeight - 1 >= layout.viewportTop();
            addDropdownField(field, x, y, width, height, openUp, editable);
        }
    }

    private void addContentToggleField(Layout layout, ToggleField field, int x, int y, int width, int height) {
        if (isVisibleInContent(layout, y, height)) {
            addToggleField(field, x, y, width, height);
        }
    }

    private void addContentBooleanField(Layout layout, BooleanField field, int x, int y, int width, int height) {
        if (isVisibleInContent(layout, y, height)) {
            addBooleanField(field, x, buttonY(y), width, height);
        }
    }

    private void addContentButton(Layout layout, int x, int y, int width, int height, String title, String description, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled) {
        if (isVisibleInContent(layout, y, height)) {
            addButton(x, y, width, height, title, description, action, variant, enabled);
        }
    }

    private StyledButtonWidget addButton(int x, int y, int width, int height, String title, String description, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled) {
        String tooltip = description == null ? "" : description;
        if (!enabled && tooltip.isBlank() && isConfigEditPage()) {
            tooltip = configEditDeniedMessage();
        }

        StyledButtonWidget button = new StyledButtonWidget(x, y, width, height, title, tooltip, action, variant);
        button.active = enabled;
        addDrawableChild(button);
        return button;
    }

    private int pageTop(Layout layout) {
        return layout.viewportTop() - renderedScroll;
    }

    private void markContentBottom(Layout layout, int screenBottomY) {
        pageContentHeight = Math.max(pageContentHeight, screenBottomY - pageTop(layout));
    }

    private boolean isVisibleInContent(Layout layout, int y, int height) {
        return y >= layout.viewportTop() && y + height <= layout.viewportBottom();
    }

    private boolean isVisibleInContentPartial(Layout layout, int y, int height) {
        return y + height > layout.viewportTop() && y < layout.viewportBottom();
    }

    private boolean isInsideContent(Layout layout, double mouseX, double mouseY) {
        return mouseX >= layout.contentX()
                && mouseX <= layout.contentX() + layout.contentWidth()
                && mouseY >= layout.viewportTop()
                && mouseY <= layout.viewportBottom();
    }

    private void updateMaxScroll(Layout layout) {
        maxScroll = Math.max(0.0F, pageContentHeight - layout.viewportHeight());
        clampScroll(layout);
    }

    private boolean updateSmoothScroll(Layout layout, float delta) {
        updateMaxScroll(layout);
        float before = scrollOffset;
        float smoothing = 1.0F - (float) Math.pow(0.001F, Math.max(0.0F, delta) / 8.0F);
        if (Math.abs(targetScrollOffset - scrollOffset) < 0.5F) {
            scrollOffset = targetScrollOffset;
        } else {
            scrollOffset += (targetScrollOffset - scrollOffset) * Math.max(0.0F, Math.min(1.0F, smoothing));
        }

        clampScroll(layout);
        rememberCurrentScroll();
        int oldRenderedScroll = renderedScroll;
        renderedScroll = Math.round(scrollOffset);
        return Math.round(before) != renderedScroll || oldRenderedScroll != renderedScroll;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void clampScroll(Layout layout) {
        float nextMaxScroll = Math.max(0.0F, pageContentHeight - layout.viewportHeight());
        scrollOffset = clamp(scrollOffset, 0.0F, nextMaxScroll);
        targetScrollOffset = clamp(targetScrollOffset, 0.0F, nextMaxScroll);
        maxScroll = nextMaxScroll;
        renderedScroll = Math.round(scrollOffset);
    }

    private void rememberCurrentScroll() {
        pageScrollOffsets.put(currentPage, scrollOffset);
        pageTargetScrollOffsets.put(currentPage, targetScrollOffset);
    }

    private void restorePageScroll(Page page) {
        scrollOffset = pageScrollOffsets.getOrDefault(page, 0.0F);
        targetScrollOffset = pageTargetScrollOffsets.getOrDefault(page, scrollOffset);
        renderedScroll = Math.round(scrollOffset);
    }

    private void renderScrollBar(DrawContext context, Layout layout) {
        updateMaxScroll(layout);
        if (maxScroll <= 0.5F) {
            return;
        }

        int trackX = layout.scrollBarX();
        int trackTop = layout.viewportTop();
        int trackHeight = Math.max(16, layout.viewportHeight());
        int thumbHeight = Math.max(24, Math.round(trackHeight * (layout.viewportHeight() / (float) Math.max(layout.viewportHeight(), pageContentHeight))));
        int thumbY = trackTop + Math.round((trackHeight - thumbHeight) * (scrollOffset / maxScroll));
        context.fill(trackX, trackTop, trackX + 2, trackTop + trackHeight, 0x664C5A66);
        context.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xCC7FC2FF);
    }

    private void updateNavigationScroll(Layout layout) {
        int pageCount = visiblePages().size();
        int buttonHeight = 24;
        int buttonGap = 6;
        int totalHeight = pageCount * buttonHeight + Math.max(0, pageCount - 1) * buttonGap;
        maxNavScroll = Math.max(0.0F, totalHeight - navigationHeight(layout));
        navScroll = clamp(navScroll, 0.0F, maxNavScroll);
    }

    private void renderNavigationScrollBar(DrawContext context, Layout layout) {
        updateNavigationScroll(layout);
        if (maxNavScroll <= 0.5F) {
            return;
        }

        int trackX = layout.panelX() + layout.navWidth() - 7;
        int trackTop = navigationTop(layout);
        int trackHeight = navigationHeight(layout);
        int totalHeight = Math.round(trackHeight + maxNavScroll);
        int thumbHeight = Math.max(20, Math.round(trackHeight * (trackHeight / (float) Math.max(trackHeight, totalHeight))));
        int thumbY = trackTop + Math.round((trackHeight - thumbHeight) * (navScroll / maxNavScroll));
        context.fill(trackX, trackTop, trackX + 2, trackTop + trackHeight, 0x554C5A66);
        context.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xAA7FC2FF);
    }

    private int navigationTop(Layout layout) {
        return layout.panelY() + 48;
    }

    private int navigationBottom(Layout layout) {
        return layout.panelY() + layout.panelHeight() - 14;
    }

    private int navigationHeight(Layout layout) {
        return Math.max(24, navigationBottom(layout) - navigationTop(layout));
    }

    private boolean isInsideNavigation(Layout layout, double mouseX, double mouseY) {
        return mouseX >= layout.panelX()
                && mouseX <= layout.panelX() + layout.navWidth()
                && mouseY >= navigationTop(layout)
                && mouseY <= navigationBottom(layout);
    }

    private void renderDropdownOverlays(DrawContext context, int mouseX, int mouseY, float delta) {
        for (DropdownWidget dropdown : dropdownFields.values()) {
            dropdown.renderOverlay(context, mouseX, mouseY, delta);
        }
    }

    private void renderHoverTooltip(DrawContext context) {
        if (!hoverTooltip.isBlank()) {
            int tooltipWidth = Math.min(360, Math.max(160, width - 24));
            List<Text> lines = wrapTooltipText(hoverTooltip, tooltipWidth);
            context.drawTooltip(textRenderer, lines, hoverTooltipX, hoverTooltipY);
        }
    }

    private List<Text> wrapTooltipText(String text, int maxWidth) {
        List<Text> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        while (!remaining.isEmpty()) {
            String line = textRenderer.trimToWidth(remaining, maxWidth);
            if (line.isBlank()) {
                line = remaining.substring(0, 1);
            }
            lines.add(Text.literal(line));
            remaining = remaining.substring(line.length()).trim();
        }
        return lines.isEmpty() ? List.of(Text.empty()) : lines;
    }

    private void setHoverTooltip(String tooltip, int mouseX, int mouseY) {
        if (tooltip == null || tooltip.isBlank()) {
            return;
        }

        hoverTooltip = tooltip;
        hoverTooltipX = mouseX;
        hoverTooltipY = mouseY;
    }

    private void renderToast(DrawContext context, Layout layout, float delta) {
        if (toastDurationMs <= 0L || toastMessage.isBlank()) {
            return;
        }

        long elapsedMs = System.currentTimeMillis() - toastStartTimeMs;
        long totalMs = toastDurationMs;
        if (elapsedMs >= totalMs) {
            toastDurationMs = 0L;
            return;
        }

        float age = Math.max(0L, elapsedMs);
        float alpha;
        if (age < TOAST_FADE_IN_MS) {
            alpha = smoothStep(age / TOAST_FADE_IN_MS);
        } else if (age > TOAST_FADE_IN_MS + TOAST_HOLD_MS) {
            alpha = 1.0F - smoothStep((age - TOAST_FADE_IN_MS - TOAST_HOLD_MS) / TOAST_FADE_OUT_MS);
        } else {
            alpha = 1.0F;
        }

        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        int maxWidth = Math.max(90, Math.min(260, layout.usableContentWidth() - 12));
        String text = trim(toastMessage, maxWidth - 18);
        int toastWidth = Math.min(maxWidth, textRenderer.getWidth(text) + 18);
        int toastHeight = 24;
        int toastX = layout.usableContentWidth() < 320
                ? layout.contentX() + (layout.usableContentWidth() - toastWidth) / 2
                : layout.contentX() + layout.usableContentWidth() - toastWidth - 6;
        int toastY = layout.footerTop() - toastHeight - 8 + Math.round((1.0F - alpha) * 4.0F);

        context.fill(toastX, toastY, toastX + toastWidth, toastY + toastHeight, withAlpha(0xCC101820, alpha));
        context.fill(toastX, toastY, toastX + toastWidth, toastY + 1, withAlpha(toastKind.color, alpha));
        context.fill(toastX, toastY + toastHeight - 1, toastX + toastWidth, toastY + toastHeight, withAlpha(0xFF4C5A66, alpha));
        context.fill(toastX, toastY, toastX + 1, toastY + toastHeight, withAlpha(0xFF4C5A66, alpha));
        context.fill(toastX + toastWidth - 1, toastY, toastX + toastWidth, toastY + toastHeight, withAlpha(0xFF4C5A66, alpha));
        context.drawText(textRenderer, Text.literal(text), toastX + 9, toastY + 7, withAlpha(toastKind.color, alpha), true);
    }

    private float smoothStep(float value) {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }

    private void closeDropdowns() {
        for (DropdownWidget dropdown : dropdownFields.values()) {
            dropdown.close();
        }
    }

    private boolean hasExpandedDropdown() {
        for (DropdownWidget dropdown : dropdownFields.values()) {
            if (dropdown.isExpanded()) {
                return true;
            }
        }
        return false;
    }

    private void switchPage(Page page) {
        if (page == currentPage) {
            return;
        }

        if (!applyVisibleInputs(false)) {
            return;
        }

        closeDropdowns();
        rememberCurrentScroll();
        currentPage = page;
        if (page != Page.WILDCARD) {
            selectedWildcardSettings = null;
        }
        restorePageScroll(page);
        clearAndInit();
    }

    private void toggleField(ToggleField field) {
        if (!canEditConfig() || editableConfig == null || !applyVisibleInputs(true)) {
            if (!canEditConfig()) {
                showError(configEditDeniedMessage());
            }
            return;
        }

        editableConfig = setToggle(editableConfig, field, !getToggle(editableConfig, field));
        clearAndInit();
    }

    private void setAllWildcards(boolean enabled) {
        if (!canEditConfig() || editableConfig == null || !applyVisibleInputs(true)) {
            if (!canEditConfig()) {
                showError(configEditDeniedMessage());
            }
            return;
        }

        ConfigSnapshot updated = editableConfig;
        for (ToggleField field : ToggleField.values()) {
            updated = setToggle(updated, field, enabled);
        }
        editableConfig = updated;
        clearAndInit();
    }

    private void openWildcardSettings(ToggleField field) {
        if (!hasWildcardSettings(field) || editableConfig == null) {
            return;
        }

        if (!applyVisibleInputs(true)) {
            return;
        }

        selectedWildcardSettings = field;
        clearAndInit();
    }

    private void closeWildcardSettings() {
        if (!applyVisibleInputs(true)) {
            return;
        }

        selectedWildcardSettings = null;
        clearAndInit();
    }

    private void toggleBooleanField(BooleanField field) {
        if (!canEditConfig() || editableConfig == null || !applyVisibleInputs(true)) {
            if (!canEditConfig()) {
                showError(configEditDeniedMessage());
            }
            return;
        }

        editableConfig = setBoolean(editableConfig, field, !getBoolean(editableConfig, field));
        clearAndInit();
    }

    private void selectDropdownField(DropdownField field, String value) {
        if (!canEditConfig() || editableConfig == null || !applyVisibleInputs(true)) {
            if (!canEditConfig()) {
                showError(configEditDeniedMessage());
            }
            return;
        }

        editableConfig = setDropdownValue(editableConfig, field, value);
        clearAndInit();
    }

    private void setTargetToCurrentLocation() {
        if (!canEditConfig() || editableConfig == null || !applyVisibleInputs(true)) {
            if (!canEditConfig()) {
                showError(configEditDeniedMessage());
            }
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            showError("当前未连接到世界，无法读取当前位置。");
            return;
        }

        BlockPos pos = client.player.getBlockPos();
        ModConfig copy = editableConfig.toConfig();
        copy.targetDimension = client.world.getRegistryKey().getValue().toString();
        copy.targetX = pos.getX();
        copy.targetY = pos.getY();
        copy.targetZ = pos.getZ();
        copy.validate();
        editableConfig = ConfigSnapshot.from(copy);
        showToast("已设定为当前位置，保存后生效。", StatusKind.INFO);
        clearAndInit();
    }

    private boolean applyVisibleInputs(boolean showErrors) {
        if (editableConfig == null) {
            return true;
        }

        ConfigSnapshot updated = editableConfig;
        for (Map.Entry<NumberField, TextFieldWidget> entry : numberFields.entrySet()) {
            if (!entry.getValue().active) {
                continue;
            }

            String raw = entry.getValue().getText().trim();
            int value;
            try {
                value = Integer.parseInt(raw);
            } catch (NumberFormatException exception) {
                showError(showErrors
                        ? entry.getKey().label + " 必须是整数。"
                        : "请先修正当前页的数字输入。");
                return false;
            }

            if (value < entry.getKey().minValue) {
                showError(entry.getKey().label + " 必须不小于 " + entry.getKey().minValue + "。");
                return false;
            }

            updated = setNumber(updated, entry.getKey(), value);
        }

        for (Map.Entry<StringField, TextFieldWidget> entry : stringFields.entrySet()) {
            if (!entry.getValue().active) {
                continue;
            }

            String value = entry.getValue().getText().trim();
            if (value.isBlank()) {
                showError(entry.getKey().label + " 不能为空。");
                return false;
            }

            updated = setString(updated, entry.getKey(), value);
        }

        editableConfig = updated;
        return true;
    }

    private void saveConfig() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return;
        }

        if (editableConfig == null) {
            return;
        }

        if (!canSaveConfig()) {
            showInfo("没有未保存的配置。");
            return;
        }

        if (!applyVisibleInputs(true)) {
            return;
        }

        if (!hasUnsavedChanges()) {
            showInfo("没有未保存的配置。");
            return;
        }

        cachedEditableConfig = editableConfig;
        manualSaveRequested = true;
        sendPayload(new HunterWildcardPackets.UpdateConfigPayload(editableConfig), HunterWildcardPackets.C2S_UPDATE_CONFIG, "已提交保存请求。");
    }

    private void restoreDefaultConfig() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return;
        }

        ModConfig defaults = new ModConfig();
        defaults.validate();
        editableConfig = ConfigSnapshot.from(defaults);
        cachedEditableConfig = editableConfig;
        manualReloadRequested = false;
        manualSaveRequested = false;
        showToast("已恢复默认配置，保存后生效。", StatusKind.INFO);
        clearAndInit();
    }

    private void reloadConfig() {
        if (!canEditConfig()) {
            showError(configEditDeniedMessage());
            return;
        }

        cachedEditableConfig = null;
        manualReloadRequested = true;
        sendPayload(new HunterWildcardPackets.ReloadConfigPayload(), HunterWildcardPackets.C2S_RELOAD_CONFIG, "已提交重新加载请求。");
    }

    private void requestConfig() {
        requestConfig(true);
    }

    private void requestConfig(boolean updateMessage) {
        if (updateMessage) {
            setFooterStatus("正在请求服务器数据...", StatusKind.INFO);
        }
        sendPayload(new HunterWildcardPackets.RequestConfigPayload(), HunterWildcardPackets.C2S_REQUEST_CONFIG, "正在等待服务器同步...", false);
    }

    private void sendDebugAction(DebugAction action) {
        if (!canManage) {
            showError("只有 OP 可以执行调试操作。");
            return;
        }

        if (!isDebugPageEnabled()) {
            showError("请先使用 /hw ts true 打开调试页。");
            return;
        }

        sendPayload(new HunterWildcardPackets.DebugActionPayload(action), HunterWildcardPackets.C2S_DEBUG_ACTION, "已提交调试操作。");
    }

    private void sendTestWildcard(ToggleField field) {
        if (!canManage) {
            showError("只有 OP 可以测试外卡。");
            return;
        }

        if (!isDebugPageEnabled()) {
            showError("请先使用 /hw ts true 打开调试页。");
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
            if (id.equals(HunterWildcardPackets.C2S_REQUEST_CONFIG)) {
                setFooterStatus("服务器同步失败：未启用猎人外卡同步。", StatusKind.ERROR);
            }
            if (updateMessage) {
                showError("当前服务器未启用猎人外卡同步。");
            }
            return;
        }

        try {
            ClientPlayNetworking.send(payload);
            if (updateMessage) {
                showToast(successMessage, StatusKind.INFO);
            }
        } catch (IllegalStateException exception) {
            if (id.equals(HunterWildcardPackets.C2S_REQUEST_CONFIG)) {
                setFooterStatus("服务器同步失败：当前未连接到服务器。", StatusKind.ERROR);
            }
            if (updateMessage) {
                showError("当前未连接到服务器。");
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

    private void showInfo(String message) {
        showToast(message, StatusKind.INFO);
    }

    private void showSuccess(String message) {
        showToast(message, StatusKind.SUCCESS);
    }

    private void showError(String message) {
        showToast(message, StatusKind.ERROR);
    }

    private void setFooterStatus(String message, StatusKind kind) {
        statusMessage = message;
        statusKind = kind;
    }

    private void showToast(String message, StatusKind kind) {
        toastMessage = message == null ? "" : message;
        toastKind = kind == null ? StatusKind.INFO : kind;
        toastStartTimeMs = System.currentTimeMillis();
        toastDurationMs = TOAST_FADE_IN_MS + TOAST_HOLD_MS + TOAST_FADE_OUT_MS;
    }

    private void applySync(SyncConfigPayload payload) {
        boolean preserveLocalEdits = payload.canManage()
                && editableConfig != null
                && !manualReloadRequested
                && !manualSaveRequested
                && (hasUnsavedChanges() || hasVisibleInputChanges() || hasFocusedTextField());
        serverSync = payload;
        canManage = payload.canManage();
        ensureVisiblePage();
        if (manualReloadRequested || manualSaveRequested || !preserveLocalEdits) {
            editableConfig = payload.config();
            cachedEditableConfig = null;
        } else {
            cachedEditableConfig = editableConfig;
        }

        if (manualReloadRequested) {
            showToast("已重新加载服务器配置。", StatusKind.SUCCESS);
            setFooterStatus("", StatusKind.INFO);
        } else if (manualSaveRequested) {
            showToast("已保存服务器配置。", StatusKind.SUCCESS);
            setFooterStatus("", StatusKind.INFO);
        } else if (!hasSyncedOnce) {
            setFooterStatus("", StatusKind.INFO);
        } else if ("正在请求服务器数据...".equals(statusMessage) || "正在等待服务器同步...".equals(statusMessage)) {
            setFooterStatus("", StatusKind.INFO);
        }

        hasSyncedOnce = true;
        manualReloadRequested = false;
        manualSaveRequested = false;
        clearAndInit();
    }

    private boolean isDebugPageEnabled() {
        return serverSync != null && serverSync.debugPageEnabled();
    }

    private boolean isGameActive() {
        return serverSync != null && serverSync.gameState() != GameState.WAITING;
    }

    private boolean canChangeTeam() {
        return serverSync != null && serverSync.gameState() == GameState.WAITING;
    }

    private boolean canEditConfig() {
        return canManage && serverSync != null && serverSync.gameState() == GameState.WAITING;
    }

    private String configEditDeniedMessage() {
        if (!canManage) {
            return "只有 OP 可以修改配置。";
        }

        return "游戏开始后不能修改配置。";
    }

    private List<Page> visiblePages() {
        List<Page> pages = new ArrayList<>();
        pages.add(Page.GAME);
        pages.add(Page.TEAM);
        pages.add(Page.BASIC);
        pages.add(Page.VICTORY);
        pages.add(Page.RESPAWN);
        pages.add(Page.WILDCARD);
        if (isDebugPageEnabled()) {
            pages.add(Page.DEBUG);
        }
        return pages;
    }

    private void ensureVisiblePage() {
        if (currentPage == Page.DEBUG && !isDebugPageEnabled()) {
            currentPage = Page.GAME;
            restorePageScroll(currentPage);
        }
    }

    private boolean isConfigEditPage() {
        return currentPage == Page.BASIC || currentPage == Page.VICTORY || currentPage == Page.RESPAWN || currentPage == Page.WILDCARD;
    }

    private boolean isHunterKillCountMode() {
        return editableConfig != null
                && HunterVictoryType.fromConfig(editableConfig.hunterVictoryType(), HunterVictoryType.RUNNERS_OUT) == HunterVictoryType.RUNNER_KILL_COUNT;
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
            case BLOCK_DECAY_SECONDS -> config.blockDecaySeconds();
            case PEARL_FRENZY_MAX_PEARLS -> config.pearlFrenzyMaxPearls();
            case PEARL_FRENZY_INTERVAL_SECONDS -> config.pearlFrenzyIntervalSeconds();
            case WIND_CHARGE_BRAWL_INTERVAL_SECONDS -> config.windChargeBrawlIntervalSeconds();
            case WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT -> config.windChargeExplosionMultiplierPercent();
            case HUNTER_PREPARE_BOUNDARY_RADIUS -> config.hunterPrepareBoundaryRadius();
            case HUNTER_PREPARE_BOUNDARY_WARN_DISTANCE -> config.hunterPrepareBoundaryWarnDistance();
            case SURVIVE_TIME_SECONDS -> config.surviveTimeSeconds();
            case TARGET_X -> config.targetX();
            case TARGET_Y -> config.targetY();
            case TARGET_Z -> config.targetZ();
            case TARGET_RADIUS -> config.targetRadius();
            case TARGET_ITEM_COUNT -> config.targetItemCount();
            case HUNTER_LIVES -> config.hunterLives();
            case RUNNER_LIVES -> config.runnerLives();
            case RUNNER_RESPAWN_SECONDS -> config.runnerRespawnSeconds();
            case HUNTER_RUNNER_KILL_TARGET -> config.hunterRunnerKillTarget();
        };
    }

    private ConfigSnapshot setNumber(ConfigSnapshot config, NumberField field, int value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case PREPARING_SECONDS -> copy.preparingSeconds = value;
            case ENDING_SECONDS -> copy.endingSeconds = value;
            case COMPASS_UPDATE_SECONDS -> copy.compassUpdateSeconds = value;
            case HUNTER_RESPAWN_SECONDS -> copy.hunterRespawnSeconds = value;
            case WILDCARD_INTERVAL_SECONDS -> copy.wildcardIntervalSeconds = value;
            case WILDCARD_DURATION_SECONDS -> copy.wildcardDurationSeconds = value;
            case ACTION_BAR_INTERVAL_SECONDS -> copy.actionBarIntervalSeconds = value;
            case HUNTER_RADAR_INTERVAL_SECONDS -> copy.hunterRadarIntervalSeconds = value;
            case SUPPLY_DROP_INTERVAL_SECONDS -> copy.supplyDropIntervalSeconds = value;
            case BLOCK_DECAY_SECONDS -> copy.blockDecaySeconds = value;
            case PEARL_FRENZY_MAX_PEARLS -> copy.pearlFrenzyMaxPearls = value;
            case PEARL_FRENZY_INTERVAL_SECONDS -> copy.pearlFrenzyIntervalSeconds = value;
            case WIND_CHARGE_BRAWL_INTERVAL_SECONDS -> copy.windChargeBrawlIntervalSeconds = value;
            case WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT -> copy.windChargeExplosionMultiplierPercent = value;
            case HUNTER_PREPARE_BOUNDARY_RADIUS -> copy.hunterPrepareBoundaryRadius = value;
            case HUNTER_PREPARE_BOUNDARY_WARN_DISTANCE -> copy.hunterPrepareBoundaryWarnDistance = value;
            case SURVIVE_TIME_SECONDS -> copy.surviveTimeSeconds = value;
            case TARGET_X -> copy.targetX = value;
            case TARGET_Y -> copy.targetY = value;
            case TARGET_Z -> copy.targetZ = value;
            case TARGET_RADIUS -> copy.targetRadius = value;
            case TARGET_ITEM_COUNT -> copy.targetItemCount = value;
            case HUNTER_LIVES -> copy.hunterLives = value;
            case RUNNER_LIVES -> copy.runnerLives = value;
            case RUNNER_RESPAWN_SECONDS -> copy.runnerRespawnSeconds = value;
            case HUNTER_RUNNER_KILL_TARGET -> copy.hunterRunnerKillTarget = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private String getString(ConfigSnapshot config, StringField field) {
        return switch (field) {
            case TARGET_ITEM_ID -> config.targetItemId();
        };
    }

    private ConfigSnapshot setString(ConfigSnapshot config, StringField field, String value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case TARGET_ITEM_ID -> copy.targetItemId = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private String getDropdownValue(ConfigSnapshot config, DropdownField field) {
        return switch (field) {
            case RUNNER_VICTORY_TYPE -> config.runnerVictoryType();
            case HUNTER_VICTORY_TYPE -> config.hunterVictoryType();
            case HUNTER_RESPAWN_MODE -> config.hunterRespawnMode();
            case RUNNER_RESPAWN_MODE -> config.runnerRespawnMode();
            case RUNNER_TEAM_LOSS_MODE -> config.runnerTeamLossMode();
            case TARGET_DIMENSION -> config.targetDimension();
        };
    }

    private ConfigSnapshot setDropdownValue(ConfigSnapshot config, DropdownField field, String value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case RUNNER_VICTORY_TYPE -> copy.runnerVictoryType = value;
            case HUNTER_VICTORY_TYPE -> copy.hunterVictoryType = value;
            case HUNTER_RESPAWN_MODE -> copy.hunterRespawnMode = value;
            case RUNNER_RESPAWN_MODE -> copy.runnerRespawnMode = value;
            case RUNNER_TEAM_LOSS_MODE -> copy.runnerTeamLossMode = value;
            case TARGET_DIMENSION -> copy.targetDimension = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
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
            case HUNGER_CHASE -> config.enableHungerChase();
            case WEAPON_OVERHEAT -> config.enableWeaponOverheat();
            case LIGHT_LOAD -> config.enableLightLoad();
            case BLOCK_DECAY -> config.enableBlockDecay();
            case PEARL_FRENZY -> config.enablePearlFrenzy();
            case WIND_CHARGE_BRAWL -> config.enableWindChargeBrawl();
            case BLOOD_RAGE -> config.enableBloodRage();
            case DISABLED_WILDCARD -> config.enableDisabledWildcard();
        };
    }

    private ConfigSnapshot setToggle(ConfigSnapshot config, ToggleField field, boolean value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case SPEED_RUSH -> copy.enableSpeedRush = value;
            case FEATHERWEIGHT -> copy.enableFeatherweight = value;
            case GLOWING -> copy.enableGlowing = value;
            case NIGHT_HUNT -> copy.enableNightHunt = value;
            case EXPLOSIVE_DEATH -> copy.enableExplosiveDeath = value;
            case SUPPLY_DROP -> copy.enableSupplyDrop = value;
            case HUNTER_RADAR -> copy.enableHunterRadar = value;
            case COMPASS_CHAOS -> copy.enableCompassChaos = value;
            case HUNGER_CHASE -> copy.enableHungerChase = value;
            case WEAPON_OVERHEAT -> copy.enableWeaponOverheat = value;
            case LIGHT_LOAD -> copy.enableLightLoad = value;
            case BLOCK_DECAY -> copy.enableBlockDecay = value;
            case PEARL_FRENZY -> copy.enablePearlFrenzy = value;
            case WIND_CHARGE_BRAWL -> copy.enableWindChargeBrawl = value;
            case BLOOD_RAGE -> copy.enableBloodRage = value;
            case DISABLED_WILDCARD -> copy.enableDisabledWildcard = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private boolean getBoolean(ConfigSnapshot config, BooleanField field) {
        return switch (field) {
            case HUNTER_PREPARE_BOUNDARY_ENABLED -> config.hunterPrepareBoundaryEnabled();
            case RUNNER_DEATH_NO_DROPS -> config.runnerDeathNoDrops();
            case HUNTER_DEATH_NO_DROPS -> config.hunterDeathNoDrops();
        };
    }

    private ConfigSnapshot setBoolean(ConfigSnapshot config, BooleanField field, boolean value) {
        ModConfig copy = config.toConfig();
        switch (field) {
            case HUNTER_PREPARE_BOUNDARY_ENABLED -> copy.hunterPrepareBoundaryEnabled = value;
            case RUNNER_DEATH_NO_DROPS -> copy.runnerDeathNoDrops = value;
            case HUNTER_DEATH_NO_DROPS -> copy.hunterDeathNoDrops = value;
        }
        copy.validate();
        return ConfigSnapshot.from(copy);
    }

    private String stateName(GameState state) {
        return switch (state) {
            case WAITING -> "等待中";
            case PREPARING -> "准备中";
            case RUNNING -> "运行中";
            case ENDING -> "结算中";
        };
    }

    private int stateColor(GameState state) {
        return switch (state) {
            case WAITING -> 0xFF9FAAB4;
            case PREPARING -> 0xFFFFD966;
            case RUNNING -> 0xFF77E287;
            case ENDING -> 0xFFFF8A8A;
        };
    }

    private String formatSeconds(int seconds) {
        return seconds < 0 ? "无" : seconds + " 秒";
    }

    private String wildcardDisplayName() {
        if (serverSync == null || serverSync.activeWildcard() == null || serverSync.activeWildcard().isBlank() || "无".equals(serverSync.activeWildcard())) {
            return "暂无外卡";
        }
        return serverSync.activeWildcard();
    }

    private String compactWildcardDisplayName() {
        String name = wildcardDisplayName();
        return "暂无外卡".equals(name) ? "无" : name;
    }

    private boolean hasWildcardSettings(ToggleField field) {
        return field == ToggleField.HUNTER_RADAR
                || field == ToggleField.SUPPLY_DROP
                || field == ToggleField.BLOCK_DECAY
                || field == ToggleField.PEARL_FRENZY
                || field == ToggleField.WIND_CHARGE_BRAWL;
    }

    private int enabledWildcardCount(ConfigSnapshot config) {
        int count = 0;
        for (ToggleField field : ToggleField.values()) {
            if (getToggle(config, field)) {
                count++;
            }
        }
        return count;
    }

    private String startGameTooltip(boolean waiting) {
        if (!canManage) {
            return "只有 OP 可以开始游戏。";
        }

        if (!waiting) {
            return "只有等待中状态可以开始游戏。";
        }

        if (serverSync == null || serverSync.hunterCount() == 0 || serverSync.runnerCount() == 0) {
            return "至少需要 1 名猎人和 1 名逃亡者。";
        }

        return "";
    }

    private String startConditionDisplay(String startTooltip) {
        if (startTooltip == null || startTooltip.isBlank()) {
            return "满足";
        }

        if (startTooltip.contains("至少需要")) {
            return "需要 1 猎人 + 1 逃亡者";
        }
        if (startTooltip.contains("只有 OP")) {
            return "仅 OP 可开始";
        }
        if (startTooltip.contains("等待中")) {
            return "仅等待中可开始";
        }
        return startTooltip;
    }

    private int wildcardToggleColumns(int width) {
        if (width >= 420) {
            return 2;
        }
        return 1;
    }

    private int teamButtonColumns(int width) {
        if (width >= 620) {
            return 3;
        }
        if (width >= 380) {
            return 2;
        }
        return 1;
    }

    private String trim(String text, int width) {
        return textRenderer.trimToWidth(text, Math.max(10, width));
    }

    private int withAlpha(int color, float alpha) {
        int baseAlpha = color >>> 24;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }

    private void renderFooterStatus(DrawContext context, Layout layout) {
        List<StatusSegment> segments = footerSegments();
        if (segments.isEmpty()) {
            return;
        }

        int x = layout.contentX();
        int y = layout.footerTop() + 8;
        int right = layout.contentX() + layout.usableContentWidth();
        for (int i = 0; i < segments.size(); i++) {
            StatusSegment segment = segments.get(i);
            if (x >= right) {
                return;
            }

            String text = trim(segment.text(), right - x);
            if (!text.isBlank()) {
                context.drawText(textRenderer, Text.literal(text), x, y, segment.color(), false);
                x += textRenderer.getWidth(text);
            }

            if (i < segments.size() - 1 && x + textRenderer.getWidth(" | ") < right) {
                context.drawText(textRenderer, Text.literal(" | "), x, y, 0xFF6F7C86, false);
                x += textRenderer.getWidth(" | ");
            }
        }
    }

    private List<StatusSegment> footerSegments() {
        List<StatusSegment> segments = new ArrayList<>();
        if (serverSync == null) {
            if (!statusMessage.isBlank()) {
                segments.add(new StatusSegment(statusMessage, statusKind.color));
            }
            return segments;
        }

        if (isConfigEditPage()) {
            String modeText = canEditConfig()
                    ? "配置模式：可修改"
                    : (canManage ? "配置锁定：游戏已开始" : "配置模式：只读");
            int modeColor = canEditConfig() ? 0xFF7FC2FF : 0xFF9FAAB4;
            segments.add(new StatusSegment(modeText, modeColor));
            if (hasUnsavedChanges()) {
                segments.add(new StatusSegment("未保存修改", 0xFFFFB347));
            }
            if (!statusMessage.isBlank() && (statusKind == StatusKind.ERROR || !hasUnsavedChanges())) {
                segments.add(new StatusSegment(statusMessage, statusKind.color));
            }
            return segments;
        }

        if (!statusMessage.isBlank()) {
            segments.add(new StatusSegment(statusMessage, statusKind.color));
        }
        return segments;
    }

    private boolean hasUnsavedChanges() {
        return canEditConfig() && editableConfig != null && serverSync != null && !editableConfig.equals(serverSync.config());
    }

    private boolean canSaveConfig() {
        return canEditConfig()
                && editableConfig != null
                && serverSync != null
                && (hasUnsavedChanges() || hasVisibleInputChanges());
    }

    private boolean hasVisibleInputChanges() {
        if (!canEditConfig() || editableConfig == null) {
            return false;
        }

        for (Map.Entry<NumberField, TextFieldWidget> entry : numberFields.entrySet()) {
            TextFieldWidget field = entry.getValue();
            if (!field.active) {
                continue;
            }

            String raw = field.getText().trim();
            if (raw.isBlank()) {
                return true;
            }

            try {
                if (Integer.parseInt(raw) != getNumber(editableConfig, entry.getKey())) {
                    return true;
                }
            } catch (NumberFormatException exception) {
                return true;
            }
        }

        for (Map.Entry<StringField, TextFieldWidget> entry : stringFields.entrySet()) {
            TextFieldWidget field = entry.getValue();
            if (field.active && !field.getText().trim().equals(getString(editableConfig, entry.getKey()))) {
                return true;
            }
        }

        return false;
    }

    private boolean hasFocusedTextField() {
        for (TextFieldWidget field : numberFields.values()) {
            if (field.isFocused()) {
                return true;
            }
        }

        for (TextFieldWidget field : stringFields.values()) {
            if (field.isFocused()) {
                return true;
            }
        }

        return false;
    }

    private void ensureFocusedInputVisible(Layout layout) {
        for (TextFieldWidget field : numberFields.values()) {
            if (field.isFocused()) {
                ensureVisible(layout, field.getY(), field.getHeight());
                return;
            }
        }

        for (TextFieldWidget field : stringFields.values()) {
            if (field.isFocused()) {
                ensureVisible(layout, field.getY(), field.getHeight());
                return;
            }
        }
    }

    private void ensureVisible(Layout layout, int widgetY, int widgetHeight) {
        int margin = 8;
        if (widgetY < layout.viewportTop() + margin) {
            targetScrollOffset -= layout.viewportTop() + margin - widgetY;
        } else if (widgetY + widgetHeight > layout.viewportBottom() - margin) {
            targetScrollOffset += widgetY + widgetHeight - (layout.viewportBottom() - margin);
        }

        updateMaxScroll(layout);
        targetScrollOffset = clamp(targetScrollOffset, 0.0F, maxScroll);
        rememberCurrentScroll();
    }

    private Layout layout() {
        int panelWidth = Math.min(1120, Math.max(360, width - 24));
        if (panelWidth > width - 8) {
            panelWidth = Math.max(220, width - 8);
        }

        int panelHeight = Math.min(520, Math.max(260, height - 24));
        if (panelHeight > height - 8) {
            panelHeight = Math.max(200, height - 8);
        }

        int panelX = (width - panelWidth) / 2;
        int panelY = (height - panelHeight) / 2;
        int navWidth = panelWidth < 520 ? 92 : panelWidth < 760 ? 104 : 116;
        int contentGap = panelWidth < 760 ? 12 : 16;
        int rightPadding = panelWidth < 760 ? 10 : 12;
        int contentX = panelX + navWidth + contentGap;
        int contentY = panelY + 48;
        int contentWidth = panelX + panelWidth - contentX - rightPadding;
        int footerHeight = 50;
        int footerTop = panelY + panelHeight - footerHeight;
        int viewportTop = contentY;
        int viewportBottom = Math.max(viewportTop + 40, footerTop - 12);
        int scrollBarX = contentX + contentWidth - 6;
        return new Layout(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                navWidth,
                contentX,
                contentY,
                contentWidth,
                viewportTop,
                viewportBottom,
                footerTop,
                footerHeight,
                SCROLL_BAR_RESERVE,
                scrollBarX
        );
    }

    private class CardBuilder {
        private final Layout layout;
        private final int x;
        private final int y;
        private final int width;
        private int cursorY;
        private int bottomY;
        private boolean finished;

        CardBuilder(Layout layout, int x, int y, int width, String title) {
            this.layout = layout;
            this.x = x;
            this.y = y;
            this.width = width;
            this.cursorY = contentStartY(y);
            this.bottomY = cursorY;
            addCardTitle(layout, title, x + CARD_PADDING_X, y + CARD_PADDING_TOP);
        }

        int width() {
            return width;
        }

        void info(String label, String value, int valueColor) {
            addInfoRow(layout, label, value, contentX(), cursorY, contentWidth(), valueColor);
            advanceRow();
        }

        void number(NumberField field) {
            number(field, canManage);
        }

        void number(NumberField field, boolean editable) {
            addInputRow(layout, field, contentX(), cursorY, contentWidth(), editable);
            advanceRow();
        }

        void coordinates(NumberField xField, NumberField yField, NumberField zField) {
            int nextY = addCoordinateRow(layout, xField, yField, zField, contentX(), cursorY, contentWidth());
            bottomY = Math.max(bottomY, nextY - ROW_GAP);
            cursorY = nextY;
        }

        void numberPair(NumberField left, NumberField right) {
            int contentWidth = contentWidth();
            if (contentWidth < 420) {
                number(left);
                number(right);
                return;
            }

            int gap = 12;
            int fieldWidth = (contentWidth - gap) / 2;
            addInputRow(layout, left, contentX(), cursorY, fieldWidth, canManage);
            addInputRow(layout, right, contentX() + fieldWidth + gap, cursorY, fieldWidth, canManage);
            advanceRow();
        }

        void string(StringField field) {
            addInputRow(layout, field, contentX(), cursorY, contentWidth());
            advanceRow();
        }

        void dropdown(DropdownField field) {
            dropdown(field, canManage);
        }

        void dropdown(DropdownField field, boolean editable) {
            addDropdownRow(layout, field, contentX(), cursorY, contentWidth(), editable);
            advanceRow();
        }

        void booleanField(BooleanField field) {
            addToggleRow(layout, field, contentX(), cursorY, contentWidth());
            advanceRow();
        }

        void hint(String text) {
            addHintText(layout, text, contentX(), cursorY + 2, contentWidth());
            bottomY = Math.max(bottomY, cursorY + HINT_HEIGHT);
            cursorY += HINT_HEIGHT + ROW_GAP;
        }

        void button(String title, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled, int maxWidth) {
            buttonGrid(List.of(new ButtonSpec(title, action, variant, enabled)), 1, maxWidth);
        }

        void buttonGrid(List<ButtonSpec> buttons, int requestedColumns, int maxButtonWidth) {
            int nextY = addButtonRow(layout, buttons, contentX(), cursorY, contentWidth(), requestedColumns, maxButtonWidth);
            bottomY = Math.max(bottomY, nextY - ROW_GAP);
            cursorY = nextY;
        }

        void gap(int height) {
            cursorY += Math.max(0, height);
            bottomY = Math.max(bottomY, cursorY);
        }

        void toggleGrid(ToggleField[] fields, int requestedColumns) {
            int gap = 8;
            int columns = Math.max(1, Math.min(requestedColumns, fields.length));
            while (columns > 1 && (contentWidth() - (columns - 1) * gap) / columns < 118) {
                columns--;
            }

            int itemWidth = Math.min(WILDCARD_TOGGLE_MAX_WIDTH, Math.max(80, (contentWidth() - (columns - 1) * gap) / columns));
            int gridWidth = itemWidth * columns + (columns - 1) * gap;
            int gridX = contentX() + Math.max(0, (contentWidth() - gridWidth) / 2);
            int rows = (fields.length + columns - 1) / columns;
            for (int i = 0; i < fields.length; i++) {
                ToggleField field = fields[i];
                int column = i % columns;
                int row = i / columns;
                addWildcardToggleTile(
                        layout,
                        field,
                        gridX + column * (itemWidth + gap),
                        cursorY + row * (WILDCARD_TOGGLE_HEIGHT + gap),
                        itemWidth,
                        WILDCARD_TOGGLE_HEIGHT
                );
            }
            int nextY = cursorY + rows * WILDCARD_TOGGLE_HEIGHT + (rows - 1) * gap + ROW_GAP;
            bottomY = Math.max(bottomY, nextY - ROW_GAP);
            cursorY = nextY;
        }

        int height() {
            return Math.max(CARD_PADDING_TOP + CARD_TITLE_HEIGHT + CARD_PADDING_BOTTOM, bottomY - y + CARD_PADDING_BOTTOM);
        }

        int finish() {
            return finish(height());
        }

        int finish(int forcedHeight) {
            if (!finished) {
                drawCardBorder(layout, x, y, width, forcedHeight);
                finished = true;
            }
            return y + forcedHeight;
        }

        private int contentX() {
            return x + CARD_PADDING_X;
        }

        private int contentWidth() {
            return Math.max(40, width - CARD_PADDING_X * 2);
        }

        private void advanceRow() {
            bottomY = Math.max(bottomY, rowContentBottom(cursorY));
            cursorY = nextRowY(cursorY);
        }
    }

    @FunctionalInterface
    private interface CardBody {
        void build(CardBuilder card);
    }

    private record ButtonSpec(String title, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled, String tooltip) {
        ButtonSpec(String title, ButtonWidget.PressAction action, ButtonVariant variant, boolean enabled) {
            this(title, action, variant, enabled, "");
        }
    }

    private enum Page {
        GAME("游戏状态", "查看当前对局、队伍、身份和外卡状态"),
        TEAM("队伍管理", "选择你的阵营，查看双方人数"),
        BASIC("基础规则", "设置游戏的基本时间、边界和对局参数"),
        VICTORY("胜利规则", "设置逃亡者与猎人的胜利方式"),
        RESPAWN("生命复活", "设置双方生命数、复活模式和复活时间"),
        WILDCARD("外卡规则", "设置外卡触发间隔、启用状态和具体外卡"),
        DEBUG("调试操作", "执行调试命令开启后的测试操作");

        private final String label;
        private final String description;

        Page(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    private enum NumberField {
        PREPARING_SECONDS("准备时间", "秒", 1),
        ENDING_SECONDS("结算时间", "秒", 1),
        COMPASS_UPDATE_SECONDS("指南针刷新", "秒", 1),
        HUNTER_RESPAWN_SECONDS("猎人复活", "秒", 1),
        WILDCARD_INTERVAL_SECONDS("外卡间隔", "秒", 1),
        WILDCARD_DURATION_SECONDS("外卡持续", "秒", 1),
        ACTION_BAR_INTERVAL_SECONDS("状态栏刷新", "秒", 1),
        HUNTER_RADAR_INTERVAL_SECONDS("雷达播报", "秒", 1),
        SUPPLY_DROP_INTERVAL_SECONDS("空投间隔", "秒", 1),
        BLOCK_DECAY_SECONDS("方块消失时间", "秒", 1),
        PEARL_FRENZY_MAX_PEARLS("珍珠上限", "个", 1),
        PEARL_FRENZY_INTERVAL_SECONDS("珍珠补给间隔", "秒", 1),
        WIND_CHARGE_BRAWL_INTERVAL_SECONDS("风弹补给间隔", "秒", 1),
        WIND_CHARGE_EXPLOSION_MULTIPLIER_PERCENT("风弹反弹倍率", "%", 1),
        HUNTER_PREPARE_BOUNDARY_RADIUS("准备区域半径", "格", 1),
        HUNTER_PREPARE_BOUNDARY_WARN_DISTANCE("边界警告距离", "格", 0),
        SURVIVE_TIME_SECONDS("存活时间", "秒", 1),
        TARGET_X("目标 X", "", Integer.MIN_VALUE),
        TARGET_Y("目标 Y", "", Integer.MIN_VALUE),
        TARGET_Z("目标 Z", "", Integer.MIN_VALUE),
        TARGET_RADIUS("目标半径", "格", 1),
        TARGET_ITEM_COUNT("目标物品数量", "个", 1),
        HUNTER_LIVES("猎人生命数", "条", 0),
        RUNNER_LIVES("逃亡者生命数", "条", 1),
        RUNNER_RESPAWN_SECONDS("逃亡者复活", "秒", 1),
        HUNTER_RUNNER_KILL_TARGET("击杀目标", "次", 1);

        private final String label;
        private final String unit;
        private final int minValue;

        NumberField(String label, String unit, int minValue) {
            this.label = label;
            this.unit = unit;
            this.minValue = minValue;
        }

        private boolean allowsNegative() {
            return minValue < 0;
        }
    }

    private enum StringField {
        TARGET_ITEM_ID("目标物品 ID", 128);

        private final String label;
        private final int maxLength;

        StringField(String label, int maxLength) {
            this.label = label;
            this.maxLength = maxLength;
        }
    }

    private enum DropdownField {
        RUNNER_VICTORY_TYPE("逃亡者胜利条件", List.of(
                option(RunnerVictoryType.DRAGON.name(), RunnerVictoryType.DRAGON.getDisplayName()),
                option(RunnerVictoryType.SURVIVE_TIME.name(), RunnerVictoryType.SURVIVE_TIME.getDisplayName()),
                option(RunnerVictoryType.REACH_LOCATION.name(), RunnerVictoryType.REACH_LOCATION.getDisplayName()),
                option(RunnerVictoryType.COLLECT_ITEM.name(), RunnerVictoryType.COLLECT_ITEM.getDisplayName())
        )),
        HUNTER_VICTORY_TYPE("猎人胜利方式", List.of(
                option(HunterVictoryType.RUNNERS_OUT.name(), "淘汰逃亡者"),
                option(HunterVictoryType.RUNNER_KILL_COUNT.name(), "达到击杀数")
        )),
        HUNTER_RESPAWN_MODE("猎人复活模式", List.of(
                option(RespawnMode.INFINITE.name(), "无限复活"),
                option(RespawnMode.LIMITED_LIVES.name(), "有限生命"),
                option(RespawnMode.NO_RESPAWN.name(), "不复活")
        )),
        RUNNER_RESPAWN_MODE("逃亡者复活模式", List.of(
                option(RespawnMode.INFINITE.name(), "无限复活"),
                option(RespawnMode.LIMITED_LIVES.name(), "有限生命"),
                option(RespawnMode.NO_RESPAWN.name(), "不复活")
        )),
        RUNNER_TEAM_LOSS_MODE("逃亡者失败规则", List.of(
                option(RunnerTeamLossMode.ANY_RUNNER_OUT.name(), "任意出局即失败"),
                option(RunnerTeamLossMode.ALL_RUNNERS_OUT.name(), "全员出局才失败")
        )),
        TARGET_DIMENSION("目标维度", List.of(
                option("minecraft:overworld", "主世界"),
                option("minecraft:the_nether", "下界"),
                option("minecraft:the_end", "末地")
        ));

        private final String label;
        private final List<DropdownWidget.Option> options;

        DropdownField(String label, List<DropdownWidget.Option> options) {
            this.label = label;
            this.options = options;
        }
    }

    private static DropdownWidget.Option option(String value, String displayName) {
        return new DropdownWidget.Option(value, displayName);
    }

    private enum BooleanField {
        HUNTER_PREPARE_BOUNDARY_ENABLED("启用猎人准备区域"),
        RUNNER_DEATH_NO_DROPS("逃亡者死亡不掉落"),
        HUNTER_DEATH_NO_DROPS("猎人死亡不掉落");

        private final String label;

        BooleanField(String label) {
            this.label = label;
        }
    }

    private enum ToggleField {
        SPEED_RUSH("疾速追猎", "全员加速。"),
        FEATHERWEIGHT("轻盈之身", "跳跃提升，缓慢落地。"),
        GLOWING("全员发光", "全员发光。"),
        NIGHT_HUNT("暗夜追猎", "入夜，猎人夜视。"),
        EXPLOSIVE_DEATH("死亡爆炸", "死亡或击杀会爆炸。"),
        SUPPLY_DROP("补给空投", "落下随机补给箱。"),
        HUNTER_RADAR("猎人雷达", "猎人获得距离提示。"),
        COMPASS_CHAOS("指南针干扰", "猎人指南针偏移。"),
        HUNGER_CHASE("饥饿追逐", "更易饥饿，进食获得速度效果"),
        WEAPON_OVERHEAT("武器过热", "连打会过热。"),
        LIGHT_LOAD("轻装上阵", "轻甲加速，重甲减速。"),
        BLOCK_DECAY("方块腐化", "新放方块会消失。"),
        PEARL_FRENZY("珍珠狂潮", "定期获得珍珠，但别随便扔：）"),
        WIND_CHARGE_BRAWL("风弹乱斗", "定期获得风弹。"),
        BLOOD_RAGE("血怒时刻", "低血量获得强化。"),
        DISABLED_WILDCARD("暂时停用", "没有额外效果。");

        private final String label;
        private final String description;

        ToggleField(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }

    private enum ButtonVariant {
        PRIMARY,
        NORMAL,
        SELECTED,
        DANGER,
        TOGGLE_ON,
        TOGGLE_OFF,
        DISABLED
    }

    private enum StatusKind {
        INFO(0xFFFFD966),
        SUCCESS(0xFF77E287),
        ERROR(0xFFFF8A8A);

        private final int color;

        StatusKind(int color) {
            this.color = color;
        }
    }

    private record Layout(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int navWidth,
            int contentX,
            int contentY,
            int contentWidth,
            int viewportTop,
            int viewportBottom,
            int footerTop,
            int footerHeight,
            int scrollBarReserve,
            int scrollBarX
    ) {
        int usableContentWidth() {
            return Math.max(80, contentWidth - scrollBarReserve);
        }

        int viewportHeight() {
            return viewportBottom - viewportTop;
        }
    }

    private record StatusSegment(String text, int color) {
    }

    private record StatusBlock(String label, String value, int color) {
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

    private record WrappedLabel(String text, int x, int y, int height, int color, boolean shadow, int width, int maxLines) {
        int maxWidth(Layout layout) {
            return width > 0 ? width : Math.max(20, layout.panelX() + layout.panelWidth() - x - 12);
        }
    }

    private record Box(int x, int y, int width, int height, int color, int borderColor) {
    }

    private record Icon(ItemStack stack, int x, int y) {
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
            if (isHovered() && !description.isBlank()) {
                setHoverTooltip(description, mouseX, mouseY);
            }
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
            int titlePadding = height <= 18 ? 1 : 4;
            int titleY = y + Math.max(titlePadding, (height - textRenderer.fontHeight) / 2);
            if ("i".equals(title) && height <= 14) {
                titleY = y + Math.max(0, (height - textRenderer.fontHeight + 1) / 2);
            }
            context.drawText(textRenderer, net.minecraft.text.Text.literal(title), titleX, titleY, palette.titleColor, true);
        }

        @Override
        protected void drawLabel(net.minecraft.client.font.DrawnTextConsumer textConsumer) {
        }

        private Palette palette(ButtonVariant variant, boolean hovered) {
            return switch (variant) {
                case PRIMARY -> new Palette(hovered ? 0xCC246C86 : 0xAA1F536A, hovered ? 0xFF7FE7FF : 0xFF54B8D6, 0xFF7FE7FF, 0xFFFFFFFF, 0xFFD7F8FF);
                case SELECTED -> new Palette(0xAA345B78, 0xFF7FC2FF, 0xFF7FC2FF, 0xFFFFFFFF, 0xFFD7ECFF);
                case DANGER -> new Palette(hovered ? 0xAA6D3434 : 0x8845292F, hovered ? 0xFFFF8A8A : 0xFFD76474, 0xFFFF8A8A, 0xFFFFFFFF, 0xFFFFC2C8);
                case TOGGLE_ON -> new Palette(hovered ? 0xAA2E5C49 : 0x88324B3F, hovered ? 0xFF77E287 : 0xFF55B978, 0xFF77E287, 0xFFFFFFFF, 0xFFD7F8E1);
                case TOGGLE_OFF -> new Palette(hovered ? 0xAA3A4652 : 0x88303A46, hovered ? 0xFF8A98A6 : 0xFF59636C, 0xFF8A98A6, 0xFFE1E6EB, 0xFF9FAAB4);
                case DISABLED -> new Palette(0x66303A46, 0xFF59636C, 0xFF59636C, 0xFF9FAAB4, 0xFF9FAAB4);
                default -> new Palette(hovered ? 0xAA3E5570 : 0x88303A46, hovered ? 0xFF74B6FF : 0xFF4C5A66, 0xFF74B6FF, 0xFFFFFFFF, 0xFFC9D4DE);
            };
        }
    }

    private record Palette(int background, int border, int accent, int titleColor, int descriptionColor) {
    }
}
