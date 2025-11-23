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

import io.grpc.Context;

/**
 * The AuthenticationConstants class provides constant keys used to retrieve user authentication
 * details from the context of an application. These constants are intended to facilitate secure and
 * consistent handling of user credentials across different components.
 * <p>
 * This class is final and cannot be instantiated, ensuring that its only purpose is to define
 * constants.
 */
public final class AuthenticationConstants {
  /**
   * A constant key used to retrieve the username from the application context.
   * <p>
   * This key is a unique identifier in the context that maps to a String value representing the
   * username of an authenticated user.
   * <p>
   * It is typically used in authentication workflows to access or store the username within a
   * secure context.
   */
  public static final Context.Key<String> USERNAME = Context.key("username");
  /**
   * A constant key used to retrieve the credential information from the application context.
   * <p>
   * This key maps to an {@link Optional} of {@link String}, representing authentication credentials
   * such as tokens, secrets, or passwords. The use of {@link Optional} ensures that the presence of
   * credentials is explicitly handled, allowing for cases where credentials might be absent or
   * null.
   * <p>
   * It is typically utilized in authentication workflows to securely access or store sensitive
   * credential data within a controlled context.
   */
  public static final Context.Key<Optional<String>> CREDENTIAL = Context.key("credential");

  private AuthenticationConstants() {}
}
