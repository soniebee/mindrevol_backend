package com.mindrevol.core.modules.box.service;

import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.entity.Box;
import com.mindrevol.core.modules.box.entity.BoxMember;
import com.mindrevol.core.modules.box.entity.BoxRole;
import com.mindrevol.core.modules.box.mapper.BoxMapper;
import com.mindrevol.core.modules.box.repository.BoxMemberRepository;
import com.mindrevol.core.modules.box.repository.BoxRepository;
import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.core.modules.journey.entity.Journey;
import com.mindrevol.core.modules.journey.entity.JourneyStatus; // Import enum chuẩn
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoxServiceImpl implements BoxService {

    private final BoxRepository boxRepository;
    private final BoxMemberRepository boxMemberRepository;
    private final UserRepository userRepository;
    private final BoxMapper boxMapper;

    @Override
    @Transactional
    public BoxDetailResponse createBox(CreateBoxRequest request, String userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));

        Box box = boxMapper.toEntity(request);
        box.setOwner(owner);
        box.setLastActivityAt(LocalDateTime.now());
        box = boxRepository.save(box);

        BoxMember member = BoxMember.builder()
                .box(box)
                .user(owner)
                .role(BoxRole.ADMIN)
                .build();
        boxMemberRepository.save(member);

        return boxMapper.toDetailResponse(box, 1, BoxRole.ADMIN.name());
    }

    @Override
    public Page<BoxResponse> getMyBoxes(String userId, Pageable pageable) {
        Page<Box> boxes = boxRepository.findMyBoxes(userId, pageable);

        return boxes.map(box -> {
            long memberCount = boxMemberRepository.countByBoxId(box.getId());
            return boxMapper.toResponse(box, memberCount);
        });
    }

    @Override
    public BoxDetailResponse getBoxDetail(String boxId, String userId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Box"));

        BoxMember myMembership = boxMemberRepository.findByBoxIdAndUserId(boxId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải là thành viên của Box này"));

        long memberCount = boxMemberRepository.countByBoxId(boxId);

        BoxDetailResponse response = boxMapper.toDetailResponse(box, memberCount, myMembership.getRole().name());

        List<JourneyResponse> ongoing = new ArrayList<>();
        List<JourneyResponse> ended = new ArrayList<>();

        if (box.getJourneys() != null) {
            for (Journey j : box.getJourneys()) {
                JourneyResponse jr = JourneyResponse.builder()
                        .id(j.getId())
                        .name(j.getName())
                        .status(j.getStatus()) // Sử dụng status thực tế
                        .build();

                // Dùng Enum JourneyStatus để phân loại chính xác
                if (JourneyStatus.COMPLETED.equals(j.getStatus())) {
                    ended.add(jr);
                } else {
                    ongoing.add(jr);
                }
            }
        }

        response.setOngoingJourneys(ongoing);
        response.setEndedJourneys(ended);

        return response;
    }
}