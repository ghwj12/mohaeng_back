package org.poolpool.mohaeng.event.participation.dto;

import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;

public class ParticipationBoothFacilityDto {

    // participation_booth_facility
    private Long    pctBoothFaciId;
    private Long    hostBoothFaciId;
    private Long    pctBoothId;
    private Integer faciCount;   // 신청한 수량
    private Integer faciPrice;   // 신청 당시 결제 금액

    // ✅ host_booth_facility (시설 원본 정보)
    private String  faciName;       // 시설명
    private String  faciUnit;       // 단위
    private Integer unitPrice;      // 단가 (host 기준 원가)
    private Boolean hasCount;       // 수량 제한 여부
    private Integer totalCount;     // 총 수량
    private Integer remainCount;    // 잔여 수량

    public static ParticipationBoothFacilityDto fromEntity(ParticipationBoothFacilityEntity e) {
        ParticipationBoothFacilityDto d = new ParticipationBoothFacilityDto();
        d.pctBoothFaciId  = e.getPctBoothFaciId();
        d.hostBoothFaciId = e.getHostBoothFaciId();
        d.pctBoothId      = e.getPctBoothId();
        d.faciCount       = e.getFaciCount();
        d.faciPrice       = e.getFaciPrice();
        return d;
    }

    public ParticipationBoothFacilityEntity toEntity(Long pctBoothId) {
        ParticipationBoothFacilityEntity e = new ParticipationBoothFacilityEntity();
        e.setPctBoothFaciId(this.pctBoothFaciId);
        e.setHostBoothFaciId(this.hostBoothFaciId);
        e.setPctBoothId(pctBoothId);
        e.setFaciCount(this.faciCount);
        e.setFaciPrice(this.faciPrice);
        return e;
    }

    // getter / setter
    public Long    getPctBoothFaciId()           { return pctBoothFaciId; }
    public void    setPctBoothFaciId(Long v)     { this.pctBoothFaciId = v; }
    public Long    getHostBoothFaciId()          { return hostBoothFaciId; }
    public void    setHostBoothFaciId(Long v)    { this.hostBoothFaciId = v; }
    public Long    getPctBoothId()               { return pctBoothId; }
    public void    setPctBoothId(Long v)         { this.pctBoothId = v; }
    public Integer getFaciCount()                { return faciCount; }
    public void    setFaciCount(Integer v)       { this.faciCount = v; }
    public Integer getFaciPrice()                { return faciPrice; }
    public void    setFaciPrice(Integer v)       { this.faciPrice = v; }
    public String  getFaciName()                 { return faciName; }
    public void    setFaciName(String v)         { this.faciName = v; }
    public String  getFaciUnit()                 { return faciUnit; }
    public void    setFaciUnit(String v)         { this.faciUnit = v; }
    public Integer getUnitPrice()                { return unitPrice; }
    public void    setUnitPrice(Integer v)       { this.unitPrice = v; }
    public Boolean getHasCount()                 { return hasCount; }
    public void    setHasCount(Boolean v)        { this.hasCount = v; }
    public Integer getTotalCount()               { return totalCount; }
    public void    setTotalCount(Integer v)      { this.totalCount = v; }
    public Integer getRemainCount()              { return remainCount; }
    public void    setRemainCount(Integer v)     { this.remainCount = v; }
}
