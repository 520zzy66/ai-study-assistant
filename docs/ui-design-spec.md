# AI Study Assistant UI Design Specification

Version: 1.0

Author: ChatGPT

Status: Draft

---

# Part 1 Product Foundation

---

# 1. Product Overview

## 1.1 Project Name

AI Study Assistant

---

## 1.2 Product Positioning

AI Study Assistant 是一款面向高校学生的 AI 学习平台。

它不是聊天机器人。

它也不是传统的在线教育平台。

它更像是一款帮助学生完成整个学习流程的 Productivity Tool（效率工具）。

AI 在产品中属于一种能力，而不是产品本身。

用户真正使用的是：

学习资料

↓

AI 总结

↓

文档问答

↓

自动练习

↓

错题整理

↓

学习计划

↓

持续学习

因此，整个产品应该围绕学习效率展开，而不是围绕 AI 聊天展开。

---

## 1.3 Product Goal

帮助学生：

- 更快理解知识
- 更快整理资料
- 更快查找答案
- 更快完成复习
- 更科学安排学习计划

最终提高学习效率。

---

## 1.4 Design Goal

整个产品需要体现：

Professional

Minimal

Elegant

Reliable

Focused

Calm

Trustworthy

AI 不应该成为页面视觉中心。

学习内容才是页面核心。

---

# 2. Design Philosophy

---

## 2.1 Content First

所有页面都必须突出内容。

不是突出按钮。

不是突出动画。

不是突出 AI。

内容永远排第一。

例如：

学习资料

Markdown

题目

学习计划

应该拥有最大的视觉权重。

---

## 2.2 Simplicity

减少视觉噪音。

避免：

过多颜色

渐变

发光

复杂背景

炫酷动画

每一个元素都应该具有存在价值。

---

## 2.3 Consistency

整个系统必须保持一致。

包括：

颜色

字体

按钮

圆角

卡片

输入框

弹窗

表格

所有页面保持统一语言。

---

## 2.4 Predictability

用户应该知道：

点击以后会发生什么。

页面应该符合现代 SaaS 产品的交互习惯。

不要创造新的交互方式。

---

# 3. Target Users

---

## Primary Users

大学本科学生

研究生

考研学生

职业资格考试学习者

---

## User Characteristics

年龄：

18~28 岁

使用设备：

Notebook

Desktop

学习时间：

每天 1~5 小时

特点：

需要管理大量 PDF

需要快速查找知识

希望 AI 帮助学习

而不是聊天。

---

# 4. User Journey

用户典型流程：

登录

↓

进入 Dashboard

↓

上传学习资料

↓

AI 自动解析

↓

阅读 AI 总结

↓

提出问题

↓

生成练习

↓

完成练习

↓

加入错题本

↓

制定学习计划

↓

持续学习

整个流程应该尽量减少页面跳转。

---

# 5. Product Information Architecture

Dashboard

├── 学习资料

│ ├── 上传

│ ├── 管理

│ └── 删除

│

├── AI 总结

│ ├── Markdown

│ └── 导出

│

├── AI 问答

│ ├── 提问

│ ├── 引用

│ └── 历史记录

│

├── AI 自动出题

│ ├── 单选

│ ├── 判断

│ ├── 简答

│ └── 自动评分

│

├── 错题本

│ ├── 标签

│ ├── 收藏

│ └── 重做

│

├── 学习计划

│ ├── Timeline

│ ├── Calendar

│ └── 今日任务

│

└── 用户中心

---

# 6. Navigation Structure

Sidebar Navigation

Dashboard

学习资料

AI 总结

AI 问答

自动出题

错题本

学习计划

设置

Header

Logo

Search

Notification

Avatar

页面深度最多两级。

禁止三层菜单。

---

# 7. Design References

本产品参考以下产品：

Apple

Notion

Linear

GitHub

Arc Browser

Stripe Dashboard

飞书

设计目标：

不是复制。

而是吸收：

Apple 的留白

Notion 的阅读体验

Linear 的信息层级

GitHub 的列表

飞书的效率工具设计

最终形成统一的设计语言。

---

# 8. Design Keywords

Professional

Minimal

Clean

Modern

Elegant

Readable

Productivity

Enterprise

Content First

---

# 9. Success Metrics

用户进入 Dashboard

3 秒内知道今天要做什么。

上传资料

10 秒内找到上传按钮。

AI 回答

阅读时间不超过 30 秒。

学习计划

一眼知道今天任务。

整个系统学习成本应小于 5 分钟。

---

# 10. Global UX Principles

页面应保持：

留白充分

操作简单

颜色克制

按钮明确

反馈及时

避免：

复杂动画

隐藏入口

不明确状态

所有 AI 相关功能必须明确显示：

正在生成

生成完成

失败原因

引用来源

提高用户信任感。

# Part 2 Design System

---

# 11. Design Language

## 11.1 Overall Style

整个产品采用 **Modern Enterprise SaaS** 风格。

设计理念：

- 少即是多（Less is More）
- Content First
- Information Hierarchy
- High Readability
- Calm Interface
- Professional Experience

本产品不是 AI 聊天软件。

因此：

AI 不作为视觉主体。

页面的重点永远是：

学习内容

而不是：

聊天窗口。

---

## 11.2 Design Characteristics

整个系统应具有以下特征：

✓ 大面积留白

✓ 简洁配色

✓ 圆角卡片

✓ 浅阴影

✓ 信息密度适中

✓ 优秀排版

✓ 清晰层级

✓ 企业级 SaaS 风格

禁止：

✕

霓虹

✕

发光

✕

科技粒子

✕

机器人头像

✕

复杂背景

✕

赛博朋克

✕

过度渐变

---

# 12. Color System

整个系统使用 Neutral + Blue。

蓝色仅用于强调。

不要出现多个主题色。

---

## 12.1 Primary

Primary 500

#2563EB

主要按钮

链接

当前菜单

Progress

Primary Hover

#1D4ED8

Primary Active

#1E40AF

---

## 12.2 Neutral

Background

#F8FAFC

Page

Card

#FFFFFF

Border

#E5E7EB

Divider

#F1F5F9

Title

#111827

Body

#374151

Secondary

#6B7280

Placeholder

#9CA3AF

Disabled

#D1D5DB

---

## 12.3 Functional

Success

#16A34A

Warning

#F59E0B

Danger

#DC2626

Info

#0EA5E9

---

## 12.4 Usage Rules

整个系统：

80%

Neutral

15%

Blue

5%

Success / Warning / Error

禁止出现：

红绿蓝紫同时大量存在。

---

# 13. Typography

推荐字体：

Inter

PingFang SC

font-weight：

400

500

600

700

不要使用：

艺术字体

科技字体

等宽字体。

---

## 13.1 Text Hierarchy

Display

36

Bold

H1

32

Bold

H2

24

Bold

H3

20

SemiBold

Title

18

SemiBold

Body

16

Regular

Secondary

14

Regular

Caption

12

Regular

---

## 13.2 Line Height

Display

48

Title

40

Body

28

Caption

20

阅读体验优先。

---

# 14. Spacing System

采用：

8pt Grid

所有间距：

必须是：

4

8

12

16

24

32

40

48

64

禁止：

17px

23px

37px

等随机值。

---

## 14.1 Card Padding

Small

16

Medium

24

Large

32

---

## 14.2 Section Gap

Section

32

Card

24

Content

16

Label

8

---

# 15. Radius

统一圆角。

Small

8px

Medium

12px

Large

16px

Dialog

20px

禁止：

每个组件使用不同圆角。

---

# 16. Shadow

保持轻量。

Shadow XS

0 1px 2px rgba(0,0,0,.04)

Shadow SM

0 2px 6px rgba(0,0,0,.06)

Shadow MD

0 8px 24px rgba(0,0,0,.08)

整个系统不要超过：

MD

Hover 时：

SM → MD

即可。

---

# 17. Grid System

Desktop

12 Columns

Max Width

1440px

Content Width

1280px

Sidebar

240px

Header

64px

Content Padding

32px

Card Gap

24px

---

## Responsive

Desktop

>=1440

Large Desktop

1200~1440

Laptop

992~1200

Tablet

768~992

Mobile

<768

---

# 18. Icon System

使用：

Element Plus Icons

保持统一。

大小：

Small

16

Normal

20

Large

24

Extra

32

图标颜色：

默认：

#6B7280

Hover：

Primary

禁止：

彩色 Icon。

---

# 19. Motion

动画必须克制。

Duration

Fast

150ms

Normal

200ms

Slow

300ms

Timing

ease

ease-in-out

---

Hover

Card

translateY(-2px)

Button

Background

Color

Shadow

Input

Border Color

禁止：

Bounce

Rotate

Zoom

大范围动画。

---

# 20. Accessibility

所有文字：

对比度符合 WCAG AA。

按钮：

高度不少于：

40px

Input：

高度：

40px

Primary Button：

44px

所有点击区域：

>=40px。

支持：

Keyboard Navigation

Focus

Screen Reader

---

# 21. Theme

当前版本：

Light Theme

后续预留：

Dark Theme

Dark Theme 不影响组件结构。

仅修改：

Color Tokens。

---

# 22. Design Tokens

整个项目所有颜色不得写死。

统一定义：

Primary

Primary Hover

Primary Active

Background

Card

Border

Text

Secondary

Success

Warning

Danger

所有间距：

统一变量。

所有圆角：

统一变量。

所有阴影：

统一变量。

禁止：

组件内部直接写：

#409EFF

16px

24px

等 Magic Number。

必须引用 Design Token。

# Part 3 Component Library

---

# 23. Component Design Principles

整个系统所有组件遵循以下原则：

## Consistency（一致性）

所有组件必须：

- 使用统一圆角
- 使用统一阴影
- 使用统一颜色
- 使用统一间距
- 使用统一动画

整个系统不允许出现：

- 两种不同风格 Button
- 两种不同风格 Card
- 两种不同风格 Dialog

所有组件必须来自同一 Design System。

---

## Simplicity（简洁）

一个组件只负责一件事情。

例如：

Button

负责点击。

Card

负责承载信息。

不要让一个组件承担多个职责。

---

## Predictability（可预期）

Hover

Focus

Loading

Disabled

所有状态必须统一。

例如：

所有 Button Hover：

背景颜色加深。

所有 Card Hover：

轻微上浮。

---

# 24. Button

## Purpose

用于触发操作。

按钮必须让用户明确知道：

点击后会发生什么。

---

## Types

Primary

用于：

主要操作

例如：

上传资料

保存

提交

生成 AI 总结

颜色：

Primary Blue

---

Secondary

白底

灰边

用于：

取消

返回

普通操作

---

Danger

红色

仅用于：

删除

清空

危险操作

---

Text Button

无背景。

用于：

查看更多

跳转

辅助操作。

---

Icon Button

仅图标。

用于：

搜索

刷新

设置

收藏

通知

---

## Size

Small

32px

Medium

40px

Large

44px

Padding：

16~24px

---

## States

Default

Hover

Active

Disabled

Loading

所有状态必须统一。

Loading：

显示 Spinner。

禁止重复点击。

---

## Usage Rules

一个页面：

最多一个 Primary Button。

不要出现多个蓝色按钮竞争视觉焦点。

---

# 25. Card

## Purpose

承载信息。

整个系统大量采用 Card。

---

## Radius

16px

---

## Padding

24px

---

## Shadow

默认：

Shadow XS

Hover：

Shadow MD

TranslateY(-2px)

---

## Types

Statistic Card

Information Card

Action Card

Document Card

Answer Card

---

## Usage

Dashboard

Learning Materials

Quiz

Study Plan

全部使用 Card。

---

# 26. Input

统一使用：

Element Plus Input

---

## Height

40px

---

## Radius

12px

---

## Placeholder

Secondary Color

---

## States

Default

Hover

Focus

Disabled

Error

---

Focus：

Border：

Primary

禁止：

发光。

---

# 27. Search Bar

Search 是整个系统重要入口。

布局：

🔍

Input

Clear

Search

支持：

Enter

实时搜索

搜索历史（预留）

---

# 28. Select

统一：

40px

支持：

Search

Clear

Disabled

Multiple

不要：

自定义奇怪样式。

---

# 29. Upload

学习资料上传属于核心组件。

支持：

PDF

Word

Markdown

TXT

---

上传状态：

Waiting

Uploading

Processing

Completed

Failed

---

上传成功：

绿色提示。

上传失败：

显示原因。

---

# 30. Table

整个系统：

优先 Card。

其次 Table。

---

Table Header：

浅灰背景。

Row Hover：

Background：

#F8FAFC

支持：

分页

排序

筛选

固定列

---

# 31. Dialog

用于：

删除确认

编辑

AI 设置

---

Width

600px

Radius

20px

支持：

ESC

点击外部关闭（可配置）

---

# 32. Drawer

用于：

详情

文档信息

用户资料

移动端菜单

宽度：

480px

---

# 33. Sidebar

整个产品导航中心。

Width：

240px

包含：

Logo

Menu

Bottom Area

---

Menu：

Dashboard

学习资料

AI 总结

AI 问答

自动出题

错题本

学习计划

设置

---

Active：

蓝色背景

左侧 Indicator

---

# 34. Header

Height：

64px

左：

Logo

标题

中：

Search

右：

Notification

Avatar

Dropdown

---

# 35. Statistic Card

Dashboard 专用。

显示：

Icon

Title

Value

Trend

支持：

同比增长

本周变化

Hover：

轻微阴影。

---

# 36. AI Answer Card

AI 专属组件。

包含：

回答

引用来源

文档

页码

置信度

生成时间

复制按钮

重新生成

导出 Markdown

不要做成聊天气泡。

采用文档阅读卡片。

---

# 37. Markdown Viewer

整个系统：

Markdown 阅读体验必须优秀。

支持：

标题

代码

表格

引用

数学公式（预留）

图片

目录（可选）

字体：

16px

阅读宽度：

800px

---

# 38. Timeline

Study Plan 使用。

节点：

Today

Tomorrow

Next Week

Completed

颜色：

蓝色

完成：

绿色

---

# 39. Progress

用于：

学习进度

Quiz

上传

AI 分析

高度：

8px

圆角：

999px

---

# 40. Tag

用于：

知识点

难度

状态

标签

不要超过：

4种颜色。

推荐：

Blue

Green

Orange

Gray

---

# 41. Empty State

所有页面必须设计 Empty State。

包括：

无资料

无学习计划

无错题

无 AI 回答

包含：

Illustration（简约）

Title

Description

Primary Action

---

# 42. Skeleton

所有异步页面：

必须 Skeleton。

不要：

Loading Spinner 覆盖整个页面。

Skeleton 优先。

---

# 43. Notification

统一：

右上角。

支持：

Success

Warning

Error

Info

自动消失：

3 秒。

---

# 44. Loading

按钮：

Spinner。

页面：

Skeleton。

AI：

Progress。

禁止：

页面一直空白等待。

---

# 45. Component Mapping

推荐与 Element Plus 对应关系：

| Design Component | Element Plus |
|------------------|-------------|
| Button | ElButton |
| Input | ElInput |
| Select | ElSelect |
| Upload | ElUpload |
| Table | ElTable |
| Dialog | ElDialog |
| Drawer | ElDrawer |
| Progress | ElProgress |
| Timeline | ElTimeline |
| Tag | ElTag |
| Notification | ElNotification |
| Skeleton | ElSkeleton |

所有业务组件应基于 Element Plus 二次封装，禁止在页面中直接堆砌原始组件。

例如：

components/base/
├── BaseButton.vue
├── BaseCard.vue
├── BaseInput.vue
├── BaseDialog.vue
├── BaseTable.vue
├── BasePageHeader.vue
├── BaseStatisticCard.vue

这样可以保证整个系统视觉统一，并方便后续维护。

# Part 4 Page Specification

---

# 46. Login

## Purpose

用户身份认证。

整个登录页应该给人：

Professional

Simple

Trustworthy

而不是：

科技感

AI感

登录页应该像：

Notion

GitHub

Linear

而不是：

ChatGPT。

---

## Layout

Desktop

```

┌──────────────────────────────────────────────┐
│                                              │
│                Logo                          │
│          AI Study Assistant                  │
│                                              │
│     ┌────────────────────────────┐           │
│     │                            │           │
│     │    Username                │           │
│     │                            │           │
│     │    Password                │           │
│     │                            │           │
│     │ [ Login ]                  │           │
│     │                            │           │
│     └────────────────────────────┘           │
│                                              │
└──────────────────────────────────────────────┘

```

---

## Component Tree

LoginPage

Logo

Login Card

Input

Input

Button

Footer

---

## Login Card

Width

420px

Radius

20px

Padding

40px

Background

White

Shadow

SM

---

## Interaction

Hover

Button

Border

Input

Login Loading

Success

Failure

---

## Empty

无。

---

## Responsive

Mobile：

宽度：

100%

Padding：

24px

Card：

无阴影。

---

# 47. Dashboard

## Purpose

Dashboard 是整个产品的首页。

用户打开以后：

3 秒内知道：

今天学什么。

学习进度。

距离考试。

最近学习。

而不是：

看到聊天框。

---

## Layout

Desktop

```

Header

---------------------------------------------------

Sidebar

Content

---------------------------------------------------

Greeting

---------------------------------------------------

Statistics

---------------------------------------------------

Recent Learning

Study Progress

---------------------------------------------------

Today's Tasks

Exam Countdown

---------------------------------------------------

Weekly Activity

```

---

## Grid

12 Columns

Row Gap

24

Column Gap

24

---

## Sections

① Greeting

欢迎回来

用户名

今天日期

学习建议

---

② Statistics

四张 Card

学习时长

学习资料

AI总结

练习次数

---

③ Recent Learning

最近学习资料

最近总结

继续学习

---

④ Study Progress

Progress

Heatmap

学习曲线

---

⑤ Today's Tasks

Timeline

Checkbox

完成状态

---

⑥ Exam Countdown

考试名称

剩余天数

计划完成率

---

## Component Tree

Dashboard

Page Header

Statistic Card ×4

Recent Learning Card

Progress Card

Timeline Card

Calendar Card

Quick Action Card

---

## Empty

第一次使用：

显示：

上传学习资料

按钮。

---

## Loading

Skeleton。

---

## Error

局部刷新。

不要整个页面报错。

---

## Responsive

Tablet

两列。

Mobile

一列。

---

# 48. Learning Materials

## Purpose

管理所有学习资料。

体验参考：

Notion。

---

## Layout

```

Header

----------------------------------------

Search

Filter

Upload

----------------------------------------

Document List

```

---

## Document Card

包含：

标题

大小

上传时间

状态

操作

---

## Status

Uploading

Parsing

Ready

Failed

---

## Interaction

Hover：

显示：

更多操作。

---

## Empty

暂无学习资料。

Primary Button：

上传资料。

---

## Upload

支持：

拖拽

点击

多文件

PDF

Word

Markdown

TXT

---

# 49. AI Summary

## Purpose

阅读 AI 总结。

不是聊天。

---

## Layout

```

Left

Document List

----------------

Right

Markdown Viewer

```

---

## Left

Document

Search

Folder

Recent

---

## Right

Markdown

目录

复制

导出

重新生成

---

## Reading Width

800px

保证阅读体验。

---

## Empty

请选择一个文档。

---

## Loading

Skeleton。

不要：

Spinner。

---

# 50. AI Question Answering

## Purpose

帮助用户基于学习资料提问。

不是聊天。

更像：

知识查询。

---

## Layout

```

Question

↓

Answer Card

↓

Reference

↓

History

```

---

## Answer Card

包含：

回答

引用

来源

页码

置信度

复制

导出

重新回答

---

## Reference

Document

Page

Similarity

Snippet

---

## History

Collapse

按时间排序。

---

## Empty

输入一个问题开始。

---

# 51. Quiz

## Purpose

练习。

考试。

自动评分。

---

## Layout

```

Progress

------------------

Question

------------------

Options

------------------

Next

```

---

## Top

Question Number

Progress

Timer

---

## Bottom

上一题

下一题

提交

---

## Result

Score

Wrong

Explanation

Suggestion

---

## Empty

暂无题目。

---

# 52. Wrong Question Book

## Purpose

帮助用户复习。

---

## Layout

```

Search

Filter

Tag

--------------------

Question List

```

---

## Card

Question

Knowledge Tag

Difficulty

Wrong Times

Retry

Favorite

---

## Statistics

Wrong Count

Correct Rate

Review Times

---

# 53. Study Plan

## Purpose

帮助用户持续学习。

---

## Layout

```

Calendar

Timeline

Today's Tasks

Progress

```

---

## Calendar

Month

Week

Day

---

## Timeline

Today

Tomorrow

This Week

Next Week

---

## Task Card

Task

Duration

Status

Start

Complete

---

## Empty

暂无计划。

Primary：

生成学习计划。

---

# 54. User Center

## Layout

Avatar

Basic Information

Learning Statistics

Achievements

Account Settings

Security

---

## Statistics

Study Days

Study Hours

Documents

AI Usage

Quiz Count

---

# 55. Settings

## Sections

General

Theme

Language

AI Settings

Notification

Account

Security

About

---

## AI Settings

Temperature

Top-P

Default Summary Length

Citation Display

Auto Generate Quiz

---

# 56. Global Page Rules

所有页面必须包含：

Page Header

Breadcrumb（可选）

Title

Description

Primary Action

Content

Empty State

Loading State

Error State

Footer（可选）

---

# 57. Responsive Rules

Desktop

1440+

12 Columns

Laptop

1200

10 Columns

Tablet

768

6 Columns

Mobile

1 Column

Sidebar：

自动折叠。

---

# 58. Interaction Rules

所有按钮：

Hover

Focus

Loading

Disabled

统一。

所有 Card：

Hover：

translateY(-2px)

所有列表：

支持：

Skeleton

Empty

Pagination

Search

Filter

保持整个系统交互一致。
