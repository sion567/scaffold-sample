package com.scaffold.flow.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程定义贡献者 SPI：接入方实现本接口并声明为 Spring Bean，其流程定义即被注入
 * 当前激活的引擎实现（native / warmflow），无需改动 starter 内部代码。
 *
 * <p>注册约定：</p>
 * <ul>
 *   <li>processCode 全局唯一：多个贡献者注册同一编码时启动期 fail-fast
 *       （抛 {@link FlowException}）；</li>
 *   <li>无任何贡献者时流程注册表为空——发起未注册流程抛 {@link FlowException}；</li>
 *   <li>定义在运行期视为不可变（实现不应返回会被后续修改的实例）。</li>
 * </ul>
 *
 * @author ct
 */
@FunctionalInterface
public interface WorkflowDefinitionContributor
{
    /** 返回本贡献者提供的全部流程定义（不可为 null，可为空列表） */
    List<WorkflowDefinition> definitions();

    /** 将全部贡献者的定义按 processCode 索引（重复编码 fail-fast；保持声明顺序） */
    static Map<String, WorkflowDefinition> index(List<WorkflowDefinitionContributor> contributors)
    {
        Map<String, WorkflowDefinition> index = new LinkedHashMap<>();
        for (WorkflowDefinitionContributor contributor : contributors)
        {
            List<WorkflowDefinition> list = contributor.definitions();
            if (list == null)
            {
                throw new FlowException(
                        "WorkflowDefinitionContributor 返回 null: " + contributor.getClass().getName());
            }
            for (WorkflowDefinition definition : list)
            {
                if (index.putIfAbsent(definition.getProcessCode(), definition) != null)
                {
                    throw new FlowException("流程定义编码重复注册: " + definition.getProcessCode()
                            + "（请检查各 WorkflowDefinitionContributor）");
                }
            }
        }
        return index;
    }
}
