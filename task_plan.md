# 小铭的猎人外卡 V1 任务计划

## 目标
创建一个可构建的 Minecraft Java Edition 1.21.11 Fabric 服务端玩法 MOD，实现 Manhunt 队伍、游戏状态、指南针追踪、猎人复活、BossBar/ActionBar、以及 8 个 V1 外卡规则。

## 阶段

| 阶段 | 状态 | 内容 |
| --- | --- | --- |
| 1 | complete | 确认工作区、依赖版本、Java/Gradle 环境 |
| 2 | complete | 创建 Fabric Gradle 项目骨架和资源文件 |
| 3 | complete | 实现命令、队伍、游戏状态与生命周期 |
| 4 | complete | 实现指南针、复活、UI 和外卡系统 |
| 5 | complete | Gradle build 验证并按 1.21.11 Yarn API 修正 |
| 6 | complete | 记录结果、构建产物和后续建议 |

## 技术决定
- 使用 Java 21。
- 使用 Minecraft `1.21.11`、Yarn `1.21.11+build.6`、Fabric API `0.141.4+1.21.11`。
- 使用稳定 Fabric Loom `1.14.10`，避免默认使用 alpha 版构建插件。
- 第一版只实现服务端逻辑，不创建客户端 GUI 或自定义资源。

## 错误记录
| 错误 | 处理 |
| --- | --- |
| 本机没有全局 `gradle` 命令 | 下载临时 Gradle 发行版生成项目内 Wrapper |
| Gradle 8.14.3 解析 Loom 1.14.10 失败 | 使用错误提示要求的 Gradle 9.2.0 继续 |
| `FAIL_ON_PROJECT_REPOS` 与 Loom 仓库注入冲突 | 移除 settings 仓库限制，仓库放在 build.gradle |
| 1.21.11 Yarn API 名称差异 | 使用 vanilla CommandManager，并将玩家世界访问改为 getEntityWorld() |
| 1.21.11 权限 API 变化 | 用 Permission.Level(PermissionLevel.GAMEMASTERS) 代替 hasPermissionLevel(2) |

## 验证结果
- `.\gradlew.bat build` 成功。
- 主要产物：`build/libs/hunter-wildcard-1.0.0.jar`。
- 源码产物：`build/libs/hunter-wildcard-1.0.0-sources.jar`。
- jar 内 `fabric.mod.json` 版本为 `1.0.0`。
- 已检查源码中没有残留 `hasPermissionLevel`、`getServerWorld`、旧 Fabric `CommandManager` import。

## 追加修复
- complete: 修复 IDE 客户端/单人集成服务器测试时 `/hw` 不存在的问题。
- 原因：`fabric.mod.json` 的 `"environment": "server"` 会让物理客户端环境不加载该 mod。
- 处理：改为 `"environment": "*"` 并添加初始化日志，代码仍然只实现服务端玩法逻辑。
- 验证：`.\gradlew.bat build` 成功，jar 内 `fabric.mod.json` 已确认 `"environment": "*"`。
- complete: 修复猎人指南针刷新时重复发放的问题。
- 原因：旧逻辑依赖显示名识别指南针，刷新前可能识别不到已有指南针。
- 处理：改用 `CUSTOM_DATA` 标记识别，并在刷新时清理重复猎人指南针。
- 验证：`.\gradlew.bat build` 成功。
- complete: 将指南针命名为“追猎指南针”，并在游戏结束/停止/离队时收回。
- 处理：`CompassTracker#clear` 负责清理参与者背包，`CompassTracker#removeCompass` 负责单人清理。
- 验证：`.\gradlew.bat build` 成功。
