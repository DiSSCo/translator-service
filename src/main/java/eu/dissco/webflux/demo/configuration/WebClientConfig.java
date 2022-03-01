package eu.dissco.webflux.demo.configuration;

import eu.dissco.webflux.demo.Profiles;
import javax.xml.stream.XMLInputFactory;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@AllArgsConstructor
public class WebClientConfig {

  @Bean
  @Profile({Profiles.GEO_CASE, Profiles.NATURALIS})
  public WebClient webClientJson() {
    var size = 16 * 1024 * 1024;
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
        .build();
    return WebClient.builder()
        .codecs(conf -> conf.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder()))
        .exchangeStrategies(strategies)
        .build();
  }

  @Bean
  @Profile({Profiles.BIO_CASE, Profiles.DWCA})
  public WebClient webClientXml() {
    var size = 16 * 1024 * 1024;
    final ExchangeStrategies strategies = ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
        .build();
    return WebClient.builder()
        .exchangeStrategies(strategies)
        .build();
  }

  @Bean
  @Profile({Profiles.BIO_CASE, Profiles.DWCA})
  public XMLInputFactory xmlEventReader() {
    var factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory;
  }


}
