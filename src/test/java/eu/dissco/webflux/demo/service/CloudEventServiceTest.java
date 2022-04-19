package eu.dissco.webflux.demo.service;

import static eu.dissco.webflux.demo.util.TestUtil.ENDPOINT;
import static eu.dissco.webflux.demo.util.TestUtil.EVENT_TYPE;
import static eu.dissco.webflux.demo.util.TestUtil.SERVICE_NAME;
import static eu.dissco.webflux.demo.util.TestUtil.testCloudEvent;
import static eu.dissco.webflux.demo.util.TestUtil.testOpenDSWrapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.properties.OpenDSProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CloudEventServiceTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Mock
  private OpenDSProperties openDSProperties;
  @Mock
  private WebClientProperties webClientProperties;

  private CloudEventService service;

  @BeforeEach
  void setup() {
    service = new CloudEventService(mapper, openDSProperties, webClientProperties);
  }

  @Test
  void testCreateCloudEvent() throws JsonProcessingException {
    // Given
    given(openDSProperties.getServiceName()).willReturn(SERVICE_NAME);
    given(openDSProperties.getEvenType()).willReturn(EVENT_TYPE);
    given(webClientProperties.getEndpoint()).willReturn(ENDPOINT);

    // When
    var result = service.createCloudEvent(testOpenDSWrapper());

    // Then
    assertThat(result).usingRecursiveComparison().ignoringFields("id", "time")
        .isEqualTo(testCloudEvent());
  }

}
