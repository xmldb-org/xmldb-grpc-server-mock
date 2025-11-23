/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.mockdb.TestBinaryResource;
import org.xmldb.mockdb.TestCollection;
import org.xmldb.mockdb.TestDatabase;
import org.xmldb.mockdb.TestXMLResource;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The TestDataBaseInitializer class is responsible for initializing and managing a test database
 * lifecycle for application testing purposes. This class is an application-scoped bean that is
 * automatically instantiated at application startup and properly destroyed during shutdown.
 */
@Startup
@ApplicationScoped
public class TestDataBaseInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestDataBaseInitializer.class);

  private final TestDatabase database;

  /**
   * Creates a new instance of the TestDataBaseInitializer class. This constructor initializes the
   * test database used for application testing purposes. The TestDatabase instance is prepared to
   * manage collections and resources specific to the test lifecycle.
   */
  public TestDataBaseInitializer() {
    super();
    this.database = new TestDatabase();
  }

  /**
   * Initializes the test database with a predefined structure and resources. This method is
   * annotated with {@code @PostConstruct}, ensuring it is invoked automatically during the
   * application's startup phase.
   * <p>
   * The initialization process involves the creation of a root collection and a sub-collection,
   * populating them with designated XML and binary resources. The test database is then registered
   * with the {@code DatabaseManager}. In case of an error during registration, the incident is
   * logged.
   * <p>
   * Responsibilities performed within this method include:
   * <ul>
   * <li>Adding and organizing collections.</li>
   * <li>Adding resources to the collections.</li>
   * <li>Registering the test database with the database manager for use during the application
   * lifecycle.</li>
   * </ul>
   * <p>
   * Any failure during database registration is logged for diagnostic purposes.
   */
  @PostConstruct
  public void init() {
    LOGGER.info("Initializing test database..");
    TestCollection rootCollection = database.addCollection("db");
    rootCollection.addResource("test1.xml", TestXMLResource::new);
    rootCollection.addResource("test2.xml", TestBinaryResource::new);
    TestCollection subCollection = database.addCollection("db/child");
    subCollection.addResource("test3.xml", TestBinaryResource::new);
    rootCollection.addCollection("child", subCollection);
    try {
      DatabaseManager.registerDatabase(database);
    } catch (XMLDBException e) {
      LOGGER.error("Failed to register test database", e);
    }
  }

  /**
   * This method is responsible for cleaning up resources and properly deregistering the test
   * database during the application's shutdown process. It is annotated with {@code @PreDestroy},
   * ensuring it is called automatically when the application is shutting down or when the
   * containing bean is being removed.
   */
  @PreDestroy
  public void destroy() {
    LOGGER.info("Destroying test database...");
    DatabaseManager.deregisterDatabase(database);
  }
}
