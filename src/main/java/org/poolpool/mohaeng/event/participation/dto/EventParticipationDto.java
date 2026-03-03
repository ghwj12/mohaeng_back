package org.poolpool.mohaeng.event.participation.dto;

import java.time.LocalDate;
import org.poolpool.mohaeng.event.list.entity.EventEntity;
import org.poolpool.mohaeng.event.participation.entity.EventParticipationEntity;

public class EventParticipationDto {

    private Long pctId;
    private Long eventId;
    private LocalDate pctDate;
    private String pctStatus;
    private Long userId;
    private String pctGender;
    private String pctJob;
    private String pctRoot;
    private String pctGroup;
    private String pctRank;
    private String pctIntroduce;
    private String pctAgeGroup;

    // ✅ 행사 정보 (마이페이지 표시용)
    private String eventTitle;
    private String simpleExplain;
    private String thumbnail;
    private String eventStartDate;
    private String eventEndDate;
    private Integer payAmount;

    // ─── 행사 정보 없이 엔티티만으로 변환 ───
    public static EventParticipationDto fromEntity(EventParticipationEntity e) {
        EventParticipationDto d = new EventParticipationDto();
        d.pctId     = e.getPctId();
        d.eventId   = e.getEventId();
        d.pctStatus = e.getPctStatus();
        d.userId    = e.getUserId();
        d.pctGender = e.getPctGender();
        d.pctJob    = e.getPctJob();
        d.pctRoot   = e.getPctRoot();
        d.pctGroup  = e.getPctGroup();
        d.pctRank   = e.getPctRank();
        d.pctIntroduce = e.getPctIntroduce();
        d.pctAgeGroup  = e.getPctAgeGroup();
        if (e.getPctDate() != null) d.pctDate = e.getPctDate().toLocalDate();
        return d;
    }

    // ✅ 행사 정보 포함하여 변환
    public static EventParticipationDto fromEntityWithEvent(
            EventParticipationEntity e, EventEntity event, Integer payAmount) {

        EventParticipationDto d = fromEntity(e);

        if (event != null) {
            d.eventTitle     = event.getTitle();
            d.simpleExplain  = event.getSimpleExplain();
            d.thumbnail     = event.getThumbnail();
            d.eventStartDate = event.getStartDate() != null
                    ? event.getStartDate().toString() : null;
            d.eventEndDate   = event.getEndDate() != null
                    ? event.getEndDate().toString() : null;
        }
        d.payAmount = payAmount;
        return d;
    }

    public EventParticipationEntity toEntity() {
        EventParticipationEntity e = new EventParticipationEntity();
        e.setPctId(pctId);
        e.setEventId(eventId);
        e.setUserId(userId);
        e.setPctGender(pctGender);
        e.setPctJob(pctJob);
        e.setPctRoot(pctRoot);
        e.setPctGroup(pctGroup);
        e.setPctRank(pctRank);
        e.setPctIntroduce(pctIntroduce);
        e.setPctStatus(pctStatus);
        e.setPctAgeGroup(pctAgeGroup);
        if (pctDate != null) e.setPctDate(pctDate.atStartOfDay());
        return e;
    }

    // ─── Getters / Setters ───
    public Long getPctId()                    { return pctId; }
    public void setPctId(Long v)              { pctId = v; }
    public Long getEventId()                  { return eventId; }
    public void setEventId(Long v)            { eventId = v; }
    public LocalDate getPctDate()             { return pctDate; }
    public void setPctDate(LocalDate v)       { pctDate = v; }
    public String getPctStatus()              { return pctStatus; }
    public void setPctStatus(String v)        { pctStatus = v; }
    public Long getUserId()                   { return userId; }
    public void setUserId(Long v)             { userId = v; }
    public String getPctGender()              { return pctGender; }
    public void setPctGender(String v)        { pctGender = v; }
    public String getPctJob()                 { return pctJob; }
    public void setPctJob(String v)           { pctJob = v; }
    public String getPctRoot()                { return pctRoot; }
    public void setPctRoot(String v)          { pctRoot = v; }
    public String getPctGroup()               { return pctGroup; }
    public void setPctGroup(String v)         { pctGroup = v; }
    public String getPctRank()                { return pctRank; }
    public void setPctRank(String v)          { pctRank = v; }
    public String getPctIntroduce()           { return pctIntroduce; }
    public void setPctIntroduce(String v)     { pctIntroduce = v; }
    public String getPctAgeGroup()            { return pctAgeGroup; }
    public void setPctAgeGroup(String v)      { pctAgeGroup = v; }
    public String getEventTitle()             { return eventTitle; }
    public void setEventTitle(String v)       { eventTitle = v; }
    public String getSimpleExplain()          { return simpleExplain; }
    public void setSimpleExplain(String v)    { simpleExplain = v; }
    public String getThumbnail()              { return thumbnail; }
    public void setThumbnail(String v)        { thumbnail = v; }
    public String getEventStartDate()         { return eventStartDate; }
    public void setEventStartDate(String v)   { eventStartDate = v; }
    public String getEventEndDate()           { return eventEndDate; }
    public void setEventEndDate(String v)     { eventEndDate = v; }
    public Integer getPayAmount()             { return payAmount; }
    public void setPayAmount(Integer v)       { payAmount = v; }
}
