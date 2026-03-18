package com.mindrevol.core.modules.box.service.impl;

import com.mindrevol.core.common.exception.BadRequestException;
import com.mindrevol.core.common.exception.ResourceNotFoundException;
import com.mindrevol.core.modules.box.dto.request.CreateBoxRequest;
import com.mindrevol.core.modules.box.dto.request.UpdateBoxRequest;
import com.mindrevol.core.modules.box.dto.response.BoxDetailResponse;
import com.mindrevol.core.modules.box.dto.response.BoxResponse;
import com.mindrevol.core.modules.box.entity.Box;
import com.mindrevol.core.modules.box.entity.BoxInvitation;
import com.mindrevol.core.modules.box.entity.BoxMember;
import com.mindrevol.core.modules.box.entity.BoxRole;
import com.mindrevol.core.modules.box.event.BoxCreatedEvent;
import com.mindrevol.core.modules.box.event.BoxMemberAddedEvent;
import com.mindrevol.core.modules.box.event.BoxMemberInvitedEvent;
import com.mindrevol.core.modules.box.mapper.BoxMapper;
import com.mindrevol.core.modules.box.repository.BoxInvitationRepository;
import com.mindrevol.core.modules.box.repository.BoxMemberRepository;
import com.mindrevol.core.modules.box.repository.BoxRepository;
import com.mindrevol.core.modules.box.service.BoxService;
import com.mindrevol.core.modules.journey.dto.response.JourneyResponse;
import com.mindrevol.core.modules.journey.entity.Journey;
import com.mindrevol.core.modules.journey.entity.JourneyStatus;
import com.mindrevol.core.modules.user.entity.User;
import com.mindrevol.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final BoxInvitationRepository boxInvitationRepository;
    private final UserRepository userRepository;
    private final BoxMapper boxMapper;

    // 📢 Tiêm công cụ phát sự kiện vào đây
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // PHẦN 1: QUẢN LÝ BOX CƠ BẢN (CODE CŨ CỦA BẠN)
    // =========================================================================

    @Override
    @Transactional
    public BoxDetailResponse createBox(CreateBoxRequest request, String userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));

        // 1. Map dữ liệu cơ bản (bao gồm cả textPosition nếu có truyền)
        Box box = boxMapper.toEntity(request);
        box.setOwner(owner);
        box.setLastActivityAt(LocalDateTime.now());

        // Đảm bảo cập nhật thủ công nếu Mapper chưa map kịp
        if (request.getTextPosition() != null) {
            box.setTextPosition(request.getTextPosition());
        }

        box = boxRepository.save(box);

<<<<<<< Updated upstream
=======
        // 2. Add owner vào làm ADMIN
>>>>>>> Stashed changes
        BoxMember member = BoxMember.builder()
                .box(box)
                .user(owner)
                .role(BoxRole.ADMIN)
                .build();
        boxMemberRepository.save(member);

        // 🔥 PRO MAX: 3. Tự động gửi lời mời bạn bè nếu có danh sách inviteUserIds
        if (request.getInviteUserIds() != null && !request.getInviteUserIds().isEmpty()) {
            for (String inviteeId : request.getInviteUserIds()) {
                try {
                    inviteMember(box.getId(), inviteeId, owner.getId());
                } catch (Exception e) {
                    // Nếu lỗi khi mời 1 người (vd: đã mời rồi), bỏ qua để tiếp tục tạo Box
                }
            }
        }

        // 🔥 PRO MAX: 4. Bắn sự kiện ra để Module Chat tự động tạo Group Chat
        eventPublisher.publishEvent(new BoxCreatedEvent(
                box.getId(), box.getName(), owner.getId(), List.of(owner.getId())
        ));

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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Box"));

        BoxMember myMembership = boxMemberRepository.findByBoxIdAndUserId(boxId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải là thành viên của Box này"));

        long memberCount = boxMemberRepository.countByBoxId(boxId);
        BoxDetailResponse response = boxMapper.toDetailResponse(box, memberCount, myMembership.getRole().name());

        // Giữ nguyên logic chia Hành trình (Journey) rất hay của bạn
        List<JourneyResponse> ongoing = new ArrayList<>();
        List<JourneyResponse> ended = new ArrayList<>();

        if (box.getJourneys() != null) {
            for (Journey j : box.getJourneys()) {
                JourneyResponse jr = JourneyResponse.builder()
                        .id(j.getId())
                        .name(j.getName())
                        .status(j.getStatus())
                        .build();

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

    @Override
    @Transactional
    public BoxDetailResponse updateBox(String boxId, UpdateBoxRequest request, String userId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Box"));

        BoxMember myMembership = boxMemberRepository.findByBoxIdAndUserId(boxId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải là thành viên của Box này"));

        // Kiểm tra quyền: Chỉ Admin mới được sửa Box
        if (!BoxRole.ADMIN.equals(myMembership.getRole())) {
            throw new BadRequestException("Chỉ Admin mới có quyền chỉnh sửa Box");
        }

        // Cập nhật các trường có thay đổi
        if (request.getName() != null) box.setName(request.getName());
        if (request.getDescription() != null) box.setDescription(request.getDescription());
        if (request.getThemeSlug() != null) box.setThemeSlug(request.getThemeSlug());
        if (request.getAvatar() != null) box.setAvatar(request.getAvatar());
        // Thêm cập nhật textPosition từ màn hình FE mới
        if (request.getTextPosition() != null) box.setTextPosition(request.getTextPosition());

        boxRepository.save(box);

        // Gọi lại hàm getBoxDetail để trả về dữ liệu mới nhất
        return getBoxDetail(boxId, userId);
    }

    @Override
    @Transactional
    public void deleteBox(String boxId, String userId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Box"));

        BoxMember myMembership = boxMemberRepository.findByBoxIdAndUserId(boxId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền thực hiện hành động này"));

        if (!BoxRole.ADMIN.equals(myMembership.getRole())) {
            throw new BadRequestException("Chỉ Admin mới có quyền xóa Box");
        }

        box.setDeletedAt(LocalDateTime.now());
        boxRepository.save(box);
    }

    @Override
    @Transactional
    public void leaveBox(String boxId, String userId) {
        BoxMember myMembership = boxMemberRepository.findByBoxIdAndUserId(boxId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải là thành viên của Box này"));

        // 🔥 Thêm chốt chặn: Không cho phép Chủ phòng tự ý rời đi mà không chuyển nhượng
        if (myMembership.getBox().getOwner().getId().equals(userId)) {
            throw new BadRequestException("Chủ phòng không thể rời đi. Hãy chuyển nhượng quyền chủ phòng trước hoặc giải tán Box.");
        }

        boxMemberRepository.delete(myMembership);
    }

    // 🔥 TÍNH NĂNG MỚI: Chuyển nhượng Chủ Phòng
    @Override
    @Transactional
    public void transferOwnership(String boxId, String newOwnerId, String currentOwnerId) {
        Box box = boxRepository.findById(boxId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Box"));

        if (!box.getOwner().getId().equals(currentOwnerId)) {
            throw new BadRequestException("Chỉ chủ phòng hiện tại mới được quyền chuyển nhượng");
        }
        if (currentOwnerId.equals(newOwnerId)) {
            throw new BadRequestException("Không thể chuyển nhượng cho chính mình");
        }

        // Đổi Owner của Box
        User newOwner = userRepository.findById(newOwnerId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        box.setOwner(newOwner);
        boxRepository.save(box);

        // Hạ cấp chủ cũ xuống MEMBER
        BoxMember oldOwnerMem = boxMemberRepository.findByBoxIdAndUserId(boxId, currentOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên không tồn tại"));
        oldOwnerMem.setRole(BoxRole.MEMBER);
        boxMemberRepository.save(oldOwnerMem);

        // Thăng cấp chủ mới lên ADMIN
        BoxMember newOwnerMem = boxMemberRepository.findByBoxIdAndUserId(boxId, newOwnerId)
                .orElseThrow(() -> new BadRequestException("Người được chuyển nhượng chưa tham gia Box"));
        newOwnerMem.setRole(BoxRole.ADMIN);
        boxMemberRepository.save(newOwnerMem);
    }


    // =========================================================================
    // PHẦN 2: QUẢN LÝ THÀNH VIÊN VÀ LỜI MỜI (Giữ nguyên gốc)
    // =========================================================================

    @Override
    @Transactional
    public void inviteMember(String boxId, String inviteeId, String inviterId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Box"));

        boolean isInviterInBox = boxMemberRepository.findByBoxIdAndUserId(boxId, inviterId).isPresent();
        if (!isInviterInBox && !box.getOwner().getId().equals(inviterId)) {
            throw new BadRequestException("Bạn không phải thành viên của Box này nên không thể mời người khác");
        }

        User targetUser = userRepository.findById(inviteeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng cần mời"));
        User requesterUser = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi xác thực người mời"));

        boolean isInviteeAlreadyInBox = boxMemberRepository.findByBoxIdAndUserId(boxId, inviteeId).isPresent();
        if (isInviteeAlreadyInBox || box.getOwner().getId().equals(inviteeId)) {
            throw new BadRequestException("Người này đã là thành viên của Box");
        }

        boolean hasPendingInvite = boxInvitationRepository.existsByBoxIdAndRecipientIdAndStatus(boxId, inviteeId, "PENDING");
        if (hasPendingInvite) {
            throw new BadRequestException("Đã gửi lời mời đến người này rồi, đang chờ họ đồng ý");
        }

        BoxInvitation invitation = BoxInvitation.builder()
                .box(box)
                .sender(userRepository.getReferenceById(inviterId))
                .recipient(targetUser)
                .status("PENDING")
                .build();

        invitation = boxInvitationRepository.save(invitation);

        eventPublisher.publishEvent(new BoxMemberInvitedEvent(box, requesterUser, targetUser));
    }

    @Override
    @Transactional
    public void handleInvitation(String invitationId, boolean isAccepted, String userId) {
        BoxInvitation invitation = boxInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời này"));

        if (!invitation.getRecipient().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xử lý lời mời của người khác");
        }

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new BadRequestException("Lời mời này đã được xử lý hoặc hết hạn");
        }

        if (isAccepted) {
            BoxMember newMember = BoxMember.builder()
                    .box(invitation.getBox())
                    .user(invitation.getRecipient())
                    .role(BoxRole.MEMBER)
                    .build();
            boxMemberRepository.save(newMember);

            invitation.setStatus("ACCEPTED");

            eventPublisher.publishEvent(new BoxMemberAddedEvent(
                    invitation.getBox(),
                    invitation.getSender(),
                    invitation.getRecipient()
            ));
        } else {
            invitation.setStatus("REJECTED");
        }

        boxInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void kickMember(String boxId, String memberId, String adminId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Box"));

        boolean isOwner = box.getOwner().getId().equals(adminId);
        boolean isAdmin = boxMemberRepository.findByBoxIdAndUserId(boxId, adminId)
                .map(m -> BoxRole.ADMIN.equals(m.getRole()))
                .orElse(false);

        if (!isOwner && !isAdmin) {
            throw new BadRequestException("Chỉ Owner hoặc Admin mới có quyền đuổi thành viên");
        }

        if (memberId.equals(adminId) || memberId.equals(box.getOwner().getId())) {
            throw new BadRequestException("Không thể đuổi chính bạn hoặc Owner của Box");
        }

        BoxMember memberToKick = boxMemberRepository.findByBoxIdAndUserId(boxId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên này không có trong Box"));

        boxMemberRepository.delete(memberToKick);
    }

    @Override
    @Transactional
    public void updateMemberRole(String boxId, String memberId, BoxRole newRole, String adminId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Box"));

        if (!box.getOwner().getId().equals(adminId)) {
            throw new BadRequestException("Chỉ Owner mới có quyền thay đổi vai trò của thành viên");
        }

        BoxMember member = boxMemberRepository.findByBoxIdAndUserId(boxId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Thành viên này không có trong Box"));

        member.setRole(newRole);
        boxMemberRepository.save(member);
    }
}