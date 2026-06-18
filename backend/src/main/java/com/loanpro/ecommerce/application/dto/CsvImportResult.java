package com.loanpro.ecommerce.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvImportResult {
    private int totalRows;
    private int successCount;
    private int errorCount;
    private String errorFileId;

    @JsonIgnore
    private byte[] errorCsvBytes;
}
