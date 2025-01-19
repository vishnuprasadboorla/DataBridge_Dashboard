package com.example.us1demo.repository;

import com.example.us1demo.entity.ConnectionInformation; // Importing the ConnectionInformation entity
import org.springframework.data.repository.CrudRepository; // Importing the CrudRepository interface for CRUD operations

import java.util.List; // Importing List interface to return lists of ConnectionInformation

// This interface extends CrudRepository to provide basic CRUD operations for ConnectionInformation entities
public interface ConnectionRepo extends CrudRepository<ConnectionInformation, Integer> {

    // Find connection information by dbUniqueId
    // This method will allow querying the ConnectionInformation entities by their dbUniqueId field
    List<ConnectionInformation> findByDbUniqueId(String dbUniqueId);

    // This method is already provided by CrudRepository
    // It retrieves all records from the ConnectionInformation table
    // You don't need to explicitly define it, but it's added here for clarity
    List<ConnectionInformation> findAll();
}