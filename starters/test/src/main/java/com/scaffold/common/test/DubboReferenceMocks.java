package com.scaffold.common.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.dubbo.config.annotation.DubboReference;

/**
 * Dubbo 消费方单测辅助：把 mock 对象注入 {@code @DubboReference} 字段。
 *
 * <p>背景：{@code @DubboReference} 是**字段注入**（不走构造器），
 * {@code @InjectMocks} 只能填构造参数，Dubbo 引用字段仍为 null。
 * 本工具按类型把 mock 填进所有 {@code @DubboReference} 字段（含父类声明），
 * 漏配的引用会直接报错指出字段名：</p>
 *
 * <pre>{@code
 * @ExtendWith(MockitoExtension.class)
 * class SysLoginServiceTest
 * {
 *     @Mock private RemoteUserService remoteUserService;   // proto 生成的接口
 *     @Mock private SysPasswordService passwordService;
 *     @InjectMocks private SysLoginService loginService;   // 构造参数由 @Mock 填充
 *
 *     @BeforeEach
 *     void setUp()
 *     {
 *         DubboReferenceMocks.inject(loginService, remoteUserService);
 *     }
 * }
 * }</pre>
 *
 * @author ct
 */
public final class DubboReferenceMocks
{
    private DubboReferenceMocks()
    {
    }

    /**
     * 按类型把 mocks 注入 target 的全部 {@code @DubboReference} 字段。
     *
     * @param target 被测对象（一般为 @InjectMocks 构造出来的实例）
     * @param mocks  mock 实例，按字段类型匹配
     * @throws IllegalStateException 存在没有匹配 mock 的 @DubboReference 字段时
     */
    public static void inject(Object target, Object... mocks)
    {
        List<String> missing = new ArrayList<>();
        Class<?> type = target.getClass();
        while (type != null && type != Object.class)
        {
            for (Field field : type.getDeclaredFields())
            {
                if (!field.isAnnotationPresent(DubboReference.class))
                {
                    continue;
                }
                Object match = null;
                for (Object mock : mocks)
                {
                    if (field.getType().isInstance(mock))
                    {
                        match = mock;
                        break;
                    }
                }
                if (match == null)
                {
                    missing.add(type.getSimpleName() + "." + field.getName()
                            + "（类型 " + field.getType().getName() + "）");
                    continue;
                }
                try
                {
                    field.setAccessible(true);
                    field.set(target, match);
                }
                catch (IllegalAccessException e)
                {
                    throw new IllegalStateException("注入 @DubboReference 字段失败: "
                            + type.getSimpleName() + "." + field.getName(), e);
                }
            }
            type = type.getSuperclass();
        }
        if (!missing.isEmpty())
        {
            throw new IllegalStateException("以下 @DubboReference 字段缺少对应类型的 mock，"
                    + "请在 inject(...) 里补上: " + missing);
        }
    }
}
