/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.grpc.server;

import static org.xmldb.grpc.server.AuthenticationConstants.USERNAME;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import jakarta.inject.Inject;

/**
 * A gRPC service implementation for handling operations in an XML database. This class provides
 * methods to interact with collections, resources, and retrieve database metadata. It implements
 * the XmlDbService interface and uses Mutiny for reactive programming.
 */
@GrpcService
public class GrpcXmlDbService implements XmlDbService {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcXmlDbService.class);

  private final ConcurrentMap<String, XmlDbContext> contexts;
  private final String dbUriPrefix;

  /**
   * Constructs a new instance of GrpcXmlDbService, initializing the service with the specified
   * database URI prefix.
   *
   * @param dbUriPrefix the database URI prefix used for constructing database-specific URIs. It is
   *        injected from the application configuration using the key "xmldb.uri.prefix".
   */
  @Inject
  public GrpcXmlDbService(@ConfigProperty(name = "xmldb.uri.prefix") String dbUriPrefix) {
    this.dbUriPrefix = dbUriPrefix;
    this.contexts = new ConcurrentHashMap<>();
  }

  private XmlDbContext context() {
    return contexts.computeIfAbsent(USERNAME.get(), this::initializeContext);
  }

  private XmlDbContext initializeContext(String username) {
    return new XmlDbContext(dbUriPrefix);
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
    LOGGER.debug("openRootCollection({})", request);
    return context().openCollection(request);
  }

  @Override
  public Uni<CollectionMeta> openChildCollection(ChildCollectionName request) {
    LOGGER.debug("openChildCollection({})", request);
    return context().openCollection(request);
  }

  @Override
  public Uni<Empty> closeCollection(HandleId request) {
    LOGGER.debug("closeCollection({})", request);
    return context().closeCollection(request);
  }

  @Override
  public Uni<Count> collectionCount(HandleId request) {
    LOGGER.debug("collectionCount({})", request);
    return context().collectionCount(request);
  }

  @Override
  public Uni<Count> resourceCount(HandleId request) {
    LOGGER.debug("resourceCount({})", request);
    return context().resourceCount(request);
  }

  @Override
  public Multi<ChildCollectionName> childCollections(HandleId request) {
    LOGGER.debug("childCollections({})", request);
    return context().childCollections(request);
  }

  @Override
  public Multi<ResourceId> listResources(HandleId request) {
    LOGGER.debug("listResources({})", request);
    return context().listResources(request);
  }

  @Override
  public Uni<ResourceMeta> openResource(ResourceId request) {
    LOGGER.debug("resource({})", request);
    return context().openResource(request);
  }

  @Override
  public Uni<Empty> closeResource(HandleId request) {
    LOGGER.debug("resource({})", request);
    return context().closeResource(request);
  }

  @Override
  public Multi<Data> resourceData(HandleId request) {
    LOGGER.info("resourceData({})", request);
    return Multi.createFrom().empty();
  }
}
