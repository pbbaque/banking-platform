package com.pbbaque.banking.account.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** Configures HTTP clients used by account-service. */
@Configuration
public class RestClientConfig {

  /**
   * Configures connection and read timeouts for outbound HTTP requests.
   *
   * @param connectTimeout maximum time allowed to establish a connection
   * @param readTimeout maximum time allowed waiting for a response
   * @return REST client customizer
   */
  @Bean
  public RestClientCustomizer restClientTimeoutCustomizer(
      @Value("${customer-service.connect-timeout:2s}") Duration connectTimeout,
      @Value("${customer-service.read-timeout:5s}") Duration readTimeout) {

    return builder -> {
      SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

      requestFactory.setConnectTimeout(connectTimeout);
      requestFactory.setReadTimeout(readTimeout);

      builder.requestFactory(requestFactory);
    };
  }
}
