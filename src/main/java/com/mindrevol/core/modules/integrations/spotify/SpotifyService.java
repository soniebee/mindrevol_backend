package com.mindrevol.core.modules.integrations.spotify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpotifyService {
    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken = null;

    // 1. Lấy Token đại diện cho App MindRevol
    private void fetchAccessToken() {
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedAuth);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("https://accounts.spotify.com/api/token", request, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            this.accessToken = root.path("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy token Spotify");
        }
    }

    // 2. Tìm kiếm bài hát
    public List<Map<String, String>> searchTracks(String query) {
        if (accessToken == null) fetchAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=5";
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            JsonNode tracks = objectMapper.readTree(response.getBody()).path("tracks").path("items");
            
            List<Map<String, String>> results = new ArrayList<>();
            for (JsonNode track : tracks) {
                results.add(Map.of(
                    "id", track.path("id").asText(),
                    "title", track.path("name").asText(),
                    "artist", track.path("artists").get(0).path("name").asText(),
                    "albumArt", track.path("album").path("images").get(2).path("url").asText() // Lấy ảnh nhỏ cho nhẹ
                ));
            }
            return results;
        } catch (Exception e) {
            // Nếu token hết hạn (401), gọi lại fetchAccessToken() và thử lại ở một luồng đàng hoàng hơn.
            fetchAccessToken(); 
            return new ArrayList<>();
        }
    }
}