# TBS — 回合制策略对战游戏

> 一个基于 **Vue 3 + Spring Boot + WebSocket + MySQL** 的在线回合制对战游戏，支持玩家实时对战，已部署上线。

🎮 在线体验：**[https://tbs.gpnusjd.icu](https://tbs.gpnusjd.icu)**

### 项目演示

![选将调位](https://github.com/user-attachments/assets/1052139b-4a4d-4878-b74e-cb2bb42cec2b)

![回合制战斗](https://github.com/user-attachments/assets/b70a58c8-7fff-4645-b197-825182115cd3)

![其他部分](https://github.com/user-attachments/assets/ff72ba93-d3dd-41e0-afa0-3be31f2edcc2)

---

## 功能概览

- 🔐 **用户系统**：注册 / 登录 / Token 认证 / 账号管理（修改头像、背景、BGM）
- 👥 **好友系统**：搜索玩家 / 添加好友 / 处理好友申请
- 🏠 **对战大厅**：创建房间 / 邀请好友 / 接受邀请
- 🎲 **快速匹配**：匹配队列，自动配对随机玩家
- 🎯 **选将阶段**：从角色池选择 5 名角色，悬停查看立绘与详情
- 🔄 **调位阶段**：调整角色站位顺序，点击交换位置，实时同步
- ⚔️ **回合制战斗**：双方轮流释放主动技能，被动自动触发
- 📊 **战斗回放**：逐帧回放，支持前进/后退查看每一步
- 🏆 **胜负判定**：一方全灭判胜，200 回合未分胜负判平局
- 🎨 **动画过渡**：窗口切换黑屏过渡，详情面板滑入滑出动画
- 🔔 **实时通知**：WebSocket 推送房间状态、邀请、位置确认
- 🔄 **断线重连**：30 秒宽限期，自动重连最多 10 次

---

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端 | Vue 3 + Vite + Element Plus | Composition API，SPA 单页应用 |
| 后端 | Spring Boot 3.2 + JPA | RESTful API + WebSocket 实时通信 |
| 数据库 | MySQL 8.0 | 用户、角色、房间数据持久化 |
| 实时通信 | WebSocket（原生） | 房间同步、战斗日志推送 |
| 静态资源 | 阿里云 OSS + CDN | 动静分离，图片/音效全球加速 |
| 部署 | Ubuntu + Nginx + systemd | HTTPS + 反向代理 + 守护进程 |

---

## 项目结构

```
├── src/main/java/com/example/game/      # 后端
│   ├── controller/                       # REST API
│   ├── service/                          # 业务逻辑
│   │   ├── RoomService.java              
│   │   ├── AuthService.java              
│   │   ├── GameService.java              
│   │   └── FriendService.java
│   │   └── MatchService.java             # 快速匹配            
│   ├── entity/                           # 实体类
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── Friend.java
│   │   ├── RoomInvitation.java
│   │   └── Skill.java
│   ├── repository/                       # 数据访问层
│   │   ├── UserRepository.java
│   │   ├── RoleRepository.java
│   │   ├── FriendRepository.java
│   │   └── RoomInvitationRepository.java
│   ├── game/                             # 游戏核心
│   │   ├── Room.java                     # 房间核心逻辑
│   │   ├── StateProcessor.java           # 状态管理
│   │   ├── ActionProcessor.java          # 行动调度
│   │   ├── AttackProcessor.java          # 攻击计算
│   │   ├── DamageProcessor.java          # 伤害结算
│   │   ├── SurvivalChecker.java          # 存活判定
│   │   ├── PositionSwapProcessor.java    # 位置交换
│   │   └── processor/                    # 技能处理器
│   │       ├── SkillProcessor/           # 主动技能
│   │       │   ├── skills/               # 10 个技能实现
│   │       │   ├── SkillExecutor.java
│   │       │   └── SkillAnnotation.java
│   │       └── PassiveProcessor/         # 被动技能
│   │           ├── passives/             # 18 个被动实现
│   │           ├── AutoPassiveExecutor.java
│   │           ├── Passive.java
│   │           └── PassiveEffect.java
│   ├── websocket/                        
│   │   └── GameWebSocketHandler.java      # 房间实时通信                        
│   ├── config/                           
│   └── TBSApplication.java               
├── game/game/src/                        # 游戏前端
│   ├── components/                       # 页面组件
│   │   ├── Login.vue                     # 登录 / 注册
│   │   ├── HomePage.vue                  # 大厅 & 房间
│   │   ├── PickPhase.vue                 # 选将阶段
│   │   ├── PositionAdjust.vue            # 调位阶段
│   │   ├── BattleLogViewer.vue           # 战斗回放
│   │   ├── BattleRoleCard.vue            # 角色卡牌
│   │   ├── AccountManage.vue             # 账号管理
│   │   └── FriendsPage.vue               # 好友页面
│   ├── composables/                      # 组合式函数
│   │   ├── useGameWs.js                  # 全局 WebSocket 连接
│   │   ├── useGameState.js               # 游戏状态管理
│   │   ├── useBattleLog.js               # 战斗日志解析
│   │   ├── useClickSound.js              # 音效管理
│   │   ├── useParticleEffect.js          # 粒子特效
│   │   └── useProjectile.js              # 弹道特效
│   ├── api/                              # 接口封装
│   │   └── match.js                      # 匹配接口
│   ├── router/                           # 路由守卫
│   ├── config/                           # 配置（CDN、技能名）
│   └── utils/                            # 工具（日志解析）
├── public/button/              # 按钮 SVG 图标
├── admin-ui/src/                         # 管理后台
│   ├── components/RoleManagement.vue     # 角色管理
│   └── api/                              # 接口封装
└── pom.xml                               # Maven 配置
```

---

## 静态资源（OSS + CDN）

所有图片、音效、BGM 托管在阿里云 OSS，与业务代码分离。

| 资源类型 | 路径 |
|------|------|
| 头像 | `https://tbs-yty.oss-cn-guangzhou.aliyuncs.com/avatar/{id}.webp` |
| 角色立绘 | `https://tbs-yty.oss-cn-guangzhou.aliyuncs.com/rolebackground/{id}.webp` |
| 背景 | `https://tbs-yty.oss-cn-guangzhou.aliyuncs.com/background/{name}` |
| 点击音效 | `https://tbs-yty.oss-cn-guangzhou.aliyuncs.com/click/click.mp3` |
| BGM | `https://tbs-yty.oss-cn-guangzhou.aliyuncs.com/Homepage_bgm/{id}.mp3` |

---

## 快速开始

### 环境要求

- JDK 21+ / Node.js 18+ / MySQL 8.0+ / Maven 3.8+

### 1. 数据库初始化

```sql
CREATE DATABASE TBS DEFAULT CHARACTER SET utf8mb4;
CREATE USER 'TBS'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON TBS.* TO 'TBS'@'localhost';
FLUSH PRIVILEGES;
```

### 2. 启动后端

```bash
cd Game
mvn clean package -DskipTests
java -jar target/team-battle-simulator-1.0.0.jar
```

### 3. 启动游戏前端

```bash
cd game/game
npm install
npm run dev
# 访问 http://localhost:5173
```

### 4. 启动管理后台

```bash
cd admin-ui
npm install
npm run dev
# 访问 http://localhost:5174
```

---

## 部署

项目通过 **Nginx 反向代理 + SSL** 部署在 Ubuntu 云服务器上，三个服务由 systemd 守护。

| 服务 | 端口 | 访问方式 |
|------|------|------|
| 后端 API | 30083 | Nginx 代理 `/api/` → `127.0.0.1:30083` |
| 游戏前端 | 30085 | Nginx 代理 `/` → `127.0.0.1:30085` |
| WebSocket | 30083 | Nginx 代理 `/ws/` → `127.0.0.1:30083`（Upgrade） |

```bash
# 后端部署
mvn clean package -DskipTests
sudo systemctl restart tbs

# 前端部署
npm run build-only
sudo systemctl restart tbs-game

# 重载 Nginx
sudo nginx -t && sudo nginx -s reload

# 查看日志
sudo journalctl -u tbs -f
```

---

## 核心技术难点

### 1. JAR 包部署后技能/被动无法加载

本地开发用 `File.listFiles()` 扫描类路径，打包成 JAR 后该方法返回空。
通过分析 JAR 包内部结构，改用 `JarURLConnection` 遍历 JAR 条目，同时兼容本地 `file:` 和 JAR `jar:` 两种协议。

### 2. 被动技能死循环导致 StackOverflow

两个角色拥有相同被动（如 `Stealing_Heaven`）互相复制，触发无限递归。
通过日志定位到 `StackOverflowError`，在自动被动执行器中加入递归深度限制和自身类型跳过逻辑。

### 3. WebSocket 全局共享与断线重连

原先每个阶段（大厅、选将、调位、战斗）各自建立一条 WebSocket 连接，组件切换时销毁重建，浪费 TCP 握手开销。
重构为全局 composable `useGameWs`，四个组件共用同一条连接，各阶段独立注册 Handler。同时实现断线重连机制：前端自动重试（最多 10 次），后端 30 秒宽限期延迟清理房间，支持玩家短暂断网后恢复对局。

### 4. HTTPS + 域名 + Nginx 反向代理

将三个服务统一到 443 端口，通过 Nginx 根据路径分发（`/api/`、`/ws/`、`/`），配置 SSL 证书实现全站 HTTPS，解决 Mixed Content 和 CORS 跨域问题。

---

## 未来优化

- [ ] 引入 Redis 缓存角色数据
- [ ] 安全问题，添加验证防止玩家通过控制台修改游戏状态
- [ ] 添加单元测试与集成测试
- [ ] Docker Compose 一键部署
- [ ] 前端迁移到 TypeScript
- [ ] 战斗日志持久化，支持历史对局回放
- [ ] 游戏表现优化，如动画，更多音效，BGM
- [ ] 沉浸式体验，角色语音，界面设计


---

## 关于

这是我的第一个全栈项目，独立完成从需求分析、架构设计、编码实现到云服务器部署的全流程。通过这个项目，我深入理解了前后端协作、实时通信、动态类加载等核心概念，也积累了排查线上问题的实战经验。
