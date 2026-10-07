package com.scaffold.common.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

/**
 * Dubbo 直连集成测试运行时：本地内存拉起 Triple/Dubbo 服务（注册中心 N/A），
 * 消费方直连本机端口发起真实 RPC。比纯 Mockito 单测多验证两层真实行为——
 * 协议暴露与序列化、Dubbo Filter 链（如 SecurityContextFilter / TripleExceptionFilter），
 * 又不依赖 Nacos。
 *
 * <pre>{@code
 * class RemoteUserProtoDirectConnectTest
 * {
 *     private DubboDirectRuntime runtime;
 *
 *     @BeforeEach
 *     void setUp()
 *     {
 *         RemoteUserProtoServiceImpl impl = new RemoteUserProtoServiceImpl(mockService, ...);
 *         runtime = DubboDirectRuntime.tri()
 *                 .service(RemoteUserService.class, impl)
 *                 .start();
 *     }
 *
 *     @AfterEach
 *     void tearDown()
 *     {
 *         runtime.close();   // destroy + reset，同 JVM 内可再次 start
 *     }
 *
 *     @Test
 *     void rpcOverTri()
 *     {
 *         RemoteUserService consumer = runtime.consumer(RemoteUserService.class);
 *         GetUserInfoResponse resp = consumer.getUserInfo(request);
 *     }
 * }
 * }</pre>
 *
 * <p>注意：直连运行时里服务实现是手工 new 的普通对象，Spring AOP 切面
 * （@InnerAuth 等）不会织入；Dubbo 级 Filter 仍然生效。</p>
 *
 * @author ct
 */
public final class DubboDirectRuntime implements AutoCloseable
{
    private final String protocol;
    private final int port;
    private final ApplicationConfig application;
    private final DubboBootstrap bootstrap;
    private final List<ReferenceConfig<?>> references = new ArrayList<>();

    private DubboDirectRuntime(String protocol, List<ServiceEntry> services)
    {
        this.protocol = protocol;
        this.port = findFreePort();
        this.bootstrap = DubboBootstrap.getInstance();
        this.application = new ApplicationConfig(
                "scaffold-test-" + UUID.randomUUID().toString().substring(0, 8));
        this.application.setQosEnable(false);
        this.bootstrap
                .application(this.application)
                .registry(new RegistryConfig("N/A"))
                .protocol(new ProtocolConfig(protocol, port));
        for (ServiceEntry entry : services)
        {
            ServiceConfig<Object> service = new ServiceConfig<>();
            service.setInterface(entry.interfaceClass);
            service.setRef(entry.impl);
            service.setProtocol(new ProtocolConfig(protocol, port));
            bootstrap.service(service);
        }
        bootstrap.start();
    }

    /**
     * Triple 协议直连运行时（本工程内部 RPC 的标准协议）
     */
    public static Builder tri()
    {
        return protocol("tri");
    }

    public static Builder protocol(String protocol)
    {
        return new Builder(protocol);
    }

    /**
     * 创建直连本机已暴露服务的消费代理（可多次调用，同接口复用同一代理）。
     * 强制 remote scope，避免同 JVM 内被 Injvm 本地调用短路、失去真实 RPC 覆盖。
     */
    public <T> T consumer(Class<T> interfaceClass)
    {
        for (ReferenceConfig<?> existing : references)
        {
            if (existing.getInterfaceClass() != null
                    && interfaceClass.isAssignableFrom(existing.getInterfaceClass()))
            {
                return interfaceClass.cast(existing.get());
            }
        }
        ReferenceConfig<T> reference = new ReferenceConfig<>();
        // 复用提供方同一 ApplicationConfig：Dubbo 3 同一模型内不允许出现第二个应用配置
        reference.setApplication(application);
        reference.setInterface(interfaceClass);
        reference.setUrl(protocol + "://127.0.0.1:" + port);
        // 显式指定 javassist 普通代理：与生产 @DubboReference 的缺省一致（按 Java 方法名走接口名路由）。
        // 不设置时，DubboStub 接口会被 ReferenceConfig 自动切成 nativestub，
        // 按 proto 全限定名+PascalCase 方法名请求，与服务端 implements 风格注册的接口名路由对不上，
        // 报 UNIMPLEMENTED: Invoker for gRPC not found
        reference.setProxy("javassist");
        reference.setScope("remote");
        reference.setCheck(false);
        T proxy = reference.get();
        references.add(reference);
        return proxy;
    }

    public int port()
    {
        return port;
    }

    @Override
    public void close()
    {
        for (ReferenceConfig<?> reference : references)
        {
            try
            {
                reference.destroy();
            }
            catch (RuntimeException ignored)
            {
                // 测试收尾，尽力销毁
            }
        }
        references.clear();
        bootstrap.destroy();
        // 复位单例，保证同 JVM 内下一个测试类还能 start 新的运行时
        DubboBootstrap.reset();
    }

    private static int findFreePort()
    {
        try (ServerSocket socket = new ServerSocket(0))
        {
            return socket.getLocalPort();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("无可用的本地端口", e);
        }
    }

    private record ServiceEntry(Class<?> interfaceClass, Object impl)
    {
    }

    public static final class Builder
    {
        private final String protocol;
        private final List<ServiceEntry> services = new ArrayList<>();

        private Builder(String protocol)
        {
            this.protocol = protocol;
        }

        /**
         * 暴露一个服务实现（实现一般是手工 new、依赖用 Mockito 打桩的普通对象）
         */
        public Builder service(Class<?> interfaceClass, Object impl)
        {
            services.add(new ServiceEntry(interfaceClass, impl));
            return this;
        }

        public DubboDirectRuntime start()
        {
            return new DubboDirectRuntime(protocol, services);
        }
    }
}
