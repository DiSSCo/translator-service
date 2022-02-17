package eu.dissco.webflux.demo.service;

import eu.dissco.webflux.demo.domain.Authoritative;
import eu.dissco.webflux.demo.domain.DarwinCore;
import eu.dissco.webflux.demo.domain.OpenDSWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class OpenDSMappingService {

    private final RorService rorService;

    public OpenDSWrapper mapToAuthoritative(DarwinCore darwinCore) {
        var authoritative = Authoritative.builder()
                .curatedObjectID(darwinCore.getOccurrenceID())
                .name(darwinCore.getScientificName())
                .midslevel(1)
                .institution(rorService.getRoRId(darwinCore.getInstitutionID()))
                .materialType(darwinCore.getBasisOfRecord())
                .physicalSpecimenId(darwinCore.getId())
                .institutionCode(darwinCore.getInstitutionID()).build();
        return OpenDSWrapper.builder().authoritative(authoritative).images(darwinCore.getImages())
                .sourceId("translator-service").build();
    }

}
