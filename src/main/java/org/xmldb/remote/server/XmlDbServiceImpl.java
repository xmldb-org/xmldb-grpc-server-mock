/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.remote.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.grpc.CountRequest;
import org.xmldb.api.grpc.CountValue;
import org.xmldb.api.grpc.EmptyRequest;
import org.xmldb.api.grpc.IdRequest;
import org.xmldb.api.grpc.IdValue;
import org.xmldb.api.grpc.Resource;
import org.xmldb.api.grpc.ResourceRequest;
import org.xmldb.api.grpc.SystemInfo;
import org.xmldb.api.grpc.XmlDbService;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class XmlDbServiceImpl implements XmlDbService {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlDbServiceImpl.class);

  @Override
  public Uni<SystemInfo> systemInfo(EmptyRequest request) {
    LOGGER.info("getSystemInfo({})", request);
    final var systemInfoBuilder = SystemInfo.newBuilder();
    systemInfoBuilder.getJavaVersionBuilder() //
        .setDate(System.getProperty("java.version.date")) //
        .setVersion(System.getProperty("java.version")) //
        .setVendor(System.getProperty("java.vendor")) //
        .setVendorVersion(System.getProperty("java.vendor.version"));
    systemInfoBuilder.setSystemVersion("1.0.0");
    return Uni.createFrom().item(systemInfoBuilder.build());
  }

  @Override
  public Uni<CountValue> collectionCount(CountRequest request) {
    return Uni.createFrom().nullItem();
  }

  @Override
  public Uni<CountValue> documentCount(EmptyRequest request) {
    return Uni.createFrom().nullItem();
  }

  @Override
  public Uni<Resource> resource(ResourceRequest request) {
    return Uni.createFrom().nullItem();
  }

  @Override
  public Multi<IdValue> collectionIds(IdRequest request) {
    return Multi.createFrom().empty();
  }

  @Override
  public Multi<IdValue> documentIds(IdRequest request) {
    return Multi.createFrom().empty();
  }
}
