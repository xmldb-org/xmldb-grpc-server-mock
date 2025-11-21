/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.mockdb;

import org.xmldb.api.base.Collection;
import org.xmldb.api.modules.BinaryResource;

public class TestBinaryResource extends TestBaseResource implements BinaryResource {
  public TestBinaryResource() {
    this(null, null);
  }

  public TestBinaryResource(String id, Collection parentCollection) {
    super(id, parentCollection);
  }
}
