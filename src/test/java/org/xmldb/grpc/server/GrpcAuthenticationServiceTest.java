package org.xmldb.grpc.server;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GrpcAuthenticationServiceTest {
  GrpcAuthenticationService authenticationService;

  @BeforeEach
  void setUp() {
    authenticationService = new GrpcAuthenticationService("defaultUser");
  }

  @ParameterizedTest
  @CsvSource(textBlock = """
          validUser:validPassword, validUser,     validPassword
          validUser,               validUser,
          :passwordOnly,           defaultUser,   passwordOnly
          '',                      defaultUser,
      """)
  void shouldValidateTokenSuccessfully(String tokenInput, String expectedUser,
      String expectedPassword) throws AccessDeniedException {
    // Given
    String token = "Basic " + Base64.getEncoder().encodeToString(tokenInput.getBytes(UTF_8));

    // When
    Credentials credentials = authenticationService.validateToken(token);

    // Then
    assertThat(credentials).isNotNull();
    assertThat(credentials.username()).isEqualTo(expectedUser);
    if (expectedPassword == null || expectedPassword.isBlank()) {
      assertThat(credentials.password()).isEmpty();
    } else {
      assertThat(credentials.password()).isPresent().contains(expectedPassword);
    }
  }

  @Test
  void shouldThrowAccessDeniedExceptionForInvalidTokenFormat() {
    // Given an incorrect token format (e.g., Bearer instead of Basic)
    String invalidToken = "Bearer invalidTokenXX";

    // When and Then validateToken should throw an AccessDeniedException
    assertThatThrownBy(() -> authenticationService.validateToken(invalidToken))
        .isInstanceOf(AccessDeniedException.class).hasMessage("Access denied");
  }

  @Test
  void shouldThrowAccessDeniedExceptionForMalformedBase64() {
    // Given a malformed Base64 token
    String malformedToken = "Basic malformedBase64==";

    // When and Then validateToken should throw an AccessDeniedException
    assertThatThrownBy(() -> authenticationService.validateToken(malformedToken))
        .isInstanceOf(AccessDeniedException.class).hasMessage("Access denied");
  }

  @Test
  void shouldThrowAccessDeniedExceptionForMalformedCredentials() {
    // Given a Base64-encoded string that does not match the expected "user:password" pattern
    String malformedCredentials = Base64.getEncoder().encodeToString("someValue".getBytes(UTF_8));
    String token = "Bearer " + malformedCredentials;

    // When and Then validateToken should throw an AccessDeniedException
    assertThatThrownBy(() -> authenticationService.validateToken(token))
        .isInstanceOf(AccessDeniedException.class).hasMessage("Access denied");
  }
}
