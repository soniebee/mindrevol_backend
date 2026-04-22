package com.mindrevol.core.modules.recap.service.strategy;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service("cloudinaryRecap")
//@Primary
public class CloudinaryRecapGeneratorImpl implements RecapGeneratorStrategy {

    @Value("${cloudinary.url:cloudinary://mock_key:mock_secret@mock_name}")
    private String cloudinaryUrl;

    @Override
    public String generateVideo(List<String> imageUrls, String audioUrl, int delayMs) throws Exception {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Không có ảnh để tạo Recap");
        }

        Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
        String uniqueTag = "recap_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Bắt đầu tải {} ảnh lên trạm Cloudinary. Tag: {}", imageUrls.size(), uniqueTag);

        try {
            // 1. Upload ảnh tạm VÀ CHUẨN HÓA KÍCH THƯỚC NGAY LÚC NÀY
            // ĐÂY LÀ CHÌA KHÓA: Cắt ảnh đều tăm tắp 1080x1920 trước khi ghép để MP4 Encoder không bị crash
            for (int i = 0; i < imageUrls.size(); i++) {
                String imgUrl = imageUrls.get(i);
                try {
                    cloudinary.uploader().upload(imgUrl, ObjectUtils.asMap(
                            "tags", uniqueTag,
                            "folder", "mindrevol_recaps/temp",
                            "transformation", new Transformation()
                                    .width(1080)
                                    .height(1920)
                                    .crop("fill")
                                    .gravity("auto")
                    ));
                } catch (Exception e) {
                    log.warn("Lỗi đẩy ảnh {} lên Cloudinary, bỏ qua.", imgUrl);
                }
            }

            // Đợi Cloudinary đồng bộ Tag và xử lý Crop để tránh lỗi rỗng
            log.info("Đợi 4 giây để Cloudinary xử lý crop và đồng bộ kho ảnh...");
            Thread.sleep(4000);

            // Quy đổi thời gian hiển thị sang FPS (Ví dụ: 1500ms = 0.67 FPS)
            double fps = 1000.0 / delayMs;
            String fpsValue = String.format(Locale.US, "%.2f", fps);

            log.info("Đang ghép video MP4 với tốc độ {} ms/ảnh (tương đương {} FPS)...", delayMs, fpsValue);
            
            // 2. Render bằng API Multi: Vì ảnh đầu vào đã đều size, MP4 sẽ được ghép thành công 100%
            Map<?, ?> result = cloudinary.uploader().multi(uniqueTag, ObjectUtils.asMap(
                    "format", "mp4",
                    "transformation", new Transformation().fps(fpsValue) // Gắn FPS để chạy chuẩn tốc độ Rùa Bò/Nhanh
            ));

            String finalVideoUrl = (String) result.get("secure_url");
            
            log.info("🎉 Cloudinary render thành công! URL Video: {}", finalVideoUrl);

            // 3. Dọn rác
            try {
                cloudinary.api().deleteResourcesByTag(uniqueTag, ObjectUtils.emptyMap());
            } catch (Exception e) {
                log.warn("Lỗi dọn dẹp ảnh tạm Cloudinary: {}", e.getMessage());
            }

            return finalVideoUrl;

        } catch (Exception e) {
            log.error("Xảy ra lỗi nghiêm trọng khi xử lý qua Cloudinary", e);
            throw new Exception("Lỗi tạo video qua Cloudinary: " + e.getMessage());
        }
    }
}