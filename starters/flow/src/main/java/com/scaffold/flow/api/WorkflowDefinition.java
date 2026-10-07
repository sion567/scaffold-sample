package com.scaffold.flow.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程定义（引擎无关视图）：由接入方经 {@link WorkflowDefinitionContributor} 注册为
 * Spring Bean 后注入当前激活的引擎实现——native 实现直接按定义路由，
 * warmflow 实现映射为 DefJson 导入。starter 不内置任何业务流程。
 *
 * <p>结构约定：</p>
 * <ul>
 *   <li>START：唯一入口（不落待办），其 PASS 后继即首个业务节点（{@link #firstNode()}）；</li>
 *   <li>BETWEEN：业务节点（产生待办），PASS 推进、REJECT 退回（{@link #previousNode}）；</li>
 *   <li>GATEWAY：网关（不落待办），路由到网关时立即按流程变量求值继续推进，
 *       条件表达式 {@code eq@@var|value}（见 {@link #matches}）；</li>
 *   <li>END：唯一出口（办结）。</li>
 * </ul>
 *
 * <p>线性流程用 {@link #linear} 工厂一步构造：自动生成相邻 PASS 推进边与
 * REJECT 退回边（start → n1 → ... → nN → end）。含网关的流程用 {@link #of} 自由组装。</p>
 *
 * @author ct
 */
public class WorkflowDefinition
{
    /** 节点类型 */
    public enum NodeType
    {
        /** 入口 */
        START,
        /** 业务节点（落待办） */
        BETWEEN,
        /** 条件网关（不落待办，按流程变量立即路由） */
        GATEWAY,
        /** 出口 */
        END
    }

    /** 跳转类型：PASS 通过推进 / REJECT 退回（returnToPrevious 沿 REJECT 边） */
    public enum SkipType
    {
        PASS,
        REJECT
    }

    /** 跳转边：sourceNode → nextNodeCode（source 由所在节点隐含） */
    public static class Skip
    {
        private final SkipType type;

        private final String skipName;

        private final String nextNodeCode;

        /** 网关条件表达式（eq@@var|value）；null/空 = 无条件命中 */
        private final String condition;

        Skip(SkipType type, String skipName, String nextNodeCode, String condition)
        {
            this.type = type == null ? SkipType.PASS : type;
            this.skipName = skipName;
            this.nextNodeCode = nextNodeCode;
            this.condition = condition;
        }

        public SkipType getType() { return type; }

        public String getSkipName() { return skipName; }

        public String getNextNodeCode() { return nextNodeCode; }

        public String getCondition() { return condition; }
    }

    /** 节点：编码全局唯一，携带按声明顺序求值的跳转边列表 */
    public static class Node
    {
        private final NodeType nodeType;

        private final String nodeCode;

        private final String nodeName;

        private final List<Skip> skips;

        Node(NodeType nodeType, String nodeCode, String nodeName, List<Skip> skips)
        {
            this.nodeType = nodeType;
            this.nodeCode = nodeCode;
            this.nodeName = nodeName;
            this.skips = skips == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(skips));
        }

        public NodeType getNodeType() { return nodeType; }

        public String getNodeCode() { return nodeCode; }

        public String getNodeName() { return nodeName; }

        public List<Skip> getSkips() { return skips; }
    }

    // ─── 工厂 ─────────────────────────────────────────────────────────────

    /** 线性流程：start → chain[0] → ... → chain[n] → end，自动生成相邻推进与退回边 */
    public static WorkflowDefinition linear(String processCode, String processName, String category,
            String... chain)
    {
        if (chain == null || chain.length == 0)
        {
            throw new IllegalArgumentException("线性流程至少需要一个业务节点: " + processCode);
        }
        List<Node> nodes = new ArrayList<>();
        nodes.add(node(NodeType.START, "start", "开始",
                List.of(skip(SkipType.PASS, "提交", chain[0], null))));
        for (int i = 0; i < chain.length; i++)
        {
            boolean last = i == chain.length - 1;
            List<Skip> skips = new ArrayList<>();
            skips.add(skip(SkipType.PASS, last ? "办结" : "通过", last ? "end" : chain[i + 1], null));
            if (i > 0)
            {
                skips.add(skip(SkipType.REJECT, "退回", chain[i - 1], null));
            }
            nodes.add(node(NodeType.BETWEEN, chain[i], chain[i], skips));
        }
        nodes.add(node(NodeType.END, "end", "结束", List.of()));
        return of(processCode, processName, category, nodes);
    }

    /** 自由组装流程（含网关时使用） */
    public static WorkflowDefinition of(String processCode, String processName, String category,
            List<Node> nodes)
    {
        return new WorkflowDefinition(processCode, processName, category, nodes);
    }

    public static Node node(NodeType nodeType, String nodeCode, String nodeName, List<Skip> skips)
    {
        return new Node(nodeType, nodeCode, nodeName, skips);
    }

    public static Skip skip(SkipType type, String skipName, String nextNodeCode, String condition)
    {
        return new Skip(type, skipName, nextNodeCode, condition);
    }

    /** 网关条件跳转：流程变量 var 的字符串值等于 value 时命中（eq@@var|value） */
    public static Skip skipIf(String skipName, String nextNodeCode, String var, String value)
    {
        return skip(SkipType.PASS, skipName, nextNodeCode, "eq@@" + var + "|" + value);
    }

    // ─── 实例 ─────────────────────────────────────────────────────────────

    private final String processCode;

    private final String processName;

    private final String category;

    private final List<Node> nodes;

    private final Map<String, Node> nodeIndex;

    private WorkflowDefinition(String processCode, String processName, String category, List<Node> nodes)
    {
        if (processCode == null || processCode.isBlank())
        {
            throw new IllegalArgumentException("processCode 不能为空");
        }
        if (nodes == null || nodes.isEmpty())
        {
            throw new IllegalArgumentException("流程定义节点不能为空: " + processCode);
        }
        Map<String, Node> index = new LinkedHashMap<>();
        long starts = nodes.stream().filter(n -> n.getNodeType() == NodeType.START).count();
        long ends = nodes.stream().filter(n -> n.getNodeType() == NodeType.END).count();
        if (starts != 1 || ends != 1)
        {
            throw new IllegalArgumentException("流程定义须恰有一个 START 与一个 END: " + processCode);
        }
        for (Node n : nodes)
        {
            if (n.getNodeCode() == null || n.getNodeCode().isBlank())
            {
                throw new IllegalArgumentException("节点编码不能为空: " + processCode);
            }
            if (index.putIfAbsent(n.getNodeCode(), n) != null)
            {
                throw new IllegalArgumentException("节点编码重复: " + processCode + "#" + n.getNodeCode());
            }
            if (n.getNodeType() == NodeType.END && !n.getSkips().isEmpty())
            {
                throw new IllegalArgumentException("END 节点不得有跳转边: " + n.getNodeCode());
            }
            if (n.getNodeType() != NodeType.END && n.getSkips().stream()
                    .noneMatch(s -> s.getType() == SkipType.PASS))
            {
                throw new IllegalArgumentException("非 END 节点至少要有一条 PASS 跳转边: " + n.getNodeCode());
            }
        }
        for (Node n : nodes)
        {
            for (Skip s : n.getSkips())
            {
                if (!index.containsKey(s.getNextNodeCode()))
                {
                    throw new IllegalArgumentException("跳转目标节点不存在: " + processCode
                            + "#" + n.getNodeCode() + " → " + s.getNextNodeCode());
                }
            }
        }
        this.processCode = processCode;
        this.processName = processName == null || processName.isBlank() ? processCode : processName;
        this.category = category == null || category.isBlank() ? "default" : category;
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
        this.nodeIndex = index;
    }

    public String getProcessCode() { return processCode; }

    public String getProcessName() { return processName; }

    public String getCategory() { return category; }

    public List<Node> getNodes() { return nodes; }

    /** 按编码取节点（不存在抛 IllegalArgumentException，属定义错误） */
    public Node node(String nodeCode)
    {
        Node n = nodeIndex.get(nodeCode);
        if (n == null)
        {
            throw new IllegalArgumentException("节点不存在: " + processCode + "#" + nodeCode);
        }
        return n;
    }

    /** 首个业务节点（START 的首个 PASS 后继） */
    public String firstNode()
    {
        return nodeIndex.values().stream()
                .filter(n -> n.getNodeType() == NodeType.START)
                .flatMap(n -> n.getSkips().stream())
                .filter(s -> s.getType() == SkipType.PASS)
                .map(Skip::getNextNodeCode)
                .findFirst()
                .orElse(null);
    }

    /** 上一业务节点（当前节点的 REJECT 后继；null = 无可退回，如首节点） */
    public String previousNode(String nodeCode)
    {
        return node(nodeCode).getSkips().stream()
                .filter(s -> s.getType() == SkipType.REJECT)
                .map(Skip::getNextNodeCode)
                .findFirst()
                .orElse(null);
    }

    /**
     * 网关条件求值：{@code eq@@var|value} 与流程变量做字符串化比较；
     * 条件为空视为恒真（无条件默认分支）。vars 为 null 时仅空条件命中。
     */
    public static boolean matches(String condition, Map<String, Object> vars)
    {
        if (condition == null || condition.isEmpty())
        {
            return true;
        }
        if (!condition.startsWith("eq@@"))
        {
            throw new FlowException("不支持的网关条件表达式（仅支持 eq@@var|value）: " + condition);
        }
        String rest = condition.substring(4);
        int sep = rest.indexOf('|');
        if (sep <= 0)
        {
            throw new FlowException("网关条件表达式格式错误（应为 eq@@var|value）: " + condition);
        }
        String var = rest.substring(0, sep);
        String value = rest.substring(sep + 1);
        return vars != null && vars.containsKey(var) && String.valueOf(vars.get(var)).equals(value);
    }

    @Override
    public String toString()
    {
        return "WorkflowDefinition(" + processCode + ", nodes=" + nodeIndex.keySet() + ")";
    }
}
