package eu.dissco.webflux.demo;

import eu.dissco.webflux.demo.service.webclients.AbstractWebClientService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class ProjectRunner implements CommandLineRunner {

  private final AbstractWebClientService webService;
  private final ConfigurableApplicationContext context;

  @Override
  public void run(String... args) {
    webService.retrieveData();
    context.close();
  }
}
