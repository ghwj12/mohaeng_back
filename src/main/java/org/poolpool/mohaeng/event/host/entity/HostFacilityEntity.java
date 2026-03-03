package org.poolpool.mohaeng.event.host.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "host_booth_facility")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HostFacilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HOST_BOOTHFACI_ID")
    private Long hostBoothfaciId;

    @Column(name = "EVENT_ID", nullable = false)
    private Long eventId;

    @Column(name = "FACI_NAME", length = 100, nullable = false)
    private String faciName;

    @Column(name = "FACI_PRICE", nullable = false)
    @Builder.Default
    private Integer faciPrice = 0;

    @Column(name = "FACI_UNIT", length = 50)
    private String faciUnit;

    @Column(name = "HAS_COUNT", nullable = false)
    @Builder.Default
    private Boolean hasCount = false;

    @Column(name = "TOTAL_COUNT", nullable = true)
    @Builder.Default
    private Integer totalCount = 0;

    @Column(name = "REMAIN_COUNT", nullable = true)
    private Integer remainCount;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}