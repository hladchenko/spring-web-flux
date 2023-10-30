package com.hladchenko.springwebflux.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hladchenko.springwebflux.entity.Picture;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequestMapping("/pictures")
@RestController
public class NasaController {

    private final WebClient webClient;

    public NasaController(WebClient webClient) {
        this.webClient = webClient;
    }

    public static final String URL = "https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?sol=17&api_key=vitG5CeayGcf74hW1oOr2KQcEZvoNx2useVarV8u";

    @GetMapping(value = "/largest", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<byte[]> getLargestPicture() {
        return webClient
                .get()
                .uri(URL)
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(JsonNode.class))
                .flatMapIterable(jsonNode -> jsonNode.findValuesAsText("img_src"))
                .flatMap(this::getPicture)
                .reduce((picture1, picture2) -> picture1.size() > picture2.size() ? picture1 : picture2)
                .map(Picture::src)
                .flatMap(src -> getBinary(src.toString()));
    }

    private Mono<Picture> getPicture(String url) {
         return webClient
                 .head()
                 .uri(url)
                 .exchangeToMono(ClientResponse::toBodilessEntity)
                 .map(HttpEntity::getHeaders)
                 .mapNotNull(HttpHeaders::getLocation)
                 .flatMap(uri -> getSize(uri.toString()).map(size -> new Picture(size, uri)));
    }

    private Mono<Long> getSize(String src) {
         return webClient.head().uri(src).exchangeToMono(ClientResponse::toBodilessEntity)
                 .map(voidResponseEntity -> voidResponseEntity.getHeaders().getContentLength());
    }

    private Mono<byte[]> getBinary(String src) {
        return webClient.get().uri(src).exchangeToMono(clientResponse -> clientResponse.bodyToMono(byte[].class));
    }
}