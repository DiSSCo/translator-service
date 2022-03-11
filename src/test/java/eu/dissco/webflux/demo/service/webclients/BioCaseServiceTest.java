package eu.dissco.webflux.demo.service.webclients;

import static eu.dissco.webflux.demo.util.TestUtil.loadResourceFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.OpenDSProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import java.io.IOException;
import javax.xml.stream.XMLInputFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class BioCaseServiceTest {

  private final XMLInputFactory factory = XMLInputFactory.newFactory();
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  @Mock
  private WebClient webClient;
  @Mock
  private WebClientProperties properties;
  @Mock
  private OpenDSProperties openDSProperties;
  @Mock
  private RequestHeadersUriSpec headersSpec;
  @Mock
  private RequestHeadersSpec uriSpec;
  @Mock
  private ResponseSpec responseSpec;
  @Mock
  private RorService rorService;
  @Mock
  private KafkaService kafkaService;
  @Captor
  private ArgumentCaptor<OpenDSWrapper> actualOpenDS;
  private BioCaseService service;

  @BeforeEach
  void setup() throws IOException {
    var configuration = new Configuration(Configuration.VERSION_2_3_31);
    configuration.setTemplateLoader(
        new FileTemplateLoader(new ClassPathResource("templates").getFile()));
    service = new BioCaseService(mapper, webClient, properties, openDSProperties, factory, configuration,
        kafkaService, rorService);
  }

  @Test
  void testRetrieveData206() throws IOException {
    // Given
    givenJsonWebclient();
    given(responseSpec.bodyToMono(any(Class.class))).willReturn(
        Mono.just(loadResourceFile("biocase/biocase-206-response.xml")));
    given(properties.getItemsPerRequest()).willReturn(10);
    given(properties.getContentNamespace()).willReturn("http://www.tdwg.org/schemas/abcd/2.06");

    // When
    service.retrieveData();

    // Then
    then(rorService).shouldHaveNoInteractions();
    then(kafkaService).shouldHaveNoInteractions();
  }

  @Test
  void testRetrieveData21() throws IOException {
    // Given
    givenJsonWebclient();
    given(responseSpec.bodyToMono(any(Class.class))).willReturn(
        Mono.just(loadResourceFile("biocase/biocase-21-response.xml")));
    given(properties.getItemsPerRequest()).willReturn(10);
    given(openDSProperties.getServiceName()).willReturn("biocase-sng-service");
    given(properties.getContentNamespace()).willReturn("http://www.tdwg.org/schemas/abcd/2.1");
    given(rorService.getRoRId(anyString())).willReturn("https://ror.org/053avzc18");
    var expected = givenExpected21();

    // When
    service.retrieveData();

    // Then
    then(rorService).should()
        .getRoRId(eq("Institute of Vertebrate Biology, The Czech Academy of Sciences (IVB CAS)"));
    then(kafkaService).should().sendMessage(actualOpenDS.capture());
    var actual = actualOpenDS.getValue();
    assertThat(actual.getAuthoritative()).isEqualTo(expected.getAuthoritative());
    assertThat(actual.getImages()).isEqualTo(expected.getImages());
    assertThat(actual.getSourceId()).isEqualTo(expected.getSourceId());
    assertThat(actual.getUnmapped()).hasToString(expected.getUnmapped().toString());
  }

  private OpenDSWrapper givenExpected21() throws IOException {
    return OpenDSWrapper.builder().authoritative(
            Authoritative.builder()
                .physicalSpecimenId("NMPC-DIP-0001")
                .institution("https://ror.org/053avzc18")
                .name("Anthomyza gracilis")
                .materialType("PreservedSpecimen")
                .institutionCode(
                    "Institute of Vertebrate Biology, The Czech Academy of Sciences (IVB CAS)")
                .midslevel(1)
                .build()
        )
        .unmapped(mapper.readTree(loadResourceFile("biocase/unmapped-biocase-21-response.json")))
        .sourceId("biocase-sng-service")
        .build();
  }

  private void givenJsonWebclient() {
    given(properties.getEndpoint()).willReturn(
        "https://api.biodiversitydata.nl/v2/specimen/download");
    given(webClient.get()).willReturn(headersSpec);
    given(headersSpec.uri(anyString())).willReturn(uriSpec);
    given(uriSpec.retrieve()).willReturn(responseSpec);
  }
}
