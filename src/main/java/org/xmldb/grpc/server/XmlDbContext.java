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
import java.util.stream.Collector;

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
import org.xmldb.api.grpc.CreateResourceMeta;
import org.xmldb.api.grpc.Empty;
import org.xmldb.api.grpc.HandleId;
import org.xmldb.api.grpc.ResourceData;
import org.xmldb.api.grpc.ResourceId;
import org.xmldb.api.grpc.ResourceLoadRequest;
import org.xmldb.api.grpc.ResourceMeta;
import org.xmldb.api.grpc.ResourceStoreRequest;
import org.xmldb.api.grpc.ResourceTransferStatus;
import org.xmldb.api.grpc.ResourceType;
import org.xmldb.api.grpc.RootCollectionName;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.XMLResource;

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
  private final Map<HandleId, ResourceEntry> openResources;

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

  private Uni<ResourceMeta> registerResource(Resource resource, String resourceIdString)
      throws XMLDBException {
    final HandleId handleId = createHandleId();
    openResources.put(handleId, new ResourceEntry(resource, resourceIdString));
    return Uni.createFrom().item(createResourceMeta(resource, handleId));
  }

  private ResourceMeta createResourceMeta(Resource resource, HandleId handleId)
      throws XMLDBException {
    return ResourceMeta.newBuilder().setResourceId(handleId).setContentType(contentTypeOf(resource))
        .setType(convert(resource.getResourceType()))
        .setCreationTime(resource.getCreationTime().toEpochMilli())
        .setLastModificationTime(resource.getLastModificationTime().toEpochMilli()).build();
  }

  private String contentTypeOf(Resource resource) {
    return switch (resource.getResourceType()) {
      case BINARY_RESOURCE -> "application/octet-stream";
      case XML_RESOURCE -> "text/xml";
    };
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

  Uni<ResourceMeta> createResource(CreateResourceMeta createMeta) {
    try {
      final ResourceId resourceId = createMeta.getResourceId();
      final Collection collection = openCollections.get(resourceId.getCollectionId());
      final String resourceIdString = resourceId.getResourceId();
      final Resource resource =
          collection.createResource(resourceIdString, resourceTypeOf(createMeta.getType()));
      return registerResource(resource, resourceIdString);
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource for {}", createMeta.getResourceId(), e);
      return Uni.createFrom().failure(e);
    }
  }

  @SuppressWarnings("unchecked")
  <R> Class<R> resourceTypeOf(ResourceType type) throws XMLDBException {
    return switch (type) {
      case XML -> (Class<R>) XMLResource.class;
      case BINARY -> (Class<R>) BinaryResource.class;
      case UNRECOGNIZED -> throw new XMLDBException(ErrorCodes.NO_SUCH_RESOURCE);
    };
  }

  Uni<ResourceMeta> openResource(ResourceId resourceId) {
    try {
      final Collection collection = openCollections.get(resourceId.getCollectionId());
      final String resourceIdString = resourceId.getResourceId();
      return registerResource(collection.getResource(resourceIdString), resourceIdString);
    } catch (XMLDBException e) {
      LOGGER.error("Error getting resource for {}", resourceId, e);
      return Uni.createFrom().failure(e);
    }
  }

  Uni<Empty> closeResource(HandleId handleId) {
    try {
      final ResourceEntry resourceEntry = openResources.remove(handleId);
      if (resourceEntry == null) {
        LOGGER.warn("Resource {} not found", handleId);
      } else {
        LOGGER.info("Closing resourceEntry {}", resourceEntry.id());
        resourceEntry.close();
      }
      return Uni.createFrom().item(EMPTY);
    } catch (XMLDBException e) {
      LOGGER.error("Error closing resource {}", handleId, e);
      return Uni.createFrom().failure(e);
    }
  }

  Uni<ResourceId> createId(HandleId handleId) {
    try {
      final Collection collection = openCollections.get(handleId);
      return Uni.createFrom().item(ResourceId.newBuilder().setCollectionId(handleId)
          .setResourceId(collection.createId()).build());
    } catch (XMLDBException e) {
      LOGGER.error("Error creating new id for {}", handleId, e);
      return Uni.createFrom().failure(e);
    }
  }

  Uni<Empty> removeResource(HandleId handleId) {
    try (final ResourceEntry resourceEntry = openResources.get(handleId)) {
      final Collection collection = resourceEntry.getParentCollection();
      collection.removeResource(resourceEntry.resource());
      return Uni.createFrom().item(EMPTY);
    } catch (XMLDBException e) {
      LOGGER.error("Error creating new id for {}", handleId, e);
      return Uni.createFrom().failure(e);
    }
  }

  Multi<ResourceData> loadResourceData(ResourceLoadRequest loadRequest) {
    final ResourceEntry resourceEntry = openResources.get(loadRequest.getResourceId());
    if (resourceEntry == null) {
      return Multi.createFrom().failure(new XMLDBException(ErrorCodes.NO_SUCH_RESOURCE));
    }
    final ResourceData.Builder builder = ResourceData.newBuilder();
    return Multi.createFrom().publisher(subscriber -> new LoadResourceDataSubscription(builder,
        loadRequest.getChunkSize(), resourceEntry::getContentAsStream, subscriber));
  }

  Uni<ResourceTransferStatus> storeResourceData(Multi<ResourceStoreRequest> resourceData) {
    try (final StoreResourceContext resourceContext =
        new StoreResourceContext(openResources::get, this::createResourceMeta)) {
      return resourceData.collect()
          .with(Collector.of(() -> resourceContext, StoreResourceContext::accept,
              StoreResourceContext::combine, StoreResourceContext::finish));
    } catch (XMLDBException e) {
      LOGGER.error("Error storing resource {}", resourceData, e);
      return Uni.createFrom().failure(e);
    }
  }
}
