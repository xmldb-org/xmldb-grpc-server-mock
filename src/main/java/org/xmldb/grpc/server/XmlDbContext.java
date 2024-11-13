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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.ErrorCodes;
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

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * The {@code XmlDbContext} class provides a context for managing and interacting with a test XML
 * database.
 * <p>
 * This class allows opening and closing collections, managing resources, and retrieving metadata
 * for these entities within the database
 * <p>
 * It maintains mappings of currently open collections and resources for efficient lookup and
 * operations.
 * <p>
 * The class primarily interacts with a test database setup and provides functionality to
 * programmatically interact with collections and resources, ensuring that operations such as
 * opening and closing collections, as well as fetching resource and collection information, can be
 * performed in a structured manner.
 * <p>
 * Logging is handled through SLF4J for tracking key operations and errors.
 * <p>
 * This class uses reactive programming paradigms with {@code Uni} and {@code Multi} from Mutiny for
 * returning asynchronous results of operations.
 */
public class XmlDbContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlDbContext.class);
  private static final Empty EMPTY = Empty.getDefaultInstance();
  private static final Pattern SOURCE_URL_PATTERN =
      Pattern.compile("xmldb:\\w+:(?://[\\w.:]+)?(?<path>/.+)");

  private final String dbUriPrefix;
  private final Map<HandleId, Collection> openCollections;
  private final Map<HandleId, Resource<?>> openResources;

  /**
   * Constructor for the XmlDbContext class. Initializes the database URI prefix and sets up
   * internal data structures for managing open collections and resources.
   *
   * @param dbUriPrefix The URI prefix used as a base for accessing database collections and
   *        resources.
   */
  public XmlDbContext(String dbUriPrefix) {
    this.dbUriPrefix = dbUriPrefix;
    openCollections = new HashMap<>();
    openResources = new HashMap<>();
  }

  private static HandleId createHandleId() {
    final UUID uuid = UUID.randomUUID();
    return HandleId.newBuilder().setMostSignificantBits(uuid.getMostSignificantBits())
        .setLeastSignificantBits(uuid.getLeastSignificantBits()).build();
  }

  private Uni<CollectionMeta> registerCollection(Collection collection, String collectionIdent)
      throws XMLDBException {
    if (collection == null) {
      LOGGER.info("Collection ({}) is null, returning null item instead of error", collectionIdent);
      return Uni.createFrom().nullItem();
    }
    final HandleId handleId = createHandleId();
    openCollections.put(handleId, collection);
    return Uni.createFrom()
        .item(CollectionMeta.newBuilder().setCollectionId(handleId)
            .setCreationTime(collection.getCreationTime().toEpochMilli())
            .setName(collection.getName()).build());
  }

  private Uni<ResourceMeta> registerResource(Resource<?> resource) throws XMLDBException {
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
      final String originalUri = rootCollectionName.getUri();
      final Matcher urlMatcher = SOURCE_URL_PATTERN.matcher(originalUri);
      if (urlMatcher.matches()) {
        final String path = urlMatcher.group("path");
        final Properties info = new Properties();
        LOGGER.info("Opening root collection: {}", path);
        info.putAll(rootCollectionName.getInfoMap());
        final Collection collection = DatabaseManager.getCollection(dbUriPrefix + path, info);
        return registerCollection(collection, path);
      } else {
        LOGGER.warn("Invalid URI: {}", originalUri);
        return Uni.createFrom().failure(new XMLDBException(ErrorCodes.INVALID_URI));
      }
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
      return registerCollection(collection, childName);
    } catch (XMLDBException e) {
      LOGGER.error("Error opening child collection {}", childCollectionName, e);
      return Uni.createFrom().failure(e);
    }
  }

  Uni<Empty> closeCollection(HandleId handleId) {
    try {
      final Collection collection = openCollections.remove(handleId);
      if (collection == null) {
        LOGGER.warn("Collection not found for handle {}", handleId);
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
      LOGGER.error("Error getting resource count for handle {}", handleId, e);
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

  Uni<ResourceMeta> openResource(ResourceId request) {
    try {
      final Collection collection = openCollections.get(request.getCollectionId());
      return registerResource(collection.getResource(request.getResourceId()));
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource for {}", request, e);
      return Uni.createFrom().failure(e);
    }
  }

  Uni<Empty> closeResource(HandleId handleId) {
    try {
      final Resource<?> resource = openResources.remove(handleId);
      if (resource == null) {
        LOGGER.warn("Resource {} not found", handleId);
      } else {
        LOGGER.info("Closing resource {}", resource.getId());
        resource.close();
      }
      return Uni.createFrom().item(EMPTY);
    } catch (XMLDBException e) {
      LOGGER.error("Error closing resource {}", handleId, e);
      return Uni.createFrom().failure(e);
    }
  }
}
