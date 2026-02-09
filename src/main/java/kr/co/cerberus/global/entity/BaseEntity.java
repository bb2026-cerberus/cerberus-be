package kr.co.cerberus.global.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Builder; // Add Builder import for @Builder.Default

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity {
    @CreationTimestamp
    @Column(name = "create_dt", updatable = false, nullable = false)
    private LocalDateTime createDatetime;       // 생성 시간

    @UpdateTimestamp
    @Column(name = "update_dt", nullable = false)
    private LocalDateTime updateDatetime;       // 마지막 수정 시간

    @Builder.Default
    @Column(name = "delete_yn", nullable = false, length = 1)
    private String deleteYn = "N";

    public void delete() {
        this.deleteYn = "Y";
    }

    public void undelete() {
        this.deleteYn = "N";
    }

    public boolean isDeleted() {
        return "Y".equals(this.deleteYn);
    }
}