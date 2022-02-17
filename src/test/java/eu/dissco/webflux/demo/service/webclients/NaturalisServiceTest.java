package eu.dissco.webflux.demo.service.webclients;

import static eu.dissco.webflux.demo.util.TestUtil.loadResourceFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class NaturalisServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  @Mock
  private WebClient client;
  @Mock
  private RequestHeadersUriSpec headersSpec;
  @Mock
  private RequestHeadersSpec uriSpec;
  @Mock
  private ResponseSpec responseSpec;
  @Mock
  private Flux<JsonNode> jsonNodeFlux;
  private NaturalisService service;
  @Mock
  private WebClientProperties properties;
  @Mock
  private RorService rorService;
  @Mock
  private KafkaService kafkaService;

  @BeforeEach
  void setup() {
    this.service = new NaturalisService(properties, client, rorService, kafkaService);
  }

  @Test
  void testGetNaturalisStream() throws IOException {
    // Given
    givenJsonWebclient();
    var stream = Stream.of(
        mapper.readTree(loadResourceFile("naturalis/naturalis-response.json")));
    given(jsonNodeFlux.toStream()).willReturn(stream);
    given(rorService.getRoRId(anyString())).willReturn("https://ror.org/0566bfb96");
    var expected = givenExpected();

    // When
    service.retrieveData();

    // Then
    then(rorService).should().getRoRId(eq("Naturalis Biodiversity Center"));
    then(kafkaService).should().sendMessage(eq(expected));
  }

  private OpenDSWrapper givenExpected() {
    return OpenDSWrapper.builder().authoritative(
            Authoritative.builder()
                .name("Argyrolobium zanonii (Turra) P.W.Ball")
                .physicalSpecimenId("L.3892015@BRAHMS")
                .curatedObjectID("https://data.biodiversitydata.nl/naturalis/specimen/L.3892015")
                .materialType("Herbarium sheet")
                .midslevel(1)
                .institutionCode("Naturalis Biodiversity Center")
                .institution("https://ror.org/0566bfb96")
                .build()
        ).images(List.of(
            Image.builder().imageUri("https://medialib.naturalis.nl/file/id/L.3892015/format/large")
                .build()))
        .sourceId("translator-service").build();
  }

  @Test
  void testMissingScientificName() throws IOException {
    // Given
    givenJsonWebclient();
    var stream = Stream.of(
        mapper.readTree(
            loadResourceFile(
                "naturalis/naturalis-missing-scientific-name-response.json")));
    given(jsonNodeFlux.toStream()).willReturn(stream);

    // When
    service.retrieveData();

    // Then
    then(kafkaService).should().sendMessage(any());
  }

  private void givenJsonWebclient() {
    given(properties.getEndpoint()).willReturn(
        "https://api.biodiversitydata.nl/v2/specimen/download");
    given(client.get()).willReturn(headersSpec);
    given(headersSpec.uri(anyString())).willReturn(uriSpec);
    given(uriSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.bodyToFlux(any(Class.class))).willReturn(jsonNodeFlux);
  }

}
