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
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.rest.Mapping;
import org.apache.dubbo.rpc.stub.annotations.GRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;

public interface RemoteAuditService extends org.apache.dubbo.rpc.model.DubboStub {

    String JAVA_SERVICE_NAME = "com.scaffold.audit.api.proto.RemoteAuditService";
    String SERVICE_NAME = "com.scaffold.audit.api.RemoteAuditService";

    /**
     * <pre>
     *  保存采集留痕日志（collector 采集回执/失败记录）
     * </pre>
     */
    com.scaffold.audit.api.proto.BoolResponse saveCollectorAudit(com.scaffold.audit.api.proto.SaveCollectorAuditRequest request);

    CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveCollectorAuditAsync(com.scaffold.audit.api.proto.SaveCollectorAuditRequest request);

    /**
     * <pre>
     *  保存查询审计日志（谁、何时、查了谁、返回什么）
     * </pre>
     */
    com.scaffold.audit.api.proto.BoolResponse saveQueryAudit(com.scaffold.audit.api.proto.SaveQueryAuditRequest request);

    CompletableFuture<com.scaffold.audit.api.proto.BoolResponse> saveQueryAuditAsync(com.scaffold.audit.api.proto.SaveQueryAuditRequest request);
}
