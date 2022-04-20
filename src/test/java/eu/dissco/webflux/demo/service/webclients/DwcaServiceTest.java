package eu.dissco.webflux.demo.service.webclients;

import static eu.dissco.webflux.demo.util.TestUtil.EVENT_TYPE;
import static eu.dissco.webflux.demo.util.TestUtil.testCloudEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.DwcaProperties;
import eu.dissco.webflux.demo.properties.EnrichmentProperties;
import eu.dissco.webflux.demo.properties.OpenDSProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
import eu.dissco.webflux.demo.util.TestUtil;
import io.cloudevents.CloudEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

@ExtendWith(MockitoExtension.class)
class DwcaServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private WebClient webClient;
  @Mock
  private RequestHeadersUriSpec headersSpec;
  @Mock
  private RequestHeadersSpec uriSpec;
  @Mock
  private ResponseSpec responseSpec;
  @Mock
  private WebClientProperties properties;
  @Mock
  private OpenDSProperties openDSProperties;
  @Mock
  private DwcaProperties dwcaProperties;
  @Mock
  private KafkaService kafkaService;
  @Mock
  private EnrichmentProperties enrichmentProperties;
  @Mock
  private RorService rorService;

  private DwcaService service;

  @BeforeEach
  void setup() {
    this.service = new DwcaService(mapper, openDSProperties, properties,
        kafkaService, rorService, webClient, dwcaProperties, enrichmentProperties);
  }

  @Test
  @Disabled("Can only be run local due to filesystem permissions")
  void testRetrieveData() throws IOException {
    // Given
    given(properties.getEndpoint()).willReturn("https://endpoint");
    given(dwcaProperties.getDownloadFile())
        .willReturn(getAbsolutePath() + "/darwin.zip");
    given(dwcaProperties.getTempFolder())
        .willReturn(getAbsolutePath() + "/temp");
    given(openDSProperties.getEvenType()).willReturn(EVENT_TYPE);
    given(webClient.get()).willReturn(headersSpec);
    given(headersSpec.uri(anyString())).willReturn(uriSpec);
    given(uriSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.bodyToFlux(DataBuffer.class)).willReturn(
        DataBufferUtils.read(new ClassPathResource("dwca/darwin.zip"),
            new DefaultDataBufferFactory(), 1000));

    // When
    service.retrieveData();

    // Then
    then(kafkaService).should(times(57105)).sendMessage(any(CloudEvent.class));
    FileSystemUtils.deleteRecursively(Path.of("src/test/resources/dwca/test/temp"));
    Files.delete(Path.of("src/test/resources/dwca/test/darwin.zip"));
  }

  @Test
  void testIOException() {
    // Given
    given(properties.getEndpoint()).willReturn("https://endpoint");
    given(dwcaProperties.getDownloadFile()).willReturn(
        new ClassPathResource("src/test/resources/dwca/invalid-dir/darwin.zip").getPath());
    given(webClient.get()).willReturn(headersSpec);
    given(headersSpec.uri(anyString())).willReturn(uriSpec);
    given(uriSpec.retrieve()).willReturn(responseSpec);
    given(responseSpec.bodyToFlux(DataBuffer.class)).willReturn(
        DataBufferUtils.read(new ClassPathResource("dwca/darwin.zip"),
            new DefaultDataBufferFactory(), 1000));

    // When
    service.retrieveData();

    // Then
    then(kafkaService).shouldHaveNoInteractions();
    then(dwcaProperties).shouldHaveNoMoreInteractions();
  }

  private String getAbsolutePath() {
    String path = "src/test/resources/dwca/test";
    File file = new File(path);
    return file.getAbsolutePath();
  }

}
