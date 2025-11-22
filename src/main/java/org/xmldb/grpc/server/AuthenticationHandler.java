/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.grpc.Status.UNAUTHENTICATED;
import static org.xmldb.grpc.server.AuthenticationConstants.CONTEXT_USERNAME_KEY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerInterceptor;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@GlobalInterceptor
public class AuthenticationHandler implements ServerInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandler.class);

  public static final Metadata.Key<String> AUTHENTICATION =
      Metadata.Key.of("Authentication", ASCII_STRING_MARSHALLER);

  private final AuthenticationService authenticationService;

  @Inject
  public AuthenticationHandler(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
      Metadata metadata, io.grpc.ServerCallHandler<ReqT, RespT> serverCallHandler) {
    final String header = metadata.get(AUTHENTICATION);
    if (Strings.isNullOrEmpty(header)) {
      LOGGER.error("No authentication header provided");
      serverCall.close(UNAUTHENTICATED.withDescription("No authentication header"), metadata);
    } else {
      try {
        LOGGER.debug("Processing authentication header: {}", header);
        String username = authenticationService.validateToken(header);
        Context context = Context.current().withValue(CONTEXT_USERNAME_KEY, username);
        return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
      } catch (AccessDeniedException e) {
        LOGGER.debug("Access denied for processed header", e);
        serverCall.close(
            UNAUTHENTICATED.withCause(e).withDescription("Rejected by Authentication Service"),
            metadata);
      }
    }
    return new ServerCall.Listener<ReqT>() {};
  }
}
