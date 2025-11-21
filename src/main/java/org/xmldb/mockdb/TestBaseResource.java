/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.mockdb;

import static org.xmldb.api.base.ErrorCodes.NOT_IMPLEMENTED;

import java.io.OutputStream;
import java.time.Instant;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

public abstract class TestBaseResource implements Resource {
  private final String id;
  private final Collection parentCollection;
  private final Instant creation;

  private boolean closed;

  protected TestBaseResource(String id, Collection parentCollection) {
    this.id = id;
    this.parentCollection = parentCollection;
    creation = Instant.now();
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
  public Object getContent() throws XMLDBException {
    throw new XMLDBException(NOT_IMPLEMENTED);
  }

  @Override
  public void getContentAsStream(OutputStream stream) throws XMLDBException {
    throw new XMLDBException(NOT_IMPLEMENTED);
  }

  @Override
  public void setContent(Object value) throws XMLDBException {
    throw new XMLDBException(NOT_IMPLEMENTED);
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
    return creation;
  }
}
