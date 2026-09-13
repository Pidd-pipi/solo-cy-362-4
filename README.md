# 剧本杀门店运营管理系统

面向剧本杀线下门店，提供剧本库管理、场次排期、玩家组局、DM排班和会员运营等一站式门店运营工具。

## Docker Compose 快速启动

首次启动前复制环境变量文件：

```bash
cp .env.example .env
docker compose up -d
```

访问地址：

- 前端：http://localhost:28502
- 后端健康检查：http://localhost:29502/health
- API 示例：http://localhost:28502/api/overview

## 项目主要功能

- 剧本库与DM管理：录入剧本信息（名称、类型、难度、时长、人数、主持人DM要求），上传剧本封面与简介，关联专属DM主持人，支持按标签筛选与搜索。
- 场次排期与拼车位：门店设置每日开放场次时段（如14:00场、19:00场），每场显示已报名人数和剩余空位，玩家可单人报名加入拼车位或自行组满局。
- **场次报名与候补队列**：门店创建场次并设置剧本、开始时间与人数上限；玩家查看可报名场次并在线报名/取消。同一玩家在同一场次只能保留一条有效报名——名额未满直接报名，满员后按报名时间进入候补队列（顺位自动递增）；有人取消时首位候补自动转正、其余顺位自动前移，名额/状态/顺位同步更新。**报名截止同时参考场次状态与开始时间：开始时间到达后，即使门店尚未手动开场，报名与候补也立即停止（接口拒绝且 `registerable` 标记自动失效）**；已开始或已取消的场次同样禁止报名，重复报名与非法取消均有明确中文提示。页面提供场次列表、报名表单、取消入口与候补顺位展示。
- 玩家组局与角色分配：满局后DM可为玩家分配角色，支持随机分配与手动调整，系统记录每次组局玩家名单与角色分配结果。
- 会员积分与等级体系：注册会员消费积累积分，设置等级规则（如青铜/白银/黄金/钻石），不同等级享受折扣与优先拼车位权益，积分可兑换周边或抵扣费用。
- 营收与上座率分析：管理员查看每日/周/月营收报表、各剧本上座率排行、DM带本场次与评分统计，支持导出营业数据。

## 场次报名与候补模块接口

所有接口均带 `/api` 前缀（Nginx 会去掉前缀转发到后端），也支持无前缀直连。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/sessions?status=SCHEDULED` | 场次列表（可按状态过滤），含已报人数、剩余名额、候补人数 |
| GET | `/api/sessions/stats` | 场次实时指标（总览页：报名中/满员/候补人次/平均上座率） |
| GET | `/api/sessions/{id}` | 场次详情 |
| POST | `/api/sessions` | 门店创建场次（scriptId/startTime/capacity） |
| POST | `/api/sessions/{id}/start` | 门店开场（开场后禁止报名，候补自动失效） |
| POST | `/api/sessions/{id}/cancel` | 门店取消场次（全部报名/候补失效） |
| POST | `/api/sessions/{id}/register` | 玩家报名，body `{"playerId":1}`；未满直接成功，满员返回 WAITLISTED 与候补顺位 |
| DELETE | `/api/sessions/{id}/register?playerId=1` | 玩家取消；占名额者取消时首位候补自动转正 |
| GET | `/api/sessions/{id}/waitlist` | 候补队列（按顺位） |
| GET | `/api/sessions/{id}/registrations` | 场次全部有效报名 |
| GET | `/api/players` / POST `/api/players` | 玩家列表 / 注册 |
| GET | `/api/players/{id}/registrations` | 我的报名（跨场次，含候补顺位） |
| GET | `/api/scripts` | 剧本库（创建场次时选择） |

业务错误统一返回 `400/409` 与 `{"message": "中文提示"}`，例如：

- `你已成功报名该场次，请勿重复报名`
- `你已在该场次候补队列中，当前候补顺位第 2 位，请勿重复报名`
- `本场次名额已满，你已进入候补队列，当前候补顺位第 1 位`
- `该场次已开始，无法报名` / `该场次已取消，无法报名`
- `你没有该场次的有效报名，取消失败`

并发正确性：报名/取消在事务内先 `SELECT ... FOR UPDATE` 锁定场次行串行化，配合 `(session_id, player_id)` 唯一约束兜底；集成测试包含 20 人并发抢 4 个名额的场景，保证恰好 4 人成功、其余按 1..16 顺位候补、无超卖。

**时间基准（跨浏览器时区一致）**：场次开始时间以门店统一时区（默认 `Asia/Shanghai`，可用 `APP_TIMEZONE` 配置）墙钟存储，接口统一序列化为带偏移的 ISO-8601 绝对时刻（如 `2026-09-13T19:00:00+08:00`，等价于 `11:00Z`）。页面比较的是同一绝对时刻并按门店时区展示，因此玩家无论处于哪个浏览器时区，未到点不会提前禁用、到点后（即便门店未手动开场）才自动截止；报名弹窗也会在跨过开始时刻时即时禁用。

## 本地开发方式

前端：

```bash
cd frontend
npm install
npm run dev
```

后端：

```bash
cd backend
mvn spring-boot:run
```

## 技术栈

| 分层 | 技术 |
| --- | --- |
| 前端 | Vue 3 + TypeScript、Element Plus、Vite |
| 后端 | Spring Boot + Java |
| 数据库 | PostgreSQL |
| 认证 | JWT |
| 依赖 | MyBatis、Maven |

## 项目目录结构

```text
.
├── backend/              # 后端服务
├── database/             # 数据库脚本
├── frontend/             # 前端应用
├── docker-compose.yml    # 一键部署编排
├── .env.example          # 环境变量示例
└── README.md
```

## 环境变量说明

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| COMPOSE_PROJECT_NAME | Compose 项目名，避免中文目录名导致项目名为空 | ldmurdergame |
| DB_NAME | 数据库名称 | app |
| DB_USER | 数据库用户 | app |
| DB_PASSWORD | 数据库密码 | app_pwd |
| DB_ROOT_PASSWORD | 数据库 root 密码 | root_pwd |
| JWT_SECRET | JWT 签名密钥 | change_me_to_a_long_random_string |
| FRONTEND_PORT | 前端宿主机端口 | 28502 |
| BACKEND_PORT | 后端宿主机端口 | 29502 |
| DB_PORT | 数据库宿主机端口 | 5432 |

## Docker 部署说明

- 使用 `docker compose up -d` 启动，不需要额外传入 `-p`。
- `docker-compose.yml` 顶层已声明 `name: ldmurdergame`，并且 `.env` 包含 `COMPOSE_PROJECT_NAME=ldmurdergame`，可在中文目录名下启动。
- 数据库数据保存在命名卷 `db_data` 中，不依赖当前目录名。
- 前端容器由 Nginx 托管静态资源，并把 `/api/` 反向代理到 `backend:29502`。
- 若本地端口冲突，可修改 `.env` 中的 `FRONTEND_PORT`、`BACKEND_PORT`、`DB_PORT`。

常用命令：

```bash
docker compose config --quiet
docker compose ps
docker compose down
```

## License

MIT
