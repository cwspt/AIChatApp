# AIChatApp Project Tracker

更新时间：2026-05-28

## 1. 项目目标

AIChatApp 是一个本地 Android 多 AI Provider 聊天客户端，目标是让用户可以在一个移动端应用里配置多组 AI API，切换不同 Provider 和模型进行对话，并安全保存本机配置与聊天历史。

首版聚焦：

- 文本聊天，不包含图片、语音、文件上传、工具调用和云同步。
- 同时支持 GPT / OpenAI Responses、DeepSeek / OpenAI-compatible Chat Completions、本地 TokenHubProxy 代理。
- 支持多组 API 配置，包括新增、编辑、克隆、切换、加密保存 API Key。
- 支持本地会话历史、归档、恢复、删除确认、置顶、分组、对话列表抽屉。
- 支持 AI Markdown 回复预览、复制、编辑重发、分享文本、分享 Markdown 文件、分享长图。
- 支持本地 Jenkins release 构建、共享 Android 工具链、本地 release 签名。

## 2. 状态标记

| 标记 | 含义 |
| --- | --- |
| Done | 已实现并通过基础验证，可以继续迭代优化 |
| In Progress | 已部分实现，仍需要继续完善 |
| Planned | 已规划，尚未开始 |
| Blocked | 被外部依赖、产品决策或验证条件阻塞 |
| Watch | 可用但需要持续观察稳定性、体验或兼容性 |

## 3. 优先级定义

| 优先级 | 含义 |
| --- | --- |
| P0 | 基础可用性、安全性、构建发布或核心聊天链路，必须优先处理 |
| P1 | 高频体验能力，会明显影响日常使用效率 |
| P2 | 增强功能、体验细节、可维护性优化 |
| P3 | 长期扩展、实验性能力或低频高级能力 |

## 4. 当前总体进度

| 模块 | 当前状态 | 进度 | 说明 |
| --- | --- | --- | --- |
| Android 项目骨架 | Done | 100% | 已创建独立项目 `D:\Projects\Personal\AI\AIChatApp`，包名 `com.personal.aichat` |
| Git / Remote | Done | 100% | 已初始化 git 并推送到 `git@github.com:cwspt/AIChatApp.git` |
| 共享工具链 | Done | 100% | 使用 `D:\Projects\Personal\.devtools\android` 和本地 Gradle 8.7 zip |
| Provider 抽象 | Done | 85% | 已有统一模型和 Adapter 抽象，Anthropic / Gemini 预留未接入 |
| GPT / OpenAI Responses | Done | 80% | 支持 `/v1/responses`、SSE 增量文本、reasoning effort 配置 |
| DeepSeek / OpenAI-compatible | Done | 80% | 支持 `/chat/completions`，适配 DeepSeek 默认配置 |
| TokenHubProxy | Done | 75% | 作为 Responses 兼容代理入口接入，默认可改局域网地址 |
| 多组 API 配置 | Done | 85% | 支持新增、编辑、克隆、切换、保存 API Key |
| API Key 安全保存 | Done | 85% | 使用 AndroidX Security Crypto，本地加密保存，不入 Room |
| 会话与消息本地存储 | Done | 90% | Room 保存 Provider、会话、消息，已有多轮迁移 |
| 聊天 UI | In Progress | 75% | 主聊天、输入框、气泡、Markdown、滚动条、回到底部已实现，仍需真机继续打磨 |
| 会话管理 | Done | 80% | 支持置顶、归档、恢复、删除确认、分组、抽屉列表 |
| 分享导出 | In Progress | 75% | 支持文本、Markdown 文件、长图，单气泡图片保存曾修复但需持续真机验证 |
| Markdown 渲染 | In Progress | 70% | 支持标题、列表、代码、表格、基础 inline 样式，复杂 Markdown 仍有限 |
| CI / Jenkins | Done | 90% | 已适配 AIChatApp 的本地 release pipeline |
| Release 签名 | Done | 90% | 已生成本地 `keystore.properties` 和 JKS，均被 git 忽略 |
| 自动化测试 | In Progress | 35% | 有 Adapter / SSE 等基础单元测试，UI 和数据层覆盖不足 |

## 5. 已完成目标

### 5.1 项目与工程化

| ID | 目标 | 状态 | 优先级 | 验收状态 |
| --- | --- | --- | --- | --- |
| ENG-001 | 创建独立 Android 项目 `AIChatApp` | Done | P0 | 项目可独立打开和构建 |
| ENG-002 | 使用 Kotlin + Jetpack Compose + Material3 | Done | P0 | 主 UI 已由 Compose 实现 |
| ENG-003 | 接入 Room / DataStore / OkHttp / Security Crypto | Done | P0 | 数据、偏好、网络、密钥存储均已落地 |
| ENG-004 | 使用共享 Android SDK、JDK、Gradle cache、本地 Gradle zip | Done | P0 | wrapper 指向本地 `gradle-8.7-bin.zip` |
| ENG-005 | 初始化 git 并推送 remote | Done | P0 | remote 为 `git@github.com:cwspt/AIChatApp.git` |
| ENG-006 | 增加本地 Jenkins release pipeline | Done | P0 | `Jenkinsfile.local-keystore` 已适配 AIChatApp |
| ENG-007 | 生成本地 release 签名配置 | Done | P0 | release assemble 已通过，私钥文件未入 git |

### 5.2 Provider 与 API 接入

| ID | 目标 | 状态 | 优先级 | 验收状态 |
| --- | --- | --- | --- | --- |
| API-001 | 定义统一 Provider 数据模型 | Done | P0 | `ChatProviderConfig`、`ProviderType` 已存在 |
| API-002 | 定义统一流式事件模型 | Done | P0 | `ChatStreamEvent.Started/TextDelta/Completed/Failed` 已存在 |
| API-003 | GPT / OpenAI Responses Adapter | Done | P0 | 请求 `/responses`，支持 SSE 文本 delta |
| API-004 | DeepSeek / OpenAI-compatible Adapter | Done | P0 | 请求 `/chat/completions`，支持 `[DONE]` 和 delta |
| API-005 | TokenHubProxy Adapter | Done | P1 | 通过 Responses 兼容代理接入 |
| API-006 | GPT 推理强度选择 | Done | P1 | 支持 Auto / Low / Medium / High / XHigh |
| API-007 | 额外 Headers JSON | Done | P2 | 支持配置非 Authorization 额外 header |
| API-008 | API 错误信息映射 | In Progress | P1 | 已映射 HTTP 状态和 provider message，仍可优化中文提示 |

### 5.3 配置与安全

| ID | 目标 | 状态 | 优先级 | 验收状态 |
| --- | --- | --- | --- | --- |
| SEC-001 | API Key 不保存到 Room | Done | P0 | Room 仅保存 `secretRef` |
| SEC-002 | API Key 使用加密 SharedPreferences 保存 | Done | P0 | `EncryptedApiKeyStore` 已实现 |
| SEC-003 | 支持再次打开配置时识别已保存 Key | Done | P0 | 配置页显示是否已有 Key，不回显明文 |
| SEC-004 | 网络日志不打印 Authorization / Key | Done | P0 | 当前未加入明文网络日志 |
| SEC-005 | release keystore 与配置不入 git | Done | P0 | `.gitignore` 忽略 `keystore.properties` 和 `keystore/` |
| SEC-006 | 密钥备份策略 | Planned | P1 | 需要在文档中补充 JKS 丢失后的风险和备份流程 |

### 5.4 聊天与会话体验

| ID | 目标 | 状态 | 优先级 | 验收状态 |
| --- | --- | --- | --- | --- |
| CHAT-001 | 创建和保存会话 | Done | P0 | Room 保存 conversations / messages |
| CHAT-002 | 流式显示 AI 回复 | Done | P0 | delta 增量更新 assistant 消息 |
| CHAT-003 | 失败时保留错误状态 | Done | P0 | 失败消息带 `FAILED` 和错误信息 |
| CHAT-004 | 重试最后一条用户消息 | Done | P1 | `retryLast` 已实现 |
| CHAT-005 | 用户消息编辑重发 | Done | P1 | 用户气泡支持编辑重发 |
| CHAT-006 | 气泡快捷复制 | Done | P1 | 支持复制单条内容 |
| CHAT-007 | 气泡显示本地时间 | Done | P1 | 已转换为 local time 显示 |
| CHAT-008 | 气泡内容局部选择复制 | Watch | P1 | Android Text 选择体验仍需真机确认 |
| CHAT-009 | Markdown 预览 | In Progress | P1 | 已有基础 Markdown parser，复杂语法不足 |
| CHAT-010 | 长对话滚动条 | Done | P1 | 已有滚动进度条和跳转 |
| CHAT-011 | 向下快捷箭头 | Done | P1 | 离底部较远时可快速回到底部 |

### 5.5 会话列表、归档和分组

| ID | 目标 | 状态 | 优先级 | 验收状态 |
| --- | --- | --- | --- | --- |
| CONV-001 | 左上角展开聊天列表抽屉 | Done | P1 | Drawer 列举会话和分组 |
| CONV-002 | 会话置顶 | Done | P1 | 支持置顶和取消置顶 |
| CONV-003 | 会话归档 | Done | P1 | 支持归档当前会话 |
| CONV-004 | 已归档会话恢复 | Done | P1 | 支持恢复归档会话 |
| CONV-005 | 删除对话二次确认 | Done | P0 | 删除前弹确认 |
| CONV-006 | 会话分组 | Done | P1 | 支持通过 groupName 分组 |
| CONV-007 | 置顶聊天、置顶文件夹、普通聊天、普通文件夹分区展示 | In Progress | P1 | 已有分组展示，细分信息架构还可优化 |
| CONV-008 | 批量移动会话到分组 | Planned | P1 | 当前更偏单会话编辑分组，批量能力待做 |

### 5.6 分享与导出

| ID | 目标 | 状态 | 优先级 | 验收状态 |
| --- | --- | --- | --- | --- |
| SHARE-001 | 分享完整对话为文本 | Done | P1 | 支持系统分享 Intent |
| SHARE-002 | 分享选中气泡为文本 | Done | P1 | 多选后分享节选 |
| SHARE-003 | 分享完整对话为 Markdown 文件 | Done | P1 | 可导出 `.md` 文件 |
| SHARE-004 | 分享完整对话为长图 | Done | P1 | 可保存到相册或用文件分享 fallback |
| SHARE-005 | 分享单个气泡为文本 | Done | P1 | 单气泡菜单支持 |
| SHARE-006 | 分享单个气泡为图片 | Done | P1 | 已接入图片分享和相册保存路径 |
| SHARE-007 | 长图里使用 Markdown 预览格式 | In Progress | P1 | 已渲染标题、列表、代码、表格，复杂语法待提升 |
| SHARE-008 | 网络错误气泡导出策略 | Done | P2 | 已倾向导出错误提示而非空白 |
| SHARE-009 | 选择到这里 | Done | P1 | 多选后支持范围选择 |

### 5.7 UI 与视觉

| ID | 目标 | 状态 | 优先级 | 验收状态 |
| --- | --- | --- | --- | --- |
| UI-001 | 主语言中文化 | Done | P1 | 主要 UI 文案已中文 |
| UI-002 | 生成应用图标 | Done | P2 | 已有 launcher drawable 资源 |
| UI-003 | 输入框避免重叠 | Done | P0 | 已调整底部布局 |
| UI-004 | 聊天气泡 Markdown 表格边框 | Done | P1 | UI 表格已加 border |
| UI-005 | 导出图片表格边框 | Done | P1 | 长图 renderer 已加表格线 |
| UI-006 | 抽屉当前选中项可读性 | Done | P1 | 已修复只显示背景不显示文本的问题 |
| UI-007 | 大屏 / 横屏适配 | Planned | P2 | 当前以移动端优先，未系统适配平板 |

## 6. 当前能力清单

### 6.1 Provider 类型

| 类型 | 当前用途 | 状态 | 备注 |
| --- | --- | --- | --- |
| `OPENAI_RESPONSES` | GPT / OpenAI 官方 Responses API | Done | Base URL 示例：`https://api.openai.com/v1` |
| `OPENAI_COMPATIBLE_CHAT` | DeepSeek 和兼容 Chat Completions 的服务 | Done | DeepSeek Base URL 示例：`https://api.deepseek.com` |
| `TOKENHUB_PROXY` | 本机或局域网代理转发 | Done | 默认可指向 TokenHubProxy 的 `/v1` |
| `ANTHROPIC_MESSAGES` | Claude Messages API | Planned | 类型已预留，Adapter 未实现 |
| `GEMINI_GENERATE_CONTENT` | Gemini GenerateContent API | Planned | 类型已预留，Adapter 未实现 |

### 6.2 数据表

| 表 | 用途 | 当前字段重点 |
| --- | --- | --- |
| `providers` | Provider 配置元数据 | `type`、`baseUrl`、`defaultModel`、`reasoningEffort`、`secretRef` |
| `conversations` | 会话元数据 | `title`、`providerId`、`model`、`groupName`、`isArchived`、`isDeleted`、`isPinned` |
| `messages` | 消息内容 | `role`、`content`、`status`、`providerId`、`model`、`errorMessage` |

### 6.3 构建与发布

| 能力 | 状态 | 说明 |
| --- | --- | --- |
| Debug 构建 | Done | 可使用共享环境执行 `:app:assembleDebug` |
| Release 构建 | Done | `:app:assembleRelease` 已通过 |
| Jenkins 本地 release pipeline | Done | `Jenkinsfile.local-keystore` 已适配 |
| 本地 Gradle 分发包 | Done | wrapper 使用 `file:///D:/Projects/Personal/.devtools/android/downloads/gradle-8.7-bin.zip` |
| Release 签名 | Done | 本地 JKS 和 `keystore.properties` 已生成，需安全备份 |
| 版本号自增 | Done | Jenkins 可自动 bump `versionCode` 和 patch 版本 |

## 7. 已知风险与观察项

| ID | 风险 | 状态 | 优先级 | 建议处理 |
| --- | --- | --- | --- | --- |
| RISK-001 | release keystore 丢失会导致同包名应用无法正常升级 | Watch | P0 | 立即将 JKS 和密码文件备份到安全位置 |
| RISK-002 | 移动端保存第三方 API Key 有本机安全取舍 | Watch | P0 | 后续增加本机锁、导出警告、Key 删除确认 |
| RISK-003 | Android Web / Markdown 完整渲染复杂度较高 | Watch | P1 | 评估引入成熟 Markdown Compose 组件或 WebView renderer |
| RISK-004 | 长图导出超长对话可能触达 Bitmap 尺寸或内存限制 | Watch | P1 | 增加分页导出、PDF 导出或自动分段长图 |
| RISK-005 | 各 Provider SSE 事件格式可能变化 | Watch | P1 | 增加更完整的 MockWebServer 单元测试 |
| RISK-006 | DeepSeek / OpenAI-compatible 的非流式或错误结构可能差异较大 | Watch | P1 | 增强错误解析和兼容分支 |
| RISK-007 | 当前 UI 功能密度越来越高，移动端学习成本上升 | Watch | P1 | 后续梳理信息架构和设置入口 |

## 8. 近期实施计划

### 8.1 P0：稳定核心链路

| ID | 任务 | 状态 | 验收标准 |
| --- | --- | --- | --- |
| NEXT-P0-001 | 用真实 GPT Key 验证 OpenAI Responses 流式聊天 | Planned | 能发送、流式返回、失败提示可读 |
| NEXT-P0-002 | 用真实 DeepSeek Key 验证 OpenAI-compatible 流式聊天 | Planned | 默认 Base URL + Key 可正常回复 |
| NEXT-P0-003 | 备份 release keystore 和 `keystore.properties` | Planned | 私钥文件安全复制到至少一个非仓库位置 |
| NEXT-P0-004 | Jenkins 真实跑一次 release pipeline | Planned | Jenkins 成功归档 `app-release.apk` |
| NEXT-P0-005 | 搜索仓库确认无真实 API Key / Authorization / keystore 提交 | Planned | `git status` 干净，敏感文件被 ignore |

### 8.2 P1：提升日常使用体验

| ID | 任务 | 状态 | 验收标准 |
| --- | --- | --- | --- |
| NEXT-P1-001 | 优化 Provider 错误提示中文化 | Planned | 401、超时、断网、Base URL 错误都有明确提示 |
| NEXT-P1-002 | 批量移动会话到已有分组或新分组 | Planned | 可多选会话并批量归类 |
| NEXT-P1-003 | 优化聊天列表抽屉的信息架构 | Planned | 明确区分置顶会话、置顶分组、普通会话、普通分组 |
| NEXT-P1-004 | 提升 Markdown 预览完整度 | Planned | 支持引用、链接、粗体斜体、代码语言标签、任务列表 |
| NEXT-P1-005 | 分享长图分页或分段 | Planned | 超长对话不会截断或 OOM |
| NEXT-P1-006 | 补齐数据层和 Provider Adapter 单元测试 | Planned | 覆盖 Provider 保存、会话状态、错误映射、SSE delta |

### 8.3 P2：长期可维护性

| ID | 任务 | 状态 | 验收标准 |
| --- | --- | --- | --- |
| NEXT-P2-001 | 拆分过大的 Compose 文件 | Planned | `AIChatAppRoot.kt` 按功能拆为 screen / component / markdown / dialog |
| NEXT-P2-002 | 增加 README 的用户使用指南 | Planned | 包含 GPT、DeepSeek、TokenHubProxy 配置步骤 |
| NEXT-P2-003 | 增加 release checklist 文档 | Planned | 每次发布有固定验证清单 |
| NEXT-P2-004 | 增加数据库 migration 测试 | Planned | schema 迁移可自动验证 |
| NEXT-P2-005 | 优化大屏 / 横屏布局 | Planned | 平板和折叠屏上列表与聊天区可并列显示 |

### 8.4 P3：扩展能力

| ID | 任务 | 状态 | 验收标准 |
| --- | --- | --- | --- |
| NEXT-P3-001 | Anthropic Messages Adapter | Planned | Claude API 可配置并完成文本流式聊天 |
| NEXT-P3-002 | Gemini GenerateContent Adapter | Planned | Gemini API 可配置并完成文本聊天 |
| NEXT-P3-003 | 会话导出为 PDF | Planned | 支持分享或保存完整 PDF |
| NEXT-P3-004 | Prompt 模板 / 系统提示词管理 | Planned | 可保存常用提示词并按会话应用 |
| NEXT-P3-005 | MVC 项目登录联动 | Planned | 如后续需要，再接入共享 FirstWebClient 或新共享模块 |
| NEXT-P3-006 | 多端同步或备份恢复 | Planned | 明确本地备份格式和导入流程 |

## 9. 里程碑规划

### M1：本地可用聊天客户端

状态：Done

验收标准：

- 可安装 debug / release APK。
- 可配置 GPT、DeepSeek、TokenHubProxy。
- 可保存多组 API 配置。
- 可创建会话、发送消息、流式接收回复。
- API Key 不入 git、不入 Room。

### M2：日常可持续使用版本

状态：In Progress

验收标准：

- 会话列表、分组、置顶、归档、恢复、删除确认体验稳定。
- Markdown 回复在聊天气泡和导出图片中都足够可读。
- 长对话滚动、回到底部、范围多选、分享导出稳定。
- 常见网络错误和鉴权错误能用中文清楚说明。
- Jenkins release 构建可一键产出 APK。

### M3：可维护扩展版本

状态：Planned

验收标准：

- Provider Adapter 单元测试覆盖核心请求体、SSE 和错误映射。
- Compose UI 按模块拆分，避免单文件持续膨胀。
- 数据库 migration 有测试保护。
- README 和 release checklist 完整。
- Anthropic / Gemini 可按统一 Adapter 模型扩展。

### M4：高级生产力版本

状态：Planned

验收标准：

- 支持 Prompt 模板、系统提示词、常用模型参数。
- 支持 PDF / 分段长图 / Markdown 文件等更完整导出。
- 支持本地备份与恢复。
- 可选接入 MVC 项目能力或账户体系。

## 10. 验证清单

每次核心功能变更后建议执行：

```powershell
cd D:\Projects\Personal\AI\AIChatApp
.\gradlew.bat :app:testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat :app:assembleDebug --console=plain --no-daemon
```

每次 release 前建议执行：

```powershell
cd D:\Projects\Personal\AI\AIChatApp
.\gradlew.bat :app:assembleRelease --console=plain --no-daemon
git status --short
git check-ignore -v keystore.properties keystore/aichat-release.jks
```

手动验证建议：

- GPT Provider：配置 `https://api.openai.com/v1`，选择支持 Responses 的模型，验证推理强度选项。
- DeepSeek Provider：配置 `https://api.deepseek.com`，选择 DeepSeek 模型，验证流式回复和错误提示。
- TokenHubProxy Provider：在手机可访问代理地址时验证局域网代理链路。
- 聊天体验：长对话滚动、回到底部、复制、编辑重发、重试、删除确认。
- 会话管理：置顶、归档、恢复、分组、抽屉切换。
- 分享导出：单气泡文本、单气泡图片、选中消息文本、选中消息长图、完整对话 Markdown 文件、完整对话长图。

## 11. Tracker 维护规则

- 每完成一个功能，更新对应条目的状态和进度。
- 每次发现明显 Bug，先放入 `已知风险与观察项` 或近期计划，再决定是否提升到 P0 / P1。
- 每次 release 前更新 `当前总体进度` 和 `里程碑规划`。
- 不在本文件写入真实 API Key、keystore 密码、Jenkins 凭据或任何可用于访问服务的敏感信息。
- 如果任务已经完成但尚未真机验证，优先标记为 `Watch`，不要直接标成完全稳定。
