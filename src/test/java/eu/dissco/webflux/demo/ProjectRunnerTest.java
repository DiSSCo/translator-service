package eu.dissco.webflux.demo;

import static eu.dissco.webflux.demo.util.TestUtil.testAuthoritative;
import static eu.dissco.webflux.demo.util.TestUtil.testDarwin;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.DarwinCore;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.OpenDSMappingService;
import eu.dissco.webflux.demo.service.WebFluxService;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectRunnerTest {

  @Mock
  private WebFluxService webService;

  @Mock
  private OpenDSMappingService mappingService;

  @Mock
  private KafkaService kafkaService;

  private ProjectRunner runner;

  @BeforeEach
  void setup() {
    this.runner = new ProjectRunner(webService, mappingService, kafkaService);
  }

  @Test
  void testRun() {
    // Given
    given(webService.getNaturalisStream()).willReturn(Stream.of(testDarwin()));
    given(mappingService.mapToAuthoritative(any(DarwinCore.class))).willReturn(testAuthoritative());

    // When
    runner.run();

    // Then
    then(mappingService).should().mapToAuthoritative(any(DarwinCore.class));
    then(kafkaService).should().sendMessage(any(Authoritative.class));
  }


}
