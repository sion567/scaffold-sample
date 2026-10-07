# Drools 实战坑位与复用模板

> 适用：ct-command 派遣决策表、二次研判规则、规则沙箱及一切 Drools 类功能开发。
> 来源：`samples/brms-sample`（双管线决策引擎）与 `samples/sandbox-sample`（沙箱三层能力、防护栈）源码级验证结论。
> 配套：《实战指挥平台设计文档》§14.4/14.5/C.5（决策表落地与治理设计）、《情指行一体化总体设计方案》§3.1/3.2（派遣决策表/二次研判/沙箱设计）、《API.md》ct-command 决策表端点、《alert-center-design.md》（另一套阈值规则体系，见 §9 边界说明）。
> 版本：V1.0（2026-09-15）。

---

## 1. 依赖与字符集基线

- **Drools 10.2.0 三件套缺一不可**：`drools-decisiontables` + `drools-compiler` + `drools-mvel`。compiler 需显式引入；**缺 mvel 时运行期抛 `UnsupportedOperationException`（ConstraintBuilder）**，编译期不报错。
- 不引 kjar/kie-ci/Maven 运行时：规则全部在**应用内编译**（KieFileSystem → KieBuilder），DRL 文本存数据库。
- **DRL 中文乱码根治**（`common/DrlCodec.java`，42 行零依赖工具类，可直接拷贝）：KieBuilder 按平台默认字符集（Windows GBK）解码 DRL 资源，UTF-8 中文会乱码进运行期数据。解法：非 ASCII 字符统一转 `\uXXXX` 转义后以 US_ASCII 字节写入 KieFileSystem，任何字符集环境编译运行一致。两条管线（DRL 直生成/决策表）都必须接入。
- 主工程已有的"决策表动作列只流转数字编码"纪律是本方案的业务侧特例，通用解法即 DrlCodec。

## 2. KieContainer 生命周期模板（原子热切换）

来源：`brms-sample/.../judgment/JudgmentRuleEngine.java`、`sandbox-sample/.../rules/engine/OnlineRuleContainer.java`。

```java
private final AtomicReference<KieContainer> containerRef = new AtomicReference<>();
private final AtomicReference<String> versionRef   = new AtomicReference<>("init");

public void publish(List<String> drlList, String version) {
    KieFileSystem kfs = kieServices.newKieFileSystem();
    drlList.forEach(drl -> kfs.write("src/main/resources/rules/" + drl.path(), drl.content()));
    KieBuilder builder = kieServices.newKieBuilder(kfs).buildAll();
    if (builder.getResults().hasMessages(Message.Level.ERROR)) {
        throw new BizException(409, "规则编译失败：" + builder.getResults().getMessages()); // 发布门禁
    }
    KieContainer old = containerRef.getAndUpdate(c -> {
        KieContainer nc = kieServices.newKieContainer(builder.getKieModule().getReleaseId());
        return nc;
    });
    disposeQuietly(old);   // 在途会话不受影响，释放失败仅告警
    versionRef.set(version);
}
```

要点：

1. **编译失败抛 409，旧容器继续服务**（发布门禁，`brms.dispatch.gate-enabled: true`）；
2. `AtomicReference.getAndUpdate` 原子切换，评估用 `StatelessKieSession`（每次新建、天然线程安全）；
3. 旧容器 `dispose` 静默失败仅告警（在途会话优先）；
4. `KieSession` 用完必须 `finally dispose`（无状态会话除外）——泄漏会拖垮内存；
5. releaseId 用"版本 + UUID"避免 KieRepository 按 GAV 缓存旧模块（sandbox 实测坑）。

热更新两种触发方式互补：**页面按钮触发**（brms：保存→试编译→发布）与**DRL 指纹轮询**（sandbox：`OnlineRuleContainer` 每分钟比对 rule_version，重建成功才 swap，失败保持旧容器并记 RULE_RELOAD 审计）。

## 3. 决策表全链路（数据库矩阵 → 可热替换规则）

来源：`brms-sample/.../dispatch/DecisionTableGenerator.java`、`DroolsDispatchEngine.java`。

管线：`t_resp_matrix` 矩阵行 → POI HSSF 生成 XLS → `SpreadsheetCompiler` 编译 DRL → 试编译预览 → 发布编译 + 原子切换 + sha256 摘要入 `t_rule_version`。

运维友好设计：`/api/matrix/drl-preview` 编译预览接口——业务改矩阵后可直接看生成的 DRL。

**条件列安全纪律**：条件列全部做枚举映射（值域白名单），**任何情况不允许用户输入直接编译为 DRL**（天然免疫规则注入）。

### 3.1 决策表组合坑（官方文档不覆盖此完整组合，实测结论）

| 坑 | 解法 |
|---|---|
| 多行同时满足时泛化行压过细类行 | 每行 PRIORITY 列 = `salience 100 + 条件数×10`（**条件多的行优先**） |
| 多行连击 / RHS 死循环 | 每行附加守卫条件 `responseLevel == null`（绑定独立 pattern），RHS 末尾 `update(j)` 触发属性反应性——高 salience 行回填后低 salience 行激活被自动取消，结构性保证单行命中 |
| 重复绑定错误 | 每列绑定独立变量（f/g/h/i/j），同名绑定在未合并 pattern 下报重复绑定 |
| RHS 语法 | 决策表 RHS **不自动补分号**，语句必须自带 `;` |
| NAME 列 | Drools 10.2 无 NAME 列标记时 col0 必须留空 |

## 4. 无状态会话兜底规则：两种正确解法（死循环陷阱）

来源：`brms-sample/.../judgment/SecondaryJudgmentService.java`、`rules/seed/default.drl` 头注释——**实测结论，勿把兜底写进 DRL**。

- 陷阱：无状态会话中兜底行激活在事实插入时即创建；其他规则改写 dispatchCount 后不 `update` 会留下失效激活，`update` 在约束不含被改属性时又自激活死循环（评估超时）。
- **解法 A（默认推荐）**：兜底放 Java 服务层——规则评估后 `dispatchCount` 仍为 null 即未命中，服务层补齐默认值。简单、无激活语义陷阱。
- **解法 B（确需规则内兜底时）**：唯一命中守卫 + `update(j)`（§3.1 结构），决策表场景已验证。
- 两条管线分别选了 A/B 并都在样例中验证——按场景选，不要混用。

## 5. 多规则叠加语义收敛（DRL RHS 编写规范）

来源：`brms-sample/.../judgment/WarningFact.java`。

**规则只做加法，语义收敛收口在 Fact 帮助方法**：

| 方法 | 语义 |
|---|---|
| `raiseRisk` | 单调上调、下限红（多领域包命中不回退） |
| `setPriorityAtLeast` | 取最高优先级 |
| `raiseDispatchCount` | 取最大 |
| `addReason` | 汇总可解释理由 |

约定：`dispatchCount = 0` 表示"建议不派"；领导可 force 强制发起但**必须留痕**（`command/InitiateService`）。agenda-group 分组触发（risk/dispatch 两个 agenda-group，由编排层按环节 fire）。

## 6. 防护栈：超时降级 + 预热 + 双熔断路径

来源：`brms-sample/.../judgment/SecondaryJudgmentService.java`、`sandbox-sample/.../sandbox/DroolsSandbox.java`。

1. **专用线程池 + 超时降级**：2 线程专用池 + `future.get(evalTimeoutMs)`（3s），超时维持初判结果（正常派 1 人），指令不中断；
2. **启动预热**（`RuleSeeder`，ApplicationRunner）：Drools 首次评估含会话初始化与 JIT，**不预热则首个业务请求被超时降级**——容易被忽略的实战细节；
3. **Resilience4j 编程式防护**（sandbox，推荐编程式而非注解——可按路径区分实例）：
   - Timeout：专用固定线程池 + `cancel(true)` 中断（sleep 型规则立即中止）；
   - Bulkhead：maxConcurrent=4、maxWait 200ms；
   - CircuitBreaker：失败率 50%/滑动窗 8/最小调用 4/Open 态 20s；**线上 `droolsOnline` 与沙箱 `droolsSandboxTry` 两条独立计数路径——坏草稿打不开线上熔断器**；
   - Fallback 不写死在防护层，由调用方（编排组件）执行：降级 = 默认低风险正常派警。
4. L2 回归走 `evaluateRaw`（仅超时、不计熔断），坏草稿演示不污染回归统计。

## 7. 规则治理三件套（版本 / 审计 / 决策留痕）

来源：`brms-sample` 表结构与 `SecondaryJudgmentService` 三段式 API，生产对应 Flyway `V6__brms_rule_trace` 等。

- 三表：`t_rule_def`（DRL 文本，领域分包）+ `t_rule_version`（版本/digest=SHA256/status EFFECTIVE→HISTORY，engine 列区分 DISPATCH/JUDGMENT 双管线）+ `t_rule_audit_log`（EDIT/PUBLISH/TRIAL/INITIATE/LIFECYCLE，before/after 留痕）；
- API 三段式：**保存先试编译**（validate 返回错误列表）→ **发布整体编译**（409 门禁 + 原子切换）→ **版本/审计登记**；
- 决策留痕：指令表冗余 6 列决策上下文（initial_risk/dispatch_count/officer_ids/...），预警表回填研判上下文 13 列——**每次规则决策必须可解释、可追溯**。

## 8. 规则沙箱三层能力（工程落点）

来源：`sandbox-sample/.../sandbox/`，设计条款见总体设计 §3.1.4。

| 层 | 能力 | 实现要点 |
|---|---|---|
| L1 试算 | 草稿规则现场评估 | 草稿容器现场编译 → 评估 → `finally dispose`；返回草稿 vs 线上对照 |
| L2 回归 | 草稿 vs 线上对历史样本对判 | **同 JVM 双容器**（线上 AtomicReference vs 草稿临时）对同批历史事件各判一遍；单线程异步、上限 500 条；报告含草稿矩阵 JSON 快照可复跑 |
| L3 发布门禁 | 未确认差异不放行 | 最新回归报告有未确认差异 → 409 + 差异摘要；人工 confirm 后进入 §7 发布三段式 + 容器 reload，**一个事务**完成 |

量化验证：36 条样本检出 32 处差异；刻意删除兜底行后回归**精确检出 17 条疑似漏判**（验证 L2 对"规则收窄导致漏判"的敏感度）。

**差异机械分类**（`DiffClassifier`）：NEW_HIT / MISS / LEVEL_CHANGE / ACTION_CHANGE 四类 + SAME，差异说明机械生成（禁止自然语言解释——不可审计）；MISS（疑似漏判）置顶。

**五条硬性隔离约束的落点**（逐条可验收）：

1. 容器隔离：沙箱容器与线上容器是两个对象，结构性隔离（无开关可配错）；
2. 无副作用：`SideEffectGuard` ThreadLocal 开关，写路径组件统一入口 `assertWritable` 校验，沙箱上下文内必抛 `SideEffectViolation`；单测覆盖"异常后 exit 恢复"；
3. 只读依赖：只读静态表；动态状态不可达时退回静态候选打降级标记；历史样本表无 PII 列，跨批次关联用 **HMAC 哈希**（确定性不可还原）；
4. 资源限制：单次评估 3s 超时 + L2 上限 500 条单线程；
5. 权限审计：独立权限点 `command:matrix:sandbox`，全部动作落 `t_rule_audit_log`。

**发布五步事务**：ONLINE 全转 ARCHIVED 留痕 → DRAFT 转 ONLINE 打版本 → rule_version 登记 → 容器原子 reload → 审计。

## 9. 编排与规则的分工（含裁决口径）

- 分工边界（sandbox 验证结论）：**编排层管"环节重组"（地址解析→历史查询→研判→派警→清单），规则层管"每个环节内的规则判定"**；环节共享上下文对象，组件声明读写键（不可变 DTO 原则，评审可查）。
- 生产口径（**以《情指行设计文档差距分析与补充清单》G14 裁决为准**）：LiteFlow **不引入生产**，用轻量自研 chain 承担编排层；本样例的分工模式、agenda-group 触发方式、安全纪律照搬即可。引用样例结论时必须带此裁决注记。
- 安全纪律：链定义入库 + 变更审计 + 仅管理员可改 + **禁止用户输入拼进 EL/链定义**。
- 双载体等价性：同一矩阵走 DRL 直生成与 XLS 决策表两条管线，单测断言结论等价（`DroolsRuleEngineTest.xlsPipelineMatchesDrlPipeline`）——矩阵口径变更时双管线互相印证。

## 10. alert 阈值规则 vs Drools 规则：两套体系边界

全库存在两套并存的"规则"，开发前先分清用哪套：

| | alert 阈值规则（ct-alert） | Drools 规则（ct-command） |
|---|---|---|
| 模型 | 窗口 + 阈值 + 防抖（alert_rule） | DRL / 决策表矩阵 |
| 执行 | ct-job → Kafka Streams 计算层 | 应用内 KieContainer 评估 |
| 场景 | 指标型预警（客流超限、密度告警） | 决策型判定（派遣、二次研判、风险定级） |
| 治理 | alert 规则 CRUD | t_rule_def/t_rule_version/t_rule_audit_log |

选型口诀：**指标超限找 alert，业务决策找 Drools**；两者都改"规则"，但存储、执行、治理完全不同，禁止混用表和审批流。详见 alert-center-design.md。

## 11. 新增规则类功能开发检查清单

- [ ] 依赖是否三件套齐全？DRL 是否经 DrlCodec ASCII 安全化？
- [ ] 条件列是否枚举映射（用户输入零直编译）？
- [ ] 发布是否"试编译 → 整体编译门禁 409 → 原子切换 → 版本/审计"？
- [ ] 兜底规则是否按 §4 选了正确解法（默认 Java 层）？
- [ ] 多规则叠加是否收口在 Fact 帮助方法？
- [ ] 是否专用线程池 + 超时降级 + 启动预热？
- [ ] KieSession/KieContainer 是否无泄漏（finally dispose / disposeQuietly）？
- [ ] 决策是否留痕（决策上下文冗余列 + 可解释理由）？
- [ ] 沙箱评估是否满足五条隔离约束？回归差异是否四分类机械生成？
- [ ] 该"规则"到底归 alert 体系还是 Drools 体系（§10）？

---

## 修订记录

| 版本 | 日期 | 说明 |
|---|---|---|
| V1.0 | 2026-09-15 | 首版：自 samples/brms-sample 与 sandbox-sample 源码、README 沉淀 |
