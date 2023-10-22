package com.hladchenko.springwebflux.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RequestMapping("/pictures")
@RestController
public class NasaController {

    public static final String URL = "";

    @GetMapping("/largest")
    public Flux<String> getLargestPicture() {

        Flux<String> srcFlux = WebClient.create(URL)
                .get().exchangeToMono(clientResponse -> clientResponse.bodyToMono(JsonNode.class))
                .map(jsonNode -> jsonNode.get("photos"))
                .flatMapIterable(jsonNode -> jsonNode.findValuesAsText("img_src"));


        return srcFlux;
    }
}