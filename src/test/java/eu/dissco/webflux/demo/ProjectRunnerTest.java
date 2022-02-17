package eu.dissco.webflux.demo;

import eu.dissco.webflux.demo.domain.DarwinCore;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.OpenDSMappingService;
import eu.dissco.webflux.demo.service.webclients.NaturalisService;
import eu.dissco.webflux.demo.service.webclients.GeoCaseService;
import eu.dissco.webflux.demo.service.webclients.WebClientInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static eu.dissco.webflux.demo.util.TestUtil.testDarwin;
import static eu.dissco.webflux.demo.util.TestUtil.testOpenDSWrapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProjectRunnerTest {

    @Mock
    private WebClientInterface webService;

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
        given(webService.retrieveData()).willReturn(Stream.of(testDarwin()));
        given(mappingService.mapToAuthoritative(any(DarwinCore.class))).willReturn(testOpenDSWrapper());

        // When
        runner.run();

        // Then
        then(mappingService).should().mapToAuthoritative(any(DarwinCore.class));
        then(kafkaService).should().sendMessage(any(OpenDSWrapper.class));
    }


}
