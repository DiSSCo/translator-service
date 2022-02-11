package eu.dissco.webflux.demo.service;

import static eu.dissco.webflux.demo.util.TestUtil.testDarwin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.properties.NaturalisProperties;
import eu.dissco.webflux.demo.util.TestUtil;
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
class WebFluxServiceTest {

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
  private WebFluxService service;
  private NaturalisProperties properties = new NaturalisProperties();

  @BeforeEach
  void setup() {
    this.service = new WebFluxService(properties, client);
  }

  @Test
  void testGetNaturalisStream() throws IOException {
    // Given
    givenWebclient();
    var stream = Stream.of(
        mapper.readTree(TestUtil.loadResourceFile("naturalis/naturalis-response.json")));
    given(jsonNodeFlux.toStream()).willReturn(stream);

    // When
    var result = service.getNaturalisStream();

    // Then
    assertThat(result.toList()).isEqualTo(List.of(testDarwin()));
  }

  @Test
  void testMissingScientificName() throws IOException {
    // Given
    givenWebclient();
    var stream = Stream.of(
        mapper.readTree(
            TestUtil.loadResourceFile(
                "naturalis/naturalis-missing-scientific-name-response.json")));
    given(jsonNodeFlux.toStream()).willReturn(stream);

    // When
    var result = service.getNaturalisStream();

    // Then
    assertThat(result.toList()).isEqualTo(List.of(testDarwin("Unknown")));
  }

  private void givenWebclient() {
    given(client.get()).willReturn(headersSpec);
    given(headersSpec.uri(anyString())).willReturn(uriSpec);
    given(uriSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.bodyToFlux(any(Class.class))).willReturn(jsonNodeFlux);
  }


}
