/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.grpc.HandleId;
import org.xmldb.api.grpc.ResourceMeta;

/**
 * Functional interface representing a function that takes a {@link Resource} and a {@link HandleId}
 * as input and produces a {@link ResourceMeta} as output. This function may throw an
 * {@link XMLDBException}.
 */
@FunctionalInterface
public interface ResourceMetaFunction {
  /**
   * Applies the function to the given {@link Resource} and {@link HandleId} and returns the
   * resulting {@link ResourceMeta}. This operation may throw an {@link XMLDBException}.
   *
   * @param resource the input resource to be processed
   * @param handleId the handle identifier associated with the resource
   * @return the resulting {@link ResourceMeta} after processing
   * @throws XMLDBException if an error occurs during the operation
   */
  ResourceMeta apply(Resource<?> resource, HandleId handleId) throws XMLDBException;
}
