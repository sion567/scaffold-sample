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

package com.scaffold.file.api.proto;

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

public final class DubboRemoteFileServiceTriple {

    public static final String SERVICE_NAME = RemoteFileService.SERVICE_NAME;

    private static final StubServiceDescriptor serviceDescriptor = new StubServiceDescriptor(SERVICE_NAME, RemoteFileService.class);

    static {
        org.apache.dubbo.rpc.protocol.tri.service.SchemaDescriptorRegistry.addSchemaDescriptor(SERVICE_NAME, RemoteFileProto.getDescriptor());
        StubSuppliers.addSupplier(SERVICE_NAME, DubboRemoteFileServiceTriple::newStub);
        StubSuppliers.addSupplier(RemoteFileService.JAVA_SERVICE_NAME,  DubboRemoteFileServiceTriple::newStub);
        StubSuppliers.addDescriptor(SERVICE_NAME, serviceDescriptor);
        StubSuppliers.addDescriptor(RemoteFileService.JAVA_SERVICE_NAME, serviceDescriptor);
    }

    @SuppressWarnings("unchecked")
    public static RemoteFileService newStub(Invoker<?> invoker) {
        return new RemoteFileServiceStub((Invoker<RemoteFileService>)invoker);
    }

    private static final StubMethodDescriptor uploadMethod = new StubMethodDescriptor("Upload",
    com.scaffold.file.api.proto.UploadRequest.class, com.scaffold.file.api.proto.UploadResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.file.api.proto.UploadRequest::parseFrom,
    com.scaffold.file.api.proto.UploadResponse::parseFrom);

    private static final StubMethodDescriptor uploadAsyncMethod = new StubMethodDescriptor("Upload",
    com.scaffold.file.api.proto.UploadRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.file.api.proto.UploadRequest::parseFrom,
    com.scaffold.file.api.proto.UploadResponse::parseFrom);

    private static final StubMethodDescriptor uploadProxyAsyncMethod = new StubMethodDescriptor("UploadAsync",
    com.scaffold.file.api.proto.UploadRequest.class, com.scaffold.file.api.proto.UploadResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.file.api.proto.UploadRequest::parseFrom,
    com.scaffold.file.api.proto.UploadResponse::parseFrom);

    private static final StubMethodDescriptor deleteMethod = new StubMethodDescriptor("Delete",
    com.scaffold.file.api.proto.DeleteFileRequest.class, com.scaffold.file.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(),obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.file.api.proto.DeleteFileRequest::parseFrom,
    com.scaffold.file.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor deleteAsyncMethod = new StubMethodDescriptor("Delete",
    com.scaffold.file.api.proto.DeleteFileRequest.class, java.util.concurrent.CompletableFuture.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.file.api.proto.DeleteFileRequest::parseFrom,
    com.scaffold.file.api.proto.BoolResponse::parseFrom);

    private static final StubMethodDescriptor deleteProxyAsyncMethod = new StubMethodDescriptor("DeleteAsync",
    com.scaffold.file.api.proto.DeleteFileRequest.class, com.scaffold.file.api.proto.BoolResponse.class, MethodDescriptor.RpcType.UNARY,
    obj -> ((com.google.protobuf.Message) obj).toByteArray(), obj -> ((com.google.protobuf.Message) obj).toByteArray(), com.scaffold.file.api.proto.DeleteFileRequest::parseFrom,
    com.scaffold.file.api.proto.BoolResponse::parseFrom);

    static{
        serviceDescriptor.addMethod(uploadMethod);
        serviceDescriptor.addMethod(uploadProxyAsyncMethod);
        serviceDescriptor.addMethod(deleteMethod);
        serviceDescriptor.addMethod(deleteProxyAsyncMethod);
    }

    public static class RemoteFileServiceStub implements RemoteFileService, Destroyable {
        private final Invoker<RemoteFileService> invoker;

        public RemoteFileServiceStub(Invoker<RemoteFileService> invoker) {
            this.invoker = invoker;
        }

        @Override
        public void $destroy() {
              invoker.destroy();
         }

        @Override
        public com.scaffold.file.api.proto.UploadResponse upload(com.scaffold.file.api.proto.UploadRequest request){
            return StubInvocationUtil.unaryCall(invoker, uploadMethod, request);
        }

        public CompletableFuture<com.scaffold.file.api.proto.UploadResponse> uploadAsync(com.scaffold.file.api.proto.UploadRequest request){
            return StubInvocationUtil.unaryCall(invoker, uploadAsyncMethod, request);
        }

        public void upload(com.scaffold.file.api.proto.UploadRequest request, StreamObserver<com.scaffold.file.api.proto.UploadResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, uploadMethod , request, responseObserver);
        }

        @Override
        public com.scaffold.file.api.proto.BoolResponse delete(com.scaffold.file.api.proto.DeleteFileRequest request){
            return StubInvocationUtil.unaryCall(invoker, deleteMethod, request);
        }

        public CompletableFuture<com.scaffold.file.api.proto.BoolResponse> deleteAsync(com.scaffold.file.api.proto.DeleteFileRequest request){
            return StubInvocationUtil.unaryCall(invoker, deleteAsyncMethod, request);
        }

        public void delete(com.scaffold.file.api.proto.DeleteFileRequest request, StreamObserver<com.scaffold.file.api.proto.BoolResponse> responseObserver){
            StubInvocationUtil.unaryCall(invoker, deleteMethod , request, responseObserver);
        }
    }

    public static abstract class RemoteFileServiceImplBase implements RemoteFileService, ServerService<RemoteFileService> {
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
        public CompletableFuture<com.scaffold.file.api.proto.UploadResponse> uploadAsync(com.scaffold.file.api.proto.UploadRequest request){
                return CompletableFuture.completedFuture(upload(request));
        }

        @Override
        public CompletableFuture<com.scaffold.file.api.proto.BoolResponse> deleteAsync(com.scaffold.file.api.proto.DeleteFileRequest request){
                return CompletableFuture.completedFuture(delete(request));
        }

        // This server stream type unary method is <b>only</b> used for generated stub to support async unary method.
        // It will not be called if you are NOT using Dubbo3 generated triple stub and <b>DO NOT</b> implement this method.

        public void upload(com.scaffold.file.api.proto.UploadRequest request, StreamObserver<com.scaffold.file.api.proto.UploadResponse> responseObserver){
            uploadAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        public void delete(com.scaffold.file.api.proto.DeleteFileRequest request, StreamObserver<com.scaffold.file.api.proto.BoolResponse> responseObserver){
            deleteAsync(request).whenComplete((r, t) -> {
                if (t != null) {
                    responseObserver.onError(t);
                } else {
                    responseObserver.onNext(r);
                    responseObserver.onCompleted();
                }
            });
        }

        @Override
        public final Invoker<RemoteFileService> getInvoker(URL url) {
            PathResolver pathResolver = url.getOrDefaultFrameworkModel()
            .getExtensionLoader(PathResolver.class)
            .getDefaultExtension();
            Map<String, StubMethodHandler<?, ?>> handlers = new HashMap<>();
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/Upload");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/UploadAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/Upload");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/UploadAsync");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/Delete");
            pathResolver.addNativeStub( "/" + SERVICE_NAME + "/DeleteAsync");
            // for compatibility
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/Delete");
            pathResolver.addNativeStub( "/" + JAVA_SERVICE_NAME + "/DeleteAsync");
            BiConsumer<com.scaffold.file.api.proto.UploadRequest, StreamObserver<com.scaffold.file.api.proto.UploadResponse>> uploadFunc = this::upload;
            handlers.put(uploadMethod.getMethodName(), new UnaryStubMethodHandler<>(uploadFunc));
            BiConsumer<com.scaffold.file.api.proto.UploadRequest, StreamObserver<com.scaffold.file.api.proto.UploadResponse>> uploadAsyncFunc = syncToAsync(this::upload);
            handlers.put(uploadProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(uploadAsyncFunc));
            BiConsumer<com.scaffold.file.api.proto.DeleteFileRequest, StreamObserver<com.scaffold.file.api.proto.BoolResponse>> deleteFunc = this::delete;
            handlers.put(deleteMethod.getMethodName(), new UnaryStubMethodHandler<>(deleteFunc));
            BiConsumer<com.scaffold.file.api.proto.DeleteFileRequest, StreamObserver<com.scaffold.file.api.proto.BoolResponse>> deleteAsyncFunc = syncToAsync(this::delete);
            handlers.put(deleteProxyAsyncMethod.getMethodName(), new UnaryStubMethodHandler<>(deleteAsyncFunc));

            return new StubInvoker<>(this, url, RemoteFileService.class, handlers);
        }

        @Override
        public com.scaffold.file.api.proto.UploadResponse upload(com.scaffold.file.api.proto.UploadRequest request){
            throw unimplementedMethodException(uploadMethod);
        }

        @Override
        public com.scaffold.file.api.proto.BoolResponse delete(com.scaffold.file.api.proto.DeleteFileRequest request){
            throw unimplementedMethodException(deleteMethod);
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
