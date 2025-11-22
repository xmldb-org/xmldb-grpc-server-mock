/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.grpc.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.grpc.ChildCollectionName;
import org.xmldb.api.grpc.CollectionMeta;
import org.xmldb.api.grpc.Count;
import org.xmldb.api.grpc.Empty;
import org.xmldb.api.grpc.HandleId;
import org.xmldb.api.grpc.ResourceId;
import org.xmldb.api.grpc.ResourceMeta;
import org.xmldb.api.grpc.ResourceType;
import org.xmldb.api.grpc.RootCollectionName;
import org.xmldb.mockdb.TestBinaryResource;
import org.xmldb.mockdb.TestCollection;
import org.xmldb.mockdb.TestDatabase;
import org.xmldb.mockdb.TestXMLResource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class XmlDbContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlDbContext.class);
  private static final Empty EMPTY = Empty.getDefaultInstance();

  private final TestDatabase db;
  private final Map<HandleId, Collection> openCollections;
  private final Map<HandleId, Resource<?>> openResources;

  public XmlDbContext() {
    db = new TestDatabase();
    openCollections = new HashMap<>();
    openResources = new HashMap<>();
    TestCollection rootCollection = db.addCollection("/db");
    rootCollection.addResource("test1.xml", TestXMLResource::new);
    rootCollection.addResource("test2.xml", TestBinaryResource::new);
    TestCollection subCollection = db.addCollection("/db/child");
    subCollection.addResource("test3.xml", TestBinaryResource::new);
    rootCollection.addCollection("child", subCollection);
  }

  private static HandleId createHandleId() {
    UUID uuid = UUID.randomUUID();
    return HandleId.newBuilder().setMostSignificantBits(uuid.getMostSignificantBits())
        .setLeastSignificantBits(uuid.getLeastSignificantBits()).build();
  }

  private Uni<CollectionMeta> registerCollection(Collection collection) throws XMLDBException {
    final HandleId handleId = createHandleId();
    openCollections.put(handleId, collection);
    return Uni.createFrom()
        .item(CollectionMeta.newBuilder().setCollectionId(handleId)
            .setCreationTime(collection.getCreationTime().toEpochMilli())
            .setName(collection.getName()).build());
  }

  private Uni<ResourceMeta> registerResource(Resource resource) throws XMLDBException {
    final HandleId handleId = createHandleId();
    openResources.put(handleId, resource);
    return Uni.createFrom()
        .item(ResourceMeta.newBuilder().setType(convert(resource.getResourceType()))
            .setCreationTime(resource.getCreationTime().toEpochMilli())
            .setLastModificationTime(resource.getLastModificationTime().toEpochMilli()).build());
  }

  private ResourceType convert(org.xmldb.api.base.ResourceType resourceType) {
    return switch (resourceType) {
      case BINARY_RESOURCE -> ResourceType.BINARY;
      case XML_RESOURCE -> ResourceType.XML;
    };
  }

  Uni<CollectionMeta> openCollection(RootCollectionName rootCollectionName) {
    try {
      final String path = rootCollectionName.getUri().replaceFirst("^.+/", "/");
      final Properties info = new Properties();
      LOGGER.info("Opening root collection: {}", path);
      info.putAll(rootCollectionName.getInfoMap());
      final Collection collection = db.getCollection(path, info);
      return registerCollection(collection);
    } catch (XMLDBException e) {
      LOGGER.error("Error opening root collection {}", rootCollectionName, e);
      return Uni.createFrom().failure(e);
    }
  }

  Uni<CollectionMeta> openCollection(ChildCollectionName childCollectionName) {
    try {
      final Collection parentCollection =
          openCollections.get(childCollectionName.getCollectionId());
      final String childName = childCollectionName.getChildName();
      LOGGER.info("Opening child collection: {}/{}", parentCollection.getName(), childName);
      final Collection collection = parentCollection.getChildCollection(childName);
      return registerCollection(collection);
    } catch (XMLDBException e) {
      LOGGER.error("Error opening child collection {}", childCollectionName, e);
      return Uni.createFrom().failure(e);
    }
  }

  Uni<Empty> closeCollection(HandleId handleId) {
    try {
      final Collection collection = openCollections.remove(handleId);
      if (collection == null) {
        LOGGER.warn("Collection {} not found", handleId);
      } else {
        LOGGER.info("Closing Collection {}", collection.getName());
        collection.close();
      }
      return Uni.createFrom().item(EMPTY);
    } catch (XMLDBException e) {
      LOGGER.error("Error closing collection {}", handleId, e);
      return Uni.createFrom().failure(e);
    }
  }

  Uni<Count> resourceCount(HandleId handleId) {
    try {
      final Collection collection = openCollections.get(handleId);
      return Uni.createFrom()
          .item(Count.newBuilder().setCount(collection.getResourceCount()).build());
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource count for {}", handleId, e);
      return Uni.createFrom().failure(e);
    }
  }

  Multi<ResourceId> listResources(HandleId handleId) {
    try {
      final Collection collection = openCollections.get(handleId);
      final List<ResourceId> resources = collection.listResources().stream()
          .map(resourceId -> ResourceId.newBuilder().setResourceId(resourceId).build()).toList();
      return Multi.createFrom().iterable(resources);
    } catch (XMLDBException e) {
      LOGGER.error("Error listing resources for {}", handleId, e);
      return Multi.createFrom().failure(e);
    }
  }

  Uni<Count> collectionCount(HandleId handleId) {
    try {
      final Collection collection = openCollections.get(handleId);
      return Uni.createFrom()
          .item(Count.newBuilder().setCount(collection.getChildCollectionCount()).build());
    } catch (XMLDBException e) {
      LOGGER.error("Error getting collection count for {}", handleId, e);
      return Uni.createFrom().failure(e);
    }
  }

  Multi<ChildCollectionName> childCollections(HandleId handleId) {
    try {
      final Collection collection = openCollections.get(handleId);
      List<ChildCollectionName> resources =
          collection.listChildCollections().stream().map(name -> ChildCollectionName.newBuilder()
              .setCollectionId(handleId).setChildName(name).build()).toList();
      return Multi.createFrom().iterable(resources);
    } catch (XMLDBException e) {
      LOGGER.error("Error getting child collections for {}", handleId, e);
      return Multi.createFrom().failure(e);
    }
  }

  Uni<ResourceMeta> resource(ResourceId request) {
    try {
      final Collection collection = openCollections.get(request.getCollectionId());
      return registerResource(collection.getResource(request.getResourceId()));
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource for {}", request, e);
      return Uni.createFrom().failure(e);
    }
  }
}
