package org.poolpool.mohaeng.ai.dto;

import lombok.Data;

@Data
public class BizOcrResponse {
    private String businessNumber;
    private String companyName;
    private String ownerName;
    private String birthDate;       
    private String openDate;
    private Boolean isValid;
    private String validationStatus;
    private String validationMessage;
}