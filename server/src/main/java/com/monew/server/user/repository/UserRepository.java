package com.monew.server.user.repository;

import com.monew.server.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    // 회원가입 이메일 중복 검사 (논리삭제 포함 전체 — 복구 보장 위해 이메일은 계속 점유)
    boolean existsByEmail(String email);

    // 로그인용 — 삭제 여부는 서비스에서 isDeleted()로 판단
    Optional<User> findByEmail(String email);

    // 활성 사용자 조회 (논리삭제 제외)
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
