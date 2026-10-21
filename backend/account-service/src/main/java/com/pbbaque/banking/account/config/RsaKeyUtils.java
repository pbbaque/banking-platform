package com.pbbaque.banking.account.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.Resource;

/** Utility methods for loading RSA keys used by JWT validation. */
public final class RsaKeyUtils {

  private RsaKeyUtils() {}

  /**
   * Reads an RSA public key from a PEM resource.
   *
   * @param resource PEM resource
   * @return RSA public key
   * @throws Exception when the key cannot be read or parsed
   */
  public static RSAPublicKey readPublicKey(Resource resource) throws Exception {
    String key = readResource(resource);

    String normalized =
        key.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

    byte[] decoded = Base64.getDecoder().decode(normalized);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
  }

  private static String readResource(Resource resource) throws IOException {
    return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }
}
