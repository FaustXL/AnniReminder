<img width="1080" height="2340" alt="08-settings" src="https://github.com/user-attachments/assets/59a04a2d-3d99-4ae2-8510-fe7d73884d0b" /># 纪念日提醒 (AnniReminder)

一个把 **生日 / 纪念日** 批量写入 **系统日历** 的安卓应用。UI 采用 Sundance Film Festival 2023 海报风格（橙 #FF4B00 / 黑 / 灰 / 大圆角卡片 / 等宽字体排版）。

<img width="270" height="585" alt="11-release-on-emulator" src="https://github.com/user-attachments/assets/088aaf55-8aee-4d0a-ae2b-23ddb61781c6" />
<img width="270" height="585" alt="09-delete-button-fixed" src="https://github.com/user-attachments/assets/19990839-57e5-4823-8eb3-1643c7839f61" />
<img width="270" height="585" alt="10-stepper-tip" src="https://github.com/user-attachments/assets/7472fa48-e5d2-436e-bbaa-518e980d91ea" />
<img width="270" height="585" alt="08-settings" src="https://github.com/user-attachments/assets/b9f65656-edfe-476b-9207-80a3ac10c089" />

## 功能

- ✅ **多人管理**：每人一张卡片，首页一览所有人的日子与倒计时（"还有 N 天"）
- ✅ **农历 / 公历**：每条日子可独立选择农历或公历
  - 农历：自定义滚轮选择 月（含**闰月**）/ 日，使用系统内置 ICU `ChineseCalendar` 换算，无第三方依赖
  - 公历：Material 日期选择器
- ✅ **一人多条日子**：生日、纪念日、恋爱纪念日……可任意添加多条
- ✅ **可调重复年数**：连续写入 1–30 年（默认 10 年）
- ✅ **提前 N 天提醒**（0–30 天，默认 3 天，0 = 不提前）
- ✅ **每条日子写两条日程**：
  - 当天 09:00（设备本地时间）：`🎂 张三 · 生日`
  - 提前 N 天 09:00：`⏰ 还有N天：张三 · 生日`
  - 均带 09:00 准点响铃提醒
- ✅ **增删改全同步**：编辑 = 自动删旧日程再重建；删除人物/日子 = 同步删除日历日程
- ✅ **目标日历可选**：设置页选择写入哪个日历账号；没有任何可写日历时自动创建本地日历「纪念日」
- ✅ 数据本地保存（应用私有目录 JSON），无需网络、无第三方依赖

## 已验证（Android 16 模拟器）

| 场景 | 结果 |
| --- | --- |
| 农历九月初九 → 2026-10-18（2026 年重阳节 ✓），写入 10 年 | 20 条日程 |
| 公历 8/20 恋爱纪念日，提前 7 天，写 3 年 | 6 条日程 |
| 编辑 Mom 改为 12 年 | 旧 20 条删除，重建 24 条 |
| 删除 Dad | 其 6 条日程同步从系统日历清除 |
| 提醒 | 26 条 Reminders 全部 method=ALERT |

验证截图见 `docs/`。

## 构建

要求：JDK 17+、Android SDK（platform 35，`local.properties` 里配置 `sdk.dir`）。

```bat
gradlew.bat :app:assembleDebug
```

> 本项目 `settings.gradle.kts` 配置了阿里云 Maven 镜像（国内加速），镜像不可用时自动回退到 google() / mavenCentral()，海外用户可按需删除镜像行。

产物：`app/build/outputs/apk/debug/app-debug.apk`（minSdk 26 / targetSdk 35）

## 安装后注意

1. 首次启动会请求 **日历读写权限**，必须允许
2. 提醒铃响由 **系统日历应用** 负责 —— 请保持日历应用的通知权限开启（MIUI/鸿蒙等 ROM 需在设置里允许其自启动与通知）
3. 如果设备上一个日历账号都没有，App 会自动创建本地日历「纪念日」（可在系统日历 App 中看到并取消勾选）

## 代码结构

```
app/src/main/java/com/faust/annireminder/
├── MainActivity.kt          # 入口 + 权限 + 导航 + Snackbar
├── model/Models.kt          # Person / DateEntry / 农历中文显示
├── lunar/Lunar.kt           # DateExpander：农历↔公历换算与年度展开
├── data/Store.kt            # JSON 文件持久化
├── calendar/CalendarHelper.kt  # 系统日历 Provider：查询/建日历/写/删日程
└── ui/
    ├── Theme.kt             # Sundance 配色
    ├── Components.kt        # WheelPicker 滚轮 / Stepper / 分段开关
    ├── HomeScreen.kt        # 首页（人物卡 + 倒计时）
    ├── EditScreen.kt        # 编辑页（多日子 + 滚轮 + 步进器）
    ├── SettingsScreen.kt    # 设置页（目标日历）
    └── AppViewModel.kt      # 状态 + 业务逻辑
```

## 已知边界

- 农历生日逢闰月年份：选择「闰X月」时，无闰月的年份自动过平月 X 月；X 月无 30 日的年份钳制为 29 日
- 公历 2/29 生日在平年顺延为 3 月 1 日
- 日程为 09:00 定时日程（非全天），保证各日历 App 响铃时间一致；如需全天样式可后续扩展
