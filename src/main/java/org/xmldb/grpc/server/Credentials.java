/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The Credentials record is a data container used to encapsulate user credentials. It consists of a
 * username, represented as a string, and a password, provided via a {@link Supplier} for deferred
 * password evaluation. This design allows for lazy-loading or secure handling of password data,
 * ensuring sensitive information is accessed only when needed.
 * <p>
 * The Credentials record provides an immutable and compact representation of the username-password
 * pair, making it suitable for use in authentication and authorization workflows.
 *
 * @param username The identifier or username associated with the user or account.
 * @param password A {@link Optional} providing access to the user's password if available.
 */
public record Credentials(String username, Optional<String> password) {
}
