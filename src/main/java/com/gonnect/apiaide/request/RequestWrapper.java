package com.gonnect.apiaide.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RequestWrapper {

    private final WebClient client;

    @Autowired
    public RequestWrapper(WebClient.Builder builder,
                          @Value("${API_TOKEN}") String apiToken) {
        this.client = builder.defaultHeader("Authorization", "Bearer " + apiToken)
                .build();
    }

    public <T> ResponseEntity<T> get(String url, Class<T> responseType, Object... uriVars) {
        return client.get()
                .uri(url, uriVars)
                .retrieve()
                .toEntity(responseType)
                .block();
    }

    public <T> ResponseEntity<T> post(String url, Object request, Class<T> responseType, Object... uriVars) {
        return client.post()
                .uri(url, uriVars)
                .bodyValue(request)
                .retrieve()
                .toEntity(responseType)
                .block();
    }

    public <T> ResponseEntity<T> put(String url, Object request, Class<T> responseType, Object... uriVars) {
        return client.put()
                .uri(url, uriVars)
                .bodyValue(request)
                .retrieve()
                .toEntity(responseType)
                .block();
    }

    public <T> ResponseEntity<T> patch(String url, Object request, Class<T> responseType, Object... uriVars) {
        return client.patch()
                .uri(url, uriVars)
                .bodyValue(request)
                .retrieve()
                .toEntity(responseType)
                .block();
    }

    public <T> ResponseEntity<T> delete(String url, Class<T> responseType, Object... uriVars) {
        return client.delete()
                .uri(url, uriVars)
                .retrieve()
                .toEntity(responseType)
                .block();
    }
}
