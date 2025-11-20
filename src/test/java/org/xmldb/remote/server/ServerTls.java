/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.remote.server;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerCredentials;
import io.grpc.TlsServerCredentials;

public class ServerTls implements AuthenticationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerTls.class);

  private final int port;
  private final ServerCredentials credentials;

  private Server server;

  public ServerTls(int port, ServerCredentials credentials) {
    this.port = port;
    this.credentials = credentials;
  }

  private void start() throws IOException {
    server = Grpc.newServerBuilderForPort(port, credentials) //
        .intercept(new AuthenticationHandler(this)) //
        .addService(new XmlDbService()) //
        .build().start();
    LOGGER.info("Server started, listening on {}", port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        LOGGER.error("*** shutting down gRPC server since JVM is shutting down");
        ServerTls.this.stop();
        LOGGER.error("*** server shut down");
      }
    });
  }

  private void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  @Override
  public String validateToken(String authentication) throws AccessDeniedException {
    try {
      var basicAuth = new String(Base64.getDecoder().decode(authentication), UTF_8);
      int index = basicAuth.indexOf(':');
      if (index > 0) {
        var username = basicAuth.substring(0, index);
        var password = basicAuth.substring(index + 1);
        LOGGER.info("Authenticating using {} / {}", username, password);
        return username;
      }
    } catch (RuntimeException e) {
      throw new AccessDeniedException("Token parse failed", e);
    }
    throw new AccessDeniedException("Access denied");
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length < 1 || args.length > 4) {
      System.out.println(
          """
              USAGE: ServerTls port [certChainFilePath privateKeyFilePath] [trustCertCollectionFilePath]
              Note: You only need to supply trustCertCollectionFilePath if you want to enable Mutual TLS.
              """);
      System.exit(0);
    }
    try {
      final ServerCredentials serverCredentials;
      if (args.length == 1) {
        serverCredentials = InsecureServerCredentials.create();
      } else {
        // If only providing a private key, you can use TlsServerCredentials.create() instead of
        // interacting with the Builder.
        TlsServerCredentials.Builder tlsBuilder =
            TlsServerCredentials.newBuilder().keyManager(new File(args[1]), new File(args[2]));
        if (args.length == 4) {
          tlsBuilder.trustManager(new File(args[3]));
          tlsBuilder.clientAuth(TlsServerCredentials.ClientAuth.REQUIRE);
        }
        serverCredentials = tlsBuilder.build();
      }
      final ServerTls server = new ServerTls(Integer.parseInt(args[0]), serverCredentials);
      server.start();
      server.blockUntilShutdown();
    } catch (RuntimeException e) {
      LOGGER.error("Failed to start server", e);
    }
  }
}
