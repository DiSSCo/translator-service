package eu.dissco.webflux.demo.configuration;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import eu.dissco.webflux.demo.properties.NaturalisProperties;

@Configuration
@AllArgsConstructor
public class WebfluxConfig {

  private final NaturalisProperties properties;

  @Primary
  @Bean
  public WebClient webClient() {
   return WebClient.builder()
        .codecs(conf -> conf.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder()))
        .build();
  }

}
