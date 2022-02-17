package eu.dissco.webflux.demo.service.webclients;

import eu.dissco.webflux.demo.domain.DarwinCore;

import java.util.stream.Stream;

public interface WebClientInterface {

    Stream<DarwinCore> retrieveData();
}
