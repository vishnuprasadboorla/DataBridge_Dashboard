package com.example.us1demo.controller;

import com.example.us1demo.entity.ApiResponse2;
import com.example.us1demo.entity.ConnectionInformation;
import com.example.us1demo.repository.ConnectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.*;
import java.util.List;

// Annotates this class as a REST controller to handle HTTP requests for the /api/report endpoint
@RestController
@RequestMapping("/api/report")
public class ReportController {

    // Autowires the ConnectionRepo to fetch database connection information
    @Autowired
    private ConnectionRepo connectionRepo;

    // Defines a GET endpoint to generate a report based on a database table
    @GetMapping
    public ApiResponse2 generateReport(@RequestParam String dbUniqueId,
                                       @RequestParam String schemaName,
                                       @RequestParam String tableName) {
        // File name where the report will be saved
        String logFileName = "aggregated_report.txt";

        // Using try-with-resources to ensure the BufferedWriter is closed after use
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName))) {

            // Fetch the connection information using the provided dbUniqueId
            List<ConnectionInformation> connections = connectionRepo.findByDbUniqueId(dbUniqueId);
            if (connections.isEmpty()) {
                // Return an error response if no connection is found
                return new ApiResponse2("Error", "Connection details not found for dbUniqueId: " + dbUniqueId, null);
            }
            // Retrieve the first connection from the list
            ConnectionInformation connectionInformation = connections.get(0);

            // Establish a connection to the database using the connection details
            Connection conn = DriverManager.getConnection(
                    connectionInformation.getUrl(),
                    connectionInformation.getUsername(),
                    connectionInformation.getPassword()
            );

            // SQL query to group data by department, count employees, and calculate the average salary
            String query = String.format(
                    "SELECT department, COUNT(*) AS employee_count, AVG(salary) AS avg_salary " +
                            "FROM %s.%s GROUP BY department", schemaName, tableName
            );

            // Execute the query using a Statement object
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(query);

            // Write the header row to the report file
            writer.write("Department\tEmployee Count\tAverage Salary");
            writer.newLine();

            // Process each row in the result set
            while (resultSet.next()) {
                // Retrieve the values of the current row
                String department = resultSet.getString("department");
                int employeeCount = resultSet.getInt("employee_count");
                double avgSalary = resultSet.getDouble("avg_salary");

                // Write the row data to the report file
                writer.write(department + "\t" + employeeCount + "\t" + avgSalary);
                writer.newLine(); // Move to the next line in the file
            }

            // Close resources after processing the result set
            resultSet.close();
            stmt.close();
            conn.close();

            // Return a success response with the log file name
            return new ApiResponse2("Success", "Report generated successfully", logFileName);

        } catch (Exception e) {
            // Handle any exceptions and return an error response with the error message
            return new ApiResponse2("Error", "An error occurred while generating the report: " + e.getMessage(), null);
        }
    }
}