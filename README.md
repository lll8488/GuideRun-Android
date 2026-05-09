# GuideRun 助盲跑APP
## 项目介绍
GuideRun是一款面向视障跑者与陪跑志愿者的专业安卓工具APP，解决视障人群独立跑步的安全隐患与双方对接困难的痛点。产品以"安全、专业、简单、无障碍"为核心原则，实现从需求发布到跑步完成的全流程闭环。

## 小组信息
- 组长：袁纪欢
- 组员：刘绍滨
- 课程：Android
- 版本：MVP 1.0

## MVP核心功能
1. 双身份用户体系（手机号登录、身份选择、个人信息编辑）
2. 陪跑专业规范学习与考核（4节课程+10题判断题）
3. 需求发布与匹配（地图选点、需求列表、接单功能）
4. 跑步语音辅助（前台服务计时、周期口令播报、一键紧急求助）
5. 跑步记录管理（自动生成记录、历史查询、删除）

## 技术栈
- 开发语言：Kotlin
- 核心技术：Activity/Fragment、原生SQLite、Foreground Service、TextToSpeech、Vibrator
- 第三方库：百度地图SDK v5.2.0、GraphView v4.2.2
- 无障碍：Android TalkBack全场景适配

## 运行环境
- 最小SDK：API 24（Android 7.0）
- 目标SDK：API 34（Android 14）
- 测试设备：Pixel 5（API 24）模拟器

## 项目结构