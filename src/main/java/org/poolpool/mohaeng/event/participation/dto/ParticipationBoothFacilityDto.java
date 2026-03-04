package org.poolpool.mohaeng.event.participation.dto;

import org.poolpool.mohaeng.event.participation.entity.ParticipationBoothFacilityEntity;

public class ParticipationBoothFacilityDto {
	private Long pctBoothFaciId;
	private Long hostBoothFaciId;
	private Long pctBoothId;
	private Integer faciCount;
	private Integer faciPrice;

	public static ParticipationBoothFacilityDto fromEntity(ParticipationBoothFacilityEntity e) {
		ParticipationBoothFacilityDto d = new ParticipationBoothFacilityDto();
		d.pctBoothFaciId = e.getPctBoothFaciId();
		d.hostBoothFaciId = e.getHostBoothFaciId();
		d.pctBoothId = e.getPctBoothId();
		d.faciCount = e.getFaciCount();
		d.faciPrice = e.getFaciPrice();
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

	// getter/setter
	public Long getPctBoothFaciId() {
		return pctBoothFaciId;
	}

	public void setPctBoothFaciId(Long pctBoothFaciId) {
		this.pctBoothFaciId = pctBoothFaciId;
	}

	public Long getHostBoothFaciId() {
		return hostBoothFaciId;
	}

	public void setHostBoothFaciId(Long hostBoothFaciId) {
		this.hostBoothFaciId = hostBoothFaciId;
	}

	public Long getPctBoothId() {
		return pctBoothId;
	}

	public void setPctBoothId(Long pctBoothId) {
		this.pctBoothId = pctBoothId;
	}

	public Integer getFaciCount() {
		return faciCount;
	}

	public void setFaciCount(Integer faciCount) {
		this.faciCount = faciCount;
	}

	public Integer getFaciPrice() {
		return faciPrice;
	}

	public void setFaciPrice(Integer faciPrice) {
		this.faciPrice = faciPrice;
	}
}
