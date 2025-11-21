/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

/**
 * The AuthenticationService interface provides a contract for validating authorization tokens.
 * Implementations of this interface are responsible for interpreting the provided authentication
 * token, extracting relevant credentials, and verifying the user's access rights.
 *
 * The validation process may throw an AccessDeniedException if the token is invalid, improperly
 * formatted, or if access is denied due to insufficient privileges.
 */
public interface AuthenticationService {
  /**
   * Validates the provided authentication token and returns associated user information. The method
   * interprets the given token, extracts credentials, and verifies access rights.
   *
   * @param authentication the authentication token used for validating access, typically provided
   *        as a string representation of a JWT, session token, or similar credential.
   * @return the validated username if the token is valid and authentication is successful.
   * @throws AccessDeniedException if the token is invalid, improperly formatted, expired, or if the
   *         user lacks necessary access permissions.
   */
  String validateToken(String authentication) throws AccessDeniedException;
}
