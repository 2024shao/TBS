# TBS — 回合制策略对战游戏

> 一个基于 **Vue 3 + Spring Boot + WebSocket + MySQL** 的在线回合制对战游戏，支持玩家实时对战，已部署上线。

🎮 在线体验：**[https://tbs.gpnusjd.icu](https://tbs.gpnusjd.icu)**

### 项目演示

![选将调位](https://tbs-yty.oss-cn-guangzhou.aliyuncs.com/demo/pick.gif)

![回合制战斗](https://tbs-yty.oss-cn-guangzhou.aliyuncs.com/demo/battle.gif)

![其他部分](https://tbs-yty.oss-cn-guangzhou.aliyuncs.com/demo/other.gif)

---

## 功能概览

- 🔐 **用户系统**：注册 / 登录 / Token 认证 / 账号管理（修改头像、背景、BGM）
- 👥 **好友系统**：搜索玩家 / 添加好友 / 处理好友申请
- 🏠 **对战大厅**：创建房间 / 邀请好友 / 接受邀请
- 🎯 **选将阶段**：从角色池选择 5 名角色，悬停查看立绘与详情
- 🔄 **调位阶段**：调整角色站位顺序，点击交换位置，实时同步
- ⚔️ **回合制战斗**：双方轮流释放主动技能，被动自动触发
- 📊 **战斗回放**：逐帧回放，支持前进/后退查看每一步
- 🏆 **胜负判定**：一方全灭判胜，200 回合未分胜负判平局
- 🎨 **动画过渡**：窗口切换黑屏过渡，详情面板滑入滑出动画
- 🔔 **实时通知**：WebSocket 推送房间状态、邀请、位置确认

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
│   ├── controller/                       # REST API 控制器
│   ├── service/                          # 业务逻辑
│   │   ├── Room.java                     # 房间核心逻辑
│   │   ├── AuthService.java              # 用户认证
│   │   └── processor/                    # 技能 & 被动处理器
│   ├── websocket/                        # WebSocket 处理
│   └── Repository/                       # 数据访问层
├── game/game/src/                        # 游戏前端
│   ├── components/                       # 页面组件
│   │   ├── Login.vue                     # 登录 / 注册
│   │   ├── HomePage.vue                  # 大厅 & 房间
│   │   ├── PickPhase.vue                 # 选将阶段
│   │   ├── PositionAdjust.vue            # 调位阶段
│   │   └── BattleLogViewer.vue           # 战斗回放
│   ├── composables/                      # 组合式函数
│   │   ├── useGameState.js               # 游戏状态管理
│   │   ├── useBattleLog.js               # 战斗日志解析
│   │   └── useClickSound.js              # 音效管理
│   ├── api/                              # 接口封装
│   └── router/                           # 路由配置
├── admin-ui/src/                         # 管理后台前端
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

### 3. WebSocket 实时状态同步

选将、调位、战斗三个阶段需要双方实时同步。通过 `gameState.phase` 驱动组件切换，各阶段独立维护 WebSocket 连接，保证状态一致性。

### 4. HTTPS + 域名 + Nginx 反向代理

将三个服务统一到 443 端口，通过 Nginx 根据路径分发（`/api/`、`/ws/`、`/`），配置 SSL 证书实现全站 HTTPS，解决 Mixed Content 和 CORS 跨域问题。

---

## 未来优化

- [ ] 引入 Redis 缓存角色数据
- [ ] 添加单元测试与集成测试
- [ ] Docker Compose 一键部署
- [ ] 前端迁移到 TypeScript
- [ ] 战斗日志持久化，支持历史对局回放
- [ ] 匹配队列，自动匹配对手
- [ ] 游戏表现优化，如动画，更多音效，BGM
- [ ] 沉浸式体验，角色语音，界面设计


---

## 关于

这是我的第一个全栈项目，独立完成从需求分析、架构设计、编码实现到云服务器部署的全流程。通过这个项目，我深入理解了前后端协作、实时通信、动态类加载等核心概念，也积累了排查线上问题的实战经验。