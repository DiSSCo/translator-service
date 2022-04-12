package eu.dissco.webflux.demo.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;

public class TestUtil {

  public static final String EVENT_ID = "6474f841-b548-48d9-9a53-a9fd3df84084";
  public static final String SERVICE_NAME = "translator-test-service";
  public static final String ENDPOINT = "https://endpoint.com";
  public static final String EVENT_TYPE = "dissco/translator-event";

  public static final String INSTITUTION_NAME = "Naturalis Biodiversity Center";
  public static final String ROR_INSTITUTION = "https://ror.org/0566bfb96";
  public static final String OCCURRENCE_ID = "https://data.biodiversitydata.nl/naturalis/specimen/L.3892015";
  public static final String ID = "L.3892015@BRAHMS";
  public static final String BASIS_OF_RECORD = "Herbarium sheet";
  public static final String SCIENTIFIC_NAME = "Argyrolobium zanonii (Turra) P.W.Ball";
  public static final String IMAGE_URI = "https://medialib.naturalis.nl/file/id/L.3892015/format/large";
  private static final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  public static String loadResourceFile(String fileName) throws IOException {
    return new String(new ClassPathResource(fileName).getInputStream()
        .readAllBytes(), StandardCharsets.UTF_8);
  }

  public static OpenDSWrapper testOpenDSWrapper() {
    return OpenDSWrapper.builder().authoritative(testAuthoritative()).images(List.of(testImage()))
        .sourceId("translator-service").build();
  }

  public static CloudEvent testCloudEvent()
      throws JsonProcessingException {
    return testCloudEvent(testOpenDSWrapper());
  }

  public static CloudEvent testCloudEvent(OpenDSWrapper openDSWrapper)
      throws JsonProcessingException {
    return CloudEventBuilder.v1()
        .withId(EVENT_ID)
        .withSource(URI.create(ENDPOINT))
        .withSubject(SERVICE_NAME)
        .withTime(OffsetDateTime.now(ZoneOffset.UTC))
        .withType(EVENT_TYPE)
        .withDataContentType("application/json")
        .withData(mapper.writeValueAsBytes(testOpenDSWrapper()))
        .build();
  }

  public static Authoritative testAuthoritative() {
    return Authoritative.builder()
        .midslevel(1)
        .curatedObjectID(OCCURRENCE_ID)
        .physicalSpecimenId(ID)
        .institutionCode(INSTITUTION_NAME)
        .institution(ROR_INSTITUTION)
        .materialType(BASIS_OF_RECORD)
        .name(SCIENTIFIC_NAME)
        .build();
  }

  public static Image testImage() {
    return Image.builder()
        .imageUri(IMAGE_URI)
        .build();
  }

}
