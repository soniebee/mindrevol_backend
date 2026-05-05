package com.mindrevol.core.modules.integrations.spotify;

import com.mindrevol.core.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyService spotifyService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> searchSpotifyTracks(
            @RequestParam("q") String query) {
        
        // Gọi Service để tìm bài hát
        List<Map<String, String>> tracks = spotifyService.searchTracks(query);
        
        // Trả về đúng format ApiResponse của dự án
        return ResponseEntity.ok(ApiResponse.success(tracks));
    }
}