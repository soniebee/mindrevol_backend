package com.mindrevol.core.modules.tutorial.service.impl;

import com.mindrevol.core.common.utils.SecurityUtils;
import com.mindrevol.core.modules.tutorial.dto.TutorialStatusResponse;
import com.mindrevol.core.modules.tutorial.entity.TutorialProgress;
import com.mindrevol.core.modules.tutorial.repository.TutorialProgressRepository;
import com.mindrevol.core.modules.tutorial.service.TutorialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TutorialServiceImpl implements TutorialService {

    private final TutorialProgressRepository tutorialProgressRepository;

    @Override
    public TutorialStatusResponse getTutorialStatus() {
        // Đổi từ Long sang String, hàm getCurrentUserId() đã có sẵn đúng như file bạn gửi
        String userId = SecurityUtils.getCurrentUserId();

        Optional<TutorialProgress> progressOpt = tutorialProgressRepository.findByUserId(userId);

        if (progressOpt.isPresent()) {
            return new TutorialStatusResponse(progressOpt.get().isCompleted());
        } else {
            return new TutorialStatusResponse(false);
        }
    }

    @Override
    @Transactional
    public TutorialStatusResponse markAsCompleted() {
        // Tương tự, dùng String ở đây
        String userId = SecurityUtils.getCurrentUserId();

        TutorialProgress progress = tutorialProgressRepository.findByUserId(userId)
                .orElseGet(() -> {
                    TutorialProgress newProgress = new TutorialProgress();
                    newProgress.setUserId(userId);
                    return newProgress;
                });

        progress.setCompleted(true);
        tutorialProgressRepository.save(progress);

        return new TutorialStatusResponse(true);
    }
}