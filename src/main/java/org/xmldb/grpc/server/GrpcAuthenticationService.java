/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.grpc.server;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A service implementation responsible for validating gRPC authentication tokens. This class
 * specifically handles Basic Authentication, extracting and validating user credentials from the
 * provided token.
 * <p>
 * It parses the token for a basic authentication string, decodes the Base64 representation, and
 * matches the extracted data against a predefined credential pattern. If the token is valid and
 * matches the expected format, it constructs a {@link Credentials} object with the relevant user
 * information. If the token is invalid, malformed, or doesn't conform to the accepted format, an
 * {@link AccessDeniedException} is thrown.
 * <p>
 * This implementation uses logging to record the validation process and errors encountered.
 */
@ApplicationScoped
public class GrpcAuthenticationService implements AuthenticationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcAuthenticationService.class);
  private static final Pattern BASIC_PATTERN =
      compile("^Basic (?<authString>.*)$", CASE_INSENSITIVE);
  private static final Pattern CREDENTIAL_PATTERN = compile("^(?<user>\\w+)?:(?<pwd>\\w+)?$");

  /**
   * Default constructor for the GrpcAuthenticationService class.
   * <p>
   * Initializes an instance of the GrpcAuthenticationService. This constructor invokes the parent
   * class constructor and prepares the service for handling authentication-related tasks, such as
   * validating gRPC authentication tokens.
   */
  public GrpcAuthenticationService() {
    super();
  }

  @Override
  public Credentials validateToken(final String authentication) throws AccessDeniedException {
    LOGGER.debug("validateToken({})", authentication);
    final Matcher matcher = BASIC_PATTERN.matcher(authentication);
    if (matcher.matches()) {
      final String authString =
          new String(Base64.getDecoder().decode(matcher.group("authString")), UTF_8);
      final Matcher credMatcher = CREDENTIAL_PATTERN.matcher(authString);
      if (credMatcher.matches()) {
        final String user = credMatcher.group("user");
        return new Credentials(user == null ? "anonymous" : user,
            Optional.ofNullable(credMatcher.group("pwd")));
      } else {
        LOGGER.error("No match on '{}' for credential pattern", authString);
      }
    }
    throw new AccessDeniedException("Access denied");
  }
}
