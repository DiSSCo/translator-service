package eu.dissco.webflux.demo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

public class RorWebClient {
  public static final String ROR_CLIENT = "rorClient";

  @Bean (name = ROR_CLIENT)
  public WebClient rorClient() {
    return WebClient.builder().baseUrl("https://api.ror.org/organizations")
        .build();
  }

}
