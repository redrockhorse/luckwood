# Luckwood - 运气分析助手

一款集成足球比赛分析和彩票号码预测的 Android 应用，基于 Jetpack Compose 和 Material Design 3 构建。

## 📱 功能特性

### ⚽ 足球比赛分析
- **智能查询**：支持按日期时间范围查询未来比赛
- **多联赛支持**：涵盖英超、西甲、德甲、意甲、法甲等主流联赛
- **AI 推荐**：基于历史数据分析，提供比赛结果预测
- **数据分析**：
  - 主客队历史战绩统计
  - 胜平负概率分析
  - 期望回报计算
  - 数据质量评估
- **灵活筛选**：
  - 多选联赛过滤
  - 时间升序/降序排序
- **详细信息**：
  - 历史对阵记录
  - 同对手交锋标识
  - 赔率信息展示

### 🎲 彩票号码预测
- **双色球预测**：基于最近一期号码生成 5 组预测
- **大乐透预测**：智能生成多组候选号码
- **算法优化**：采用特定随机算法确保号码分布合理

## 🛠️ 技术栈

### 核心框架
- **Kotlin** 1.8.10
- **Android Gradle Plugin** 8.3.0
- **Jetpack Compose** - 现代化 UI 框架
- **Material Design 3** - Google 最新设计规范

### 主要依赖
- **Navigation Compose** 2.7.5 - 导航管理
- **Retrofit** 2.9.0 - 网络请求
- **Gson** 2.10.1 - JSON 解析
- **OkHttp** 4.11.0 - HTTP 客户端
- **Coroutines** 1.7.3 - 异步编程

### 架构特点
- MVVM 架构模式
- 声明式 UI（Jetpack Compose）
- 响应式数据流
- RESTful API 集成

## 📦 项目结构

```
app/src/main/java/com/example/luckwood/
├── MainActivity.kt           # 主活动，包含所有 UI 组件
├── LotteryPredictor.kt      # 彩票预测算法
├── ApiService.kt            # Retrofit API 服务定义
├── ApiModels.kt             # 数据模型类
├── MatchDataManager.kt      # 比赛数据管理器
└── ui/theme/                # Material Design 主题配置
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## 🚀 快速开始

### 环境要求
- Android Studio Electric Eel (2022.1.1) 或更高版本
- JDK 8 或更高版本
- Android SDK 24+ (Android 7.0+)
- Gradle 8.4

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd luckwood
   ```

2. **打开项目**
   - 使用 Android Studio 打开项目目录
   - 等待 Gradle 同步完成

3. **运行应用**
   - 连接 Android 设备或启动模拟器
   - 点击 Run 按钮或使用快捷键 `Shift + F10`

## 📖 使用指南

### 足球比赛分析

#### 1. 查询比赛
- 打开应用，默认进入"足球"标签页
- 点击"选择日期和时间"按钮
  - 第一步：在日历中选择日期，点击"下一步"
  - 第二步：滚动选择小时（0-23），点击"确定"
- 分别设置开始时间和结束时间
- 点击"查询比赛"按钮

#### 2. 浏览列表
- 查看所有符合时间范围的比赛
- 使用顶部的**联赛筛选**卡片：
  - 点击联赛标签选择/取消
  - 使用"全选"或"清空"快速操作
- 点击右上角的排序按钮（↑/↓）切换时间排序

#### 3. 查看详情
- 点击任意比赛进入详情页
- 详情页包含：
  - **基本信息**：时间、对阵、赔率
  - **AI 推荐**：推荐结果、概率、期望回报
  - **数据质量**：可信度评估
  - **球队分析**：主客队历史战绩和概率统计
  - **历史比赛**：相关历史对阵记录（可展开/折叠）

### 彩票预测

#### 1. 切换功能
- 点击底部导航栏的"彩票"图标

#### 2. 双色球预测
- 选择"双色球"单选按钮
- 输入最近一期的 6 个红球号码
- 点击"生成预测号码"
- 查看 5 组预测结果

#### 3. 大乐透预测
- 选择"大乐透"单选按钮
- 输入最近一期的 5 个号码
- 点击"生成预测号码"
- 查看多组预测结果

## 🎨 界面设计

### 设计原则
- **Material Design 3**：遵循 Google 最新设计规范
- **响应式布局**：适配不同屏幕尺寸
- **色彩语义化**：使用颜色传达信息重要性
- **信息层次**：清晰的视觉层次结构

### 主要界面
1. **足球查询页**：日期时间选择器
2. **比赛列表页**：过滤、排序、浏览比赛
3. **比赛详情页**：全面的数据分析展示
4. **彩票预测页**：号码输入和预测结果

## ⚙️ 配置说明

### 网络安全
应用允许 HTTP 明文传输（用于访问足球分析 API）：
```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

### 权限要求
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 📝 开发说明

### 构建命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 运行测试
./gradlew test

# 清理项目
./gradlew clean
```

### 代码风格
- 遵循 Kotlin 官方代码规范
- 使用 Compose 声明式 UI 范式
- 函数式编程优先

### 主要组件

#### UI 组件
- `MainScreen`：主界面容器，包含导航
- `FootballScreen`：足球查询界面
- `FootballMatchListScreen`：比赛列表界面
- `FootballDetailScreen`：比赛详情界面
- `LotteryScreen`：彩票预测界面

#### 数据组件
- `RetrofitClient`：API 客户端单例
- `MatchDataManager`：比赛数据管理器
- `LotteryPredictor`：彩票预测算法

## 🔮 未来规划

- [ ] 添加用户收藏功能
- [ ] 支持比赛结果推送通知
- [ ] 增加更多彩票类型
- [ ] 数据本地缓存
- [ ] 支持多语言
- [ ] 深色模式优化
- [ ] 数据可视化图表

## 📄 许可证

本项目仅供学习和研究使用。

## 👥 贡献

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有问题或建议，请通过 Issue 反馈。

---

**注意**：本应用仅供娱乐和学习用途，预测结果仅供参考，不构成任何投资建议。

