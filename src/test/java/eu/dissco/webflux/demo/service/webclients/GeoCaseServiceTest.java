package eu.dissco.webflux.demo.service.webclients;

import static eu.dissco.webflux.demo.util.TestUtil.loadResourceFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
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
import reactor.core.publisher.Mono;

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
  @Mock
  private RorService rorService;
  @Mock
  private KafkaService kafkaService;

  private GeoCaseService service;

  @BeforeEach
  void setup() {
    service = new GeoCaseService(client, properties, rorService, kafkaService);
  }

  @Test
  void testRetrieveData() throws IOException {
    // Given
    givenJsonWebclient();
    var flux = Mono.just(mapper.readTree(loadResourceFile("geocase/geocase-response.json")));
    given(responseSpec.bodyToMono(any(Class.class))).willReturn(flux);
    given(properties.getItemsPerRequest()).willReturn(1);
    given(rorService.getRoRId(anyString())).willReturn("Unknown");
    var expected = givenExpected();

    // When
    service.retrieveData();

    // Then
    then(rorService).should(times(2)).getRoRId(eq("University of Tartu, Natural History Museum"));
    then(kafkaService).should(times(2)).sendMessage(eq(expected));
  }

  private OpenDSWrapper givenExpected() {
    return OpenDSWrapper.builder().authoritative(
        Authoritative.builder()
            .midslevel(1)
            .physicalSpecimenId("638-222")
            .name("Algae (informal)")
            .materialType("Fossil")
            .curatedObjectID("https://geocollections.info/specimen/289080")
            .institutionCode("University of Tartu, Natural History Museum")
            .institution("Unknown")
            .build()
    ).images(
        List.of(Image.builder().imageUri(
                "https://files.geocollections.info/medium/4d/59/4d59cfd2-1c22-408c-88e1-111b1364470d.jpg")
            .build())
    ).sourceId("translator-service")
        .unmapped(mapper.createObjectNode()).build();
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
