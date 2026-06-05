# 小铭的猎人外卡 V1 进度

## 2026-06-06
- 读取 `planning-with-files-zh` 技能说明，确认需要维护 `task_plan.md`、`findings.md`、`progress.md`。
- 检查工作区为空目录。
- 查询 Fabric Maven 元数据，确认 1.21.11 相关依赖版本。
- 检查本机 Java 为 21.0.10；全局 Gradle 不存在，需要使用 Wrapper 或临时 Gradle。
- 已创建 Fabric 项目骨架、命令、游戏状态机、队伍、指南针、复活、BossBar/ActionBar 和 8 个外卡规则源码。
- 尝试用临时 Gradle 8.14.3 生成 Wrapper 失败，原因是 Loom 1.14.10 要求 Gradle plugin API 9.2.0。
- 使用 Gradle 9.2.0 时发现 `FAIL_ON_PROJECT_REPOS` 与 Loom 仓库注入冲突，已调整 Gradle 仓库配置。
- 第一轮 `compileJava` 发现 CommandManager import 和玩家世界访问器名称不匹配，已切换到 vanilla `CommandManager` 和 `getEntityWorld()`。
- 第二轮 `compileJava` 发现 `hasPermissionLevel` 不存在，已改为 `Permission.Level(PermissionLevel.GAMEMASTERS)`。
- 使用 `.\gradlew.bat build` 完成构建，生成 `build/libs/hunter-wildcard-1.0.0.jar` 和 `build/libs/hunter-wildcard-1.0.0-sources.jar`。
- 已读取 jar 内 `fabric.mod.json`，确认版本为 `1.0.0`、mod id 为 `hunterwildcard`、入口为 `com.xiaoming.hunterwildcard.HunterWildcardMod`。
- 已用 `rg` 检查没有残留旧 API 字符串：`hasPermissionLevel`、`getServerWorld`、`getWorld()`、旧 Fabric `CommandManager` import。

## 2026-06-06 指令失效修复
- 根据 IDE 测试场景排查 `/hw` 指令不存在的问题。
- 将 `src/main/resources/fabric.mod.json` 的 `environment` 从 `server` 改为 `*`，使 mod 在客户端/单人集成服务器测试时也会加载入口。
- 在 `HunterWildcardMod` 添加初始化日志：`Hunter Wildcard loaded. Server commands registered.`
- 重新运行 `.\gradlew.bat build` 成功。
- 已检查 `build/libs/hunter-wildcard-1.0.0.jar` 内 `fabric.mod.json`，确认 `"environment": "*"`。

## 2026-06-06 猎人指南针重复发放修复
- 排查到 `CompassTracker` 每次刷新前都会调用 `giveCompass`，但原本只靠显示名识别已有指南针，识别不稳定会导致重复插入。
- 将猎人指南针识别改为 `DataComponentTypes.CUSTOM_DATA` 内的 `hunterwildcard_compass=true` 标记。
- 兼容旧版已发出的指南针：按显示名和发光标记识别后自动补写新标记。
- 刷新时会保留第一枚猎人指南针，并清理多余的重复猎人指南针。
- 重新运行 `.\gradlew.bat build` 成功，产物为 `build/libs/hunter-wildcard-1.0.0.jar`。

## 2026-06-06 追猎指南针命名与结束清理
- 将指南针显示名从“猎人指南针”改为“追猎指南针”。
- 保留旧“猎人指南针”的兼容识别，旧测试物品仍可被更新或清理。
- 在 `CompassTracker` 增加 `clear(GameContext)` 和 `removeCompass(ServerPlayerEntity)`。
- 游戏结束、管理员停止、服务器停止的清理路径会收回参与者背包里的追猎指南针。
- 玩家执行 `/hw leave` 时也会收回该玩家的追猎指南针。
- 重新运行 `.\gradlew.bat build` 成功，产物为 `build/libs/hunter-wildcard-1.0.0.jar`。
