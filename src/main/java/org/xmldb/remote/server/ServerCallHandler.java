/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.remote.server;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.grpc.Status.UNAUTHENTICATED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.xmldb.remote.server.AuthenticationConstants.CONTEXT_USERNAME_KEY;

import java.util.Base64;

import com.google.common.base.Strings;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerInterceptor;

public final class ServerCallHandler implements ServerInterceptor {
  public static final Metadata.Key<String> AUTHENTICATION =
      Metadata.Key.of("Authentication", ASCII_STRING_MARSHALLER);

  private final AuthenticationService authenticationService;

  ServerCallHandler(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
      Metadata metadata, io.grpc.ServerCallHandler<ReqT, RespT> serverCallHandler) {
    String header = metadata.get(AUTHENTICATION);

    if (Strings.isNullOrEmpty(header)) {
      serverCall.close(UNAUTHENTICATED.withDescription("No authentication header"), metadata);
    } else {
      String token = new String(Base64.getDecoder().decode(header), UTF_8);
      try {
        String username = authenticationService.validateToken(token);
        Context context = Context.current().withValue(CONTEXT_USERNAME_KEY, username);
        return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
      } catch (AccessDeniedException e) {
        serverCall.close(UNAUTHENTICATED.withDescription("Rejected by Authentication Service"),
            metadata);
      }
    }
    return new ServerCall.Listener<ReqT>() {};
  }
}
