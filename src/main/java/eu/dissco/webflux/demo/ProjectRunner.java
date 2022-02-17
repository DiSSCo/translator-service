package eu.dissco.webflux.demo;

import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.OpenDSMappingService;
import eu.dissco.webflux.demo.service.webclients.WebClientInterface;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectRunner implements CommandLineRunner {

  private final WebClientInterface webService;
  private final OpenDSMappingService mappingService;
  private final KafkaService kafkaService;

  @Override
  public void run(String... args) {
    var objectStream = webService.retrieveData();
    objectStream.map(mappingService::mapToAuthoritative).forEach(kafkaService::sendMessage);
  }
}
