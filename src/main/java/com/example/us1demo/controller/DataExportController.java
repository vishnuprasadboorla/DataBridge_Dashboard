package com.example.us1demo.controller;

import com.example.us1demo.entity.ApiResponse2;
import com.example.us1demo.entity.TransformationRequest;
import com.example.us1demo.entity.ConnectionInformation;
import com.example.us1demo.repository.ConnectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.*;
import java.util.List;

@RestController
@RequestMapping("/api/data")
public class DataExportController {

    @Autowired
    private ConnectionRepo connectionRepo;

    // Export data from a database table to a .txt file
    @GetMapping("/export-txt")
    public ApiResponse2 exportDataToTxt(@RequestParam String dbUniqueId,  // Database unique identifier passed as a request parameter
                                        @RequestParam String schemaName,  // Schema name passed as a request parameter
                                        @RequestParam String tableName) { // Table name passed as a request parameter
        try {
            // Fetch connection details using the dbUniqueId
            List<ConnectionInformation> connections = connectionRepo.findByDbUniqueId(dbUniqueId);
            if (connections.isEmpty()) {
                // Return an error response if no connection details are found
                return new ApiResponse2("Error", "Connection details not found for dbUniqueId: " + dbUniqueId);
            }

            // Get the first connection information from the list
            ConnectionInformation connectionInformation = connections.get(0);

            // Establish a connection to the database using the fetched details
            Connection conn = DriverManager.getConnection(
                    connectionInformation.getUrl(),
                    connectionInformation.getUsername(),
                    connectionInformation.getPassword()
            );

            // Build a SQL query to fetch all data from the specified table
            String query = String.format("SELECT * FROM %s.%s", schemaName, tableName);

            // Execute the query and retrieve the result set
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(query);

            // Define the file path to save the .txt file
            String filePath = "/home/mt24066/Downloads/" + tableName + ".txt";

            // Create a BufferedWriter to write data to the .txt file
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

            // Get the number of columns in the result set
            int columnCount = resultSet.getMetaData().getColumnCount();

            // Write the column names as headers in the .txt file, separated by tabs
            for (int i = 1; i <= columnCount; i++) {
                writer.write(resultSet.getMetaData().getColumnName(i)); // Write column name
                if (i < columnCount) writer.write("\t"); // Add a tab separator if not the last column
            }
            writer.newLine(); // Add a new line after the headers

            // Write the data rows to the file
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    writer.write(resultSet.getString(i)); // Write the value of each column
                    if (i < columnCount) writer.write("\t"); // Add a tab separator if not the last column
                }
                writer.newLine(); // Add a new line after each row
            }

            // Close the writer, result set, statement, and connection to release resources
            writer.close();
            resultSet.close();
            stmt.close();
            conn.close();

            // Return a success response with the file path
            return new ApiResponse2("Success", "Data exported successfully", filePath);
        } catch (Exception e) {
            // Catch any exceptions and return an error response with the exception message
            return new ApiResponse2("Error", "An error occurred while exporting data: " + e.getMessage());
        }
    }


    // Export data to a .csv file
    @GetMapping("/export-csv")
    public ApiResponse2 exportDataToCsv(@RequestParam String dbUniqueId,
                                        @RequestParam String schemaName,
                                        @RequestParam String tableName) {
        try {
            List<ConnectionInformation> connections = connectionRepo.findByDbUniqueId(dbUniqueId);
            if (connections.isEmpty()) {
                return new ApiResponse2("Error", "Connection details not found for dbUniqueId: " + dbUniqueId);
            }
            ConnectionInformation connectionInformation = connections.get(0);

            Connection conn = DriverManager.getConnection(connectionInformation.getUrl(),
                    connectionInformation.getUsername(),
                    connectionInformation.getPassword());
            String query = String.format("SELECT * FROM %s.%s", schemaName, tableName);

            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(query);

            String filePath = "/home/mt24066/Downloads/" + tableName + ".csv";
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));

            // Write column names as the header of the CSV
            int columnCount = resultSet.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                writer.write(resultSet.getMetaData().getColumnName(i));
                if (i < columnCount) writer.write(","); // Separate by commas
            }
            writer.newLine();

            // Write data rows
            while (resultSet.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    writer.write(resultSet.getString(i));
                    if (i < columnCount) writer.write(","); // Separate by commas
                }
                writer.newLine();
            }

            writer.close();
            resultSet.close();
            stmt.close();
            conn.close();

            return new ApiResponse2("Success", "Data exported successfully", filePath);
        } catch (Exception e) {
            return new ApiResponse2("Error", "An error occurred while exporting data: " + e.getMessage());
        }
    }

    // Export data to a .json file
    @GetMapping("/export-json")
    public ApiResponse2 exportDataToJson(@RequestParam String dbUniqueId,
                                         @RequestParam String schemaName,
                                         @RequestParam String tableName) {
        try {
            List<ConnectionInformation> connections = connectionRepo.findByDbUniqueId(dbUniqueId);
            if (connections.isEmpty()) {
                return new ApiResponse2("Error", "Connection details not found for dbUniqueId: " + dbUniqueId);
            }
            ConnectionInformation connectionInformation = connections.get(0);

            Connection conn = DriverManager.getConnection(connectionInformation.getUrl(),
                    connectionInformation.getUsername(),
                    connectionInformation.getPassword());
            String query = String.format("SELECT * FROM %s.%s", schemaName, tableName);

            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(query);

            String filePath = "/home/mt24066/Downloads/" + tableName + ".json";

            // Write JSON data
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write("["); // Start of JSON array

            int columnCount = resultSet.getMetaData().getColumnCount();
            boolean isFirstRow = true;

            while (resultSet.next()) {
                if (!isFirstRow) {
                    writer.write(","); // Separate JSON objects with a comma
                }
                writer.newLine();
                writer.write("  {");

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = resultSet.getMetaData().getColumnName(i);
                    String columnValue = resultSet.getString(i);

                    writer.write(String.format("\"%s\": \"%s\"", columnName, columnValue));
                    if (i < columnCount) writer.write(", "); // Separate fields with a comma
                }

                writer.write("}");
                isFirstRow = false;
            }

            writer.newLine();
            writer.write("]"); // End of JSON array
            writer.close();

            resultSet.close();
            stmt.close();
            conn.close();

            return new ApiResponse2("Success", "Data exported successfully", filePath);
        } catch (Exception e) {
            return new ApiResponse2("Error", "An error occurred while exporting data: " + e.getMessage());
        }
    }


    // API endpoint to transform data from a source table and insert it into a target table
    @PostMapping("/transform")
    public ApiResponse2 transformAndInsertData(@RequestBody TransformationRequest transformationRequest) {
        try {
            // Fetch connection details using the unique database ID from the transformation request
            ConnectionInformation connectionInfo = connectionRepo.findByDbUniqueId(transformationRequest.getDbUniqueId())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Connection not found"));

            // Establish a connection to the database
            Connection conn = DriverManager.getConnection(
                    connectionInfo.getUrl(),
                    connectionInfo.getUsername(),
                    connectionInfo.getPassword()
            );

            // Extract source schema and table details from the request
            String sourceSchema = transformationRequest.getSourceSchema();
            String sourceTable = transformationRequest.getSourceTable();

            // Query to fetch metadata from the source table (limited to 1 row for metadata inspection)
            String query = String.format("SELECT * FROM %s.%s LIMIT 1", sourceSchema, sourceTable);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsMeta = rs.getMetaData(); // Retrieve metadata for column details

            // Extract target table name from the request
            String targetTable = transformationRequest.getTargetTable();

            // Dynamically build the CREATE TABLE query for the target table based on the source table metadata
            StringBuilder createTableQuery = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %s (", targetTable));
            for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
                createTableQuery.append(rsMeta.getColumnName(i)) // Add column name
                        .append(" ")
                        .append(rsMeta.getColumnTypeName(i)); // Add column data type
                if (i < rsMeta.getColumnCount()) {
                    createTableQuery.append(", "); // Add a comma if not the last column
                }
            }
            createTableQuery.append(")"); // Close the CREATE TABLE statement
            stmt.execute(createTableQuery.toString()); // Execute the CREATE TABLE query

            // Query to fetch all data from the source table
            query = String.format("SELECT * FROM %s.%s", sourceSchema, sourceTable);
            rs = stmt.executeQuery(query);

            // Dynamically build the INSERT query for the target table with placeholders for values
            StringBuilder insertQuery = new StringBuilder(String.format("INSERT INTO %s VALUES (", targetTable));
            for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
                insertQuery.append("?"); // Placeholder for each column value
                if (i < rsMeta.getColumnCount())
                    insertQuery.append(", ");
            }
            insertQuery.append(")");
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery.toString()); // Prepare the INSERT statement

            // Process each row from the source table
            while (rs.next()) {
                for (int i = 1; i <= rsMeta.getColumnCount(); i++) {
                    Object value = rs.getObject(i); // Get the value of the current column

                    // Apply transformation: Capitalize the value if the column name is 'name'
                    if ("name".equalsIgnoreCase(rsMeta.getColumnName(i)) && value instanceof String) {
                        value = ((String) value).toUpperCase(); // Transform the 'name' column to uppercase
                    }
                    insertStmt.setObject(i, value); // Set the transformed value in the prepared statement
                }
                insertStmt.executeUpdate(); // Execute the INSERT statement for the current row
            }

            // Log transformation details in a file
            String logFileName = "transformation_log.txt";
            BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFileName));
            logWriter.write("Data transformation and insertion completed successfully.\n");
            logWriter.write("Source Table: " + sourceSchema + "." + sourceTable + "\n");
            logWriter.write("Target Table: " + targetTable + "\n");
            logWriter.close(); // Close the log writer

            // Return a success response with the log file path
            return new ApiResponse2("Success", "Data transformed and inserted successfully", logFileName);

        } catch (Exception e) {
            // Handle any exceptions that occur during the process and return an error response
            return new ApiResponse2("Error", "An error occurred while transforming data: " + e.getMessage());
        }
    }
}
