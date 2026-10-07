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
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.rest.Mapping;
import org.apache.dubbo.rpc.stub.annotations.GRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;

public interface RemoteFileService extends org.apache.dubbo.rpc.model.DubboStub {

    String JAVA_SERVICE_NAME = "com.scaffold.file.api.proto.RemoteFileService";
    String SERVICE_NAME = "com.scaffold.file.api.RemoteFileService";

    /**
     * <pre>
     *  上传文件（二进制内容 + 文件名），成功返回文件信息
     * </pre>
     */
    com.scaffold.file.api.proto.UploadResponse upload(com.scaffold.file.api.proto.UploadRequest request);

    CompletableFuture<com.scaffold.file.api.proto.UploadResponse> uploadAsync(com.scaffold.file.api.proto.UploadRequest request);

    /**
     * <pre>
     *  删除文件；URL 非法时 code=500
     * </pre>
     */
    com.scaffold.file.api.proto.BoolResponse delete(com.scaffold.file.api.proto.DeleteFileRequest request);

    CompletableFuture<com.scaffold.file.api.proto.BoolResponse> deleteAsync(com.scaffold.file.api.proto.DeleteFileRequest request);
}
