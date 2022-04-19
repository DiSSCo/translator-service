package eu.dissco.webflux.demo.service.webclients;

import abcd206.DataSets;
import abcd206.Unit;
import abcd206.Unit.Identifications;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.Image;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.OpenDSProperties;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.CloudEventService;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@AllArgsConstructor
@Profile(Profiles.BIO_CASE)
public class BioCaseService implements WebClientInterface {

  private static final String START_AT = "startAt";
  private static final String LIMIT = "limit";

  private final ObjectMapper mapper;

  private final WebClient webClient;

  private final WebClientProperties properties;
  private final OpenDSProperties openDSProperties;

  private final XMLInputFactory factory;

  private final Configuration configuration;
  private final KafkaService kafkaService;
  private final RorService rorService;
  private final CloudEventService cloudEventService;


  @Override
  public void retrieveData() {
    var uri = properties.getEndpoint();
    var templateProperties = getTemplateProperties();
    configuration.setNumberFormat("computer");
    var finished = false;
    while (!finished) {
      var recordList = new ArrayList<OpenDSWrapper>();
      log.info("Currently at: {} still collecting...", templateProperties.get(START_AT));
      StringWriter writer = fillTemplate(templateProperties);
      try {
        finished = webClient.get().uri(uri + properties.getQueryParams() + writer).retrieve()
            .bodyToMono(String.class).map(xml -> mapToDarwin(xml, recordList)).toFuture().get();
        if (recordList.isEmpty()) {
          log.info("Unable to get records from xml");
          finished = true;
        }
      } catch (InterruptedException | ExecutionException e) {
        log.error("Failed to get response from uri", e);
        Thread.currentThread().interrupt();
        return;
      }
      updateStartAtParameter(templateProperties);
      recordList.stream()
          .filter(openDS -> openDS.getAuthoritative().getMaterialType().equals("PreservedSpecimen"))
          .map(this::addRoR)
          .map(cloudEventService::createCloudEvent)
          .filter(Objects::nonNull)
          .forEach(kafkaService::sendMessage);
    }
  }

  private OpenDSWrapper addRoR(OpenDSWrapper openDSWrapper) {
    if (openDSProperties.getOrganisationId() != null) {
      openDSWrapper.getAuthoritative()
          .setInstitution(openDSProperties.getOrganisationId());
    } else {
      openDSWrapper.getAuthoritative()
          .setInstitution(
              rorService.getRoRId(openDSWrapper.getAuthoritative().getInstitutionCode()));
    }
    return openDSWrapper;
  }

  private StringWriter fillTemplate(Map<String, Object> templateProperties) {
    var writer = new StringWriter();
    try {
      var template = configuration.getTemplate("biocase-request.ftl");
      template.process(templateProperties, writer);
    } catch (IOException | TemplateException e) {
      log.error("Failed to retrieve template", e);
    }
    return writer;
  }

  private void updateStartAtParameter(Map<String, Object> templateProperties) {
    templateProperties.put(START_AT,
        ((int) templateProperties.get(START_AT) + (int) templateProperties.get(LIMIT)));
  }

  private Map<String, Object> getTemplateProperties() {
    var map = new HashMap<String, Object>();
    map.put("contentNamespace", properties.getContentNamespace());
    map.put(LIMIT, properties.getItemsPerRequest());
    map.put(START_AT, 0);
    return map;
  }

  private boolean mapToDarwin(String xml, ArrayList<OpenDSWrapper> list) {
    var recordCount = 0;
    try {
      var xmlEventReader = factory.createXMLEventReader(new StringReader(xml));
      while (xmlEventReader.hasNext()) {
        var element = xmlEventReader.nextEvent();
        if (isStartElement(element, "content")) {
          recordCount = Integer.parseInt(
              element.asStartElement().getAttributeByName(new QName("recordCount")).getValue());
        }
        retrieveUnitData(list, xmlEventReader);
      }
      return recordCount % properties.getItemsPerRequest() != 0;
    } catch (XMLStreamException e) {
      log.info("Error converting response tot XML", e);
    }
    return false;
  }

  private void retrieveUnitData(ArrayList<OpenDSWrapper> list, XMLEventReader xmlEventReader)
      throws XMLStreamException {
    mapper.setSerializationInclusion(Include.NON_NULL);
    if (isStartElement(xmlEventReader.peek(), "DataSets")) {
      if (properties.getContentNamespace().equals("http://www.tdwg.org/schemas/abcd/2.06")) {
        try {
          mapABCD206(list, xmlEventReader);
        } catch (JAXBException e) {
          e.printStackTrace();
        }
      } else if (properties.getContentNamespace().equals("http://www.tdwg.org/schemas/abcd/2.1")) {
        try {
          mapABCD21(list, xmlEventReader);
        } catch (JAXBException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void mapABCD206(ArrayList<OpenDSWrapper> list, XMLEventReader xmlEventReader)
      throws JAXBException {
    var context = JAXBContext.newInstance(DataSets.class);
    var datasetsMarshaller = context.createUnmarshaller().unmarshal(xmlEventReader, DataSets.class);
    var datasets = datasetsMarshaller.getValue().getDataSet().get(0);
    var organisation = datasets.getMetadata().getOwners().getOwner().get(0)
        .getOrganisation().getName().getRepresentation().get(0).getText();
    for (var unit : datasets.getUnits().getUnit()) {
      var authoritative = Authoritative.builder()
          .institutionCode(organisation)
          .materialType(unit.getRecordBasis().value()).physicalSpecimenId(unit.getUnitID())
          .midslevel(1).curatedObjectID(unit.getRecordURI())
          .name(determineName206(unit.getIdentifications(), unit.getUnitID()))
          .build();
      var images = retrieveImages206(unit);
      list.add(
          OpenDSWrapper.builder().authoritative(authoritative).images(images)
              .sourceId("translator-service")
              .unmapped(getUnmapped(mapper.valueToTree(unit))).build());
    }
  }

  private List<Image> retrieveImages206(Unit unit) {
    var images = new ArrayList<Image>();
    if (unit.getMultiMediaObjects() == null || unit.getMultiMediaObjects().getMultiMediaObject()
        .isEmpty()) {
      return null;
    } else {
      unit.getMultiMediaObjects().getMultiMediaObject()
          .forEach(media -> images.add(Image.builder().imageUri(media.getFileURI()).build()));
      return images;
    }
  }

  private List<Image> retrieveImages21(abcd21.Unit unit) {
    var images = new ArrayList<Image>();
    if (unit.getMultiMediaObjects() == null || unit.getMultiMediaObjects().getMultiMediaObject()
        .isEmpty()) {
      return null;
    } else {
      unit.getMultiMediaObjects().getMultiMediaObject()
          .forEach(media -> images.add(Image.builder().imageUri(media.getFileURI()).build()));
      return images;
    }
  }

  private JsonNode getUnmapped(ObjectNode node) {
    return node.remove(List.of("unitID", "recordURI", "recordBasis"));
  }

  private void mapABCD21(ArrayList<OpenDSWrapper> list, XMLEventReader xmlEventReader)
      throws JAXBException {
    var context = JAXBContext.newInstance(abcd21.DataSets.class);
    var datasetsMarshaller = context.createUnmarshaller()
        .unmarshal(xmlEventReader, abcd21.DataSets.class);
    var datasets = datasetsMarshaller.getValue().getDataSet().get(0);
    var organisation = datasets.getMetadata().getOwners().getOwner().get(0)
        .getOrganisation().getName().getRepresentation().get(0).getText();
    for (var unit : datasets.getUnits().getUnit()) {
      var authoritative = Authoritative.builder()
          .institutionCode(organisation)
          .materialType(unit.getRecordBasis().value()).physicalSpecimenId(unit.getUnitID())
          .midslevel(1).curatedObjectID(unit.getRecordURI())
          .name(determineName21(unit.getIdentifications(), unit.getUnitID()))
          .build();
      var images = retrieveImages21(unit);
      list.add(
          OpenDSWrapper.builder().authoritative(authoritative).images(images)
              .sourceId(openDSProperties.getServiceName())
              .unmapped(getUnmapped(mapper.valueToTree(unit))).build());
    }
  }

  private String determineName21(abcd21.Unit.Identifications identifications, String unitId) {
    if (identifications != null) {
      var identification = identifications.getIdentification();
      if (identification.size() == 1) {
        return identification.get(0).getResult().getTaxonIdentified().getScientificName()
            .getFullScientificNameString();
      } else {
        for (var id : identification) {
          if (Boolean.TRUE.equals(id.isPreferredFlag())
              && id.getResult().getTaxonIdentified().getScientificName() != null) {
            return id.getResult().getTaxonIdentified().getScientificName()
                .getFullScientificNameString();
          }
        }
        log.warn("No preferred name for object: {} returning null", unitId);
        return null;
      }
    } else {
      log.warn("No identification found for object: {} returning null", unitId);
      return null;
    }
  }

  private String determineName206(Identifications identifications, String unitId) {
    if (identifications != null) {
      var identification = identifications.getIdentification();
      if (identification.size() == 1) {
        return identification.get(0).getResult().getTaxonIdentified().getScientificName()
            .getFullScientificNameString();
      } else {
        for (var id : identification) {
          if (Boolean.TRUE.equals(id.isPreferredFlag())
              && id.getResult().getTaxonIdentified().getScientificName() != null) {
            return id.getResult().getTaxonIdentified().getScientificName()
                .getFullScientificNameString();
          }
        }
        log.warn("No preferred name for object: {} returning null", unitId);
        return null;
      }
    } else {
      log.warn("No identification found for object: {} returning null", unitId);
      return null;
    }
  }

  private boolean isStartElement(XMLEvent element, String field) {
    if (element != null) {
      return element.isStartElement() && element.asStartElement().getName().getLocalPart()
          .equals(field);
    } else {
      return false;
    }
  }

}
