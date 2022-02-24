package eu.dissco.webflux.demo.service.webclients;

import eu.dissco.webflux.demo.Profiles;
import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import eu.dissco.webflux.demo.properties.WebClientProperties;
import eu.dissco.webflux.demo.service.KafkaService;
import eu.dissco.webflux.demo.service.RorService;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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

  private final WebClient webClient;

  private final WebClientProperties properties;

  private final XMLInputFactory factory;

  private final Configuration configuration;
  private final RorService rorService;
  private final KafkaService kafkaService;


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
      } catch (InterruptedException | ExecutionException e) {
        log.error("Failed to get response from uri", e);
        Thread.currentThread().interrupt();
      }
      updateStartAtParameter(templateProperties);
      recordList.forEach(kafkaService::sendMessage);
    }
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
    var organisation = "Unknown";
    try {
      var xmlEventReader = factory.createXMLEventReader(new StringReader(xml));
      while (xmlEventReader.hasNext()) {
        var element = xmlEventReader.nextEvent();
        if (isStartElement(element, "content")) {
          recordCount = Integer.parseInt(
              element.asStartElement().getAttributeByName(new QName("recordCount")).getValue());
        }
        if (isStartElement(element, "Organisation")) {
          organisation = retrieveOrganisation(xmlEventReader);
        }
        retrieveUnitData(list, xmlEventReader, element, organisation);
      }
    } catch (XMLStreamException e) {
      log.info("Error converting response tot XML", e);
    }
    return recordCount % properties.getItemsPerRequest() != 0;
  }

  private String retrieveOrganisation(XMLEventReader xmlEventReader) throws XMLStreamException {
    while (xmlEventReader.hasNext()) {
      var element = xmlEventReader.nextEvent();
      if (isStartElement(element, "Text")) {
        return getData(xmlEventReader);
      }
    }
    throw new XMLStreamException("Missing Organisation Text element");
  }

  private void retrieveUnitData(ArrayList<OpenDSWrapper> list, XMLEventReader xmlEventReader,
      XMLEvent element, String organisation) {
    try {
      if (isStartElement(element, "Unit")) {
        list.add(
            OpenDSWrapper.builder().authoritative(getUnitProperties(xmlEventReader, organisation))
                .sourceId("translator-service").build());
      }
    } catch (XMLStreamException e) {
      log.error("Failed to process Unit xml to object", e);
    }
  }

  private Authoritative getUnitProperties(XMLEventReader xmlEventReader, String organisation)
      throws XMLStreamException {
    var authoritative = Authoritative.builder().institution(rorService.getRoRId(organisation))
        .institutionCode(organisation).midslevel(1);
    while (xmlEventReader.hasNext()) {
      var element = xmlEventReader.nextEvent();
      if (isStartElement(element, "UnitID")) {
        authoritative.physicalSpecimenId(getData(xmlEventReader));
      }
      if (isStartElement(element, "FullScientificNameString")) {
        authoritative.name(getData(xmlEventReader));
      }
      if (isStartElement(element, "RecordBasis")) {
        authoritative.materialType(getData(xmlEventReader));
      }
      if (isEndElement(element, "Unit")) {
        return authoritative.build();
      }
    }
    throw new XMLStreamException("No data found in Unit XML");
  }

  private String getData(XMLEventReader xmlEventReader) throws XMLStreamException {
    return xmlEventReader.nextEvent().asCharacters().getData();
  }

  private boolean isStartElement(XMLEvent element, String field) {
    return element.isStartElement() && element.asStartElement().getName().getLocalPart()
        .equals(field);
  }

  private boolean isEndElement(XMLEvent element, String field) {
    return element.isEndElement() && element.asEndElement().getName().getLocalPart()
        .equals(field);
  }

}
