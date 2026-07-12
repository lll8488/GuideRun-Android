# 助盲跑 App 架构说明 (ARCHITECTURE.md)

## 一、整体架构：MVVM + Repository

```
┌──────────────────────────────────────────────────────────────────┐
│                       Presentation Layer                         │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │  Activity   │  │   Fragment    │  │  ViewModel   │             │
│  │ (UI 事件)   │  │  (视障首页?)  │  │ (StateFlow)  │             │
│  └─────┬──────┘  └──────┬───────┘  └──────┬───────┘             │
│        │                │                  │                      │
│        └────────────────┴──────────────────┘                      │
│                         │ observe / collect                      │
├─────────────────────────┼────────────────────────────────────────┤
│                         ▼                                         │
│                       Domain Layer                                │
│  ┌──────────────────────────────────────────┐                    │
│  │         RunningRecord / Demand            │  纯 Kotlin 数据类   │
│  │         (业务模型，不依赖框架)               │                    │
│  └──────────────────────────────────────────┘                    │
├──────────────────────────────────────────────────────────────────┤
│                        Data Layer                                 │
│  ┌──────────────────────────────────────────┐                    │
│  │          RunningRepository                │  数据仓库           │
│  │  (封装 Room + Retrofit，对外暴露统一 API)    │  (单一数据源)       │
│  └──────────┬───────────────┬───────────────┘                    │
│             │               │                                     │
│      ┌──────▼──────┐  ┌─────▼──────┐                             │
│      │  Room (本地)  │  │Retrofit(远程)│                           │
│      │  SQLite      │  │ HTTP API   │                             │
│      └─────────────┘  └────────────┘                              │
└──────────────────────────────────────────────────────────────────┘
```

### 为什么选 MVVM 而不是 MVP？

|        | MVVM                          | MVP                             |
|--------|-------------------------------|----------------------------------|
| View   | Activity 通过 collect 订阅      | Activity 实现 ViewInterface      |
| 中间层  | ViewModel (生命周期独立于Activity) | Presenter (持有 View 引用)        |
| 内存泄漏 | ViewModel 不持有 View 引用      | Presenter 持有 View，易泄漏       |
| 数据绑定 | StateFlow / LiveData           | 手动调用 View.update()           |

MVVM 的核心优势：**ViewModel 的生命周期比 Activity 长**，转屏时数据不丢失。

---

## 二、数据流

```
用户操作 (Click/Input)
       │
       ▼
  Activity.onClick()
       │
       ├─→ ViewModel.loadData()       ← 有 ViewModel 时
       │        │
       │        └─→ Repository.getRecords()
       │
       └─→ Activity 直接调 Repository  ← MVP 阶段简化（多数页面）
                │
                ├─→ Room DAO (本地命中)
                │        └─→ StateFlow emit → UI 更新
                │
                └─→ Retrofit (本地未命中)
                         └─→ 写入 Room → StateFlow emit → UI 更新
```

**当前 MVP 阶段：** 多数 Activity 直接通过 `lifecycleScope.launch` 调用 Repository，
跳过了 ViewModel 层——代码更简洁，功能无损失。

**接入 ViewModel 的页面：** `MainActivity`（含 `MainViewModel`，演示用）、
`BlindHomeActivity`（含 `BlindHomeViewModel`，管理需求列表 StateFlow）

---

## 三、核心类职责说明

### 3.1 入口与生命周期

| 类 | 职责 | 关键方法 |
|----|------|---------|
| `BlindRunnerApp` | Application，全局初始化 | `onCreate()`: 初始化 Room/TTS/Retrofit/SharedPreferences |
| `SplashActivity` | 2秒启动页 | `postDelayed → LoginActivity` |
| `BaseActivity` | 所有 Activity 基类 | `setupBackButton()`, `navigateTo()`, `showConfirmDialog()`, `handleError()` |

### 3.2 认证模块 (`ui/auth/`)

| 类 | 职责 |
|----|------|
| `LoginActivity` | 手机号+验证码登录，60秒倒计时，验证码校验 |
| `RegisterActivity` | 手机号+验证码注册，验证通过后跳转身份选择 |
| `IdentitySelectActivity` | 首次登录选「视障用户」或「陪跑志愿者」|

**登录路由：**
```
输入手机号 → 获取验证码 → 输入验证码 → navigateToHome(phone)
  ├─ 新用户（无身份信息）→ IdentitySelectActivity → Blind/VolunteerHome
  └─ 老用户 → 直接进对应首页
```

### 3.3 视障端 (`ui/blind/`)

| 类 | 职责 | 技术要点 |
|----|------|---------|
| `BlindHomeActivity` | 首页：发布需求/开始跑步/历史记录/设置 + 我的需求四状态列表 | RecyclerView + ListAdapter + DiffUtil |
| `PublishDemandActivity` | 发布陪跑需求：日期/时间/地点(文字+地图选点)/距离/确认弹窗 | DatePickerDialog + TimePickerDialog + ActivityResultLauncher + AlertDialog二次确认 |
| `MapPickerActivity` | 地图选点：高德地图+十字准星+逆地理编码+快速定位 | AMap + Geocoder + Battery_Saving 快速定位 |
| `StartRunningActivity` | 跑步准备页 + 确认弹窗 | 读取关联需求信息 |
| `RunningModeActivity` | 跑步中：计时+地图轨迹+暂停/结束/求助+30秒播报 | Foreground Service + dispatchKeyEvent(音量键) + FLAG_KEEP_SCREEN_ON |
| `RunningEndActivity` | 结果展示 + 保存记录 + 评分志愿者 + 成就检查 | withTransaction(原子写入) + UserDao.incrementRunStats |
| `HistoryActivity` | 历史记录列表+日期筛选+长按删除+统计入口 | SwipeRefreshLayout + DatePickerDialog |
| `StatisticsActivity` | 统计：总次数/总时长/平均时长/总距离 + GraphView 折线图 | GraphView LineGraphSeries（≤30数据点） |
| `HistoryDetailActivity` | 跑步轨迹回放 + GPS坐标JSON解析 | AMap Polyline + LatLngBounds 自适应缩放 |
| `BlindProfileActivity` | 个人中心：姓名/联系方式编辑 + 紧急联系人入口 + 身份切换 + 退出登录 | per-user SharedPreferences 隔离 |
| `EmergencyContactActivity` | 紧急联系人管理（最多3位） | SharedPreferences 分隔符存储 |
| `SettingsActivity` | 高对比度主题/语音开关/播报间隔/隐私政策 | Switch + ThemeHelper |

### 3.4 志愿者端 (`ui/volunteer/`)

| 类 | 职责 | 技术要点 |
|----|------|---------|
| `VolunteerHomeActivity` | 首页：待接单/训练营/排行榜/我的接单/已完成 | 双 RecyclerView(接单+已完成) |
| `DemandListActivity` | 需求列表：搜索+6标签筛选+距离排序+AMap定位 | TextWatcher + Battery_Saving定位 + Distance计算 |
| `DemandDetailActivity` | 需求详情：大字地址+静态地图+导航+确认接单+考核检查 | 高德静态地图API + ACTION_VIEW导航 + 考核通过检查 |
| `TrainingCampActivity` | 训练营首页：4节课程入口+考核状态 | 4个课程按钮 |
| `CourseDetailActivity` | 课程详情：PRD规定的完整教案+TTS全量播报 | 4节×约300字课程内容 |
| `ExamActivity` | 在线考核：10道判断题+80分通过+SQLite双写存储 | RadioGroup动态创建 + AppPrefs + UserDao.updateExamResult |
| `VolunteerProfileActivity` | 个人中心：考核状态卡片+信息编辑+身份切换+退出登录 | per-user数据隔离 |
| `LeaderboardActivity` | 志愿者排行榜：🥇🥈🥉排名+次数+评分 | ListAdapter + DiffUtil |

### 3.5 服务层 (`service/`)

| 类 | 职责 | 关键设计 |
|----|------|---------|
| `RunningService` | 跑步前台Service | startForeground + IMPORTANCE_HIGH + START_STICKY + GPS跟踪 |
| `EmergencyAccessibilityService` | 全局音量键监听 | AccessibilityService + onKeyEvent + 3秒计时 + ACTION_DIAL |

### 3.6 工具类 (`util/`)

| 类 | 职责 | 关键设计 |
|----|------|---------|
| `TtsHelper` | 全局语音合成 | AudioAttributes(USAGE_ASSISTANCE_ACCESSIBILITY) + AudioFocusRequest + 小米重试 |
| `AppPrefs` | SharedPreferences 管理 | currentUserPhone、userType、examPassed、voiceInterval 等 |
| `NotificationHelper` | 系统通知管理 | 需求被接单通知（HIGH）+ 跑步通知（LOW → HIGH） |
| `ThemeHelper` | 高对比度主题切换 | 读取 AppPrefs.highContrastMode → setTheme |
| `MapConfig` | 高德 API Key 统一读取 | 从 Manifest meta-data 读取，避免硬编码 |
| `Logger` | 日志工具 | Release 自动关闭 Debug 日志 |

### 3.7 数据层 (`data/`)

| 层次 | 类 | 职责 |
|------|-----|------|
| Entity | `RunningRecordEntity`, `UserEntity` | Room 数据库表定义 |
| DAO | `RunningRecordDao`, `UserDao` | SQL 查询接口（含 @Query 参数化查询） |
| Database | `AppDatabase` | RoomDatabase 实例 + Migration(1→7) |
| Remote | `RetrofitClient` | OkHttp + Retrofit 配置（baseUrl 可切换） |
| Remote | `ApiService` | 12个 RESTful 端点 + 旧 JSONPlaceholder 兼容 |
| Repository | `RunningRepository` | 本地优先策略，封装 Room + Retrofit |
| Domain | `RunningRecord`, `Demand` | 纯 Kotlin 数据类，与 Room/Retrofit 解耦 |

---

## 四、数据库迁移历史

| 版本 | 新增字段 | SQL |
|------|---------|-----|
| 1→2 | `ownerPhone` | `ALTER TABLE running_records ADD COLUMN ownerPhone TEXT NOT NULL DEFAULT ''` |
| 2→3 | `volunteerPhone`, `volunteerNote` | 2 × ALTER TABLE |
| 3→4 | `blindConfirmed` | `ALTER TABLE running_records ADD COLUMN blindConfirmed INTEGER NOT NULL DEFAULT 0` |
| 4→5 | `demandId`, `lat`, `lng` | 3 × ALTER TABLE |
| 5→6 | `trackJson` | `ALTER TABLE running_records ADD COLUMN trackJson TEXT NOT NULL DEFAULT ''` |
| 6→7 | `examPassed`, `examScore` | `ALTER TABLE users ADD COLUMN examPassed INTEGER ... DEFAULT 0` + `ADD COLUMN examScore INTEGER ... DEFAULT 0` |

---

## 五、一些设计选择

### 为什么用 XML 而不是 Jetpack Compose？

主要是高德地图 SDK 的 MapView 只支持 XML，而且当时团队对 XML 更熟悉，开发效率更高。Compose 后续可以考虑重构。

### 为什么用 Room 而不是 DataStore？

Room 管结构化数据（跑步记录、用户信息），需要排序筛选。DataStore 本质上就是 SharedPreferences 升级版，管键值对（设置项），各司其职。

### 验证码为什么是模拟的？

没接入短信服务（阿里云/腾讯云 SMS 要钱），直接随机生成 6 位数字存本地，输入匹配就通过。上线的话换成真实短信接口就行。

---

## 六、权限声明与用途

| 权限 | 用途 | 申请时机 |
|------|------|---------|
| `INTERNET` | 网络通信 | 静默声明 |
| `ACCESS_FINE_LOCATION` | 地图选点定位 | 仅进入 MapPickerActivity 时申请 |
| `ACCESS_COARSE_LOCATION` | 网络定位（弱GPS环境） | 同上 |
| `FOREGROUND_SERVICE` | 跑步计时保活 | 静默声明 |
| `FOREGROUND_SERVICE_LOCATION` | 跑步中 GPS 轨迹 | 静默声明 |
| `POST_NOTIFICATIONS` | 通知栏常驻（Android 13+） | 系统自动弹出 |
| `VIBRATE` | 紧急求助/操作反馈振动 | 静默声明（normal 权限） |
| `ACCESS_NETWORK_STATE` | 检测网络状态 | 静默声明 |
