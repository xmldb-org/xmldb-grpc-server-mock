/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

import java.io.Serial;

/**
 * Exception indicating that access to a resource or operation has been denied.
 * <p>
 * This exception is typically used in scenarios where a user or system component attempts to
 * perform an action for which they lack the required permissions or authentication. It provides an
 * optional message detailing the reason for the denial and can optionally include the underlying
 * cause (Throwable) of the exception.
 */
public class AccessDeniedException extends Exception {
  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new AccessDeniedException with the specified detail message.
   *
   * @param message The detail message providing additional information about the reason access was
   *        denied.
   */
  public AccessDeniedException(String message) {
    super(message);
  }

  /**
   * Constructs a new AccessDeniedException with the specified detail message and cause.
   *
   * @param message The detail message providing additional information about the reason access was
   *        denied.
   * @param cause The cause of the exception, which may provide further context about why access was
   *        denied, or null if no cause is provided.
   */
  public AccessDeniedException(String message, Throwable cause) {
    super(message, cause);
  }
}
