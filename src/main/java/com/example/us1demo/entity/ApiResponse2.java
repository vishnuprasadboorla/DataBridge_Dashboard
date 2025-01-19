package com.example.us1demo.entity;

public class ApiResponse2 {
    private String status;
    private String message;
    private String filePath;

    // Constructor
    public ApiResponse2(String status, String message) {
        this.status = status;
        this.message = message;
    }

    // Constructor for response with file path
    public ApiResponse2(String status, String message, String filePath) {
        this.status = status;
        this.message = message;
        this.filePath = filePath;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
