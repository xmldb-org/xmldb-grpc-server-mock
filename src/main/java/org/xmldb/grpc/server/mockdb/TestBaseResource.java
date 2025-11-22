/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.grpc.server.mockdb;

import java.time.Instant;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;

public abstract class TestBaseResource<T> implements Resource<T> {
  private final String id;
  private final Collection parentCollection;
  private final Instant creation;

  private boolean closed;
  private Instant lastChange;

  protected TestBaseResource(String id, Instant creation, Instant lastChange,
      Collection parentCollection) {
    this.id = id;
    this.creation = creation;
    this.lastChange = lastChange;
    this.parentCollection = parentCollection;
  }

  @Override
  public Collection getParentCollection() {
    return parentCollection;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public Instant getCreationTime() {
    return creation;
  }

  @Override
  public Instant getLastModificationTime() {
    return lastChange;
  }
}
