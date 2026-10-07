/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scaffold.audit.api.proto;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.ServerService;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.model.StubServiceDescriptor;
import org.apache.dubbo.rpc.service.Destroyable;
import org.apache.dubbo.rpc.stub.BiStreamMethodHandler;
import org.apache.dubbo.rpc.stub.ServerStreamMethodHandler;
import org.apache.dubbo.rpc.stub.StubInvocationUtil;
import org.apache.dubbo.rpc.stub.StubInvoker;
import org.apache.dubbo.rpc.stub.StubMethodHandler;
import org.apache.dubbo.rpc.stub.StubSuppliers;
import org.apache.dubbo.rpc.stub.UnaryStubMethodHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;

public final class DubboRemoteAuditServiceTriple {

    public static final String SERVICE_NAME = RemoteAuditService.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor = new StubServiceDescriptor(SERVICE_NAME, RemoteAuditService.class);

    static {
        org.apache.dubbo.rpc.protocol.tri.service.SchemaDescriptorRegistry.addSchemaDescriptor(SERVICE_NAME, RemoteAuditProto.getDescriptor());
        StubSuppliers.addSupplier(SERVICE_NAME, DubboRemoteAuditServiceTriple::newStub);
        StubSuppliers.addSupplier(RemoteAuditService.JAVA_SERVICE_NAME,  DubboRemoteAuditServiceTriple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(RemoteAuditService.JAVA_SERVICE_NAME, serviceDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static RemoteAuditService newStub(Invoker<?> invoker) {
        return new RemoteAuditServiceStub((Invoker<RemoteAuditService>)invoker);
    }

    private static final StubMethodDescriptor saveCollectorAuditMethod = new StubMethodDescriptor("SaveCollectorAudit",
    com.scaffold.audit.api.proto.SaveCollectorAuditRequest.class, com.scaffold.audit.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveCollectorAuditRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveCollectorAuditAsyncMethod = new StubMethodDescriptor("SaveCollectorAudit",
    com.scaffold.audit.api.proto.SaveCollectorAuditRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveCollectorAuditRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveCollectorAuditProxyAsyncMethod = new StubMethodDescriptor("SaveCollectorAuditAsync",
    com.scaffold.audit.api.proto.SaveCollectorAuditRequest.class, com.scaffold.audit.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveCollectorAuditRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveQueryAuditMethod = new StubMethodDescriptor("SaveQueryAudit",
    com.scaffold.audit.api.proto.SaveQueryAuditRequest.class, com.scaffold.audit.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveQueryAuditRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveQueryAuditAsyncMethod = new StubMethodDescriptor("SaveQueryAudit",
    com.scaffold.audit.api.proto.SaveQueryAuditRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveQueryAuditRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveQueryAuditProxyAsyncMethod = new StubMethodDescriptor("SaveQueryAuditAsync",
    com.scaffold.audit.api.proto.SaveQueryAuditRequest.class, com.scaffold.audit.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveQueryAuditRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    static{
        serviceDescriptor.addMethod(saveCollectorAuditMethod);
        serviceDescriptor.addMethod(saveCollectorAuditProxyAsyncMethod);
        serviceDescriptor.addMethod(saveQueryAuditMethod);
        serviceDescriptor.addMethod(saveQueryAuditProxyAsyncMethod);
    }

    public static class RemoteAuditServiceStub implements RemoteAuditService, Destroyable {
        private final Invoker<RemoteAuditService> invoker;

        public RemoteAuditServiceStub(Invoker<RemoteAuditService> invoker) {
            this.invoker = invoker;
        }

        @Override
        public void $destroy() {
              invoker.destroy();
         }

        @Override
        public com.scaffold.audit.api.proto.BoolResponse saveCollectorAudit(com.scaffold.audit.api.proto.SaveCollectorAuditRequest request){
            return StubInvocationUtil.unaryCall(invoker, saveCollectorAuditMethod, request);
        }

        public CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveCollectorAuditAsync(com.scaffold.audit.api.proto.SaveCollectorAuditRequest request){
            return StubInvocationUtil.unaryCall(invoker, saveCollectorAuditAsyncMethod, request);
        }

        public void saveCollectorAudit(com.scaffold.audit.api.proto.SaveCollectorAuditRequest request, StreamObserver<com.scaffold.audit.api.proto.BoolResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, saveCollectorAuditMethod , request, responseObserver);
        }

        @Override
        public com.scaffold.audit.api.proto.BoolResponse saveQueryAudit(com.scaffold.audit.api.proto.SaveQueryAuditRequest request){
            return StubInvocationUtil.unaryCall(invoker, saveQueryAuditMethod, request);
        }

        public CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveQueryAuditAsync(com.scaffold.audit.api.proto.SaveQueryAuditRequest request){
            return StubInvocationUtil.unaryCall(invoker, saveQueryAuditAsyncMethod, request);
        }

        public void saveQueryAudit(com.scaffold.audit.api.proto.SaveQueryAuditRequest request, StreamObserver<com.scaffold.audit.api.proto.BoolResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, saveQueryAuditMethod , request, responseObserver);
        }
    }

    public static abstract class RemoteAuditServiceImplBase implements RemoteAuditService, ServerService<RemoteAuditService> {
        private <T, R> BiConsumer<T, StreamObserver<R>> syncToAsync(java.util.function.Function<T, R> syncFun) {
            return new BiConsumer<T, StreamObserver<R>>() {
                @Override
                public void accept(T t, StreamObserver<R> observer) {
                    try {
                        R ret = syncFun.apply(t);
                        observer.onNext(ret);
                        observer.onCompleted();
                    } catch (Throwable e) {
                        observer.onError(e);
                    }
                }
            };
        }

        @Override
        public CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveCollectorAuditAsync(com.scaffold.audit.api.proto.SaveCollectorAuditRequest request){
                return CompletableFuture.completedFuture(saveCollectorAudit(request));
        }

        @Override
        public CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveQueryAuditAsync(com.scaffold.audit.api.proto.SaveQueryAuditRequest request){
                return CompletableFuture.completedFuture(saveQueryAudit(request));
        }

        // This server stream type unary method is <b>only</b> used for generated stub to support async unary method.
        // It will not be called if you are NOT using Dubbo3 generated triple stub and <b>DO NOT</b> implement this method.

        public void saveCollectorAudit(com.scaffold.audit.api.proto.SaveCollectorAuditRequest request, StreamObserver<com.scaffold.audit.api.proto.BoolResponse> responseObserver){
            saveCollectorAuditAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        public void saveQueryAudit(com.scaffold.audit.api.proto.SaveQueryAuditRequest request, StreamObserver<com.scaffold.audit.api.proto.BoolResponse> responseObserver){
            saveQueryAuditAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public final Invoker<RemoteAuditService> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
            .getExtensionLoader(PathResolver.class)
            .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/SaveCollectorAudit");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/SaveCollectorAuditAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/SaveCollectorAudit");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/SaveCollectorAuditAsync");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/SaveQueryAudit");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/SaveQueryAuditAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/SaveQueryAudit");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/SaveQueryAuditAsync");
            BiConsumer<com.scaffold.audit.api.proto.SaveCollectorAuditRequest, StreamObserver<com.scaffold.audit.api.proto.BoolResponse>> saveCollectorAuditFunc = this::saveCollectorAudit;
            handlers.put(saveCollectorAuditMethod.getMethodName(), new UnaryStubMethodHandler<>(saveCollectorAuditFunc));
            BiConsumer<com.scaffold.audit.api.proto.SaveCollectorAuditRequest, StreamObserver<com.scaffold.audit.api.proto.BoolResponse>> saveCollectorAuditAsyncFunc = syncToAsync(this::saveCollectorAudit);
            handlers.put(saveCollectorAuditProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(saveCollectorAuditAsyncFunc));
            BiConsumer<com.scaffold.audit.api.proto.SaveQueryAuditRequest, StreamObserver<com.scaffold.audit.api.proto.BoolResponse>> saveQueryAuditFunc = this::saveQueryAudit;
            handlers.put(saveQueryAuditMethod.getMethodName(), new UnaryStubMethodHandler<>(saveQueryAuditFunc));
            BiConsumer<com.scaffold.audit.api.proto.SaveQueryAuditRequest, StreamObserver<com.scaffold.audit.api.proto.BoolResponse>> saveQueryAuditAsyncFunc = syncToAsync(this::saveQueryAudit);
            handlers.put(saveQueryAuditProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(saveQueryAuditAsyncFunc));

            return new StubInvoker<>(this, url, RemoteAuditService.class, handlers);
        }

        @Override
        public com.scaffold.audit.api.proto.BoolResponse saveCollectorAudit(com.scaffold.audit.api.proto.SaveCollectorAuditRequest request){
            throw unimplementedMethodException(saveCollectorAuditMethod);
        }

        @Override
        public com.scaffold.audit.api.proto.BoolResponse saveQueryAudit(com.scaffold.audit.api.proto.SaveQueryAuditRequest request){
            throw unimplementedMethodException(saveQueryAuditMethod);
        }

        @Override
        public final ServiceDescriptor getServiceDescriptor() {
            return serviceDescriptor;
        }
        private RpcException unimplementedMethodException(StubMethodDescriptor methodDescriptor) {
            return TriRpcStatus.UNIMPLEMENTED.withDescription(String.format("Method %s is unimplemented",
                "/" + serviceDescriptor.getInterfaceName() + "/" + methodDescriptor.getMethodName())).asException();
        }
    }
}
