# Warm-Flow 集成坑位速查与引擎集成范式

> 适用：一切接入流程引擎的模块（ct-command 指令流、规则审批流及后续审批类功能）。
> 来源：`samples/flow-sample`（Warm-Flow 1.8.9 单引擎干净参考实现）与 `samples/sandbox-sample`（双引擎对比、并发验证）源码级验证结论。
> 配套：《情指行一体化总体设计方案》§1.3（三引擎分工）/ §6-A7、A10（双引擎 POC 结论与定标）、《ct-flow双引擎POC执行手册》（达梦侧验证规程）、《情指行设计文档差距分析与补充清单》G10/G14。
> 版本：V1.0（2026-09-15）。

---

## 0. 选型结论（已定标，勿重开议题）

- **生产引擎定标 Warm-Flow 1.8.9**（Apache-2.0 无附加条款）。FlowLong 因双协议附加条款与本项目"乙方交付源码"字面冲突（违反即自动升级 AGPL-3.0 等），**不用**；其动态办理人优势可用转办/候选人等价实现。
- FlowLong 并发防重优于 Warm-Flow 的**源码级根因**（POC 实测）：FlowLong `flw_his_task` 与 `flw_task` 同主键，完成 = INSERT 历史 + DELETE 待办，并发第二个 INSERT 主键冲突令事务回滚；Warm-Flow `flow_his_task` 主键由 idFill 独立生成雪花 ID（插入必成功）且删除返回值未校验，**两道口都不设防** → 必须用业务层乐观锁兜底（§3）。
- LiteFlow **生产不引入**（差距分析 G14 裁决：用轻量自研 chain）。样例中 LiteFlow 仅用于验证"编排层/规则层"分工模式，见《Drools实战坑位与复用模板》§8。

## 1. Warm-Flow 1.8.9 升级变更清单（1.3.3 → 1.8.9）

| 变更 | 说明 |
|---|---|
| groupId `org.dromara` → `org.dromara.warm` | 依赖坐标变更 |
| `FlowFactory` → `FlowEngine` | 静态门面更名，如 `FlowEngine.insService().start(...)` |
| `importXml` 废弃 | 流程定义改用 `DefJson/NodeJson/SkipJson` 对象树（纯代码定义，无设计器依赖） |
| DDL 变更 | 新增 `flow_form` 表；`flow_definition` 增 `model_value`；`flow_task` 增 `flow_status`；`flow_node` 增 `any_node_skip`（完整 8 张 flow_* 建表参考：`samples/flow-sample/src/main/resources/schema-h2.sql` L203-373） |
| 条件表达式格式变更 | 见 §2 第 3 条，**静默失效类，重点看** |

## 2. API 坑位速查表（每条都实测踩过）

来源：`samples/flow-sample/src/main/java/.../flow/WarmFlowWorkflowService.java`、`samples/sandbox-sample/.../flow/warmflow/`。

| # | 坑 | 现象 | 正确写法 |
|---|---|---|---|
| 1 | `importDef` 后未 `active` | 启动流程实例时 NPE（`DefJson` 无 activityStatus 字段） | `importDef` → **`defService().active(id)`** → `publish`，三步缺一不可 |
| 2 | `SkipJson.nowNodeCode` 漏填 | `importDef` 直接 NPE（按分组校验） | 每条 SkipJson 必填 nowNodeCode |
| 3 | 条件表达式旧格式 | **不报错、网关路由时静默失效** | 1.8.9 格式 `eq@@hasProblem\|true`（1.3.3 是 `@@eq@@\|hasProblem@@eq@@true`，升级后必须全量改） |
| 4 | 按 businessId 查待办 | `newTask().setBusinessId(id)` 条件被**静默忽略，返回全表** | `FlowTask.businessId` 是 `@TableField(exist=false)`（HisTask 同）。**先查 flow_instance 拿 instanceId，再按 instanceId 查任务** |
| 5 | 撤回/终止实例 | `removeById` 只逻辑删实例，**遗留孤儿待办** | 必须走 `taskService().termination(..., ignore(true))` |
| 6 | `@Version` 乐观锁失效 | `updateById` 报 `MP_OPTLOCK_VERSION_ORIGINAL not found` | `@Version` 字段必须配套注册 `OptimisticLockerInnerInterceptor`（见 `MybatisPlusConfig`） |
| 7 | 唯一必需配置 | — | `warm-flow.banner: false`；其余零配置可随 starter 跑，引擎表用官方 DDL 自建 |
| 8 | 日志噪音 | — | `logging.level.org.dromara.warm: WARN` |

办理/驳回标准姿势：`taskService().skip(taskId, FlowParams.skipType(PASS/REJECT)...)`，按 nodeCode 定位待办；相邻退回 = REJECT 到相邻节点。

## 3. 引擎集成范式（业务真值源 + 乐观锁抢占）

### 3.1 架构姿势：业务表是真值源，引擎只是跟随者

来源：`samples/flow-sample/.../service/InstructionService.java`、`CommandQueryService.java`。

- 回调处理四步，**同一个 `@Transactional`**：① 状态机校验（requireStatus 拒绝乱序/迟到回调）→ ② 业务落库（指令 + 工单 + 状态流水 + 回执）→ ③ `workflow.completeByNode` 推进引擎 → ④ 审计。引擎失败整体回滚，业务状态自动还原。
- **展示层节点态从业务状态机推导，不查引擎**（`flowNodes()`：ASSIGNED→sign 为 CURRENT，无问题归档时 review 标 SKIPPED）。业务表真值源、引擎待办互为印证——避免前端依赖引擎查询，也为换引擎留了余地。
- 撤回窗口、签收截止（`ack_deadline`）等业务语义挂工单表，不挂引擎。

### 3.2 引擎门面 SPI（业务代码不直喷引擎 API）

```java
public interface WorkflowService {          // flow-sample 与 sandbox-sample 同接口
    void deployIfNeeded(String defKey);
    void start(String businessId, Map<String,Object> vars);
    List<TaskView> currentTasks(String businessId);
    void completeByNode(String businessId, String nodeCode, Map<String,Object> vars);
    void rejectByNode(String businessId, String nodeCode, String reason);
    void withdraw(String businessId);
    List<HistoryView> history(String businessId);
    boolean ended(String businessId);
}
```

业务模块只依赖该接口；sandbox-sample 以同接口双实现驱动双引擎 POC（`WorkflowEngineRegistry`：List 注入自动发现 + `flow.engine` 配置选默认引擎，**双引擎同时部署，配置只决定业务默认用哪个，不裁剪另一个**）。

### 3.3 并发防重：乐观锁抢占三段式（弥补引擎层无防重）

来源：`samples/sandbox-sample/.../service/InstructionService.java` L350-395，并发单测 `DualEngineFlowTest.concurrentAckOnlyOneSucceedsAtBusinessLayer`（4 线程同签，断言恰 1 成功且引擎只推进 1 格）。

- **普通动作**：`preempt()` 条件 UPDATE（`WHERE status=期望 AND version=?` + `setSql("version = version + 1")`）先抢业务状态机，影响 0 行抛 409；**抢占成功者才推进引擎**，同事务内引擎失败整体回滚、抢占自动还原。
- **RESULT 类动作**（目标状态由网关路由决定）：`requireClaim()` 先占行权（仅推进版本号锁行）→ 完成引擎 → 按网关路由结果回填最终状态，两段式。
- 结论：**业务表乐观锁是仲裁点，引擎只是跟随者**。适用于任何自身无防重能力的下游引擎/第三方系统。

## 4. 双引擎同进程共存（bean 撞名解法）

来源：`samples/sandbox-sample/.../config/FlowLongCoexistConfig.java`。

- 问题：Warm-Flow `BeanConfig` 与 FlowLong `MybatisPlusConfiguration` 定义同名 @Bean（instanceDao/taskDao/hisTaskDao/taskService），同上下文启动即 `BeanDefinitionOverrideException`。
- 解法：应用配置中以**独立 bean 名**（flwInstanceDao/flwTaskDao/flwTaskService...）预先注册 FlowLong 的撞名 bean；FlowLong 侧 `@ConditionalOnMissingBean` 自动让位，Warm-Flow 独占原名，按类型注入互不感知。
- 这是**第三方 starter 撞名的通用解法**（抢注替身 + 条件装配让位）；升级 starter 版本后应重跑 POC 清单确认仍成立。
- pom 坑：FlowLong starter 传递 SB2 版 `mybatis-plus-boot-starter`（mybatis-spring 2.x），必须 exclusions，否则与 SB3 starter（mybatis-spring 3.x）冲突。

## 5. 双引擎 POC 可执行清单（换库/换版本重跑即可）

来源：`samples/sandbox-sample/.../flow/poc/EnginePocChecklistService.java`，端点 `POST /api/engine/poc-checklist/{warmflow|flowlong}`。

| # | 用例 | 判定 |
|---|---|---|
| 1 | 建表与基础查询 | 通过 |
| 2 | 待办分页 | 通过 |
| 3 | **并发完成同一待办**（两线程 CountDownLatch 同时签收） | 必须恰 1 成功（两引擎在此项表现不同，见 §0） |
| 4 | 同事务一致性（业务表 + 引擎任务同事务强制回滚） | 两边必须一致回滚 |
| 5 | 相邻退回 | 通过 |

达梦 POC 只需换数据源重跑同一清单（预计半天，步骤见《ct-flow双引擎POC执行手册》T1~T7）。

## 6. 融合通信与催办督办通用模式（flow-sample 附产）

### 6.1 通信降级链（配置驱动 + 无状态看门狗）

来源：`samples/flow-sample/.../comm/CommOrchestrator.java`、`CommProps.java`。

- 一行配置表达降级策略，**`,` 分降级步、`|` 分并行组**：

```yaml
comm:
  strategy:
    "Ⅰ": "PUSH|VOICE,SMS"    # 第一步 PUSH+VOICE 并行；超时降级到 SMS
    "Ⅲ": "PUSH,SMS"          # PUSH 失败 → SMS
  degrade-timeout-seconds: 12  # 方案值 Ⅲ=30s/Ⅳ=60s，样例压缩便于演示
  suppress-seconds: 10         # 方案值 300：同指令同通道同目标抑制窗口，防重复轰炸
```

- 看门狗（`@Scheduled` 每秒）**无内存状态**：当前步全组"受理失败或超时未送达"→ 自动降级下一步，任一 DELIVERED/ACK 即停；计划态完全可由 `t_comm_message` + 策略重建，**重启安全**。
- 转派 = round+1、旧轮冻结；**全通道失败不阻塞业务**（工单照常 SENT + 状态流水 TIMEOUT 红警转人工）——通道是增强不是单点。

### 6.2 送达三态与回执幂等

- 三态状态机 `SENT → DELIVERED → ACK`（FAILED 终态），message_id 唯一幂等；
- **语义分层**：DELIVERED="通道送达"（通道回执），ACK="人知道了"（警务通回调，由业务 ACK 分支触发）——两个概念必须分开建模；
- 回执幂等生产实现：进程内 Map 换 Redis SETNX。

### 6.3 催办督办（策略表驱动，与引擎解耦）

来源：`samples/flow-sample/.../service/ReminderService.java`。

- 只扫业务表（t_instruction/t_dispatch_order），**不依赖引擎内部定时器**；
- 策略表 `t_reminder_policy` 按 `instruction_type × reminder_dim`（SIGN 签收 / DLIVERY 送达双维度）配置首催/间隔/上限/升级阈值；
- 督办事件 = 状态流水 `status=TIMEOUT`，remark 用【】前缀分类（送达督办/催办N/升级督办/全通道失败），前端时间线按前缀解析渲染。

## 7. 席位进度图前后端契约（X6 只读）

来源：`samples/flow-sample/.../service/CommandQueryService.java`、`static/app.js`。

- 后端 `detail()` 返回统一 JSON：`{instruction, order, flowNodes:[{code,name,state:DONE/CURRENT/PENDING/SKIPPED}], messages, statusLogs, receipts, audits, notifications, policies}`——**节点态由状态机推导**（§3.1）；
- 前端 `lib/x6.min.js` UMD 本地内置（公安内网离线可用），`interacting:false` 只读 + ctrl 滚轮缩放，画三泳道：指令流程线性进度 / 通信降级链按 degradeSeq 分列（并行组纵向堆叠）/ 催办督办标记；
- 轮询时 JSON 未变跳过重绘，防按钮抖动；
- 移植 ct-ui：`workstation.vue` 加 `@antv/x6` 2.x 依赖 + 搬 `drawGraph()` 即可。**刻意选只读进度而非编辑器**（流程变更走版本化定义，不开放运行期改图）。

## 8. 新增流程类功能开发检查清单

- [ ] importDef 后是否 active + publish 三步齐全？
- [ ] 条件表达式是否用 1.8.9 新格式？升级后是否全量排查旧格式？
- [ ] 是否经 WorkflowService SPI 访问引擎，业务代码零直喷？
- [ ] 业务落库与引擎推进是否同事务？状态机校验是否前置？
- [ ] 并发动作是否有乐观锁抢占（preempt/requireClaim）？并发单测是否覆盖"恰 1 成功"？
- [ ] 展示层节点态是否从状态机推导而非查引擎？
- [ ] 撤回是否走 termination（而非 removeById）？
- [ ] 按 businessId 查任务是否先查 flow_instance？
- [ ] 通知类功能是否区分"通道送达"与"人已签收"？回执是否幂等？
- [ ] 催办策略是否表驱动、双维度、带升级链？

---

## 修订记录

| 版本 | 日期 | 说明 |
|---|---|---|
| V1.0 | 2026-09-15 | 首版：自 samples/flow-sample 与 sandbox-sample 源码、README 沉淀 |
