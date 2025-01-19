package com.example.us1demo.entity;

public class TransformationRequest {
    private String dbUniqueId;
    private String sourceSchema;
    private String sourceTable;
    private String targetTable;

    // Getters and setters
    public String getDbUniqueId() {
        return dbUniqueId;
    }

    public void setDbUniqueId(String dbUniqueId) {
        this.dbUniqueId = dbUniqueId;
    }

    public String getSourceSchema() {
        return sourceSchema;
    }

    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }
}
