# Cup Flow Coffee POS — 产品与 Sprint 文档

本目录保存 Cup Flow Coffee POS 第一阶段产品基线和各 Sprint 执行文档。

| 文档 | 说明 |
| --- | --- |
| [Sprint 0 PRD](./sprint0/PRD.md) | 第一阶段产品目标、业务流程、功能需求和成功指标 |
| [Sprint 0 User Stories](./sprint0/USER_STORIES.md) | 第一阶段核心用户故事、优先级及验收标准 |
| [Sprint 0 Scope](./sprint0/SCOPE.md) | 第一阶段范围、后续边界和迭代建议 |
| [Sprint 1 PRD](./sprint1/PRD.md) | Figma UI 设计与基础工程搭建的目标、需求、交付和 DoD |
| [Sprint 1 User Stories](./sprint1/USER_STORIES.md) | Sprint 1 设计、工程、质量与验收故事 |
| [Sprint 1 Scope](./sprint1/SCOPE.md) | Sprint 1 范围内、范围外、交付边界与变更规则 |
| [Sprint 1 Test Strategy](./sprint1/TEST_STRATEGY.md) | 测试分层、自动化重点、需求追踪与基础链路验收清单 |
| [Sprint 2 PRD](./sprint2/PRD.md) | 登录、会话、角色权限、安全基线与评审待决事项 |
| [Sprint 2 Features](./sprint2/FEATURES.md) | Sprint 2 用户可感知能力、边界、依赖与完成条件 |
| [Sprint 2 User Stories](./sprint2/USER_STORIES.md) | 登录体系用户故事、验收标准、依赖与需求覆盖矩阵 |
| [Sprint 2 Tasks](./sprint2/TASKS.md) | 已批准开发 Task、依赖批次、追踪矩阵和 Ready/Done 标准 |
| [Sprint 2 API Contract](./sprint2/API_CONTRACT.md) | 登录、当前身份、退出、Cookie、CSRF 与错误码契约 |
| [Sprint 2 Auth Flow](./sprint2/AUTH_FLOW.md) | 登录、会话恢复、角色授权、退出与会话生命周期流程图 |
| [Sprint 2 Auth Sequence](./sprint2/AUTH_SEQUENCE.md) | 登录、CSRF、限流、会话、授权与退出的参与方交互时序图 |
| [Sprint 2 Auth UI/UX](./sprint2/AUTH_UIUX/README.md) | 完整登录 UI 画板、状态覆盖审查与 Figma 交接说明 |
| [Sprint 2 Test Strategy](./sprint2/TEST_STRATEGY.md) | 测试分层、稳定用例编号及 Story—Task—Test 追踪 |

## 第一阶段范围

第一阶段只建设以下六项核心能力：

1. 登录。
2. POS 点单。
3. 收银。
4. 订单管理。
5. 商品后台。
6. 简单经营看板。

当前假设为单品牌、单门店、人民币、简体中文的在线 Web POS。前端采用 React，后端采用 Java；具体框架、数据库和部署方式在技术方案中确定。

第一阶段 Sprint 0 基线版本为 `v0.2`；Sprint 1 文档初稿版本为 `v0.1`。需求使用统一编号关联 PRD、用户故事、开发任务和测试用例；评审通过后方可作为实施基线。

Sprint 2 登录体系需求、接口和测试基线已于 2026-08-24 升级为 v1.0；开发按 `TASKS.md` 的依赖
和 Ready 状态执行，需求变化先更新文档与追踪关系。
