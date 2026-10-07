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

public final class DubboRemoteLogServiceTriple {

    public static final String SERVICE_NAME = RemoteLogService.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor = new StubServiceDescriptor(SERVICE_NAME, RemoteLogService.class);

    static {
        org.apache.dubbo.rpc.protocol.tri.service.SchemaDescriptorRegistry.addSchemaDescriptor(SERVICE_NAME, RemoteLogProto.getDescriptor());
        StubSuppliers.addSupplier(SERVICE_NAME, DubboRemoteLogServiceTriple::newStub);
        StubSuppliers.addSupplier(RemoteLogService.JAVA_SERVICE_NAME,  DubboRemoteLogServiceTriple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(RemoteLogService.JAVA_SERVICE_NAME, serviceDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static RemoteLogService newStub(Invoker<?> invoker) {
        return new RemoteLogServiceStub((Invoker<RemoteLogService>)invoker);
    }

    private static final StubMethodDescriptor saveLogMethod = new StubMethodDescriptor("SaveLog",
    com.scaffold.audit.api.proto.SaveLogRequest.class, com.scaffold.audit.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveLogRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveLogAsyncMethod = new StubMethodDescriptor("SaveLog",
    com.scaffold.audit.api.proto.SaveLogRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveLogRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveLogProxyAsyncMethod = new StubMethodDescriptor("SaveLogAsync",
    com.scaffold.audit.api.proto.SaveLogRequest.class, com.scaffold.audit.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveLogRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveLogininforMethod = new StubMethodDescriptor("SaveLogininfor",
    com.scaffold.audit.api.proto.SaveLogininforRequest.class, com.scaffold.audit.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveLogininforRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveLogininforAsyncMethod = new StubMethodDescriptor("SaveLogininfor",
    com.scaffold.audit.api.proto.SaveLogininforRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveLogininforRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor saveLogininforProxyAsyncMethod = new StubMethodDescriptor("SaveLogininforAsync",
    com.scaffold.audit.api.proto.SaveLogininforRequest.class, com.scaffold.audit.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.audit.api.proto.SaveLogininforRequest::parseFrom,
    com.scaffold.audit.api.proto.BoolResponse::parseFrom);

    static{
        serviceDescriptor.addMethod(saveLogMethod);
        serviceDescriptor.addMethod(saveLogProxyAsyncMethod);
        serviceDescriptor.addMethod(saveLogininforMethod);
        serviceDescriptor.addMethod(saveLogininforProxyAsyncMethod);
    }

    public static class RemoteLogServiceStub implements RemoteLogService, Destroyable {
        private final Invoker<RemoteLogService> invoker;

        public RemoteLogServiceStub(Invoker<RemoteLogService> invoker) {
            this.invoker = invoker;
        }

        @Override
        public void $destroy() {
              invoker.destroy();
         }

        @Override
        public com.scaffold.audit.api.proto.BoolResponse saveLog(com.scaffold.audit.api.proto.SaveLogRequest request){
            return StubInvocationUtil.unaryCall(invoker, saveLogMethod, request);
        }

        public CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveLogAsync(com.scaffold.audit.api.proto.SaveLogRequest request){
            return StubInvocationUtil.unaryCall(invoker, saveLogAsyncMethod, request);
        }

        public void saveLog(com.scaffold.audit.api.proto.SaveLogRequest request, StreamObserver<com.scaffold.audit.api.proto.BoolResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, saveLogMethod , request, responseObserver);
        }

        @Override
        public com.scaffold.audit.api.proto.BoolResponse saveLogininfor(com.scaffold.audit.api.proto.SaveLogininforRequest request){
            return StubInvocationUtil.unaryCall(invoker, saveLogininforMethod, request);
        }

        public CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveLogininforAsync(com.scaffold.audit.api.proto.SaveLogininforRequest request){
            return StubInvocationUtil.unaryCall(invoker, saveLogininforAsyncMethod, request);
        }

        public void saveLogininfor(com.scaffold.audit.api.proto.SaveLogininforRequest request, StreamObserver<com.scaffold.audit.api.proto.BoolResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, saveLogininforMethod , request, responseObserver);
        }
    }

    public static abstract class RemoteLogServiceImplBase implements RemoteLogService, ServerService<RemoteLogService> {
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
        public CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveLogAsync(com.scaffold.audit.api.proto.SaveLogRequest request){
                return CompletableFuture.completedFuture(saveLog(request));
        }

        @Override
        public CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveLogininforAsync(com.scaffold.audit.api.proto.SaveLogininforRequest request){
                return CompletableFuture.completedFuture(saveLogininfor(request));
        }

        // This server stream type unary method is <b>only</b> used for generated stub to support async unary method.
        // It will not be called if you are NOT using Dubbo3 generated triple stub and <b>DO NOT</b> implement this method.

        public void saveLog(com.scaffold.audit.api.proto.SaveLogRequest request, StreamObserver<com.scaffold.audit.api.proto.BoolResponse> responseObserver){
            saveLogAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        public void saveLogininfor(com.scaffold.audit.api.proto.SaveLogininforRequest request, StreamObserver<com.scaffold.audit.api.proto.BoolResponse> responseObserver){
            saveLogininforAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public final Invoker<RemoteLogService> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
            .getExtensionLoader(PathResolver.class)
            .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/SaveLog");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/SaveLogAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/SaveLog");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/SaveLogAsync");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/SaveLogininfor");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/SaveLogininforAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/SaveLogininfor");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/SaveLogininforAsync");
            BiConsumer<com.scaffold.audit.api.proto.SaveLogRequest, StreamObserver<com.scaffold.audit.api.proto.BoolResponse>> saveLogFunc = this::saveLog;
            handlers.put(saveLogMethod.getMethodName(), new UnaryStubMethodHandler<>(saveLogFunc));
            BiConsumer<com.scaffold.audit.api.proto.SaveLogRequest, StreamObserver<com.scaffold.audit.api.proto.BoolResponse>> saveLogAsyncFunc = syncToAsync(this::saveLog);
            handlers.put(saveLogProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(saveLogAsyncFunc));
            BiConsumer<com.scaffold.audit.api.proto.SaveLogininforRequest, StreamObserver<com.scaffold.audit.api.proto.BoolResponse>> saveLogininforFunc = this::saveLogininfor;
            handlers.put(saveLogininforMethod.getMethodName(), new UnaryStubMethodHandler<>(saveLogininforFunc));
            BiConsumer<com.scaffold.audit.api.proto.SaveLogininforRequest, StreamObserver<com.scaffold.audit.api.proto.BoolResponse>> saveLogininforAsyncFunc = syncToAsync(this::saveLogininfor);
            handlers.put(saveLogininforProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(saveLogininforAsyncFunc));

            return new StubInvoker<>(this, url, RemoteLogService.class, handlers);
        }

        @Override
        public com.scaffold.audit.api.proto.BoolResponse saveLog(com.scaffold.audit.api.proto.SaveLogRequest request){
            throw unimplementedMethodException(saveLogMethod);
        }

        @Override
        public com.scaffold.audit.api.proto.BoolResponse saveLogininfor(com.scaffold.audit.api.proto.SaveLogininforRequest request){
            throw unimplementedMethodException(saveLogininforMethod);
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
