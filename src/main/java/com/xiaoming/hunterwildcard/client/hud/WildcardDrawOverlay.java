package com.xiaoming.hunterwildcard.client.hud;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WildcardDrawOverlay {
    private static final long DRAW_FADE_IN_MS = 180L;
    private static final long DRAW_SPIN_MS = 4300L;
    private static final long DRAW_FADE_OUT_MS = 300L;
    private static final long DRAW_TOTAL_MS = 5000L;
    private static final long FEEDBACK_IN_MS = 260L;
    private static final long FEEDBACK_HOLD_MS = 2300L;
    private static final long FEEDBACK_OUT_MS = 420L;
    private static final long FEEDBACK_TOTAL_MS = FEEDBACK_IN_MS + FEEDBACK_HOLD_MS + FEEDBACK_OUT_MS;
    private static final long INTRO_SLIDE_MS = 260L;
    private static final long OBJECTIVE_SLIDE_MS = 260L;
    private static final long OBJECTIVE_NOTICE_IN_MS = 260L;
    private static final long OBJECTIVE_NOTICE_HOLD_MS = 2300L;
    private static final long OBJECTIVE_NOTICE_OUT_MS = 420L;
    private static final long OBJECTIVE_NOTICE_TOTAL_MS = OBJECTIVE_NOTICE_IN_MS + OBJECTIVE_NOTICE_HOLD_MS + OBJECTIVE_NOTICE_OUT_MS;
    private static final float OBJECTIVE_PANEL_SCALE = 0.8F;
    private static final String[] SPIN_NAMES = {
            "疾速追猎",
            "轻盈之身",
            "全员发光",
            "暗夜追猎",
            "死亡爆炸",
            "补给空投",
            "猎人雷达",
            "指南针干扰",
            "饥饿追逐",
            "武器过热",
            "轻装上阵",
            "方块腐化",
            "珍珠狂潮",
            "风弹乱斗",
            "血怒时刻",
            "暂时停用"
    };

    private static long drawStartTimeMs = -1L;
    private static String finalWildcard = "";
    private static boolean revealSoundPlayed;
    private static boolean introVisible;
    private static boolean introHiding;
    private static long introTransitionStartTimeMs = -1L;
    private static String introName = "";
    private static String introDescription = "";
    private static boolean weaponOverheatVisible;
    private static int weaponOverheatHeat;
    private static int weaponOverheatMaxHeat = 1;
    private static final List<FeedbackEntry> feedbackEntries = new ArrayList<>();
    private static boolean objectiveVisible;
    private static boolean objectiveHiding;
    private static long objectiveTransitionStartTimeMs = -1L;
    private static String objectiveText = "";
    private static String objectiveStyle = "runner";
    private static final List<ObjectiveNoticeEntry> objectiveNoticeEntries = new ArrayList<>();

    private WildcardDrawOverlay() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                Identifier.of(HunterWildcardMod.MOD_ID, "wildcard_draw_overlay"),
                WildcardDrawOverlay::render
        );
    }

    public static void start(String wildcardName) {
        finalWildcard = wildcardName == null || wildcardName.isBlank() ? "未知外卡" : wildcardName;
        drawStartTimeMs = System.currentTimeMillis();
        revealSoundPlayed = false;

        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.2F));
    }

    public static void showKillFeedback(String hunterName, String runnerName, int remainingKills, int currentKills, int targetKills) {
        String hunter = hunterName == null || hunterName.isBlank() ? "猎人" : hunterName;
        String runner = runnerName == null || runnerName.isBlank() ? "逃亡者" : runnerName;
        int remaining = Math.max(0, remainingKills);
        int current = Math.max(0, currentKills);
        int target = Math.max(1, targetKills);
        String status = remaining <= 0 ? "击杀目标完成" : "还差 " + remaining + " 次胜利 (" + current + "/" + target + ")";
        showFeedback("有效击杀", hunter + " -> " + runner, status, "hunter");
    }

    public static void showFeedback(String title, String line1, String line2, String style) {
        feedbackEntries.add(new FeedbackEntry(
                title == null || title.isBlank() ? "反馈" : title,
                line1 == null ? "" : line1,
                line2 == null ? "" : line2,
                style == null || style.isBlank() ? "neutral" : style,
                System.currentTimeMillis()
        ));

        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.15F));
    }

    public static void setObjectiveStatus(boolean visible, String text, String style) {
        long now = System.currentTimeMillis();
        if (visible) {
            boolean wasFullyVisible = objectiveVisible && !objectiveHiding;
            objectiveVisible = true;
            objectiveHiding = false;
            if (!wasFullyVisible) {
                objectiveTransitionStartTimeMs = now;
            }
            objectiveText = text == null ? "" : text;
            objectiveStyle = style == null || style.isBlank() ? "runner" : style;
            return;
        }

        if (objectiveVisible && !objectiveHiding) {
            objectiveHiding = true;
            objectiveTransitionStartTimeMs = now;
        }
    }

    public static void showObjectiveNotice(String message, String style) {
        String safeMessage = message == null ? "" : message;
        if (safeMessage.isBlank()) {
            return;
        }

        objectiveNoticeEntries.add(new ObjectiveNoticeEntry(
                safeMessage,
                style == null || style.isBlank() ? "coordinate" : style,
                System.currentTimeMillis()
        ));

        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.05F));
    }

    public static void setIntro(boolean visible, String wildcardName, String description) {
        if (visible) {
            introVisible = true;
            introHiding = false;
            introTransitionStartTimeMs = System.currentTimeMillis();
            introName = wildcardName == null ? "" : wildcardName;
            introDescription = description == null ? "" : description;
            return;
        }

        if (introVisible) {
            introHiding = true;
            introTransitionStartTimeMs = System.currentTimeMillis();
        }
    }

    public static void setWeaponOverheat(int heat, int maxHeat, boolean visible) {
        weaponOverheatVisible = visible;
        weaponOverheatHeat = Math.max(0, heat);
        weaponOverheatMaxHeat = Math.max(1, maxHeat);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        renderDrawPanel(context);
        renderIntroPanel(context);
        renderObjectiveStatusPanel(context);
        renderWeaponOverheatBar(context);
        renderObjectiveNoticePanels(context);
        renderFeedbackPanels(context);
    }

    private static void renderWeaponOverheatBar(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!weaponOverheatVisible || client.options.hudHidden) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int barWidth = 36;
        int barHeight = 3;
        int x = screenWidth / 2 - barWidth / 2;
        int y = screenHeight / 2 + 11;
        int heat = Math.min(weaponOverheatHeat, weaponOverheatMaxHeat);
        int fillWidth = Math.round(barWidth * (heat / (float) weaponOverheatMaxHeat));
        int fillColor = weaponHeatColor(heat, weaponOverheatMaxHeat);

        context.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0x96000000);
        context.fill(x, y, x + barWidth, y + barHeight, 0x80161B22);
        if (fillWidth > 0) {
            context.fill(x, y, x + fillWidth, y + barHeight, fillColor);
        }

        for (int i = 1; i < weaponOverheatMaxHeat; i++) {
            int segmentX = x + Math.round(barWidth * (i / (float) weaponOverheatMaxHeat));
            context.fill(segmentX, y, segmentX + 1, y + barHeight, 0x70000000);
        }
    }

    private static void renderIntroPanel(DrawContext context) {
        if (!introVisible || introName.isBlank()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        String title = "外卡：" + introName;
        int maxAvailableWidth = Math.max(80, screenWidth - 6);
        int compactWidth = Math.min(Math.min(164, maxAvailableWidth), Math.max(112, screenWidth / 2));
        int wideWidth = Math.max(compactWidth, Math.min(224, maxAvailableWidth));
        boolean needsWide = textRenderer.getWidth(title) > compactWidth - 12
                || (!introDescription.isBlank() && textRenderer.getWidth(introDescription) > compactWidth - 12);
        int panelWidth = needsWide ? wideWidth : compactWidth;
        int maxDescriptionLines = needsWide ? 5 : 3;
        List<String> descriptionLines = introDescription.isBlank()
                ? List.of()
                : wrap(textRenderer, introDescription, panelWidth - 12, maxDescriptionLines);
        int panelHeight = descriptionLines.isEmpty() ? 20 : 20 + descriptionLines.size() * 11;
        long elapsed = introTransitionStartTimeMs < 0L ? INTRO_SLIDE_MS : System.currentTimeMillis() - introTransitionStartTimeMs;
        if (introHiding && elapsed >= INTRO_SLIDE_MS) {
            introVisible = false;
            introHiding = false;
            introTransitionStartTimeMs = -1L;
            introName = "";
            introDescription = "";
            return;
        }

        float progress = smooth(Math.min(1.0F, elapsed / (float) INTRO_SLIDE_MS));
        if (introHiding) {
            progress = 1.0F - progress;
        }
        float alpha = Math.max(0.0F, Math.min(1.0F, progress));
        int panelX = -Math.round((panelWidth + 2) * (1.0F - progress));
        int panelY = 22;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, withAlpha(0xD8161B22, alpha));
        context.fill(panelX, panelY, panelX + 2, panelY + panelHeight, withAlpha(0xFF7FC2FF, alpha));
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, withAlpha(0xAA7FC2FF, alpha));

        context.drawText(textRenderer, Text.literal(trim(textRenderer, title, panelWidth - 12)), panelX + 5, panelY + 3, withAlpha(0xFFFFFFFF, alpha), true);
        for (int i = 0; i < descriptionLines.size(); i++) {
            context.drawText(textRenderer, Text.literal(descriptionLines.get(i)), panelX + 5, panelY + 16 + i * 11, withAlpha(0xFFC9D4DE, alpha), false);
        }
    }

    private static void renderObjectiveStatusPanel(DrawContext context) {
        if (!objectiveVisible) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int maxAvailableWidth = Math.max(96, screenWidth - 8);
        int panelWidth = Math.min(Math.min(204, maxAvailableWidth), Math.max(156, textRenderer.getWidth(objectiveText) + 42));
        int panelHeight = 36;
        int visualPanelWidth = scaledObjectiveSize(panelWidth);
        int visualPanelHeight = scaledObjectiveSize(panelHeight);
        long elapsed = objectiveTransitionStartTimeMs < 0L ? OBJECTIVE_SLIDE_MS : System.currentTimeMillis() - objectiveTransitionStartTimeMs;
        if (objectiveHiding && elapsed >= OBJECTIVE_SLIDE_MS) {
            objectiveVisible = false;
            objectiveHiding = false;
            objectiveTransitionStartTimeMs = -1L;
            objectiveText = "";
            return;
        }

        float progress = smooth(Math.min(1.0F, elapsed / (float) OBJECTIVE_SLIDE_MS));
        if (objectiveHiding) {
            progress = 1.0F - progress;
        }
        float alpha = Math.max(0.0F, Math.min(1.0F, progress));
        int panelX = -Math.round((visualPanelWidth + 2) * (1.0F - progress));
        int panelY = objectiveBaseY(screenHeight, visualPanelHeight);
        int accent = objectiveAccent(objectiveStyle);

        renderScaledObjectiveStatusPanel(context, textRenderer, panelX, panelY, panelWidth, panelHeight, accent, alpha);
    }

    private static void renderObjectiveNoticePanels(DrawContext context) {
        if (objectiveNoticeEntries.isEmpty()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<ObjectiveNoticeEntry> iterator = objectiveNoticeEntries.iterator();
        while (iterator.hasNext()) {
            ObjectiveNoticeEntry entry = iterator.next();
            if (entry.topStartTimeMs >= 0L && now - entry.topStartTimeMs >= OBJECTIVE_NOTICE_TOTAL_MS) {
                iterator.remove();
            }
        }

        if (objectiveNoticeEntries.isEmpty()) {
            return;
        }

        ObjectiveNoticeEntry first = objectiveNoticeEntries.get(0);
        if (first.topStartTimeMs < 0L) {
            first.topStartTimeMs = now;
        }

        for (int i = 0; i < objectiveNoticeEntries.size(); i++) {
            renderObjectiveNoticeEntry(context, objectiveNoticeEntries.get(i), i, now);
        }
    }

    private static void renderObjectiveNoticeEntry(DrawContext context, ObjectiveNoticeEntry entry, int index, long now) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int maxAvailableWidth = Math.max(96, screenWidth - 8);
        int panelWidth = Math.min(Math.min(204, maxAvailableWidth), Math.max(156, textRenderer.getWidth(entry.message) + 42));
        int panelHeight = 38;
        int visualPanelWidth = scaledObjectiveSize(panelWidth);
        int visualPanelHeight = scaledObjectiveSize(panelHeight);
        int baseY = objectiveBaseY(screenHeight, visualPanelHeight);
        if (objectiveVisible) {
            baseY += scaledObjectiveSize(36) + 5;
        }
        int targetY = baseY + index * (visualPanelHeight + 5);
        if (Float.isNaN(entry.currentY)) {
            entry.currentY = targetY;
        } else {
            entry.currentY += (targetY - entry.currentY) * 0.35F;
        }
        int panelY = Math.round(entry.currentY);
        int panelX = objectiveNoticePanelX(visualPanelWidth, entry, now);
        int accent = objectiveAccent(entry.style);

        renderScaledObjectiveNoticePanel(context, textRenderer, entry, panelX, panelY, panelWidth, panelHeight, accent);
    }

    private static void renderScaledObjectiveStatusPanel(DrawContext context, TextRenderer textRenderer, int panelX, int panelY, int panelWidth, int panelHeight, int accent, float alpha) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(panelX, panelY);
        matrices.scale(OBJECTIVE_PANEL_SCALE, OBJECTIVE_PANEL_SCALE);
        context.fill(0, 0, panelWidth, panelHeight, withAlpha(0xB8161B22, alpha));
        context.fill(0, 0, 3, panelHeight, withAlpha(accent, alpha));
        context.fill(0, 0, panelWidth, 1, withAlpha(accent, alpha));
        context.fill(0, panelHeight - 1, panelWidth, panelHeight, withAlpha(accent, alpha));

        context.drawItem(objectiveIcon(objectiveStyle), 8, 10);
        context.drawText(textRenderer, Text.literal("逃亡目标"), 30, 5, withAlpha(accent, alpha), false);
        context.drawText(textRenderer, Text.literal(trim(textRenderer, objectiveText, panelWidth - 38)), 30, 20, withAlpha(0xFFFFFFFF, alpha), true);
        matrices.popMatrix();
    }

    private static void renderScaledObjectiveNoticePanel(DrawContext context, TextRenderer textRenderer, ObjectiveNoticeEntry entry, int panelX, int panelY, int panelWidth, int panelHeight, int accent) {
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(panelX, panelY);
        matrices.scale(OBJECTIVE_PANEL_SCALE, OBJECTIVE_PANEL_SCALE);
        context.fill(0, 0, panelWidth, panelHeight, 0xB8161B22);
        context.fill(0, 0, 3, panelHeight, accent);
        context.fill(0, 0, panelWidth, 1, accent);
        context.fill(0, panelHeight - 1, panelWidth, panelHeight, accent);

        context.drawItem(objectiveIcon(entry.style), 8, 11);
        context.drawText(textRenderer, Text.literal("目标提示"), 30, 5, accent, false);
        context.drawText(textRenderer, Text.literal(trim(textRenderer, entry.message, panelWidth - 38)), 30, 20, 0xFFFFFFFF, true);
        matrices.popMatrix();
    }

    private static int objectiveNoticePanelX(int panelWidth, ObjectiveNoticeEntry entry, long now) {
        int travel = panelWidth + 16;
        long entryElapsed = now - entry.startTimeMs;
        if (entryElapsed < OBJECTIVE_NOTICE_IN_MS) {
            float progress = smooth(entryElapsed / (float) OBJECTIVE_NOTICE_IN_MS);
            return -travel + Math.round(travel * progress);
        }

        long topElapsed = entry.topStartTimeMs < 0L ? 0L : now - entry.topStartTimeMs;
        long outStart = OBJECTIVE_NOTICE_IN_MS + OBJECTIVE_NOTICE_HOLD_MS;
        if (entry.topStartTimeMs >= 0L && topElapsed > outStart) {
            float progress = smooth((topElapsed - outStart) / (float) OBJECTIVE_NOTICE_OUT_MS);
            return -Math.round(travel * progress);
        }

        return 0;
    }

    private static void renderDrawPanel(DrawContext context) {
        if (drawStartTimeMs < 0L) {
            return;
        }

        long elapsed = System.currentTimeMillis() - drawStartTimeMs;
        if (elapsed >= DRAW_TOTAL_MS) {
            drawStartTimeMs = -1L;
            return;
        }

        if (elapsed >= DRAW_SPIN_MS && !revealSoundPlayed) {
            revealSoundPlayed = true;
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2F));
        }

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int panelX = 10;
        int panelY = 10;
        int panelWidth = Math.min(146, Math.max(118, screenWidth - 20));
        int panelHeight = 38;
        float alpha = drawAlpha(elapsed);
        String displayedName = displayedName(elapsed);
        boolean revealed = elapsed >= DRAW_SPIN_MS;
        int accent = revealed ? 0xFF77E287 : 0xFF7FC2FF;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, withAlpha(0xD8161B22, alpha));
        context.fill(panelX, panelY, panelX + 3, panelY + panelHeight, withAlpha(accent, alpha));
        context.fill(panelX, panelY + panelHeight - 2, panelX + Math.round(panelWidth * Math.min(1.0F, elapsed / (float) DRAW_TOTAL_MS)), panelY + panelHeight, withAlpha(accent, alpha));

        int iconX = panelX + 8;
        int iconY = panelY + 11;
        context.drawItem(WildcardIcons.iconFor(displayedName), iconX, iconY);

        context.drawText(textRenderer, Text.literal(revealed ? "外卡" : "抽取中"), panelX + 30, panelY + 6, withAlpha(0xFFC9D4DE, alpha), false);
        context.drawText(textRenderer, Text.literal(trim(textRenderer, displayedName, panelWidth - 38)), panelX + 30, panelY + 21, withAlpha(0xFFFFFFFF, alpha), true);
    }

    private static void renderFeedbackPanels(DrawContext context) {
        if (feedbackEntries.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<FeedbackEntry> iterator = feedbackEntries.iterator();
        while (iterator.hasNext()) {
            FeedbackEntry entry = iterator.next();
            if (entry.topStartTimeMs >= 0L && now - entry.topStartTimeMs >= FEEDBACK_TOTAL_MS) {
                iterator.remove();
            }
        }

        if (feedbackEntries.isEmpty()) {
            return;
        }

        FeedbackEntry first = feedbackEntries.get(0);
        if (first.topStartTimeMs < 0L) {
            first.topStartTimeMs = now;
        }

        for (int i = 0; i < feedbackEntries.size(); i++) {
            renderFeedbackEntry(context, feedbackEntries.get(i), i, now);
        }
    }

    private static void renderFeedbackEntry(DrawContext context, FeedbackEntry entry, int index, long now) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int panelWidth = Math.min(204, Math.max(160, screenWidth - 24));
        int panelHeight = 50;
        int targetX = screenWidth - panelWidth;
        int targetY = 10 + index * (panelHeight + 5);
        if (Float.isNaN(entry.currentY)) {
            entry.currentY = targetY;
        } else {
            entry.currentY += (targetY - entry.currentY) * 0.35F;
        }
        int panelY = Math.round(entry.currentY);
        int panelX = feedbackPanelX(screenWidth, panelWidth, targetX, entry, now);
        int accent = feedbackAccent(entry.style);

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0161B22);
        context.fill(panelX, panelY, panelX + 3, panelY + panelHeight, accent);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 1, accent);
        context.fill(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, accent);

        context.drawItem(feedbackIcon(entry.style), panelX + 10, panelY + 17);
        context.drawText(textRenderer, Text.literal(trim(textRenderer, entry.title, panelWidth - 44)), panelX + 34, panelY + 6, accent, false);
        context.drawText(textRenderer, Text.literal(trim(textRenderer, entry.line1, panelWidth - 44)), panelX + 34, panelY + 21, 0xFFFFFFFF, true);
        if (!entry.line2.isBlank()) {
            context.drawText(textRenderer, Text.literal(trim(textRenderer, entry.line2, panelWidth - 44)), panelX + 34, panelY + 36, 0xFFFFD966, false);
        }
    }

    private static int feedbackPanelX(int screenWidth, int panelWidth, int targetX, FeedbackEntry entry, long now) {
        int travel = panelWidth + 16;
        long entryElapsed = now - entry.startTimeMs;
        if (entryElapsed < FEEDBACK_IN_MS) {
            float progress = smooth(entryElapsed / (float) FEEDBACK_IN_MS);
            return screenWidth + 4 - Math.round(travel * progress);
        }

        long topElapsed = entry.topStartTimeMs < 0L ? 0L : now - entry.topStartTimeMs;
        long outStart = FEEDBACK_IN_MS + FEEDBACK_HOLD_MS;
        if (entry.topStartTimeMs >= 0L && topElapsed > outStart) {
            float progress = smooth((topElapsed - outStart) / (float) FEEDBACK_OUT_MS);
            return targetX + Math.round(travel * progress);
        }

        return targetX;
    }

    private static String displayedName(long elapsed) {
        if (elapsed >= DRAW_SPIN_MS) {
            return finalWildcard;
        }

        int index = (int) (elapsed / 95L) % SPIN_NAMES.length;
        return SPIN_NAMES[index];
    }

    private static float drawAlpha(long elapsed) {
        if (elapsed < DRAW_FADE_IN_MS) {
            return smooth(elapsed / (float) DRAW_FADE_IN_MS);
        }

        long fadeStart = DRAW_TOTAL_MS - DRAW_FADE_OUT_MS;
        if (elapsed > fadeStart) {
            return Math.max(0.0F, 1.0F - smooth((elapsed - fadeStart) / (float) DRAW_FADE_OUT_MS));
        }

        return 1.0F;
    }

    private static int feedbackAccent(String style) {
        return switch (style) {
            case "runner" -> 0xFF7FC2FF;
            case "respawn" -> 0xFF77E287;
            case "hunter" -> 0xFFFF8A8A;
            default -> 0xFFC9D4DE;
        };
    }

    private static int objectiveBaseY(int screenHeight, int panelHeight) {
        return Math.max(24, screenHeight / 2 - panelHeight / 2 - 18);
    }

    private static int scaledObjectiveSize(int size) {
        return Math.max(1, Math.round(size * OBJECTIVE_PANEL_SCALE));
    }

    private static int objectiveAccent(String style) {
        return switch (style) {
            case "time" -> 0xFFFFD966;
            case "item" -> 0xFF77E287;
            case "coordinate" -> 0xFF7FC2FF;
            case "hunter" -> 0xFFFF8A8A;
            default -> 0xFF7FC2FF;
        };
    }

    private static int weaponHeatColor(int heat, int maxHeat) {
        if (heat <= 0) {
            return 0x00000000;
        }
        if (heat >= maxHeat) {
            return 0xFFFF4C4C;
        }
        if (heat >= 3) {
            return 0xFFFF9F3F;
        }
        if (heat >= 2) {
            return 0xFFFFD966;
        }
        return 0xFF7FC2FF;
    }

    private static ItemStack feedbackIcon(String style) {
        return switch (style) {
            case "runner" -> new ItemStack(Items.DIAMOND);
            case "respawn" -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case "hunter" -> new ItemStack(Items.IRON_SWORD);
            default -> new ItemStack(Items.NETHER_STAR);
        };
    }

    private static ItemStack objectiveIcon(String style) {
        return switch (style) {
            case "time" -> new ItemStack(Items.CLOCK);
            case "item" -> new ItemStack(Items.DIAMOND);
            case "coordinate" -> new ItemStack(Items.COMPASS);
            default -> new ItemStack(Items.NETHER_STAR);
        };
    }

    private static float smooth(float value) {
        float clamped = Math.max(0.0F, Math.min(1.0F, value));
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    private static String trim(TextRenderer textRenderer, String text, int maxWidth) {
        String safeText = text == null ? "" : text;
        if (textRenderer.getWidth(safeText) <= maxWidth) {
            return safeText;
        }

        return textRenderer.trimToWidth(safeText, Math.max(8, maxWidth - textRenderer.getWidth("..."))) + "...";
    }

    private static List<String> wrap(TextRenderer textRenderer, String text, int maxWidth, int maxLines) {
        String safeText = text == null ? "" : text;
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < safeText.length(); i++) {
            char c = safeText.charAt(i);
            String candidate = current.toString() + c;
            if (current.length() > 0 && textRenderer.getWidth(candidate) > maxWidth) {
                lines.add(current.toString());
                current.setLength(0);
                if (lines.size() >= maxLines - 1) {
                    String remainder = safeText.substring(i);
                    lines.add(trim(textRenderer, remainder, maxWidth));
                    return lines;
                }
            }
            current.append(c);
        }
        if (current.length() > 0 && lines.size() < maxLines) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static int withAlpha(int color, float alpha) {
        int baseAlpha = color >>> 24;
        int scaledAlpha = Math.max(0, Math.min(255, Math.round(baseAlpha * alpha)));
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }

    private static final class FeedbackEntry {
        private final String title;
        private final String line1;
        private final String line2;
        private final String style;
        private final long startTimeMs;
        private long topStartTimeMs = -1L;
        private float currentY = Float.NaN;

        private FeedbackEntry(String title, String line1, String line2, String style, long startTimeMs) {
            this.title = title;
            this.line1 = line1;
            this.line2 = line2;
            this.style = style;
            this.startTimeMs = startTimeMs;
        }
    }

    private static final class ObjectiveNoticeEntry {
        private final String message;
        private final String style;
        private final long startTimeMs;
        private long topStartTimeMs = -1L;
        private float currentY = Float.NaN;

        private ObjectiveNoticeEntry(String message, String style, long startTimeMs) {
            this.message = message;
            this.style = style;
            this.startTimeMs = startTimeMs;
        }
    }
}
