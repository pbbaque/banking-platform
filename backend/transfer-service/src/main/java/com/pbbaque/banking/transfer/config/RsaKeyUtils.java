package com.pbbaque.banking.transfer.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
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
   * @param resource PEM public key resource
   * @return parsed RSA public key
   * @throws IOException when the resource cannot be read
   * @throws GeneralSecurityException when the key cannot be parsed
   */
  public static RSAPublicKey readPublicKey(Resource resource)
      throws IOException, GeneralSecurityException {

    String pem;

    try (var inputStream = resource.getInputStream()) {
      pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    String encodedKey =
        pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

    byte[] decodedKey = Base64.getDecoder().decode(encodedKey);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
  }
}
