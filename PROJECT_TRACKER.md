# AIChatApp Project Tracker

更新时间：2026-05-28

## 1. 项目目标

AIChatApp 是一个本地 Android 多 Provider AI 聊天客户端，目标是在移动端统一管理多组 AI API 配置、固定会话模型、保存本地聊天历史，并逐步支持工具调用、联网搜索、多模态附件、分享导出和后台生成。

当前产品规则：

- 会话创建后固定 `providerId/model`，已有会话不通过顶部控件切换模型。
- 模型对比通过“从某条消息分叉到其他 Provider/model”的方式完成。
- OpenAI Responses / TokenHubProxy 可使用联网搜索和图片/文件附件。
- DeepSeek / OpenAI-compatible Chat 当前以文本和函数式 web_search 为主，DeepSeek 官方 API 当前不支持图片/文件附件输入。

## 2. 状态和优先级

| 状态 | 含义 |
| --- | --- |
| Done | 已实现并通过基础验证 |
| In Progress | 已部分实现，还需要继续完善 |
| Planned | 已规划，尚未开始 |
| Watch | 可用但需要真机或长期稳定性观察 |
| Blocked | 被外部条件阻塞 |

| 优先级 | 含义 |
| --- | --- |
| P0 | 核心可用性、安全性、构建发布、聊天主链路 |
| P1 | 高频体验能力，明显影响日常使用 |
| P2 | 增强功能、体验细节、可维护性 |
| P3 | 长期扩展、实验能力、低频高级能力 |

## 3. 当前总体进度

| 模块 | 状态 | 进度 | 说明 |
| --- | --- | --- | --- |
| Android 项目骨架 | Done | 100% | Kotlin + Compose + Material3 + Room + DataStore + OkHttp |
| 本机开发环境文档 | Done | 100% | 已按本机真实路径整理本地构建说明 |
| Provider 抽象 | Done | 90% | OpenAI Responses、OpenAI-compatible、TokenHubProxy 已接入，Anthropic/Gemini 预留 |
| 会话固定模型 | Done | 100% | 请求实际使用 `conversation.model`，不再临时读 provider default |
| 会话分叉 | Done | 95% | 支持从消息气泡分叉到其他 Provider，并保存来源关系 |
| OpenAI Responses | Done | 90% | 支持流式、reasoning effort、usage 元数据、web_search、附件输入 |
| DeepSeek / OpenAI-compatible | Done | 85% | 支持 Chat Completions 流式、usage、函数式 web_search |
| 联网搜索 | Done | 85% | OpenAI hosted web_search + compatible function calling + DuckDuckGo fallback |
| 工具调用 UI | Done | 80% | 工具调用独立卡片、可折叠、显示查询词和 URL |
| 多模态附件 | Done | 75% | Provider 级附件开关、图片/文件选择、拍照、气泡附件展示、图片 app 内预览 |
| 设置页 | Done | 85% | 独立设置页，支持主题色、夜间模式、字体、debug log、搜索模式、配置导入导出 |
| 聊天 UI | In Progress | 85% | 顶栏压缩、输入框修复、自动追踪滚动、选择模式、Markdown 表格与分隔线已优化 |
| 会话列表 | In Progress | 85% | 抽屉、折叠文件夹、重命名、日期分组、创建/更新时间显示已实现 |
| 后台生成 | Done | 80% | 前台服务保活，后台完成后通知 |
| 分享导出 | In Progress | 75% | 文本、Markdown 文件、长图、单气泡分享已实现，超长内容仍需优化 |
| 自动化测试 | In Progress | 55% | Provider adapter、fork、附件请求体、web_search 解析已有单测 |
| CI / Release | Done | 90% | Jenkins 本地 release pipeline 和签名配置已接入 |

## 4. 已完成事项

### 4.1 Provider 与请求链路

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| API-001 | 统一 Provider 数据模型和 adapter 接口 | Done | P0 | `ChatProviderConfig` / `ProviderAdapter` 已落地 |
| API-002 | OpenAI Responses adapter | Done | P0 | `/v1/responses` 支持 SSE delta、usage、reasoning |
| API-003 | OpenAI-compatible Chat adapter | Done | P0 | `/chat/completions` 支持流式 delta、usage |
| API-004 | TokenHubProxy adapter | Done | P1 | 复用 Responses adapter |
| API-005 | 请求使用会话固定模型 | Done | P0 | `sendMessage`、`retryLast`、fork 自动回复均使用 `conversation.model` |
| API-006 | DeepSeek reasoning/thinking 配置入口评估 | Done | P2 | 当前保留 provider reasoning 配置，后续按 provider 差异扩展 |
| API-007 | raw response debug log | Done | P1 | 设置中可开启，assistant 消息保存原始 SSE frame |
| API-008 | assistant 元数据显示 | Done | P1 | 气泡底部显示总耗时、首 token、token usage |

### 4.2 会话固定模型与分叉

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| BRANCH-001 | `ConversationEntity` 增加 fork 来源字段 | Done | P0 | `forkedFromConversationId` / `forkedFromMessageId` 已迁移 |
| BRANCH-002 | 从消息气泡分叉到其他 Provider | Done | P0 | 复制历史消息到目标消息，保存新会话来源 |
| BRANCH-003 | 从 USER 消息分叉后自动用目标模型回复 | Done | P1 | 仓库测试覆盖 |
| BRANCH-004 | 对话列表展示分叉来源标记 | Watch | P2 | 已保留来源字段，轻量展示仍可继续优化 |
| BRANCH-005 | 顶部模型切换降级为显示属性 | Done | P1 | 新建会话时选择模型，已有会话模型固定 |

### 4.3 联网搜索和工具调用

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| SEARCH-001 | 设置中增加搜索模式 | Done | P1 | 关闭 / 自动搜索 / 强制搜索 |
| SEARCH-002 | OpenAI Responses hosted `web_search` | Done | P1 | 请求体发送 `tools: web_search` |
| SEARCH-003 | DeepSeek/OpenAI-compatible 函数式 `web_search` | Done | P1 | 模型返回 tool call 后 app 执行搜索，再回传 tool result |
| SEARCH-004 | app 侧 DuckDuckGo 搜索客户端 | Done | P2 | 解析 HTML 搜索结果并返回标题、URL、摘要 |
| SEARCH-005 | 工具调用独立 UI | Done | P1 | 非普通气泡样式，可折叠 |
| SEARCH-006 | 搜索 URL 展示 | Done | P1 | 解析 `url_citation`、正文 URL、OpenAI `action.url` |
| SEARCH-007 | OpenAI 多个 hosted web_search item 聚合 | Done | P1 | search/open_page 合并为一个工具卡片 |
| SEARCH-008 | 工具调用历史参与上下文策略 | Done | P1 | TOOL 消息落库但不回传给模型上下文 |

### 4.4 多模态附件

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| ATTACH-001 | 消息模型支持附件元数据 | Done | P1 | `ChatAttachment` + `messages.attachmentsJson` |
| ATTACH-002 | Room v7/v8 附件和 provider 附件开关迁移 | Done | P1 | schema 7/8 已导出 |
| ATTACH-003 | 输入框附件入口 | Done | P1 | 支持选择图片、选择文件、拍摄照片 |
| ATTACH-004 | 附件复制到 app 私有目录 | Done | P1 | 保存到 `files/chat_attachments` |
| ATTACH-005 | OpenAI Responses 图片/文件提交 | Done | P1 | 图片走 `input_image`，文件走 `input_file` data URL |
| ATTACH-006 | 用户气泡展示附件 | Done | P1 | 气泡内显示附件列表 |
| ATTACH-007 | 图片缩略图和 app 内预览 | Done | P1 | 图片附件显示缩略图，点击弹窗放大 |
| ATTACH-008 | provider 级附件开关 | Done | P1 | DeepSeek 默认关闭，OpenAI/TokenHub 默认开启 |
| ATTACH-009 | 大文件和图片压缩策略 | Planned | P1 | 需要限制大小、压缩图片、超大文件改走 Files API |
| ATTACH-010 | PDF/文本文件 app 内预览 | Planned | P2 | 当前非图片文件走系统打开 |

### 4.5 设置、主题和配置导入导出

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| SETTINGS-001 | 设置从弹窗升级为独立页面 | Done | P1 | 主题、字体、debug、搜索和 provider 管理集中入口 |
| SETTINGS-002 | 主题色配置 | Done | P1 | 多套调色板影响 app、聊天背景、控件、气泡 |
| SETTINGS-003 | 夜间模式 | Done | P1 | 修复顶部文字、按钮、输入框图标暗色可见性 |
| SETTINGS-004 | 字体大小调整 | Done | P1 | 设置中支持字体缩放 |
| SETTINGS-005 | Provider 配置文本导入导出 | Done | P1 | JSON 文本包含 provider 字段和 key |
| SETTINGS-006 | Provider 配置二维码导入导出 | Planned | P2 | 复杂度较高，第一版暂缓 |

### 4.6 聊天 UI 和交互

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| UI-001 | 输入框光标跳动修复 | Done | P0 | 使用 `TextFieldValue` 保持 selection |
| UI-002 | AI 流式输出自动追踪底部 | Done | P1 | 用户手动上滑后暂停追踪，回到底部后恢复 |
| UI-003 | 输出中状态和停止按钮 | Done | P1 | 发送按钮在生成中变为停止按钮 |
| UI-004 | 顶部工具条压缩 | Done | P1 | 分享/多选等操作合并到菜单 |
| UI-005 | 抽屉外区域点击关闭 | Done | P1 | 点击非抽屉区域可收起 |
| UI-006 | 选择模式点击气泡即选中 | Done | P1 | 不再必须点 checkbox |
| UI-007 | “选择到这里”快捷按钮 | Done | P1 | 滚动到其他气泡处可范围选择 |
| UI-008 | 选中气泡高亮 | Done | P1 | 边框和容器色增强 |
| UI-009 | Markdown 表格渲染 | Done | P1 | 表格边框、行高、分隔线处理已优化 |
| UI-010 | `---` Markdown 分隔线 | Done | P1 | 渲染为横线 |
| UI-011 | 滚动条平滑性 | Watch | P1 | 已优化，长气泡边界卡顿仍需真机观察 |
| UI-012 | 中文乱码修复 | In Progress | P1 | 主界面大部分已修复，tracker 本次已重写；仍需持续扫描 |

### 4.7 后台生成和通知

| ID | 任务 | 状态 | 优先级 | 验收 |
| --- | --- | --- | --- | --- |
| BG-001 | 多会话生成任务不中断 | Done | P1 | 切换会话不取消其他会话生成 |
| BG-002 | 前台服务保活生成 | Done | P1 | 生成中启动 foreground service |
| BG-003 | 后台完成通知 | Done | P1 | app 在后台且生成完成时发通知 |
| BG-004 | Android 13+ 通知权限请求 | Done | P1 | MainActivity 请求 `POST_NOTIFICATIONS` |

## 5. 已知风险

| ID | 风险 | 状态 | 优先级 | 处理建议 |
| --- | --- | --- | --- | --- |
| RISK-001 | 附件使用 Base64 data URL，图片/文件过大时会导致请求体和 token 成本过高 | Watch | P1 | 增加大小限制、图片压缩、Files API 上传 |
| RISK-002 | DeepSeek 当前不支持图片/文件附件 | Done | P1 | 默认关闭附件按钮，保留 provider 开关 |
| RISK-003 | OpenAI hosted web_search 不一定暴露所有内部访问 URL | Watch | P1 | 保留 raw log 和多策略解析 |
| RISK-004 | 长图导出超长对话可能触达 Bitmap 内存限制 | Watch | P1 | 后续做分页/PDF 导出 |
| RISK-005 | `AIChatAppRoot.kt` 文件过大 | Watch | P2 | 后续按 screen/component/dialog/markdown 拆分 |
| RISK-006 | 中文乱码可能仍残留在旧代码或历史 tracker 文本里 | Watch | P1 | 每次提交前执行 mojibake 扫描 |
| RISK-007 | Room migration 版本增长较快 | Watch | P2 | 增加 migration 自动化测试 |
| RISK-008 | release keystore 丢失会导致同包名无法升级 | Watch | P0 | 需要安全备份 JKS 和密码文件 |

## 6. 下一步计划

### 6.1 P0 / P1

| ID | 任务 | 状态 | 验收标准 |
| --- | --- | --- | --- |
| NEXT-P1-001 | 附件大小限制和错误提示 | Planned | 超过限制时不发送，并提示压缩或改用文件上传 |
| NEXT-P1-002 | 图片发送前压缩 | Planned | 大图自动压缩到合理分辨率，保留原图查看 |
| NEXT-P1-003 | OpenAI Files API 上传路径 | Planned | 大 PDF/文件不走 Base64，改用 `file_id` |
| NEXT-P1-004 | 搜索工具卡片细节优化 | Planned | 显示查询词、打开网页 URL、citation URL 的层级关系 |
| NEXT-P1-005 | 真实 GPT Key 验证图片 + PDF 输入 | Planned | 真机选择图片/PDF 后 GPT 能正确识别内容 |
| NEXT-P1-006 | 真实 DeepSeek Key 回归验证附件按钮隐藏 | Planned | DeepSeek 对话无附件按钮，纯文本/搜索功能正常 |
| NEXT-P1-007 | 滚动卡顿专项优化 | Planned | 排查长气泡交界处滚动卡顿和自绘滚动条影响 |
| NEXT-P1-008 | provider 错误提示进一步中文化 | Planned | 401、429、超时、DNS、SSL、Base URL 错误均有清晰提示 |

### 6.2 P2 / P3

| ID | 任务 | 状态 | 验收标准 |
| --- | --- | --- | --- |
| NEXT-P2-001 | PDF/文本 app 内预览 | Planned | PDF 或文本附件可在 app 内预览 |
| NEXT-P2-002 | Provider 多模态能力矩阵 | Planned | 设置页展示每个 provider 是否支持图片、文件、搜索、reasoning |
| NEXT-P2-003 | 二维码导入导出 Provider 配置 | Planned | 支持配置生成二维码和扫码导入 |
| NEXT-P2-004 | UI 文件拆分 | Planned | `AIChatAppRoot.kt` 拆分为 chat/settings/drawer/markdown/components |
| NEXT-P2-005 | Room migration 测试 | Planned | 从 schema 1 到最新 schema 自动迁移验证 |
| NEXT-P2-006 | 导出内容包含附件索引 | Planned | Markdown/长图导出能列出用户上传附件名称 |
| NEXT-P3-001 | Anthropic adapter | Planned | Claude Messages API 文本流式对话 |
| NEXT-P3-002 | Gemini adapter | Planned | Gemini GenerateContent 文本和多模态对话 |
| NEXT-P3-003 | 对话 PDF 导出 | Planned | 支持完整对话导出 PDF |
| NEXT-P3-004 | Prompt 模板和系统提示词 | Planned | 可保存常用 prompt 并按会话应用 |

## 7. 验证清单

本机 Android 环境：

```powershell
$env:JAVA_HOME='D:\Projects\Personal\AndroidApps\.devtools\android\jdk\jdk-17.0.18+8'
$env:ANDROID_HOME='D:\Projects\Personal\AndroidApps\.devtools\android\sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:GRADLE_USER_HOME='D:\Projects\Personal\AndroidApps\.gradle-user-home'
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"
```

每次核心功能变更后建议执行：

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat :app:compileDebugKotlin --console=plain --no-daemon
.\gradlew.bat :app:assembleDebug --console=plain --no-daemon
```

提交前建议执行中文乱码扫描：

```powershell
$pattern = @(
    '\u9352', '\u93C2', '\u7EEE', '\u9441', '\u7035', '\u95B0',
    '\u6D93', '\u6D63', '\u6960', '\u7481', '\u6FB6', '\u5BB8',
    '\u9422', '\u9597', '\u93C5', '\u93C6', '\u59AF',
    '\u6D34', '\u93C9', '\u6AD9', '\u6FE1', '\u704F', '\u8930',
    '\u6DC7', '\u757E', '\u7BA0', '\u6D7C', '\u934F', '\u93B4',
    '\u680D', '\u8DFA', '\u5997', '\u4E4F', '\u6BDA', '\u935A',
    '\u699B', '\u9483', '\u7C83', '\u943D', '\u6A3A', '\u7F02',
    '\u5F42', '\u7F03', '\u56E5', '\u57CC', '\u6434'
) -join '|'
rg -n $pattern app/src/main/java app/src/test/java PROJECT_TRACKER.md README.md
```

## 8. Tracker 维护规则

- 每完成一个功能，更新对应条目的状态、进度和验收说明。
- 发现明显 bug 时，先加入风险或近期计划，再决定是否提升到 P0/P1。
- 不在 tracker 写入真实 API Key、keystore 密码、Jenkins 凭据或其他敏感信息。
- 已实现但尚未真机验证的功能优先标为 `Watch`。
- 每次 release 前更新总体进度、风险和验证清单。
