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
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.rest.Mapping;
import org.apache.dubbo.rpc.stub.annotations.GRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;

public interface RemoteUserService extends org.apache.dubbo.rpc.model.DubboStub {

    String JAVA_SERVICE_NAME = "com.scaffold.system.api.proto.RemoteUserService";
    String SERVICE_NAME = "com.scaffold.system.api.RemoteUserService";

    /**
     * <pre>
     *  按用户名查询登录用户（含角色/权限集合）；查不到时 code=500
     * </pre>
     */
    com.scaffold.system.api.proto.GetUserInfoResponse getUserInfo(com.scaffold.system.api.proto.GetUserInfoRequest request);

    CompletableFuture<com.scaffold.system.api.proto.GetUserInfoResponse> getUserInfoAsync(com.scaffold.system.api.proto.GetUserInfoRequest request);

    /**
     * <pre>
     *  自助注册用户；注册关闭或账号已存在时 code=500
     * </pre>
     */
    com.scaffold.system.api.proto.BoolResponse registerUserInfo(com.scaffold.system.api.proto.RegisterUserInfoRequest request);

    CompletableFuture<com.scaffold.system.api.proto.BoolResponse> registerUserInfoAsync(com.scaffold.system.api.proto.RegisterUserInfoRequest request);

    /**
     * <pre>
     *  记录登录 IP/登录时间
     * </pre>
     */
    com.scaffold.system.api.proto.BoolResponse recordUserLogin(com.scaffold.system.api.proto.RecordUserLoginRequest request);

    CompletableFuture<com.scaffold.system.api.proto.BoolResponse> recordUserLoginAsync(com.scaffold.system.api.proto.RecordUserLoginRequest request);
}
