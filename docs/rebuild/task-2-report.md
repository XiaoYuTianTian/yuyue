# Task 2 报告：设计系统与原型

## 状态

**DONE**

## 变更

| 路径 | 说明 |
|------|------|
| `ui/theme/Color.kt` | 纸墨苔陶 token；旧 Signal/Pulse 等 deprecated 别名 |
| `ui/theme/Theme.kt` | Light/Dark ColorScheme 重绑 |
| `ui/theme/Type.kt` | 衬线标题 + 无衬线正文 |
| `ui/theme/Shape.kt` | 纸面圆角 |
| `ui/paper/PaperPrimitives.kt` | Scaffold / EventCapsule / EmptyState |
| `docs/rebuild/design-system.md` | Token 与验收说明 |
| `docs/rebuild/prototypes/index.html` | 五条可点交互原型 |

## 验收

浏览器打开 `docs/rebuild/prototypes/index.html` 走完：首次体验 → 按住预览 → 月历下钻 → 回忆 → 备份试恢复。

## 风险

- 正式 Compose 页面仍用旧布局；Theme 已切 token，视觉会部分变化。
- 未打包自定义字体文件，运行时用系统 Serif/Sans。
