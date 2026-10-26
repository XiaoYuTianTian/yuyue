# 设计系统：纸 · 墨 · 苔 · 陶

## Token

| 名 | 角色 | Light |
|----|------|-------|
| Paper `#EEF1EC` | 页底 | 冷灰绿纸，非奶油黄 |
| Ink `#1A1F1C` | 正文 | |
| Moss `#3F6B52` | 主操作 / 语音球 | |
| Clay `#9A6B4F` | 次级强调 | |
| Hazard `#B54A3C` | 破坏性 / 未完成警示 | |

实现：`android-app/.../ui/theme/{Color,Theme,Type,Shape}.kt`  
组件雏形：`ui/paper/PaperPrimitives.kt`（Scaffold / EventCapsule / EmptyState）

## 排版

- 标题：衬线（`FontFamily.Serif` / 原型用 Noto Serif SC）
- 正文：无衬线，支持动态字体
- 不使用 Inter / Roboto / 系统默认作为品牌信号

## 动效（仅服务状态）

1. 按住语音球：缩放 + 触觉（产品实现）
2. 原文浮现：轻微 blur→清晰
3. 事件落轴 / 回忆连线：自左展开的苔色线

支持系统「减少动画」。

## 五条可点原型

打开 [`prototypes/index.html`](prototypes/index.html)：

1. **首次体验**：品牌「语曰」为英雄信号 → 真实试说
2. **按住说并预览**：按住球体 → 预览卡 → 写入 / 重说
3. **月历下钻**：有记忆日期 → 当日胶囊 → 详情
4. **说话回忆**：问法芯片 / 按住问 → 可追溯节点
5. **备份恢复**：试恢复预览（checksum）→ 不自动写入

验收标准：去掉导航后首页仍能认出「语曰」；主循环只有记/看/忆；恢复强调 Room 证据优先。
