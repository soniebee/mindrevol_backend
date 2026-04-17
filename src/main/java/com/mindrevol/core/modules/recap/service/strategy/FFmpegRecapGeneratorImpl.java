package com.mindrevol.core.modules.recap.service.strategy;

import com.mindrevol.core.modules.storage.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Service("ffmpegRecap")
@RequiredArgsConstructor
@Primary
public class FFmpegRecapGeneratorImpl implements RecapGeneratorStrategy {

    private final FileStorageService fileStorageService;

    @Override
    public String generateVideo(List<String> imageUrls, String audioUrl, int delayMs) throws Exception {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Không có ảnh để tạo Recap");
        }

        String jobId = UUID.randomUUID().toString();
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "mindrevol_recap_" + jobId);
        Files.createDirectories(tempDir);
        
        String outputMp4Path = tempDir.resolve("output.mp4").toString();

        try {
            log.info("Đang tải {} ảnh về thư mục tạm...", imageUrls.size());
            for (int i = 0; i < imageUrls.size(); i++) {
                String imgUrl = imageUrls.get(i);
                String fileName = String.format("img%03d.jpg", i + 1);
                downloadFile(imgUrl, tempDir.resolve(fileName).toString());
            }

            double fps = 1000.0 / delayMs;
            String framerateStr = String.format(java.util.Locale.US, "%.2f", fps);

            String videoFilter = "scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,format=yuv420p";

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", 
                    "-framerate", framerateStr, 
                    "-i", tempDir.resolve("img%03d.jpg").toString(),
                    "-vf", videoFilter,
                    "-c:v", "libx264", 
                    "-r", "30",
                    outputMp4Path
            );

            log.info("Đang chạy FFmpeg rendering tốc độ {} ms/ảnh ({} fps)...", delayMs, framerateStr);
            Process process = pb.start();
            
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.debug("[FFmpeg] {}", line);
                    }
                } catch (IOException ignored) {}
            }).start();

            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new RuntimeException("Lỗi FFmpeg Render, Exit code: " + exitCode);
            }

            log.info("Render xong, đang upload lên Storage...");
            File finalVideo = new File(outputMp4Path);
            try (InputStream is = new FileInputStream(finalVideo)) {
                return fileStorageService.uploadStream(
                        is, 
                        "recap_" + jobId + ".mp4", 
                        "video/mp4", 
                        finalVideo.length()
                );
            }

        } finally {
            deleteDirectoryRecursively(tempDir.toFile());
            log.info("Đã dọn dẹp thư mục rác: {}", tempDir);
        }
    }

    public byte[] generatePreviewVideo(List<String> imageUrls, int delayMs) throws Exception {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Không có ảnh để tạo Recap");
        }

        String jobId = UUID.randomUUID().toString();
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "mindrevol_preview_" + jobId);
        Files.createDirectories(tempDir);
        
        String outputMp4Path = tempDir.resolve("output.mp4").toString();

        try {
            log.info("Bắt đầu tải SONG SONG {} ảnh để làm Xem trước...", imageUrls.size());
            
            // [TỐI ƯU 1] TẢI ẢNH SONG SONG (PARALLEL STREAM)
            // Thay vì dùng vòng lặp for tải từng ảnh, Java sẽ mở nhiều luồng tải cùng lúc
            IntStream.range(0, imageUrls.size()).parallel().forEach(i -> {
                String imgUrl = imageUrls.get(i);
                String fileName = String.format("img%03d.jpg", i + 1);
                try {
                    downloadFile(imgUrl, tempDir.resolve(fileName).toString());
                } catch (Exception e) {
                    log.warn("Lỗi tải ảnh ({}): {}", fileName, imgUrl);
                }
            });

            double fps = 1000.0 / delayMs;
            String framerateStr = String.format(java.util.Locale.US, "%.2f", fps);

            String videoFilter = "scale=1080:1080:force_original_aspect_ratio=increase,crop=1080:1080,format=yuv420p";

            // [TỐI ƯU 2] ÉP XUNG FFMPEG (ULTRAFAST)
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", 
                    "-threads", "0",              // Cho phép FFmpeg xài toàn bộ nhân CPU hiện có
                    "-framerate", framerateStr, 
                    "-i", tempDir.resolve("img%03d.jpg").toString(),
                    "-vf", videoFilter,
                    "-c:v", "libx264", 
                    "-preset", "ultrafast",       // TĂNG TỐC RENDER GẤP 10 LẦN (Bỏ qua nén sâu)
                    "-tune", "stillimage",        // Báo cho FFmpeg biết đây là video ghép từ ảnh tĩnh để nó tối ưu
                    "-r", "30", 
                    outputMp4Path
            );

            log.info("Bắt đầu Render FFmpeg (Ultrafast Mode) tốc độ {} fps...", framerateStr);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                // Đọc lỗi nếu thất bại
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) log.error("[FFmpeg] {}", line);
                }
                throw new RuntimeException("Lỗi FFmpeg Render Preview, Exit code: " + exitCode);
            }

            log.info("Render hoàn tất! Đang trả video về cho người dùng...");
            return Files.readAllBytes(Paths.get(outputMp4Path));

        } finally {
            deleteDirectoryRecursively(tempDir.toFile()); 
        }
    }

    private void downloadFile(String urlStr, String savePath) throws Exception {
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new URL(urlStr).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(5000); // Set timeout để không bị kẹt nếu ảnh lỗi
        connection.setReadTimeout(5000);
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, Paths.get(savePath));
        }
    }

    private void deleteDirectoryRecursively(File fileToBeDeleted) {
        File[] allContents = fileToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectoryRecursively(file);
            }
        }
        fileToBeDeleted.delete();
    }
}