package eu.dissco.webflux.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.util.TestUtil;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RorServiceTest {

  private static final String INSTITUTION_NAME = "Naturalis Biodiversity Center";
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
  private Mono<JsonNode> jsonNodeMono;
  @Mock
  private CompletableFuture<JsonNode> jsonFuture;
  private RorService rorService;

  @BeforeEach
  void setup() {
    this.rorService = new RorService(client);
  }


  @Test
  void testGetRorId() throws IOException, ExecutionException, InterruptedException {
    // Given
    givenWebclient();
    given(jsonFuture.get()).willReturn(
        mapper.readTree(TestUtil.loadResourceFile("ror-examples/naturalis-response.json")));

    // When
    var result = rorService.getRoRId(INSTITUTION_NAME);

    // Then
    assertThat(result).isEqualTo("https://ror.org/0566bfb96");
  }

  private void givenWebclient() {
    given(client.get()).willReturn(headersSpec);
    given(headersSpec.uri(anyString())).willReturn(uriSpec);
    given(uriSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.bodyToMono(any(Class.class))).willReturn(jsonNodeMono);
    given(jsonNodeMono.toFuture()).willReturn(jsonFuture);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ror-examples/low-score-response.json",
      "ror-examples/empty-response.json"})
  void testFailedRetrieval(String fileName)
      throws IOException, ExecutionException, InterruptedException {
    // Given
    givenWebclient();
    given(jsonFuture.get()).willReturn(mapper.readTree(TestUtil.loadResourceFile(fileName)));

    // When
    var result = rorService.getRoRId(INSTITUTION_NAME);

    // Then
    assertThat(result).isEqualTo("Unknown");
  }

  @Test
  void testEmptyMono() throws ExecutionException, InterruptedException {
    // Given
    givenWebclient();
    given(jsonFuture.get()).willReturn(null);

    // When
    var result = rorService.getRoRId(INSTITUTION_NAME);

    // Then
    assertThat(result).isEqualTo("Unknown");
  }

}
