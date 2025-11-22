/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.grpc.server.mockdb;

import static org.xmldb.api.base.ErrorCodes.NOT_IMPLEMENTED;

import java.io.OutputStream;
import java.time.Instant;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.BinaryResource;

public class TestBinaryResource extends TestBaseResource<byte[]> implements BinaryResource {
  public TestBinaryResource(String id, Collection parentCollection) {
    this(id, Instant.now(), parentCollection);
  }

  public TestBinaryResource(String id, Instant creation, Collection parentCollection) {
    super(id, creation, creation, parentCollection);
  }

  @Override
  public void getContentAsStream(OutputStream stream) throws XMLDBException {
    throw new XMLDBException(NOT_IMPLEMENTED);
  }

  @Override
  public byte[] getContent() throws XMLDBException {
    throw new XMLDBException(NOT_IMPLEMENTED);
  }

  @Override
  public void setContent(byte[] value) throws XMLDBException {
    throw new XMLDBException(NOT_IMPLEMENTED);
  }
}
