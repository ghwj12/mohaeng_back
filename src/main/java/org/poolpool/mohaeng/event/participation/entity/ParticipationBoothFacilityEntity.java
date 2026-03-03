package org.poolpool.mohaeng.event.participation.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "participation_booth_facility")
public class ParticipationBoothFacilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PCT_BOOTHFACI_ID")
    private Long pctBoothFaciId;

    @Column(name = "HOST_BOOTHFACI_ID", nullable = false)
    private Long hostBoothFaciId;

    @Column(name = "PCT_BOOTH_ID", nullable = false)
    private Long pctBoothId;

    @Column(name = "FACI_COUNT", nullable = false)
    private Integer faciCount;

    @Column(name = "FACI_PRICE", nullable = false)
    private Integer faciPrice;

    @PrePersist
    void prePersist() {
        if (this.faciCount == null) this.faciCount = 1;
        if (this.faciPrice == null) this.faciPrice = 0;
    }

    // getter/setter
    public Long getPctBoothFaciId() { return pctBoothFaciId; }
    public void setPctBoothFaciId(Long pctBoothFaciId) { this.pctBoothFaciId = pctBoothFaciId; }

    public Long getHostBoothFaciId() { return hostBoothFaciId; }
    public void setHostBoothFaciId(Long hostBoothFaciId) { this.hostBoothFaciId = hostBoothFaciId; }

    public Long getPctBoothId() { return pctBoothId; }
    public void setPctBoothId(Long pctBoothId) { this.pctBoothId = pctBoothId; }

    public Integer getFaciCount() { return faciCount; }
    public void setFaciCount(Integer faciCount) { this.faciCount = faciCount; }

    public Integer getFaciPrice() { return faciPrice; }
    public void setFaciPrice(Integer faciPrice) { this.faciPrice = faciPrice; }
}
