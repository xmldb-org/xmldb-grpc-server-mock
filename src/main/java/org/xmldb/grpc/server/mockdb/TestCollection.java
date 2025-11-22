/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.grpc.server.mockdb;

import static org.xmldb.api.base.ErrorCodes.INVALID_RESOURCE;
import static org.xmldb.api.base.ErrorCodes.NOT_IMPLEMENTED;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.Service;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;
import org.xmldb.api.modules.XMLResource;

public class TestCollection extends ConfigurableImpl implements Collection {
  private final TestCollectionData data;
  private final ConcurrentMap<String, Collection> childCollections;
  private final ConcurrentMap<String, Resource<?>> resources;

  private boolean closed;
  private Collection parent;

  public TestCollection(final TestCollectionData data) {
    this(data, null);
  }

  public TestCollection(final TestCollectionData data, final Collection parent) {
    this.data = data;
    this.parent = parent;
    resources = new ConcurrentHashMap<>();
    childCollections = new ConcurrentHashMap<>();
  }

  public static TestCollection create(String name) {
    return new TestCollection(new TestCollectionData(name));
  }

  public <T, R extends TestBaseResource<T>> R addResource(String id,
      BiFunction<String, Collection, R> createAction) {
    R resource = createAction.apply(id, this);
    resources.put(resource.getId(), resource);
    return resource;
  }

  public void addCollection(String child, TestCollection childCollection) {
    childCollections.put(child, childCollection);
  }

  @Override
  public final String getName() throws XMLDBException {
    return data.name();
  }

  @Override
  public <S extends Service> boolean hasService(Class<S> serviceType) {
    return false;
  }

  @Override
  public <S extends Service> Optional<S> findService(Class<S> serviceType) {
    return Optional.empty();
  }

  @Override
  public <S extends Service> S getService(Class<S> serviceType) throws XMLDBException {
    throw new XMLDBException(NOT_IMPLEMENTED);
  }

  @Override
  public int getChildCollectionCount() {
    return childCollections.size();
  }

  @Override
  public List<String> listChildCollections() {
    return childCollections.keySet().stream().toList();
  }

  @Override
  public Collection getChildCollection(String collectionName) {
    return childCollections.get(collectionName);
  }

  @Override
  public Collection getParentCollection() {
    return parent;
  }

  @Override
  public int getResourceCount() {
    return resources.size();
  }

  @Override
  public List<String> listResources() {
    return resources.keySet().stream().toList();
  }

  @Override
  public <T, R extends Resource<T>> R createResource(String id, Class<R> type)
      throws XMLDBException {
    if (BinaryResource.class.equals(type)) {
      return type.cast(new TestBinaryResource(id, this));
    } else if (XMLResource.class.equals(type)) {
      return type.cast(new TestXMLResource(id, this));
    }
    throw new XMLDBException(INVALID_RESOURCE);
  }

  @Override
  public void removeResource(Resource<?> res) throws XMLDBException {
    resources.remove(res.getId());
  }

  @Override
  public void storeResource(Resource<?> res) throws XMLDBException {
    resources.put(res.getId(), res);
  }

  @Override
  public Resource<?> getResource(String id) {
    return resources.get(id);
  }

  @Override
  public String createId() throws XMLDBException {
    throw new XMLDBException(NOT_IMPLEMENTED);
  }

  @Override
  public boolean isOpen() {
    return !closed;
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public Instant getCreationTime() {
    return data.creation();
  }

  @Override
  public String toString() {
    return "/%s".formatted(data.name());
  }
}
