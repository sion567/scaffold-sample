package com.scaffold.common.job;

/**
 * @ScaffoldJob 方法登记条目（启动扫描产物，只读）
 *
 * @author ct
 */
public class JobDefinition {
    private final String name;

    private final String group;

    private final String description;

    private final String beanName;

    private final String className;

    private final String methodName;

    public JobDefinition(
            String name,
            String group,
            String description,
            String beanName,
            String className,
            String methodName) {
        this.name = name;
        this.group = group;
        this.description = description;
        this.beanName = beanName;
        this.className = className;
        this.methodName = methodName;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getDescription() {
        return description;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    /** 回写日志用调用目标：类名.方法名 */
    public String invokeTarget() {
        return className + "." + methodName;
    }
}
