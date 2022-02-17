package eu.dissco.webflux.demo;

import static org.mockito.BDDMockito.then;

import eu.dissco.webflux.demo.service.webclients.WebClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectRunnerTest {

  @Mock
  private WebClientInterface webService;

  private ProjectRunner runner;

  @BeforeEach
  void setup() {
    this.runner = new ProjectRunner(webService);
  }

  @Test
  void testRun() {
    // Given

    // When
    runner.run();

    // Then
    then(webService).should().retrieveData();
  }

}
