package com.pbbaque.banking.audit.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.Resource;

/** Utility methods for loading RSA keys from PEM resources. */
public final class RsaKeyUtils {

  private RsaKeyUtils() {}

  /**
   * Reads an RSA public key from a PEM resource.
   *
   * @param resource public key resource
   * @return parsed RSA public key
   * @throws Exception when the key cannot be read or parsed
   */
  public static RSAPublicKey readPublicKey(Resource resource) throws Exception {
    String key = readResource(resource);

    String normalizedKey =
        key.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

    byte[] decodedKey = Base64.getDecoder().decode(normalizedKey);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
  }

  private static String readResource(Resource resource) throws IOException {
    try (var inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
