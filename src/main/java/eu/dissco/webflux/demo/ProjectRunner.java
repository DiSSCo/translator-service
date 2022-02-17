package eu.dissco.webflux.demo;

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

  @Override
  public void run(String... args) {
    webService.retrieveData();
  }
}
