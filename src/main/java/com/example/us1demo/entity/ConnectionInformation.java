package com.example.us1demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ConnectionInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer dbId;
    private String dbUniqueId;
    private String url;
    private String username;
    private String password;
    private Integer port;

    public ConnectionInformation() {
    }

    public ConnectionInformation(String dbUniqueId, String url, String username, String password, Integer port) {
        this.dbUniqueId = dbUniqueId;
        this.url = url;
        this.username = username;
        this.password = password;
        this.port = port;
    }


    @Override
    public String toString() {
        return "ConnectionInformation{" + "dbId=" + dbId + ", dbUniqueId='" + dbUniqueId + '\'' + ", url='" + url + '\'' + ", username='" + username + '\'' + ", port=" + port + '}';
    }

    public Integer getDbId() {
        return dbId;
    }

    public void setDbId(Integer dbId) {
        this.dbId = dbId;
    }

    public String getDbUniqueId() {
        return dbUniqueId;
    }

    public void setDbUniqueId(String dbUniqueId) {
        this.dbUniqueId = dbUniqueId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
