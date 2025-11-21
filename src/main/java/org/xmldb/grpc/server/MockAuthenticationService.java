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

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MockAuthenticationService implements AuthenticationService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MockAuthenticationService.class);
  private static final Pattern BASIC_PATTERN =
      Pattern.compile("^Basic (?<authString>.*)$", CASE_INSENSITIVE);

  @Override
  public String validateToken(final String authentication) throws AccessDeniedException {
    LOGGER.info("validateToken({})", authentication);
    final Matcher matcher = BASIC_PATTERN.matcher(authentication);
    if (matcher.matches()) {
      final String authString =
          new String(Base64.getDecoder().decode(matcher.group("authString")), UTF_8);
      LOGGER.info("authString({})", authString);
      if (":".equals(authString)) {
        return "anonymous";
      } else {
        return authString.split(":")[0];
      }
    }
    throw new AccessDeniedException("Access denied");
  }
}
