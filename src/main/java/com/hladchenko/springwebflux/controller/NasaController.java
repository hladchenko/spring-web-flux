package com.hladchenko.springwebflux.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hladchenko.springwebflux.entity.Picture;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequestMapping("/pictures")
@RestController
public class NasaController {

    public static final String URL = "https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?sol=17&api_key=vitG5CeayGcf74hW1oOr2KQcEZvoNx2useVarV8u";

    @GetMapping(value = "/largest", produces = "image/jpeg")
    public Mono<byte[]> getLargestPicture() {
        return WebClient
                .create()
                .get()
                .uri(URL)
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(JsonNode.class))
                .flatMapIterable(jsonNode -> jsonNode.findValuesAsText("img_src"))
                .flatMap(NasaController::getPicture)
                .reduce((picture1, picture2) -> picture1.size() > picture2.size() ? picture1 : picture2)
                .map(Picture::src)
                .flatMap(src -> getBinary(src.toString()));
    }

    private static Mono<Picture> getPicture(String url) {
         return WebClient.create(url)
                 .head()
                 .exchangeToMono(ClientResponse::toBodilessEntity)
                 .map(HttpEntity::getHeaders)
                 .mapNotNull(HttpHeaders::getLocation)
                 .flatMap(uri -> getSize(uri.toString()).map(size -> new Picture(size, uri)));
    }

    private static Mono<Long> getSize(String src) {
         return WebClient.create(src).get().exchangeToMono(ClientResponse::toBodilessEntity)
                 .map(voidResponseEntity -> voidResponseEntity.getHeaders().getContentLength());
    }

    private static Mono<byte[]> getBinary(String src) {
        return WebClient.create(src).get().exchangeToMono(clientResponse -> clientResponse.bodyToMono(byte[].class));
    }
}