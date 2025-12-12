/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.grpc.HandleId;
import org.xmldb.api.grpc.ResourceStoreRequest;
import org.xmldb.api.grpc.ResourceTransferStatus;
import org.xmldb.api.grpc.TransferStatus;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.XMLResource;

class StoreResourceContext implements Consumer<ResourceStoreRequest>, AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(StoreResourceContext.class);

  private final Function<HandleId, ResourceEntry> entryFunction;
  private final ResourceMetaFunction resourceMetaFunction;

  private TransferStatus transferStatus;
  private HandleId resourceId;
  private ResourceEntry resourceEntry;
  private ByteArrayOutputStream byteOutputStream;

  StoreResourceContext(Function<HandleId, ResourceEntry> entryFunction,
      ResourceMetaFunction resourceMetaFunction) {
    this.entryFunction = entryFunction;
    this.resourceMetaFunction = resourceMetaFunction;
    this.transferStatus = TransferStatus.UNDEFINED;
  }

  @Override
  public void accept(ResourceStoreRequest resourceStoreRequest) {
    LOGGER.debug("accept({})", resourceStoreRequest);
    if (resourceEntry == null) {
      resourceId = resourceStoreRequest.getResourceId();
      resourceEntry = entryFunction.apply(resourceId);
      if (resourceEntry == null) {
        transferStatus = TransferStatus.INVALID;
        LOGGER.warn("Resource not found to write to");
      } else {
        transferStatus = TransferStatus.SUCCESS;
        byteOutputStream = new ByteArrayOutputStream();
        LOGGER.debug("Prepared output stream for writing chunk data");
      }
    }
    if (byteOutputStream != null && TransferStatus.SUCCESS.equals(transferStatus)) {
      try {
        resourceStoreRequest.getDataChunk().writeTo(byteOutputStream);
        LOGGER.debug("Written chunk to output stream...");
      } catch (IOException e) {
        LOGGER.error("Error writing chunk to output stream", e);
        transferStatus = TransferStatus.FAILURE;
      }
    } else {
      LOGGER.info("Ignoring chunk as resource is not ready to write to");
    }
  }

  @Override
  public void close() throws XMLDBException {
    if (resourceEntry != null) {
      resourceEntry.close();
    }
  }

  ResourceTransferStatus finish() {
    LOGGER.debug("finish()");
    final ResourceTransferStatus.Builder builder = ResourceTransferStatus.newBuilder();
    if (TransferStatus.SUCCESS.equals(transferStatus)) {
      try {
        switch (resourceEntry.resource()) {
          case XMLResource xmlResource -> {
            xmlResource.setContent(byteOutputStream.toString(StandardCharsets.UTF_8));
            resourceEntry.getParentCollection().storeResource(xmlResource);
            builder.setMeta(resourceMetaFunction.apply(xmlResource, resourceId));
          }
          case BinaryResource binaryResource -> {
            binaryResource.setContent(byteOutputStream.toByteArray());
            resourceEntry.getParentCollection().storeResource(binaryResource);
            builder.setMeta(resourceMetaFunction.apply(binaryResource, resourceId));
          }
          default -> {
            LOGGER.error("Unexpected resource type: {}", resourceEntry.resource().getClass());
            transferStatus = TransferStatus.FAILURE;
          }
        }
      } catch (XMLDBException e) {
        LOGGER.error("Error storing new content to resource", e);
        transferStatus = TransferStatus.FAILURE;
      }
    }
    return builder.setStatus(transferStatus).build();
  }

  StoreResourceContext combine(StoreResourceContext storeResourceContext) {
    throw new UnsupportedOperationException();
  }
}
