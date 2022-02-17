package eu.dissco.webflux.demo.service.webclients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import eu.dissco.webflux.demo.domain.DarwinCore;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.util.TestUtil;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import java.io.IOException;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
  @Mock
  private WebClient webClient;
  @Mock
  private WebClientProperties properties;
  @Mock
  private RequestHeadersUriSpec headersSpec;
  @Mock
  private RequestHeadersSpec uriSpec;
  @Mock
  private ResponseSpec responseSpec;

  private BioCaseService service;

  @BeforeEach
  void setup() throws IOException {
    var configuration = new Configuration(Configuration.VERSION_2_3_31);
    configuration.setTemplateLoader(
        new FileTemplateLoader(new ClassPathResource("templates").getFile()));
    service = new BioCaseService(webClient, properties, factory, configuration);
  }

  @Test
  void testRetrieveData() throws IOException {
    // Given
    givenJsonWebclient();
    given(responseSpec.bodyToMono(any(Class.class))).willReturn(
        Mono.just(TestUtil.loadResourceFile("biocase/biocase-response.xml")));
    given(properties.getItemsPerRequest()).willReturn(10);
    given(properties.getContentNamespace()).willReturn("test");
    var expected = givenDarwin();

    // When
    var result = service.retrieveData();
    // Then
    then(responseSpec).should().bodyToMono(any(Class.class));
    assertThat(result.toList()).isEqualTo(List.of(expected));
  }

  private DarwinCore givenDarwin() {
    return DarwinCore.builder()
        .id("NMPC-DIP-0001")
        .institutionID("Institute of Vertebrate Biology, The Czech Academy of Sciences (IVB CAS)")
        .scientificName("Anthomyza gracilis")
        .basisOfRecord("PreservedSpecimen")
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
