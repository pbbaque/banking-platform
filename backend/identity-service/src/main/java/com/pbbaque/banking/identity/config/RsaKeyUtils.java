package com.pbbaque.banking.identity.config;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.Resource;

/** Utility methods for loading RSA public and private keys from PEM resources. */
public final class RsaKeyUtils {

  private RsaKeyUtils() {}

  /**
   * Reads an RSA public key from a PEM resource.
   *
   * @param resource resource containing the public key
   * @return parsed RSA public key
   * @throws Exception if the key cannot be read or parsed
   */
  public static RSAPublicKey readPublicKey(Resource resource) throws Exception {

    String key = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    key =
        key.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");

    byte[] decoded = Base64.getDecoder().decode(key);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
  }

  /**
   * Reads an RSA private key from a PEM resource.
   *
   * @param resource resource containing the private key
   * @return parsed RSA private key
   * @throws Exception if the key cannot be read or parsed
   */
  public static RSAPrivateKey readPrivateKey(Resource resource) throws Exception {

    String key = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    key =
        key.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

    byte[] decoded = Base64.getDecoder().decode(key);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
  }
}
