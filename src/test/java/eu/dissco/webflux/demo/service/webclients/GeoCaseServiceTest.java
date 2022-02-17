package eu.dissco.webflux.demo.service.webclients;

import static eu.dissco.webflux.demo.util.TestUtil.loadResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.DarwinCore;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import java.io.IOException;
import java.util.List;
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
class GeoCaseServiceTest {

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
  private WebClientProperties properties;

  private GeoCaseService service;

  @BeforeEach
  void setup() {
    service = new GeoCaseService(client, properties);
  }

  @Test
  void retrieveData() throws IOException {
    // Given
    givenJsonWebclient();
    var flux = Flux.just(mapper.readTree(loadResourceFile("geocase/geocase-response.json")));
    given(responseSpec.bodyToFlux(any(Class.class))).willReturn(flux).willReturn(Flux.empty());
    given(properties.getItemsPerRequest()).willReturn(1);
    var expected = givenDarwin();

    // When
    var result = service.retrieveData();

    // Then
    assertThat(result.toList()).isEqualTo(List.of(expected));
    then(responseSpec).should(times(2)).bodyToFlux(any(Class.class));
  }

  private DarwinCore givenDarwin() {
    return DarwinCore.builder()
        .id("638-222")
        .basisOfRecord("Fossil")
        .scientificName("Algae (informal)")
        .institutionID("University of Tartu, Natural History Museum")
        .occurrenceID("https://geocollections.info/specimen/289080")
        .images(List.of(Image.builder()
            .imageUri(
                "https://files.geocollections.info/medium/4d/59/"
                    + "4d59cfd2-1c22-408c-88e1-111b1364470d.jpg")
            .build())).build();
  }

  private void givenJsonWebclient() {
    given(properties.getEndpoint()).willReturn(
        "https://api.geocase.eu/v1/solr?q=%2A&fl=unitid,fullscientificname,"
            + "recordbasis,datasetowner,recordURI,images&rows=100&start=4000");
    given(client.get()).willReturn(headersSpec);
    given(headersSpec.uri(anyString())).willReturn(uriSpec);
    given(uriSpec.retrieve()).willReturn(responseSpec);
  }
}
