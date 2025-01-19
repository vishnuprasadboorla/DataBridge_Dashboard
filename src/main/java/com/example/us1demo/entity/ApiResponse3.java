package com.example.us1demo.entity;

public class ApiResponse3 {
    private String message;
    private int recordsInserted;


    public ApiResponse3(String message, int recordsInserted) {
        this.message = message;
        this.recordsInserted = recordsInserted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRecordsInserted() {
        return recordsInserted;
    }

    public void setRecordsInserted(int recordsInserted) {
        this.recordsInserted = recordsInserted;
    }
}
