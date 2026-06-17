组长：袁纪欢 2023463030735  
组员：刘绍滨 2023463030720

git链接：https://github.com/lll8488/GuideRun-Android.git

---

# 助盲跑 APP — 完整功能与页面文档

> 本文档覆盖 App 所有功能模块、页面布局、样式设计、跳转流程、数据架构、无障碍特性
  
---

# 第一部分：项目架构

## 1.1 分层架构

```
┌──────────────────────────────────────────────────────┐
│  UI 层（20 个 Activity + 1 个 Foreground Service）     │
│  ├─ 基础模块：Splash / Login / IdentitySelect         │
│  ├─ 视障模块：9 个页面                                 │
│  └─ 志愿者模块：7 个页面                               │
├──────────────────────────────────────────────────────┤
│  工具层                                               │
│  ├─ AppPrefs：统一 SharedPreferences 配置管理          │
│  ├─ TtsHelper：全局 TextToSpeech 单例                 │
│  └─ ThemeHelper：高对比度主题切换                      │
├──────────────────────────────────────────────────────┤
│  ViewModel 层                                         │
│  └─ MainViewModel：StateFlow 数据绑定                 │
├──────────────────────────────────────────────────────┤
│  Repository 层（RunningRepository）                    │
│  └─ 本地优先策略：先 Room → 再无数据则 Retrofit        │
├──────────────────┬───────────────────────────────────┤
│  Room 数据库      │  Retrofit 网络层                   │
│  ├─ 2 Entity     │  ├─ 2 GET + 1 POST                │
│  ├─ 2 Dao        │  └─ jsonplaceholder.typicode.com  │
│  └─ 1 Database   │                                   │
└──────────────────┴───────────────────────────────────┘
```

## 1.2 文件结构

```
app/src/main/java/com/blindrunner/app/
├── BlindRunnerApp.kt          # Application 入口，初始化数据库/网络/TTS
├── MainActivity.kt            # 遗留入口（不再使用）
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── RunningRecordEntity.kt   # 跑步记录数据表
│   │   │   └── UserEntity.kt            # 用户信息数据表
│   │   ├── dao/
│   │   │   ├── RunningRecordDao.kt      # 跑步记录 CRUD
│   │   │   └── UserDao.kt              # 用户查询/写入
│   │   └── AppDatabase.kt              # Room 数据库单例
│   ├── remote/
│   │   ├── api/ApiService.kt            # Retrofit API 接口
│   │   ├── model/                       # API DTO（PostResponse/UserResponse/CreatePostRequest）
│   │   └── RetrofitClient.kt            # Retrofit 单例 + OkHttp 拦截器
│   └── repository/
│       └── RunningRepository.kt         # 数据仓库（本地优先策略）
├── domain/model/
│   └── RunningRecord.kt                 # 领域模型
├── ui/
│   ├── splash/SplashActivity.kt         # 启动页
│   ├── auth/                            # 认证模块
│   │   ├── LoginActivity.kt
│   │   └── IdentitySelectActivity.kt
│   ├── blind/                           # 视障用户模块（9 页面）
│   │   ├── BlindHomeActivity.kt
│   │   ├── PublishDemandActivity.kt
│   │   ├── MapPickerActivity.kt
│   │   ├── StartRunningActivity.kt
│   │   ├── RunningModeActivity.kt
│   │   ├── RunningEndActivity.kt
│   │   ├── HistoryActivity.kt
│   │   ├── BlindProfileActivity.kt
│   │   ├── EmergencyContactActivity.kt
│   │   └── SettingsActivity.kt
│   ├── volunteer/                       # 志愿者模块（7 页面）
│   │   ├── VolunteerHomeActivity.kt
│   │   ├── DemandListActivity.kt
│   │   ├── DemandDetailActivity.kt
│   │   ├── TrainingCampActivity.kt
│   │   ├── CourseDetailActivity.kt
│   │   ├── ExamActivity.kt
│   │   └── VolunteerProfileActivity.kt
│   └── main/MainViewModel.kt            # 主视图模型
├── service/
│   └── RunningService.kt                # 跑步前台 Service
└── util/
    ├── AppPrefs.kt                      # 配置管理
    ├── TtsHelper.kt                     # TTS 语音引擎
    └── ThemeHelper.kt                   # 主题控制器
```

---

# 第二部分：全局系统特性

## 2.1 用户身份体系

| 特性 | 实现方式 |
|------|---------|
| 手机号登录 | 11 位手机号 + 验证码模拟 |
| 首次登录 | 强制跳转身份选择页 |
| 非首次登录 | 根据本地存储的身份类型直接进入对应首页 |
| 身份存储 | SharedPreferences（`user_type`）+ Room（`UserEntity`） |
| 身份切换 | 个人中心 → 清空身份 → 重新选择 |

## 2.2 语音辅助系统（TtsHelper）

| 场景 | 语音内容 |
|------|---------|
| 进入视障首页 | "视障用户首页，您可以发布需求、开始跑步或查看历史记录" |
| 发布需求 | "正在跳转到发布需求" |
| 开始跑步 | "正在跳转到开始跑步" |
| 跑步确认 | "确认开始跑步，请做好准备" |
| 跑步中 | 每 15/30/60 秒循环播报 4 条口令 |
| 历史记录 | "正在跳转到历史记录" |
| 个人中心 | "正在跳转到个人中心" |
| 被接单通知 | "您的需求已被接单，志愿者联系方式：xxx" |
| 设置变更 | 即时语音反馈 |

4 条跑步口令：  
1. "保持平稳呼吸，注意路面情况。"  
2. "前方若有转弯，请提前通过陪跑绳传递信号。"  
3. "保持当前节奏，配合陪跑绳力度反馈。"  
4. "注意避让障碍，提前减速。"

## 2.3 辅助功能设置（SettingsActivity）

| 设置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| 高对比度黑白主题 | Switch | 关闭 | 重启后生效 |
| 页面跳转语音提示 | Switch | 开启 | TTS 播报目的地 |
| 跑步语音口令播报 | Switch | 开启 | 跑步时播报 |
| 播报间隔 | 按钮组 | 30 秒 | 15/30/60 秒三档 |

## 2.4 紧急求助机制

| 触发方式 | 场景 | 行为 |
|---------|------|------|
| 屏幕按钮 | 跑步模式 🆘 按钮 | 震动反馈 → 拉起拨号盘填入紧急联系人号码 |
| 音量上键长按 3 秒 | 跑步模式 | 节奏震动 → 自动拉起拨号盘 |

## 2.5 数据持久化

| 存储方式 | 用途 |
|---------|------|
| Room（SQLite） | 跑步记录、用户信息、需求匹配 |
| SharedPreferences | 登录状态、身份类型、考核成绩、设置项 |

---

# 第三部分：页面详细说明

---

## 3.1 基础模块

### 3.1.1 启动页（SplashActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_splash.xml` — 深绿背景 `#1B5E20`，居中文字 |
| 样式 | 品牌页："🏃 助盲跑" + "安全·专业·简单·无障碍" |
| 逻辑 | 延迟 2 秒后自动跳转 `LoginActivity`，同时 `finish()` |
| 无障碍 | `contentDescription="助盲跑应用启动页"` |

---

### 3.1.2 登录页（LoginActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_login.xml` — 浅灰 `#FAFAFA` 背景，居中表单 |
| 组件 | 手机号输入框（`et_phone`）、验证码输入框（`et_code`）、"获取验证码"按钮（`btn_get_code`）、"登录"大按钮（`btn_login`） |
| 输入框样式 | 白底 + 灰色边框 + 8dp 圆角（`edit_bg.xml`） |
| 校验规则 | 手机号必须 11 位；验证码不能为空 |
| 登录成功 | Toast 提示 → 存入 Room/SharedPreferences → 首次跳转 `IdentitySelectActivity`，非首次跳转对应首页 |
| 无障碍 | 所有控件含 `contentDescription`；输入框类型 `phone`/`number` |

---

### 3.1.3 身份选择页（IdentitySelectActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_identity_select.xml` — 居中两枚大按钮 |
| 组件 | "视障用户"按钮（`btn_blind`）、"陪跑志愿者"按钮（`btn_volunteer`） |
| 按钮尺寸 | 80dp 高，22sp 字号 |
| 选择后 | 存入 SharedPreferences + Room → 跳转对应首页 |

---

## 3.2 视障用户模块（9 个页面）

### 3.2.1 视障首页（BlindHomeActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_blind_home.xml` — 纯白背景，顶部绿色标题栏 |
| 标题栏 | 深绿 `#1B5E20`，64dp 高，"助盲跑" + 👤 个人中心入口 |
| 功能入口 | 三大圆角按钮 + "我的需求"列表 |
| 按钮 1 | "发布陪跑需求" — 绿色实心（`btn_green_round`），80dp 高，24sp 字 |
| 按钮 2 | "开始跑步" — 绿色描边（`btn_outline_round`），80dp 高 |
| 按钮 3 | "历史记录" — 浅灰（`btn_light_round`），72dp 高 |
| 需求列表 | `RecyclerView`（`rv_my_demands`），展示待接单/已接单需求 |
| 需求项 | `item_blind_demand.xml` — 地点(20sp) + 日期时长(16sp) + 状态标签(18sp)，白色底，点击弹窗 |
| 已接单弹窗 | AlertDialog 显示志愿者姓名+电话，TTS 语音朗读 |
| 刷新机制 | `onResume()` 自动重新加载 |
| 进入语音 | "视障用户首页，您可以发布需求、开始跑步或查看历史记录" |

**需求状态标签对应：**
| status 值 | 显示文本 | 标签颜色 |
|-----------|---------|---------|
| pending | 待接单 | 橙色底 `#FFF3E0` 橙字 `#E65100` |
| accepted | 已接单 ✓ | `statusLabel()` 方法 |
| completed | 已完成 | `statusLabel()` 方法 |

---

### 3.2.2 发布需求页（PublishDemandActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_publish_demand.xml` — ScrollView，24dp 内边距 |
| 标题 | "发布陪跑需求"，28sp，深绿色 |
| 返回按钮 | `←` 按钮，56dp |
| 日期选择 | 点击弹出 DatePickerDialog，按钮显示选中日期 |
| 时间选择 | 点击弹出 TimePickerDialog，按钮显示选中时间 |
| 跑步地点 | EditText 输入框 + "地图选点"描边按钮（跳转 `MapPickerActivity`） |
| 预计时长 | 3 个圆角按钮："30分"/"45分"/"1小时"（`btn_light_round`） |
| 特殊备注 | 多行 EditText，100dp 高，选填 |
| 提交按钮 | "发布需求"绿色实心大按钮（`btn_green_round`），72dp，24sp |
| 提交校验 | 日期/时间/地点三项必填 |
| 数据存储 | `RunningRecord` → Room `insertLocal()`，状态 `pending` |
| 无障碍 | 所有标签 20sp，输入框含 `contentDescription` |

---

### 3.2.3 地图选点页（MapPickerActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_map_picker.xml` — 全屏 FrameLayout |
| 地图区 | 灰色背景模拟地图，中央 "＋" 十字准星 |
| 底部面板 | 半透明白色，显示地址文字 + 两个操作按钮 |
| 定位实现 | `LocationManager` 获取 GPS/NETWORK 最后位置 |
| 逆地理编码 | `Geocoder` 将经纬度转为中文地址（"XX区XX路附近"） |
| 权限处理 | 无权限时弹出系统授权弹窗，被拒绝则使用默认位置（北京奥林匹克公园） |
| 确认按钮 | 将地址+经纬度通过 `setResult` 回传给发布页，`finish()` |
| 取消按钮 | 直接 `finish()` |
| 降级策略 | 定位失败或权限拒绝 → 默认地址 39.9829,116.3978 |

---

### 3.2.4 开始跑步页（StartRunningActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_start_running.xml` — 居中竖向布局，纯白背景 |
| 标题 | "准备开始跑步"，32sp 深绿 |
| 信息展示 | 从 Room 查询已接单需求，显示地点/时长/模式（22sp） |
| 地点传递 | 将已接单需求的地点存入 `SharedPreferences`，供 `RunningEndActivity` 使用 |
| 开始按钮 | "开始跑步"绿色实心大按钮，280dp 宽，100dp 高，28sp |
| TTS | 进入时"准备开始跑步，确认信息后点击开始跑步按钮"，点击开始"确认开始跑步，请做好准备" |
| 返回按钮 | "返回"透明文字按钮 |

---

### 3.2.5 跑步模式页（RunningModeActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_running_mode.xml` — 全屏黑色背景，`keepScreenOn="true"` |
| 计时器 | 顶部粗体等宽数字 `00:00:00`，48sp，白色 |
| 状态文字 | "助盲跑进行中"，18sp 绿色 |
| 按钮区 | 三个大按钮水平排列 |
| 暂停按钮 | 100×100dp，⏸ 图标，点击切换 ▶/⏸ |
| 结束按钮 | 120×120dp，红色 `#D32F2F`，"结束" 22sp 粗体 |
| 求助按钮 | 100×100dp，橙色 `#FF9800`，🆘 图标 |
| 震动反馈 | 暂停 50ms / 结束 100ms / 求助三段节奏震动 |
| 前台 Service | `RunningService`：START_STICKY 保活，通知栏"助盲跑进行中"，`dataSync` 类型 |
| 语音播报 | 每可配置间隔（默认30s）播报口令，可通过设置开关 |
| 紧急拨号 | 🆘 按钮 + 音量上键长按 3 秒，读取 `AppPrefs.emergencyContact` 填入拨号盘 |
| 音量键逻辑 | `onKeyDown` 检测 `KEYCODE_VOLUME_UP`，使用 `downTime/eventTime` 同基准判断 3 秒 |
| 防重复触发 | `emergencyTriggered` 标志位 |
| 结束后 | 解绑 Service，停止 Service，传递 `duration_seconds` 到 `RunningEndActivity` |

---

### 3.2.6 跑步结束页（RunningEndActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_running_end.xml` — 居中竖向，白色背景 |
| 完成图标 | "🎉"，64sp |
| 标题 | "跑步完成"，32sp 深绿 |
| 用时展示 | 从 Intent 读取 `duration_text`，26sp 展示 |
| 查看记录 | 绿色大按钮 → `HistoryActivity` |
| 返回首页 | 描边按钮 → `BlindHomeActivity` |
| 自动保存 | `onCreate` 时自动将记录写入 Room：日期=当天，时长=秒转分，地点=从 SharedPrefs 读取已接单需求的地点 |

---

### 3.2.7 历史记录页（HistoryActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_history.xml` — 白色背景 |
| 标题栏 | 深绿 `#1B5E20`，64dp，"历史记录" + `←` 返回 |
| 日期筛选 | 两个浅灰按钮"开始日期"/"结束日期"，点击弹出 DatePickerDialog |
| 统计卡片 | 浅绿 `#F1F8E9` 背景，两列："总次数"（28sp 粗体深绿）+ "总时长" |
| 统计按钮 | 📊 图标，点击弹出 AlertDialog 统计弹窗（总次数/总时长/平均/最长/常去地点） |
| 记录列表 | `RecyclerView` + `HistoryAdapter` + `item_history.xml` |
| 记录项 | 横排：左侧日期(18sp 粗体) + 地点(14sp)，右侧时长(16sp 绿色粗体) |
| 短按 | Toast 显示地点和时长 |
| 长按 | 调用 `repository.deleteById()` 删除 → 刷新列表 → Toast "已删除" |
| 实时统计 | 每次加载/筛选后自动更新统计卡片数字 |

---

### 3.2.8 视障个人中心（BlindProfileActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_blind_profile.xml` — ScrollView，白色 |
| 标题 | "个人中心"，28sp 深绿 |
| 信息编辑 | 姓名输入框（`et_name`）+ 联系方式输入框（`et_phone`），白底灰边框，64dp 高 |
| 紧急联系人 | 橙色描边大按钮"紧急联系人设置"（`btn_outline_orange`）→ `EmergencyContactActivity` |
| 保存按钮 | 绿色实心大按钮：校验姓名非空，Toast "保存成功" |
| 辅助功能设置 | 绿色描边按钮 → `SettingsActivity` |
| 切换身份 | 透明按钮"切换到志愿者身份"→ 清空 SharedPrefs → `IdentitySelectActivity` |

---

### 3.2.9 紧急联系人页（EmergencyContactActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_emergency_contact.xml` — ScrollView，白色 |
| 标题 | "紧急联系人"，28sp 橙色 `#E65100` |
| 说明文字 | "跑步时可一键拨打（最多3位）" |
| 输入项 | 联系人 1/2/3 各含姓名 + 电话 EditText，白底灰边框 |
| 联系人1 | 必填（校验电话非空） |
| 联系人2/3 | 选填 |
| 存储 | `SharedPreferences`：`emergency_contact` 存联系人1电话 |

---

### 3.2.10 辅助功能设置页（SettingsActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_settings.xml` — ScrollView，浅灰 `#FAFAFA` |
| 标题 | "辅助功能设置"，28sp 深绿 |
| 高对比度 | Switch + 说明，切换后 Toast 提示"下次启动生效" + TTS 播报 |
| 导航语音 | Switch，即时生效 + TTS 反馈 |
| 跑步语音 | Switch，即时生效 + TTS 反馈 |
| 播报间隔 | 三按钮：15秒/30秒/60秒，选中项为绿色，其余浅灰 |

---

## 3.3 志愿者模块（7 个页面）

### 3.3.1 志愿者首页（VolunteerHomeActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_volunteer_home.xml` — 浅灰 `#F5F6FA` 背景 |
| 标题栏 | 深绿，56dp，"GuideRun" |
| 欢迎卡片 | 白色 CardView，圆角 16dp，阴影 2dp，"你好，志愿者 👋" 22sp + 引导文字 |
| 功能卡片 | 两个并排 CardView（100dp 高） |
| ─ 待接单 | 📋 图标 32sp + "待接单" 15sp，点击 → `DemandListActivity` |
| ─ 训练营 | 📖 图标 32sp + "训练营" 15sp，点击 → `TrainingCampActivity` |
| 已接单列表 | "我的接单" 18sp 标题 + RecyclerView + `MyOrdersAdapter` |
| 数据来源 | Room 查询 `status == "accepted"` 的记录 |
| 列表项 | `item_order.xml` — 绿色圆点 + 日期地点时长(14sp) |

---

### 3.3.2 需求列表页（DemandListActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_demand_list.xml` — 浅灰背景 |
| 标题栏 | 深绿，56dp，"陪跑需求" + ← 返回 |
| 搜索栏 | 白色 CardView + 🔍 图标 + 搜索输入框 |
| 筛选标签 | 横向排列标签："全部"（绿色选中）/ "30分钟" / "45分钟" / "1小时" |
| 需求列表 | `RecyclerView` + `DemandAdapter` + `item_demand.xml` |
| 数据来源 | `repository.getRecords()`，过滤 `status != "completed"` |
| 列表项点击 | 传递 id/location/date/duration/distance/status → `DemandDetailActivity` |

**item_demand.xml 卡片样式：**
| 元素 | 样式 |
|------|------|
| 卡片 | CardView，圆角 14dp，阴影 2dp，白色 |
| 地点 | 17sp 粗体 `#222222` |
| 状态标签 | 28dp 高，橙色底，12sp"待接单"（`tag_status_pending`） |
| 日期行 | 绿色圆点 + 13sp 灰色文字 |
| 时长行 | 深绿圆点 + 13sp 灰色文字（"45分钟 · 5.0km"） |
| 模式标签 | 12sp 绿色 + 浅绿背景 "🏷 陪跑绳引导模式" |

---

### 3.3.3 需求详情页（DemandDetailActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_demand_detail.xml` — ScrollView，浅灰背景 |
| 地点标题 | "📍 跑步地点"，22sp 粗体深绿 |
| 地图卡片 | 白色 CardView，180dp 高，浅绿背景 + "📍" 占位图标 |
| 地址文字 | 13sp 灰色 |
| 信息行 | 三分栏：日期 / 时长 / 距离，各有 12sp 标签 + 16sp 粗体数值 |
| 模式标签 | "🏷 陪跑绳引导模式"，14sp 绿色 |
| 导航按钮 | "🗺 导航到此处"，描边按钮 |
| 接单按钮 | "确认接单"绿色实心大按钮，56dp，18sp |
| 接单校验 | 读取 `exam_passed`，未通过→弹提示→跳转 `TrainingCampActivity` |
| 接单成功 | 写入 Room（status=accepted）→ AlertDialog 展示双方联系方式 → `finish()` |
| 联系互见 | 查询 Room `UserDao.getUsersByType("blind")` 获取视障用户电话 |

---

### 3.3.4 陪跑训练营（TrainingCampActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_training_camp.xml` — 浅灰背景 |
| 标题栏 | 深绿，"陪跑训练营" |
| 考核状态 | 白色文字条，显示当前考核状态 |
| 课程列表 | 4 个白色圆角按钮（`course_card`），每个 72dp 高，左对齐文字 |
| 课程 1 | "第1节  陪跑绳的标准长度与正确握持方式" |
| 课程 2 | "第2节  方向引导的力度控制与标准化口令" |
| 课程 3 | "第3节  转弯、避让障碍、启停的标准流程" |
| 课程 4 | "第4节  突发情况应急处理与沟通注意事项" |
| 点击 | 传递课程标题 → `CourseDetailActivity` |
| 底部按钮 | "开始考核（10道判断题）"绿色实心大按钮 → `ExamActivity` |

---

### 3.3.5 课程详情页（CourseDetailActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_course_detail.xml` — 浅灰背景 |
| 标题栏 | 深绿，动态显示课程名称 |
| 内容卡片 | 白色 CardView，圆角 14dp，阴影 2dp |
| 正文 | 16sp，行距 8dp，`#444444`，含课程要点列表 |
| 语音播报按钮 | "▶ 语音播报"绿色实心按钮，52dp（触发 TTS） |

---

### 3.3.6 在线考核页（ExamActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_exam.xml` — 浅灰背景 |
| 标题栏 | 深绿，"在线考核" |
| 信息条 | "10道判断题 · 满分100分" + "80分通过"（橙色粗体） |
| 题目容器 | 动态生成 LinearLayout |
| 题目卡片 | 白色背景，投影 2dp，16dp 间距 |
| 每题结构 | 题号标题(14sp 深绿粗体) + 题目文字(15sp `#333333`) + 水平 RadioGroup |
| 正确选项 | 10 题中正确答案：题 1-2,5-8,10 选"正确"；题 3,4,9 选"错误" |
| 提交 | 遍历所有 RadioGroup，对比正确答案计算分数 |
| 80 分以上 | Toast "恭喜！考核通过！" → 存入 SharedPreferences `exam_passed=true` |
| 80 分以下 | Toast "未通过，请重新学习后再次考核" |
| 不限重考 | 提交后直接 `finish()`，可再次进入 |

---

### 3.3.7 志愿者个人中心（VolunteerProfileActivity）

| 属性 | 说明 |
|------|------|
| 布局 | `activity_volunteer_profile.xml` — 浅灰背景 |
| 头像区 | 72dp 圆形头像（👤），"志愿者" 20sp 粗体 |
| 考核状态卡片 | 白色 CardView + 📝 图标 + 状态文字（通过=绿底绿字 / 未通过=橙底橙字）|
| 信息编辑 | 姓名 + 联系方式输入框，白底灰边框 |
| 回看训练营 | 绿色描边按钮 → `TrainingCampActivity` |
| 保存 | 绿色实心按钮 |
| 切换身份 | "切换到视障用户身份"透明按钮 |

---

# 第四部分：完整页面流转图

## 4.1 认证流程

```
启动页 ──(2秒)──→ 登录页 ──(首次)──→ 身份选择页
                      │                    ├── 视障用户 → 视障首页
                      │                    └── 志愿者 → 志愿者首页
                      └──(非首次)──→ 根据 user_type 直接进入对应首页
```

## 4.2 视障用户完整流程

```
视障首页
├── 发布需求 → 发布需求页 → 地图选点页 → 确认返回(-→发布页) → 提交(Room写入)
├── 开始跑步 → 开始跑步页(加载已接单需求信息)
│                → 跑步模式(前台Service+TTS+震动+计时)
│                    ├── 暂停/继续
│                    ├── 🆘求助 → 拨号盘
│                    └── 结束跑步 → 跑步结束页(Room保存记录) → 历史记录/返回首页
├── 历史记录 → 日期筛选 → 列表展示 → 短按/长按删除 → 统计弹窗
├── 个人中心 → 编辑信息 / 紧急联系人 / 辅助功能设置 / 切换身份
│              ├── 紧急联系人页 → 填写保存
│              └── 设置页 → 高对比度/导航语音/跑步语音/播报间隔
└── 查看需求状态(首页列表)
       └── 已接单 → AlertDialog弹窗(志愿者联系方式+TTS朗读)
```

## 4.3 志愿者完整流程

```
志愿者首页
├── 待接单 → 需求列表(搜索+筛选) → 需求详情(地图+信息+接单)
│                                          ├── 考核未通过 → 跳转训练营
│                                          └── 考核通过 → AlertDialog联系互见 → Room写入
├── 训练营 → 4节课程列表 → 课程详情(语音播报)
│                               └── 返回 → 开始考核 → 10题答题 → 提交(80分通过)
├── 已接单列表(首页) → 查看已接单记录
└── 个人中心 → 考核状态 / 编辑信息 / 回看训练营 / 切换身份
```

---

# 第五部分：数据结构

## 5.1 RunningRecord（领域模型）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| date | String | 日期时间，格式 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm` |
| durationMinutes | Int | 时长（分钟）|
| location | String | 跑步/需求地点 |
| distanceKm | Float | 距离（公里）|
| status | String | `pending`/`accepted`/`completed` |

## 5.2 UserEntity（Room 实体）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键，自增 |
| phone | String | 手机号 |
| name | String | 姓名 |
| userType | String | `blind`/`volunteer` |
| emergencyContact | String? | 紧急联系人电话（可空）|
| remoteId | Int? | 远程 API 同步 ID（可空） |

---

# 第六部分：UI 样式系统

## 6.1 颜色体系

| 颜色名 | 色值 | 用途 |
|--------|------|------|
| 主色-深绿 | `#1B5E20` | 标题栏、主按钮、标题文字 |
| 主色-暗绿 | `#0D3B0F` | statusBar |
| 强调色-绿 | `#4CAF50` | 模式标签、状态指示 |
| 警告色-橙 | `#E65100` | 紧急联系人、考核状态 |
| 背景-白 | `#FFFFFF` | 视障端页面背景 |
| 背景-浅灰 | `#F5F6FA` | 志愿者端页面背景 |
| 文字-黑 | `#333333` | 正文 |
| 文字-灰 | `#888888` / `#999999` | 副文本 |

## 6.2 Drawable 资源

| 文件 | 效果 |
|------|------|
| `btn_green_round.xml` | 绿色实心 + 16dp 圆角 |
| `btn_outline_round.xml` | 白色底 + 2.5dp 绿色描边 + 16dp 圆角 |
| `btn_light_round.xml` | 浅灰 `#F5F5F5` + 14dp 圆角 |
| `btn_outline_orange.xml` | 浅橙底 `#FFF3E0` + 2dp 橙色描边 + 16dp 圆角 |
| `edit_bg.xml` | 白底 + 1dp 灰边框 + 8dp 圆角 |
| `tag_selected.xml` | 绿色实心 + 16dp 圆角 |
| `tag_normal.xml` | 浅灰 `#F0F0F0` + 16dp 圆角 |
| `tag_status_pending.xml` | 浅橙底 `#FFF3E0` + 14dp 圆角 |
| `course_card.xml` | 白底 + 12dp 圆角 |

## 6.3 双端设计对比

| | 视障端 | 志愿者端 |
|------|------|------|
| 背景色 | 纯白 `#FFFFFF` | 浅灰 `#F5F6FA` |
| 标题字号 | 28-32sp | 18-24sp |
| 正文字号 | 20-22sp | 14-16sp |
| 按钮最小高度 | 60-80dp | 44-56dp |
| 布局风格 | 单列大按钮 | 卡片 + 标签 + 双列 |
| 阴影 | 无 | 1-3dp CardView 阴影 |
| 圆角 | 14-16dp（按钮） | 12-16dp（卡片） |

---

# 第七部分：数据层 API

## 7.1 Room DAO 方法

**RunningRecordDao：**
| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `insert(record)` | `Long` | 插入单条，返回 ID |
| `insertAll(records)` | — | 批量插入 |
| `update(record)` | — | 更新 |
| `delete(record)` | — | 删除 |
| `deleteById(id)` | — | 按 ID 删除 |
| `getAllRecords()` | `Flow<List<RunningRecordEntity>>` | 全部记录 |
| `getAllRecordsRaw()` | `List<RunningRecordEntity>` | 全部记录（suspend） |
| `getRecordById(id)` | `RunningRecordEntity?` | 按 ID 查 |
| `getRecordsByDateRange(start, end)` | `Flow<List<RunningRecordEntity>>` | 日期范围 |
| `getRecordsByStatus(status)` | `Flow<List<RunningRecordEntity>>` | 按状态 |
| `getRecordCount()` | `Int` | 总数 |

**UserDao：**
| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `insert(user)` | `Long` | 插入用户 |
| `update(user)` | — | 更新用户 |
| `getUserById(id)` | `UserEntity?` | 按 ID 查 |
| `getUserByPhone(phone)` | `UserEntity?` | 按手机号查 |
| `getUsersByType(userType)` | `List<UserEntity>` | 按身份类型查 |

## 7.2 Retrofit API

| 方法 | HTTP | 端点 |
|------|------|------|
| `getPosts()` | GET | `/posts` |
| `getUserById(userId)` | GET | `/users/{id}` |
| `createPost(request)` | POST | `/posts` |

Base URL：`https://jsonplaceholder.typicode.com/`

## 7.3 Repository 策略

| 方法 | 策略 |
|------|------|
| `getRecords(forceRefresh)` | 本地有数据→直接返回；无数据或强制→网络拉取→写入本地→返回 |
| `refreshRecordsFromNetwork()` | 强制网络刷新，返回 `Result` |
| `syncRecordToRemote(record)` | POST 到 API → 存入本地 |
| `insertLocal(record)` | 直接写入 Room |
| `deleteById(id)` | 直接删除 Room |

---

# 第八部分：单元测试

## 8.1 DAO 测试（仪器化）

[RunningRecordDaoTest.kt](app/src/androidTest/java/com/blindrunner/app/RunningRecordDaoTest.kt)

| 测试方法 | 覆盖 |
|---------|------|
| `insertAndQueryRecord` | 插入 + 查询 |
| `insertAndDeleteRecord` | 插入 + 删除 |
| `queryByStatus_returnsOnlyMatchingRecords` | 按状态筛选 |
| `queryByDateRange_returnsFilteredRecords` | 按日期范围查询 |

## 8.2 Repository 测试（JVM）

[RunningRepositoryTest.kt](app/src/test/java/com/blindrunner/app/RunningRepositoryTest.kt)

| 测试方法 | 覆盖 |
|---------|------|
| `postResponseToEntity_mapsFieldsCorrectly` | API→Entity 映射 |
| `entityToDomainModel_preservesAllFields` | Entity→Domain 映射 |
| `domainModelToEntity_preservesAllFields` | Domain→Entity 映射 |
