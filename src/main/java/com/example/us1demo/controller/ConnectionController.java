package com.example.us1demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.us1demo.entity.ApiResponse;
import com.example.us1demo.entity.ApiResponse2;
import com.example.us1demo.entity.ApiResponse3;
import com.example.us1demo.entity.ConnectionInformation;
import com.example.us1demo.repository.ConnectionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import java.io.*;
import java.sql.*;

import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ConnectionController {

    @Autowired
    private ConnectionRepo connectionRepo;


    // Existing POST method to save connection details
    @PostMapping("/connection")
    public ResponseEntity<?> saveConnectionDetails(@RequestBody ConnectionInformation connectionDetails) {
        try {
            // Save connection details to the database
            ConnectionInformation savedConnection = connectionRepo.save(connectionDetails);

            // Generate file name and path for saving the connection details
            String fileName = savedConnection.getDbUniqueId() + ".txt";
            File file = new File(fileName);

            // Writing connection details to the file
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(savedConnection.toString());
            } catch (IOException e) {
                // Return an error response if file writing fails
                return ResponseEntity.internalServerError().body(
                        new ApiResponse("Failed to write connection details to the file", null)
                );
            }

            // Return a success response with the file name and a success message
            return ResponseEntity.ok().body(
                    new ApiResponse("Connection details have been successfully saved to the database and file", fileName)
            );

        } catch (Exception e) {
            // Catch all other exceptions and return an internal server error
            return ResponseEntity.internalServerError().body(
                    new ApiResponse("Failed to save connection details to the database", null)
            );
        }
    }


    @GetMapping("/connection")
    public ResponseEntity<?> getAllConnectionDetails() {
        try {
            // Fetch all connection details
            List<ConnectionInformation> connectionDetails = connectionRepo.findAll();

            if (connectionDetails.isEmpty()) {
                return ResponseEntity.status(404).body("No connection details found.");
            }

            return ResponseEntity.ok(connectionDetails);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to retrieve connection details");
        }
    }


    // Handle the POST request for CSV file import
    @PostMapping("/data/import/csv")
    public ApiResponse2 importCsvToDatabase(
            @RequestParam("file") MultipartFile file,  // Accepts the CSV file as input
            @RequestParam("databaseUniqueId") String databaseUniqueId) {  // Accepts the unique database ID

        // Define a log file to store the import status
        String logFileName = "import_log.txt";

        try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFileName))) {  // Create a BufferedWriter to write logs

            // Retrieve the connection information from the database using the provided unique ID
            ConnectionInformation connectionInfo = connectionRepo.findByDbUniqueId(databaseUniqueId)
                    .stream().findFirst().orElseThrow(() -> new RuntimeException("Connection not found"));

            // Establish a database connection using the retrieved connection info
            Connection conn = DriverManager.getConnection(
                    connectionInfo.getUrl(), connectionInfo.getUsername(), connectionInfo.getPassword()
            );

            // Read the CSV file content
            InputStream inputStream = file.getInputStream();  // Get input stream from the file
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));  // Wrap input stream in BufferedReader
            String headerLine = reader.readLine();  // Read the header row from the CSV

            // Check if the header is empty, if so, throw an exception
            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty or has no data.");
            }

            // Extract column names from the header row
            String[] csvColumns = headerLine.split(",");
            List<String[]> dataRows = new ArrayList<>();

            // Read and store each data row from the CSV
            String line;
            while ((line = reader.readLine()) != null) {
                dataRows.add(line.split(","));  // Split each line by comma and store as a data row
            }

            // Dynamically infer column data types based on the content of the CSV
            Map<String, String> columnTypes = inferColumnTypes(csvColumns, dataRows);

            // Dynamically create the target table in the database
            String targetTable = "imported_data";  // Name of the target table
            StringBuilder createTableQuery = new StringBuilder(String.format("CREATE TABLE IF NOT EXISTS %s (", targetTable));
            for (Map.Entry<String, String> column : columnTypes.entrySet()) {
                createTableQuery.append("\"").append(column.getKey()).append("\" ").append(column.getValue()).append(", ");
            }
            createTableQuery.delete(createTableQuery.length() - 2, createTableQuery.length()); // Remove the last comma
            createTableQuery.append(")");

            // Execute the query to create the table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableQuery.toString());
            }

            // Prepare the insert statement for inserting data into the table
            StringBuilder insertQuery = new StringBuilder(String.format("INSERT INTO %s VALUES (", targetTable));
            for (int i = 0; i < csvColumns.length; i++) {
                insertQuery.append("?");
                if (i < csvColumns.length - 1) insertQuery.append(", ");
            }
            insertQuery.append(")");

            // Create a PreparedStatement for the insert query
            PreparedStatement pstmt = conn.prepareStatement(insertQuery.toString());

            // Insert the CSV data into the database
            for (String[] row : dataRows) {
                for (int i = 0; i < csvColumns.length; i++) {
                    String columnName = csvColumns[i].trim();
                    String value = row[i].trim();

                    // Perform data transformation based on column names
                    if ("name".equalsIgnoreCase(columnName)) {
                        value = value.toUpperCase().replaceAll("[^a-zA-Z0-9 ]", "");  // Clean and uppercase names
                    } else if ("phone_number".equalsIgnoreCase(columnName) || "pincode".equalsIgnoreCase(columnName)) {
                        value = value.replaceAll("[^0-9]", "");  // Remove non-numeric characters
                    }

                    String columnType = columnTypes.get(columnName);

                    // Set the appropriate value for each column based on its data type
                    if ("INTEGER".equals(columnType)) {
                        pstmt.setInt(i + 1, Integer.parseInt(value));
                    } else if ("DOUBLE PRECISION".equals(columnType)) {
                        pstmt.setDouble(i + 1, Double.parseDouble(value));
                    } else if ("BOOLEAN".equals(columnType)) {
                        pstmt.setBoolean(i + 1, Boolean.parseBoolean(value));
                    } else if ("DATE".equals(columnType)) {
                        pstmt.setDate(i + 1, Date.valueOf(LocalDate.parse(value)));
                    } else if ("TIMESTAMP".equals(columnType)) {
                        pstmt.setTimestamp(i + 1, Timestamp.valueOf(LocalDateTime.parse(value)));
                    } else {
                        pstmt.setString(i + 1, value);  // Default to setting as a string
                    }
                }
                pstmt.executeUpdate();  // Execute the insert query for each row
            }

            // Write success details to the log file
            logWriter.write("CSV data imported successfully.\n");
            logWriter.write("Target Table: " + targetTable + "\n");
            logWriter.write("Total Rows Inserted: " + dataRows.size() + "\n");

            // Return a success response with the log file name
            return new ApiResponse2("Success", "CSV data imported successfully", logFileName);

        } catch (Exception e) {
            // If an error occurs, return an error response with the exception message
            return new ApiResponse2("Error", "An error occurred while importing CSV: " + e.getMessage(), null);
        }
    }

    // Helper method to infer column types dynamically
    private Map<String, String> inferColumnTypes(String[] columns, List<String[]> dataRows) {
        Map<String, String> columnTypes = new LinkedHashMap<>();
        for (int colIndex = 0; colIndex < columns.length; colIndex++) {
            String column = columns[colIndex];
            boolean isInteger = true;
            boolean isDouble = true;
            boolean isBoolean = true;
            boolean isDate = true;
            boolean isDateTime = true;

            // Check each row to infer the column type
            for (String[] row : dataRows) {
                if (colIndex >= row.length) continue;

                String value = row[colIndex].trim();
                if (value.isEmpty()) continue;

                // Try to infer the data type by attempting to parse the value
                if (isInteger) {
                    try {
                        Integer.parseInt(value);  // Check if it can be parsed as Integer
                    } catch (NumberFormatException e) {
                        isInteger = false;
                    }
                }

                if (isDouble) {
                    try {
                        Double.parseDouble(value);  // Check if it can be parsed as Double
                    } catch (NumberFormatException e) {
                        isDouble = false;
                    }
                }

                if (isBoolean) {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        isBoolean = false;
                    }
                }

                if (isDate) {
                    try {
                        LocalDate.parse(value, DateTimeFormatter.ISO_DATE);  // Check if it can be parsed as a Date
                    } catch (Exception e) {
                        isDate = false;
                    }
                }

                if (isDateTime) {
                    try {
                        LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);  // Check if it can be parsed as DateTime
                    } catch (Exception e) {
                        isDateTime = false;
                    }
                }
            }

            // Based on the results of the checks, set the column type
            if (isInteger) {
                columnTypes.put(column, "INTEGER");
            } else if (isDouble) {
                columnTypes.put(column, "DOUBLE PRECISION");
            } else if (isBoolean) {
                columnTypes.put(column, "BOOLEAN");
            } else if (isDate) {
                columnTypes.put(column, "DATE");
            } else if (isDateTime) {
                columnTypes.put(column, "TIMESTAMP");
            } else {
                columnTypes.put(column, "TEXT");  // Default to TEXT if no specific type is found
            }
        }
        return columnTypes;
    }



    // Export data to CSV
    @GetMapping("/data/export/csv")
    public ResponseEntity<?> exportDataToCsv(
            @RequestParam("dbUniqueId") String dbUniqueId,  // Accepts the unique ID of the database
            @RequestParam("schemaName") String schemaName,  // Accepts the schema name
            @RequestParam("tableName") String tableName     // Accepts the table name
    ) {

        String csvFileName = tableName + ".csv";  // Sets the CSV file name based on the table name

        try {
            // Retrieve database connection details using the provided dbUniqueId
            ConnectionInformation connectionInfo = connectionRepo.findByDbUniqueId(dbUniqueId)
                    .stream().findFirst().orElseThrow(() -> new RuntimeException("Connection not found"));

            // Establish a connection to the database
            Connection conn = DriverManager.getConnection(
                    connectionInfo.getUrl(), connectionInfo.getUsername(), connectionInfo.getPassword()
            );

            // Prepare a query to fetch all data from the specified schema and table
            String query = String.format("SELECT * FROM %s.%s", schemaName, tableName);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);  // Execute the query and retrieve the result set

            // Create a FileWriter to write the data into a CSV file
            FileWriter fileWriter = new FileWriter(csvFileName);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            // Get metadata about the table columns
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();  // Total number of columns in the table

            // Write the column names as the CSV header
            for (int i = 1; i <= columnCount; i++) {
                bufferedWriter.write(metaData.getColumnName(i));  // Get and write each column name
                if (i < columnCount) {
                    bufferedWriter.write(",");  // Add a comma between column names
                }
            }
            bufferedWriter.newLine();  // Move to the next line after writing the header

            // Write each row of the result set into the CSV file
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    bufferedWriter.write(rs.getString(i));  // Get and write each column's value
                    if (i < columnCount) {
                        bufferedWriter.write(",");  // Add a comma between values
                    }
                }
                bufferedWriter.newLine();  // Move to the next line after each row
            }

            // Close the BufferedWriter and other resources to release system resources
            bufferedWriter.close();
            rs.close();
            stmt.close();

            // Return a success response with the name of the generated CSV file
            return ResponseEntity.ok().body(new ApiResponse("Data exported to CSV successfully", csvFileName));
        } catch (Exception e) {
            // Handle any exceptions that occur and return an error response
            return ResponseEntity.internalServerError().body(
                    new ApiResponse("Failed to export data to CSV", null)
            );
        }
    }

    @PostMapping("/data/import/json")
    public ResponseEntity<?> importJsonData(@RequestBody JsonNode requestBody) {
        // Extract database connection ID and JSON data from the request body
        String dbConnectionId = requestBody.get("dbConnectionId").asText();
        JsonNode jsonData = requestBody.get("jsonData");

        int recordsInserted = 0; // Counter for inserted records

        try {
            // Fetch database connection details based on the provided ID
            ConnectionInformation connectionInfo = connectionRepo.findByDbUniqueId(dbConnectionId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Connection not found"));

            // Establish a connection to the PostgreSQL database
            try (Connection conn = DriverManager.getConnection(
                    connectionInfo.getUrl(),
                    connectionInfo.getUsername(),
                    connectionInfo.getPassword())) {

                // Extract the keys (column names) from the first JSON row
                JsonNode firstRow = jsonData.elements().next();
                Iterator<String> fieldNames = firstRow.fieldNames();

                // Build the dynamic SQL query for creating a table
                StringBuilder createTableQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS dynamic_table (");
                while (fieldNames.hasNext()) {
                    String field = fieldNames.next();
                    String columnType = "TEXT"; // Default type for columns

                    // Determine the column type based on the JSON field's data type
                    JsonNode valueNode = firstRow.get(field);
                    if (valueNode.isInt()) {
                        columnType = "INTEGER";
                    } else if (valueNode.isTextual() && isDate(valueNode.asText())) {
                        columnType = "DATE"; // If the value looks like a date, use DATE type
                    } else if (valueNode.isTextual()) {
                        columnType = "VARCHAR(255)";
                    } else if (valueNode.isLong()) {
                        columnType = "BIGINT";
                    } else if (valueNode.isArray()) {
                        columnType = "TEXT[]"; // Arrays are stored as text arrays
                    } else if (valueNode.isBoolean()) {
                        columnType = "BOOLEAN";
                    }

                    // Append the column definition to the query
                    createTableQuery.append(field).append(" ").append(columnType).append(", ");
                }

                // Remove the trailing comma and complete the query
                createTableQuery.delete(createTableQuery.length() - 2, createTableQuery.length());
                createTableQuery.append(")");

                // Execute the table creation query
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(createTableQuery.toString());
                }

                // Prepare the SQL statement for inserting data
                StringBuilder insertSQL = new StringBuilder("INSERT INTO dynamic_table (");
                StringBuilder valuesSQL = new StringBuilder("VALUES (");
                fieldNames = firstRow.fieldNames(); // Reset the iterator to traverse the fields again
                while (fieldNames.hasNext()) {
                    String field = fieldNames.next();
                    insertSQL.append(field).append(", ");
                    valuesSQL.append("?, "); // Placeholder for prepared statement
                }
                insertSQL.delete(insertSQL.length() - 2, insertSQL.length());
                valuesSQL.delete(valuesSQL.length() - 2, valuesSQL.length());
                insertSQL.append(") ").append(valuesSQL).append(")");

                // Prepare the statement for batch insertion
                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL.toString())) {

                    // Iterate through each JSON object in the data
                    Iterator<JsonNode> iterator = jsonData.elements();
                    while (iterator.hasNext()) {
                        JsonNode row = iterator.next();
                        int index = 1;

                        // Set values for the prepared statement based on data types
                        for (Iterator<String> fields = row.fieldNames(); fields.hasNext(); ) {
                            String field = fields.next();
                            JsonNode value = row.get(field);

                            if (value.isInt()) {
                                pstmt.setInt(index, value.asInt());
                            } else if (value.isLong()) {
                                pstmt.setLong(index, value.asLong());
                            } else if (value.isTextual() && isDate(value.asText())) {
                                pstmt.setDate(index, java.sql.Date.valueOf(
                                        LocalDate.parse(value.asText(), detectDateFormat(value.asText()))));
                            } else if (value.isTextual()) {
                                pstmt.setString(index, value.asText());
                            } else if (value.isBoolean()) {
                                pstmt.setBoolean(index, value.asBoolean());
                            } else {
                                pstmt.setString(index, value.toString()); // Default case
                            }

                            index++;
                        }

                        pstmt.addBatch(); // Add the current row to the batch
                        recordsInserted++;
                    }

                    // Execute the batch insert
                    pstmt.executeBatch();
                }
            }

            // Return a success response
            return ResponseEntity.ok(new ApiResponse3("JSON data parsed and inserted successfully", recordsInserted));

        } catch (Exception e) {
            e.printStackTrace();
            // Return an error response if any exception occurs
            return ResponseEntity.internalServerError().body(
                    new ApiResponse3("Failed to parse and insert JSON data", 0));
        }
    }

    // Utility method to check if a string represents a date
    private boolean isDate(String value) {
        return detectDateFormat(value) != null;
    }

    // Method to detect the format of a given date string
    private DateTimeFormatter detectDateFormat(String value) {
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                LocalDate.parse(value, formatter);
                return formatter; // Return the matching formatter
            } catch (Exception e) {
                // Ignore invalid formats and try the next one
            }
        }
        return null; // Return null if no valid format is found
    }




    @GetMapping("/data/export/json")
    public ResponseEntity<?> exportDataToJson(
            @RequestParam("dbUniqueId") String dbUniqueId,      // Accept database connection identifier as a request parameter
            @RequestParam("schemaName") String schemaName,      // Accept schema name as a request parameter
            @RequestParam("tableName") String tableName) {      // Accept table name as a request parameter

        // Construct the JSON file name dynamically using the table name
        String jsonFileName = tableName + ".json";

        try {
            // Fetch connection details using the provided dbUniqueId
            ConnectionInformation connectionInfo = connectionRepo.findByDbUniqueId(dbUniqueId)
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Connection not found")); // Handle missing connection info

            // Establish a connection to the database using the fetched connection details
            Connection conn = DriverManager.getConnection(
                    connectionInfo.getUrl(),
                    connectionInfo.getUsername(),
                    connectionInfo.getPassword()
            );

            // Build a query to convert each row of the table into JSON format using PostgreSQL's `row_to_json` function
            String query = String.format("SELECT row_to_json(t) FROM %s.%s t", schemaName, tableName);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query); // Execute the query to fetch table rows as JSON objects

            // Prepare to write the JSON output to a file
            FileWriter fileWriter = new FileWriter(jsonFileName);         // Open file for writing
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); // Use a buffered writer for efficient writing

            bufferedWriter.write("["); // Start of the JSON array
            boolean firstRow = true;   // Track the first row to avoid a trailing comma

            // Iterate through the result set and write each row as a JSON object
            while (rs.next()) {
                if (!firstRow) {
                    bufferedWriter.write(",\n"); // Separate rows with a comma and newline after the first row
                }
                bufferedWriter.write(rs.getString(1)); // Write the JSON object for the current row
                firstRow = false; // Mark that the first row has been written
            }

            bufferedWriter.write("]"); // End of the JSON array

            // Close resources to avoid memory leaks
            bufferedWriter.close(); // Close the buffered writer
            rs.close();             // Close the result set
            stmt.close();           // Close the statement

            // Return a success response with the name of the generated JSON file
            return ResponseEntity.ok().body(
                    new ApiResponse("Data exported to JSON successfully", jsonFileName)
            );

        } catch (Exception e) {
            e.printStackTrace(); // Print stack trace for debugging

            // Return an error response in case of an exception
            return ResponseEntity.internalServerError().body(
                    new ApiResponse("Failed to export data to JSON", null)
            );
        }
    }

}
