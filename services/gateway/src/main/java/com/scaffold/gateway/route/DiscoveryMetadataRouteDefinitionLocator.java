package com.scaffold.gateway.route;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Nacos metadata 动态路由：服务在注册 metadata 声明 gateway-path（如 system 服务声明 "system"），
 * 本定位器自动生成 Path=/{gateway-path}/** → lb://{service} 路由——
 * 服务名与对外路由前缀解耦，新服务零网关配置改动。
 * 与 Nacos 配置中的静态路由叠加生效；metadata 声明删除后路由自动回收。
 * scaffold.gateway.dynamic-routes.enabled=true 开启（默认关闭，网关 yml 中已启用）。
 *
 * @author scaffold
 */
@Component
@ConditionalOnProperty(prefix = "scaffold.gateway.dynamic-routes", name = "enabled", havingValue = "true")
public class DiscoveryMetadataRouteDefinitionLocator implements RouteDefinitionLocator
{
    private static final Logger log = LoggerFactory.getLogger(DiscoveryMetadataRouteDefinitionLocator.class);

    /** Nacos 注册 metadata 中声明网关路径的 key */
    public static final String METADATA_GATEWAY_PATH = "gateway-path";

    private final DiscoveryClient discoveryClient;

    private final ApplicationEventPublisher eventPublisher;

    private final long refreshSeconds;

    private final Map<String, RouteDefinition> routes = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    public DiscoveryMetadataRouteDefinitionLocator(DiscoveryClient discoveryClient,
                                                   ApplicationEventPublisher eventPublisher,
                                                   @Value("${scaffold.gateway.dynamic-routes.refresh-seconds:30}") long refreshSeconds)
    {
        this.discoveryClient = discoveryClient;
        this.eventPublisher = eventPublisher;
        this.refreshSeconds = refreshSeconds;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions()
    {
        return Flux.fromIterable(routes.values());
    }

    @PostConstruct
    public void start()
    {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scaffold-gateway-dynamic-routes");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::refresh, refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
    }

    void refresh()
    {
        try
        {
            Map<String, RouteDefinition> latest = new ConcurrentHashMap<>();
            for (String service : discoveryClient.getServices())
            {
                List<ServiceInstance> instances = discoveryClient.getInstances(service);
                if (instances == null || instances.isEmpty())
                {
                    continue;
                }
                Object path = instances.get(0).getMetadata().get(METADATA_GATEWAY_PATH);
                if (path == null || String.valueOf(path).isEmpty())
                {
                    continue;
                }
                String gatewayPath = String.valueOf(path).replaceAll("^/+|/+$", "");
                RouteDefinition definition = new RouteDefinition();
                definition.setId(service + "-metadata");
                definition.setUri(URI.create("lb://" + service));
                PredicateDefinition predicate = new PredicateDefinition("Path=/" + gatewayPath + "/**");
                List<PredicateDefinition> predicates = new ArrayList<>();
                predicates.add(predicate);
                definition.setPredicates(predicates);
                latest.put(service, definition);
            }
            if (!latest.equals(routes))
            {
                routes.clear();
                routes.putAll(latest);
                log.info("[动态路由] 按 Nacos metadata 刷新路由: {}", routes.keySet());
                eventPublisher.publishEvent(new RefreshRoutesEvent(this));
            }
        }
        catch (Exception e)
        {
            log.warn("[动态路由] 刷新失败（保留现有路由）: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop()
    {
        if (scheduler != null)
        {
            scheduler.shutdownNow();
        }
    }
}
