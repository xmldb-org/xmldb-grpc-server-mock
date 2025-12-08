/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

import java.io.OutputStream;

import org.xmldb.api.base.XMLDBException;

/**
 * Represents a functional interface that consumes an {@link OutputStream} and performs operations
 * that may throw an {@link XMLDBException}.
 */
@FunctionalInterface
public interface StreamConsumer {
  /**
   * Consumes the provided {@link OutputStream} to perform a particular operation that may throw an
   * {@link XMLDBException}.
   *
   * @param stream the {@link OutputStream} to be processed
   * @throws XMLDBException if an error occurs during the operation
   */
  void accept(OutputStream stream) throws XMLDBException;
}
