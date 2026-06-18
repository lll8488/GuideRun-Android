组长：袁纪欢 2023463030735  
组员：刘绍滨 2023463030720

git链接：https://github.com/lll8488/GuideRun-Android.git

---

# 助盲跑 APP — UI集成与核心流程实验报告

> 实验内容：19 页面搭建 + 列表详情核心流程 + ViewModel 数据绑定 + Room/Retrofit 数据接入 + 双端差异化设计

---

## 一、项目架构

```
┌──────────────────────────────────────────────┐
│  UI 层 (19 Activities + ViewModel)            │
├──────────────────────────────────────────────┤
│  Repository 层 (RunningRepository)           │  ← 单一数据源
├──────────────────────┬───────────────────────┤
│  Room 数据库           │  Retrofit 网络层       │
│  (Entity/Dao/Database) │  (ApiService)         │
└──────────────────────┴───────────────────────┘
```

---

## 二、页面总览

### 基础模块（3 页）

| 页面 | Activity | 数据接入 |
|------|------|------|
| 启动页 | `SplashActivity` | — |
| 登录页 | `LoginActivity` | Room：保存/查询用户 |
| 身份选择页 | `IdentitySelectActivity` | Room：写入用户身份 |

### 视障用户模块（9 页）

| 页面 | Activity | 数据接入 |
|------|------|------|
| 视障首页 | `BlindHomeActivity` | — |
| 发布需求页 | `PublishDemandActivity` | Room：insert 需求记录 |
| 地图选点页 | `MapPickerActivity` | — |
| 开始跑步页 | `StartRunningActivity` | — |
| 跑步模式页 | `RunningModeActivity` | 前台计时 + 紧急拨号 |
| 跑步结束页 | `RunningEndActivity` | Room：自动保存跑步记录 |
| 历史记录页 | `HistoryActivity` | Room：查询 + RecyclerView 列表 |
| 个人中心 | `BlindProfileActivity` | SharedPreferences |
| 紧急联系人页 | `EmergencyContactActivity` | SharedPreferences |

### 志愿者模块（7 页）

| 页面 | Activity | 数据接入 |
|------|------|------|
| 志愿者首页 | `VolunteerHomeActivity` | Room：已接单列表 |
| 需求列表页 | `DemandListActivity` | Room：待接单列表 + RecyclerView |
| 需求详情页 | `DemandDetailActivity` | Room：接单写入 + 考核状态判断 |
| 陪跑训练营 | `TrainingCampActivity` | — |
| 课程详情页 | `CourseDetailActivity` | — |
| 在线考核页 | `ExamActivity` | SharedPreferences：考核分数 |
| 个人中心 | `VolunteerProfileActivity` | 考核状态展示 + 切换身份 |

---

## 三、页面流转

```
启动页 ──(2s)→ 登录页 ──→ 身份选择 ──→ 视障/志愿者首页
    ├── 视障：发布需求 → 地图选点 → Room 保存
    ├── 视障：开始跑步 → 跑步模式 → 结束 → Room 保存 → 历史记录
    └── 志愿者：需求列表 → 详情 → 接单 → Room 更新
                └── 训练营 → 课程 → 考核
```

---

## 四、核心数据流程

### 列表 → 详情（志愿者接单）

```
DemandListActivity
  │  lifecycleScope.launch { repository.getRecords() }
  │  过滤 status != "completed"
  ▼
RecyclerView + DemandAdapter + CardView
  │  itemView.setOnClickListener { onClick(demand) }
  ▼
DemandDetailActivity
  │  Intent 传递 id/location/date/duration/distance
  │  btn_accept → repository.insertLocal(record) → Room 写入
  ▼
VolunteerHomeActivity
  │  加载 myOrders (status = "accepted")
```

### 跑步记录保存

```
RunningModeActivity (计时)
  │  btn_end → intent.putExtra("duration_seconds", seconds)
  ▼
RunningEndActivity
  │  lifecycleScope.launch { repository.insertLocal(record) }
  │  record: date=今天, durationMinutes=转换, status="completed"
  ▼
HistoryActivity
  │  RecyclerView + HistoryAdapter 展示所有记录
```

---

## 五、双端设计对比

| | 视障端 | 志愿者端 |
|------|------|------|
| 背景 | 纯白 `#FFFFFF` | 浅灰 `#F5F6FA` |
| 标题字号 | 28sp | 18-24sp |
| 按钮高度 | 64-80dp | 48-56dp |
| 布局 | 单列简洁 | 卡片式 + 标签 |
| 元素 | 实心/描边圆角按钮 | CardView + 阴影 + 状态徽章 |
| 跑步 | 全屏黑色极简界面 | — |

---

## 六、检查清单

| 要求 | 状态 |
|------|:--:|
| 列表/卡片布局展示数据 | ✅ |
| 点击列表跳转详情 | ✅ |
| ViewModel + StateFlow 绑定 | ✅ |
| 所有页面可返回 | ✅ |
| 模拟器无崩溃 | ✅ |
| Room 数据持久化 | ✅ |
| 双端差异化设计 | ✅ |
