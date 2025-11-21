/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.grpc.server.mockdb;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;

public class TestDatabase extends ConfigurableImpl implements Database {
  private static final String DETAULT_NAME = "testdatabase";

  private final String name;
  private final Map<String, TestCollection> collections;

  public TestDatabase() {
    this(null);
  }

  public TestDatabase(String name) {
    if (name == null || name.isEmpty()) {
      this.name = DETAULT_NAME;
    } else {
      this.name = name;
    }
    collections = new HashMap<>();
  }

  @Override
  public final String getName() throws XMLDBException {
    return name;
  }

  public TestCollection addCollection(String collectionName) {
    return collections.computeIfAbsent(collectionName, TestCollection::create);
  }

  @Override
  public Collection getCollection(String uri, Properties info) throws XMLDBException {
    return collections.get(uri);
  }

  @Override
  public boolean acceptsURI(String uri) {
    return uri.startsWith(DatabaseManager.URI_PREFIX + "test");
  }

  @Override
  public String getConformanceLevel() {
    return "0";
  }
}
