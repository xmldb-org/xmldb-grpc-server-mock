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
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.grpc.ChildCollectionName;
import org.xmldb.api.grpc.CollectionMeta;
import org.xmldb.api.grpc.Count;
import org.xmldb.api.grpc.Empty;
import org.xmldb.api.grpc.HandleId;
import org.xmldb.api.grpc.ResourceId;
import org.xmldb.api.grpc.ResourceMeta;
import org.xmldb.api.grpc.RootCollectionName;
import org.xmldb.grpc.server.mockdb.TestBinaryResource;
import org.xmldb.grpc.server.mockdb.TestCollection;
import org.xmldb.grpc.server.mockdb.TestDatabase;
import org.xmldb.grpc.server.mockdb.TestXMLResource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class XmlDbContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlDbContext.class);
  private static final Empty EMPTY = Empty.getDefaultInstance();

  private final TestDatabase db;
  private final Map<HandleId, Collection> openCollections;

  public XmlDbContext() {
    db = new TestDatabase();
    openCollections = new HashMap<>();
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
    HandleId handleId = createHandleId();
    openCollections.put(handleId, collection);
    return Uni.createFrom()
        .item(CollectionMeta.newBuilder().setCollectionId(handleId)
            .setCreationTime(collection.getCreationTime().toEpochMilli())
            .setName(collection.getName()).build());
  }

  public Uni<CollectionMeta> openCollection(RootCollectionName rootCollectionName) {
    try {
      final String path = rootCollectionName.getUri().replaceFirst("^.+/", "/");
      final Properties info = new Properties();
      LOGGER.info("Opening collection at path: {}", path);
      info.putAll(rootCollectionName.getInfoMap());
      Collection collection = db.getCollection(path, info);
      return registerCollection(collection);
    } catch (XMLDBException e) {
      LOGGER.error("Error opening collection {}", rootCollectionName, e);
      return Uni.createFrom().failure(e);
    }
  }

  public Uni<CollectionMeta> openCollection(ChildCollectionName collectionName) {
    try {
      Collection parentCollection = openCollections.get(collectionName.getCollectionId());
      Collection collection = parentCollection.getChildCollection(collectionName.getChildName());
      return registerCollection(collection);
    } catch (XMLDBException e) {
      LOGGER.error("Error opening collection {}", collectionName, e);
      return Uni.createFrom().failure(e);
    }
  }

  public Uni<Empty> closeCollection(HandleId request) {
    try {
      Collection collection = openCollections.remove(request);
      if (collection == null) {
        LOGGER.warn("Collection {} not found", request);
      } else {
        collection.close();
      }
      return Uni.createFrom().item(EMPTY);
    } catch (XMLDBException e) {
      return Uni.createFrom().failure(e);
    }
  }

  public Uni<Count> resourceCount(HandleId request) {
    try {
      Collection collection = openCollections.get(request);
      return Uni.createFrom()
          .item(Count.newBuilder().setCount(collection.getResourceCount()).build());
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource count for {}", request, e);
      return Uni.createFrom().failure(e);
    }
  }

  public Multi<ResourceId> listResources(HandleId request) {
    try {
      Collection collection = openCollections.get(request);
      List<ResourceId> resources = collection.listResources().stream()
          .map(resourceId -> ResourceId.newBuilder().setResourceId(resourceId).build()).toList();
      return Multi.createFrom().iterable(resources);
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource count for {}", request, e);
      return Multi.createFrom().failure(e);
    }
  }

  public Uni<Count> collectionCount(HandleId request) {
    try {
      Collection collection = openCollections.get(request);
      return Uni.createFrom()
          .item(Count.newBuilder().setCount(collection.getChildCollectionCount()).build());
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource count for {}", request, e);
      return Uni.createFrom().failure(e);
    }
  }

  public Multi<ChildCollectionName> childCollections(HandleId request) {
    try {
      Collection collection = openCollections.get(request);
      List<ChildCollectionName> resources =
          collection.listChildCollections().stream().map(name -> ChildCollectionName.newBuilder()
              .setCollectionId(request).setChildName(name).build()).toList();
      return Multi.createFrom().iterable(resources);
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource count for {}", request, e);
      return Multi.createFrom().failure(e);
    }
  }

  public Uni<ResourceMeta> resource(ResourceId request) {
    return Uni.createFrom().nullItem();
  }
}
