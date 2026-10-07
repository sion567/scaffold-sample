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

package com.scaffold.system.api.proto;

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

public final class DubboRemoteUserServiceTriple {

    public static final String SERVICE_NAME = RemoteUserService.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor = new StubServiceDescriptor(SERVICE_NAME, RemoteUserService.class);

    static {
        org.apache.dubbo.rpc.protocol.tri.service.SchemaDescriptorRegistry.addSchemaDescriptor(SERVICE_NAME, RemoteUserProto.getDescriptor());
        StubSuppliers.addSupplier(SERVICE_NAME, DubboRemoteUserServiceTriple::newStub);
        StubSuppliers.addSupplier(RemoteUserService.JAVA_SERVICE_NAME,  DubboRemoteUserServiceTriple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(RemoteUserService.JAVA_SERVICE_NAME, serviceDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static RemoteUserService newStub(Invoker<?> invoker) {
        return new RemoteUserServiceStub((Invoker<RemoteUserService>)invoker);
    }

    private static final StubMethodDescriptor getUserInfoMethod = new StubMethodDescriptor("GetUserInfo",
    com.scaffold.system.api.proto.GetUserInfoRequest.class, com.scaffold.system.api.proto.GetUserInfoResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.GetUserInfoRequest::parseFrom,
    com.scaffold.system.api.proto.GetUserInfoResponse::parseFrom);

    private static final StubMethodDescriptor getUserInfoAsyncMethod = new StubMethodDescriptor("GetUserInfo",
    com.scaffold.system.api.proto.GetUserInfoRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.GetUserInfoRequest::parseFrom,
    com.scaffold.system.api.proto.GetUserInfoResponse::parseFrom);

    private static final StubMethodDescriptor getUserInfoProxyAsyncMethod = new StubMethodDescriptor("GetUserInfoAsync",
    com.scaffold.system.api.proto.GetUserInfoRequest.class, com.scaffold.system.api.proto.GetUserInfoResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.GetUserInfoRequest::parseFrom,
    com.scaffold.system.api.proto.GetUserInfoResponse::parseFrom);

    private static final StubMethodDescriptor registerUserInfoMethod = new StubMethodDescriptor("RegisterUserInfo",
    com.scaffold.system.api.proto.RegisterUserInfoRequest.class, com.scaffold.system.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.RegisterUserInfoRequest::parseFrom,
    com.scaffold.system.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor registerUserInfoAsyncMethod = new StubMethodDescriptor("RegisterUserInfo",
    com.scaffold.system.api.proto.RegisterUserInfoRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.RegisterUserInfoRequest::parseFrom,
    com.scaffold.system.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor registerUserInfoProxyAsyncMethod = new StubMethodDescriptor("RegisterUserInfoAsync",
    com.scaffold.system.api.proto.RegisterUserInfoRequest.class, com.scaffold.system.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.RegisterUserInfoRequest::parseFrom,
    com.scaffold.system.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor recordUserLoginMethod = new StubMethodDescriptor("RecordUserLogin",
    com.scaffold.system.api.proto.RecordUserLoginRequest.class, com.scaffold.system.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.RecordUserLoginRequest::parseFrom,
    com.scaffold.system.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor recordUserLoginAsyncMethod = new StubMethodDescriptor("RecordUserLogin",
    com.scaffold.system.api.proto.RecordUserLoginRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.RecordUserLoginRequest::parseFrom,
    com.scaffold.system.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor recordUserLoginProxyAsyncMethod = new StubMethodDescriptor("RecordUserLoginAsync",
    com.scaffold.system.api.proto.RecordUserLoginRequest.class, com.scaffold.system.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.system.api.proto.RecordUserLoginRequest::parseFrom,
    com.scaffold.system.api.proto.BoolResponse::parseFrom);

    static{
        serviceDescriptor.addMethod(getUserInfoMethod);
        serviceDescriptor.addMethod(getUserInfoProxyAsyncMethod);
        serviceDescriptor.addMethod(registerUserInfoMethod);
        serviceDescriptor.addMethod(registerUserInfoProxyAsyncMethod);
        serviceDescriptor.addMethod(recordUserLoginMethod);
        serviceDescriptor.addMethod(recordUserLoginProxyAsyncMethod);
    }

    public static class RemoteUserServiceStub implements RemoteUserService, Destroyable {
        private final Invoker<RemoteUserService> invoker;

        public RemoteUserServiceStub(Invoker<RemoteUserService> invoker) {
            this.invoker = invoker;
        }

        @Override
        public void $destroy() {
              invoker.destroy();
         }

        @Override
        public com.scaffold.system.api.proto.GetUserInfoResponse getUserInfo(com.scaffold.system.api.proto.GetUserInfoRequest request){
            return StubInvocationUtil.unaryCall(invoker, getUserInfoMethod, request);
        }

        public CompletableFuture<com.scaffold.system.api.proto.GetUserInfoResponse> getUserInfoAsync(com.scaffold.system.api.proto.GetUserInfoRequest request){
            return StubInvocationUtil.unaryCall(invoker, getUserInfoAsyncMethod, request);
        }

        public void getUserInfo(com.scaffold.system.api.proto.GetUserInfoRequest request, StreamObserver<com.scaffold.system.api.proto.GetUserInfoResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, getUserInfoMethod , request, responseObserver);
        }

        @Override
        public com.scaffold.system.api.proto.BoolResponse registerUserInfo(com.scaffold.system.api.proto.RegisterUserInfoRequest request){
            return StubInvocationUtil.unaryCall(invoker, registerUserInfoMethod, request);
        }

        public CompletableFuture<com.scaffold.system.api.proto.BoolResponse> registerUserInfoAsync(com.scaffold.system.api.proto.RegisterUserInfoRequest request){
            return StubInvocationUtil.unaryCall(invoker, registerUserInfoAsyncMethod, request);
        }

        public void registerUserInfo(com.scaffold.system.api.proto.RegisterUserInfoRequest request, StreamObserver<com.scaffold.system.api.proto.BoolResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, registerUserInfoMethod , request, responseObserver);
        }

        @Override
        public com.scaffold.system.api.proto.BoolResponse recordUserLogin(com.scaffold.system.api.proto.RecordUserLoginRequest request){
            return StubInvocationUtil.unaryCall(invoker, recordUserLoginMethod, request);
        }

        public CompletableFuture<com.scaffold.system.api.proto.BoolResponse> recordUserLoginAsync(com.scaffold.system.api.proto.RecordUserLoginRequest request){
            return StubInvocationUtil.unaryCall(invoker, recordUserLoginAsyncMethod, request);
        }

        public void recordUserLogin(com.scaffold.system.api.proto.RecordUserLoginRequest request, StreamObserver<com.scaffold.system.api.proto.BoolResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, recordUserLoginMethod , request, responseObserver);
        }
    }

    public static abstract class RemoteUserServiceImplBase implements RemoteUserService, ServerService<RemoteUserService> {
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
        public CompletableFuture<com.scaffold.system.api.proto.GetUserInfoResponse> getUserInfoAsync(com.scaffold.system.api.proto.GetUserInfoRequest request){
                return CompletableFuture.completedFuture(getUserInfo(request));
        }

        @Override
        public CompletableFuture<com.scaffold.system.api.proto.BoolResponse> registerUserInfoAsync(com.scaffold.system.api.proto.RegisterUserInfoRequest request){
                return CompletableFuture.completedFuture(registerUserInfo(request));
        }

        @Override
        public CompletableFuture<com.scaffold.system.api.proto.BoolResponse> recordUserLoginAsync(com.scaffold.system.api.proto.RecordUserLoginRequest request){
                return CompletableFuture.completedFuture(recordUserLogin(request));
        }

        // This server stream type unary method is <b>only</b> used for generated stub to support async unary method.
        // It will not be called if you are NOT using Dubbo3 generated triple stub and <b>DO NOT</b> implement this method.

        public void getUserInfo(com.scaffold.system.api.proto.GetUserInfoRequest request, StreamObserver<com.scaffold.system.api.proto.GetUserInfoResponse> responseObserver){
            getUserInfoAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        public void registerUserInfo(com.scaffold.system.api.proto.RegisterUserInfoRequest request, StreamObserver<com.scaffold.system.api.proto.BoolResponse> responseObserver){
            registerUserInfoAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        public void recordUserLogin(com.scaffold.system.api.proto.RecordUserLoginRequest request, StreamObserver<com.scaffold.system.api.proto.BoolResponse> responseObserver){
            recordUserLoginAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public final Invoker<RemoteUserService> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
            .getExtensionLoader(PathResolver.class)
            .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/GetUserInfo");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/GetUserInfoAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/GetUserInfo");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/GetUserInfoAsync");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/RegisterUserInfo");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/RegisterUserInfoAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/RegisterUserInfo");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/RegisterUserInfoAsync");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/RecordUserLogin");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/RecordUserLoginAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/RecordUserLogin");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/RecordUserLoginAsync");
            BiConsumer<com.scaffold.system.api.proto.GetUserInfoRequest, StreamObserver<com.scaffold.system.api.proto.GetUserInfoResponse>> getUserInfoFunc = this::getUserInfo;
            handlers.put(getUserInfoMethod.getMethodName(), new UnaryStubMethodHandler<>(getUserInfoFunc));
            BiConsumer<com.scaffold.system.api.proto.GetUserInfoRequest, StreamObserver<com.scaffold.system.api.proto.GetUserInfoResponse>> getUserInfoAsyncFunc = syncToAsync(this::getUserInfo);
            handlers.put(getUserInfoProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(getUserInfoAsyncFunc));
            BiConsumer<com.scaffold.system.api.proto.RegisterUserInfoRequest, StreamObserver<com.scaffold.system.api.proto.BoolResponse>> registerUserInfoFunc = this::registerUserInfo;
            handlers.put(registerUserInfoMethod.getMethodName(), new UnaryStubMethodHandler<>(registerUserInfoFunc));
            BiConsumer<com.scaffold.system.api.proto.RegisterUserInfoRequest, StreamObserver<com.scaffold.system.api.proto.BoolResponse>> registerUserInfoAsyncFunc = syncToAsync(this::registerUserInfo);
            handlers.put(registerUserInfoProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(registerUserInfoAsyncFunc));
            BiConsumer<com.scaffold.system.api.proto.RecordUserLoginRequest, StreamObserver<com.scaffold.system.api.proto.BoolResponse>> recordUserLoginFunc = this::recordUserLogin;
            handlers.put(recordUserLoginMethod.getMethodName(), new UnaryStubMethodHandler<>(recordUserLoginFunc));
            BiConsumer<com.scaffold.system.api.proto.RecordUserLoginRequest, StreamObserver<com.scaffold.system.api.proto.BoolResponse>> recordUserLoginAsyncFunc = syncToAsync(this::recordUserLogin);
            handlers.put(recordUserLoginProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(recordUserLoginAsyncFunc));

            return new StubInvoker<>(this, url, RemoteUserService.class, handlers);
        }

        @Override
        public com.scaffold.system.api.proto.GetUserInfoResponse getUserInfo(com.scaffold.system.api.proto.GetUserInfoRequest request){
            throw unimplementedMethodException(getUserInfoMethod);
        }

        @Override
        public com.scaffold.system.api.proto.BoolResponse registerUserInfo(com.scaffold.system.api.proto.RegisterUserInfoRequest request){
            throw unimplementedMethodException(registerUserInfoMethod);
        }

        @Override
        public com.scaffold.system.api.proto.BoolResponse recordUserLogin(com.scaffold.system.api.proto.RecordUserLoginRequest request){
            throw unimplementedMethodException(recordUserLoginMethod);
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
