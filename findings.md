# 小铭的猎人外卡 V1 发现记录

## 依赖与环境
- 工作区初始为空目录。
- 本机 Java 为 `21.0.10`，符合 Java 21 要求。
- 全局 `gradle` 命令不可用，需要项目内 Gradle Wrapper 或临时 Gradle 发行版。
- 临时 Gradle `8.14.3` 无法解析 Fabric Loom `1.14.10`，错误显示 Loom 运行时变体要求 Gradle plugin API `9.2.0`；改用 Gradle `9.2.0`。
- Gradle `9.2.0` 启动后，`settings.gradle` 的 `FAIL_ON_PROJECT_REPOS` 与 Loom 自动添加仓库冲突；移除该限制并在 `build.gradle` 声明仓库。
- 1.21.11 Yarn 下命令构造使用 `net.minecraft.server.command.CommandManager`；`ServerPlayerEntity` 世界访问使用 `getEntityWorld()`。
- 1.21.11 Yarn 下 `ServerCommandSource#hasPermissionLevel` 已替换为 `PermissionPredicate`；旧 OP 等级 2 可用 `new Permission.Level(PermissionLevel.GAMEMASTERS)` 检查。
- Fabric Maven 元数据确认：
  - `net.fabricmc:yarn` 最新 release 为 `1.21.11+build.6`。
  - `net.fabricmc.fabric-loom.gradle.plugin` 可用稳定版包含 `1.14.10`。
  - `net.fabricmc:fabric-loader` 当前 release 为 `0.19.3`。
  - `net.fabricmc.fabric-api:fabric-api` 包含 `0.141.4+1.21.11`。

## 实现边界
- V1 不做客户端 GUI、自定义材质或数据库。
- 玩家退出服务器时从队伍中移除，若游戏已运行则剩余在线玩家继续。
- 逃亡者死亡直接判定猎人胜利并进入结束清理。
- 猎人死亡标记为等待复活，10 秒后在重生事件中恢复可行动状态。

## 构建结果
- `.\gradlew.bat build` 成功。
- 可安装 jar：`build/libs/hunter-wildcard-1.0.0.jar`。
- jar 内 `fabric.mod.json` 展开后版本为 `1.0.0`。

## 2026-06-06 指令失效排查
- 发现 `fabric.mod.json` 使用 `"environment": "server"`，这会导致在 IDE 客户端或单人存档的集成服务器测试时 mod 不加载，`/hw` 不会注册。
- 已改为 `"environment": "*"`，仍然不添加客户端 GUI，只让入口能在物理客户端环境中启动集成服务器命令注册。
- 已添加初始化日志，便于在 `latest.log` 检查入口是否执行。
- 修复后 `.\gradlew.bat build` 成功，`build/libs/hunter-wildcard-1.0.0.jar` 内已确认 `"environment": "*"`。

## 2026-06-06 猎人指南针重复发放
- 原因：`CompassTracker#isHunterCompass` 原本用显示名判断物品，刷新时 `giveCompass` 可能识别不到已有指南针，于是插入新指南针。
- 修复：新指南针写入 `CUSTOM_DATA` 标记 `hunterwildcard_compass=true`，后续识别读取该标记。
- 兼容：旧版显示名为“猎人指南针”且有发光覆盖的指南针会被当作旧指南针并自动补标记。
- 清理：刷新时会移除背包中额外的重复猎人指南针，只保留第一枚。
- 验证：`.\gradlew.bat build` 成功。

## 2026-06-06 指南针结束收回与重命名
- 指南针新名称为“追猎指南针”。
- 清理路径：`GameManager#clearRoundEffects` 调用 `CompassTracker#clear`，覆盖自然结束、`/hw stop` 和服务器停止。
- 离队路径：`GameManager#leave` 调用 `CompassTracker#removeCompass`。
- 旧名称“猎人指南针”继续作为兼容识别名称，避免旧测试物品残留。
- 验证：`.\gradlew.bat build` 成功。
