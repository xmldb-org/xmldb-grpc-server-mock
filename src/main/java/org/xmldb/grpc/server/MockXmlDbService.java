/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.grpc.server;

import static org.xmldb.grpc.server.AuthenticationConstants.CONTEXT_USERNAME_KEY;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.grpc.ChildCollectionName;
import org.xmldb.api.grpc.CollectionMeta;
import org.xmldb.api.grpc.Count;
import org.xmldb.api.grpc.Data;
import org.xmldb.api.grpc.Empty;
import org.xmldb.api.grpc.HandleId;
import org.xmldb.api.grpc.ResourceId;
import org.xmldb.api.grpc.ResourceMeta;
import org.xmldb.api.grpc.RootCollectionName;
import org.xmldb.api.grpc.SystemInfo;
import org.xmldb.api.grpc.XmlDbService;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@GrpcService
public class MockXmlDbService implements XmlDbService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MockXmlDbService.class);

  private final ConcurrentMap<String, XmlDbContext> contexts;

  public MockXmlDbService() {
    this.contexts = new ConcurrentHashMap<>();
  }

  @Override
  public Uni<SystemInfo> systemInfo(Empty request) {
    LOGGER.info("systemInfo({})", request);
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
  public Uni<CollectionMeta> openRootCollection(RootCollectionName request) {
    LOGGER.info("openRootCollection({})", request);
    return contexts.computeIfAbsent(CONTEXT_USERNAME_KEY.get(), username -> new XmlDbContext())
        .openCollection(request);
  }

  @Override
  public Uni<CollectionMeta> openChildCollection(ChildCollectionName request) {
    return contexts.computeIfAbsent(CONTEXT_USERNAME_KEY.get(), username -> new XmlDbContext())
        .openCollection(request);
  }

  @Override
  public Uni<Empty> closeCollection(HandleId request) {
    return contexts.computeIfAbsent(CONTEXT_USERNAME_KEY.get(), username -> new XmlDbContext())
        .closeCollection(request);
  }

  @Override
  public Uni<Count> collectionCount(HandleId request) {
    LOGGER.info("collectionCount({})", request);
    return contexts.computeIfAbsent(CONTEXT_USERNAME_KEY.get(), username -> new XmlDbContext())
        .collectionCount(request);
  }

  @Override
  public Uni<Count> resourceCount(HandleId request) {
    LOGGER.info("resourceCount({})", request);
    return contexts.computeIfAbsent(CONTEXT_USERNAME_KEY.get(), username -> new XmlDbContext())
        .resourceCount(request);
  }

  @Override
  public Uni<ResourceMeta> resource(ResourceId request) {
    LOGGER.info("resource({})", request);
    return contexts.computeIfAbsent(CONTEXT_USERNAME_KEY.get(), username -> new XmlDbContext())
        .resource(request);
  }

  @Override
  public Multi<ChildCollectionName> childCollections(HandleId request) {
    LOGGER.info("childCollections({})", request);
    return contexts.computeIfAbsent(CONTEXT_USERNAME_KEY.get(), username -> new XmlDbContext())
        .childCollections(request);
  }

  @Override
  public Multi<ResourceId> listResources(HandleId request) {
    LOGGER.info("listResources({})", request);
    return contexts.computeIfAbsent(CONTEXT_USERNAME_KEY.get(), username -> new XmlDbContext())
        .listResources(request);
  }

  @Override
  public Multi<Data> resourceData(HandleId request) {
    return Multi.createFrom().empty();
  }
}
