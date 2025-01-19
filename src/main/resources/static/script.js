document.getElementById('connectionForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    // Gather form data
    const connectionData = {
        dbUniqueId: document.getElementById('dbUniqueId').value,
        url: document.getElementById('url').value,
        username: document.getElementById('username').value,
        password: document.getElementById('password').value,
        port: parseInt(document.getElementById('port').value)
    };

    // Send data to backend to save the connection
    try {
        const response = await fetch('http://localhost:8080/api/connection', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(connectionData),
        });

        const result = await response.json();

        // Display success/failure message
        const messageDiv = document.getElementById('responseMessage');
        if (response.ok) {
            messageDiv.innerHTML = `<div class="alert alert-success">${result.message}. File Created: ${result.fileName}</div>`;
        } else {
            messageDiv.innerHTML = `<div class="alert alert-danger">${result.message}</div>`;
        }

        // Hide the alert message after 5 seconds
        setTimeout(() => {
            messageDiv.innerHTML = '';
        }, 5000);

    } catch (error) {
        document.getElementById('responseMessage').innerHTML = `<div class="alert alert-danger">Error: ${error.message}</div>`;
        setTimeout(() => {
            document.getElementById('responseMessage').innerHTML = '';
        }, 2000);
    }
});

// Dark Mode Toggle Logic
const modeToggle = document.getElementById('modeToggle');
const body = document.body;

// Check for dark mode in localStorage on initial load
if (localStorage.getItem('darkMode') === 'true') {
    body.classList.add('bg-dark', 'text-light');
    modeToggle.textContent = 'Switch to Light Mode';
} else {
    body.classList.add('bg-light', 'text-dark');
    modeToggle.textContent = 'Switch to Dark Mode';
}

modeToggle.addEventListener('click', function() {
    if (body.classList.contains('bg-dark')) {
        // Switch to light mode
        body.classList.remove('bg-dark', 'text-light');
        body.classList.add('bg-light', 'text-dark');
        modeToggle.textContent = 'Switch to Dark Mode';
        localStorage.setItem('darkMode', 'false');
    } else {
        // Switch to dark mode
        body.classList.remove('bg-light', 'text-dark');
        body.classList.add('bg-dark', 'text-light');
        modeToggle.textContent = 'Switch to Light Mode';
        localStorage.setItem('darkMode', 'true');
    }
});

// Export Data Functions
async function exportData(format) {
    const dbUniqueId = document.getElementById('exportDbUniqueId').value;
    const schemaName = document.getElementById('exportSchemaName').value;
    const tableName = document.getElementById('exportTableName').value;

    try {
        const response = await fetch(`http://localhost:8080/api/data/export-${format}?dbUniqueId=${dbUniqueId}&schemaName=${schemaName}&tableName=${tableName}`);
        const result = await response.json();

        // Display result
        const messageDiv = document.getElementById('responseMessage');
        if (response.ok) {
            messageDiv.innerHTML = `<div class="alert alert-success">${result.message}. File Created: ${result.filePath}</div>`;
        } else {
            messageDiv.innerHTML = `<div class="alert alert-danger">${result.message}</div>`;
        }

        // Hide the alert message after 5 seconds
        setTimeout(() => {
            messageDiv.innerHTML = '';
        }, 5000);
    } catch (error) {
        document.getElementById('responseMessage').innerHTML = `<div class="alert alert-danger">Error: ${error.message}</div>`;
        setTimeout(() => {
            document.getElementById('responseMessage').innerHTML = '';
        }, 2000);
    }
}

document.getElementById('exportCsvBtn').addEventListener('click', () => exportData('csv'));
document.getElementById('exportJsonBtn').addEventListener('click', () => exportData('json'));
document.getElementById('exportTxtBtn').addEventListener('click', () => exportData('txt'));

// View Saved Connections
document.getElementById('viewConnectionsBtn').addEventListener('click', async () => {
    try {
        const response = await fetch('http://localhost:8080/api/connection');
        const data = await response.json();

        const tableBody = document.getElementById('connectionsTable').querySelector('tbody');
        tableBody.innerHTML = ''; // Clear existing rows

        data.forEach((connection) => {
            const row = tableBody.insertRow();
            row.insertCell(0).textContent = connection.dbUniqueId;
            row.insertCell(1).textContent = connection.url;
            row.insertCell(2).textContent = connection.username;
            row.insertCell(3).textContent = connection.port;
        });
    } catch (error) {
        document.getElementById('responseMessage').innerHTML = `<div class="alert alert-danger">Error: ${error.message}</div>`;
    }
});

$(document).ready(function () {
    // Add column functionality
    $('#add-column').on('click', function () {
        const columnCount = $('#columns .column').length + 1;  // Calculate the next column count
        const columnHtml = `
            <div class="column row mb-3">
                <div class="col">
                    <label for="column_name_${columnCount}" class="form-label">Column Name:</label>
                    <input type="text" class="form-control" id="column_name_${columnCount}" name="column_name" required>
                </div>
                <div class="col">
                    <label for="data_type_${columnCount}" class="form-label">Data Type:</label>
                    <select class="form-select" id="data_type_${columnCount}" name="data_type">
                        <option value="TEXT">Text</option>
                        <option value="INTEGER">Integer</option>
                        <option value="DATE">Date</option>
                        <option value="DATETIME">DateTime</option>
                    </select>
                </div>
                <div class="col">
                    <label for="transformation_${columnCount}" class="form-label">Transformation:</label>
                    <select class="form-select" id="transformation_${columnCount}" name="transformation">
                        <option value="">None</option>
                        <option value="capitalize">Capitalize</option>
                        <option value="uppercase">Uppercase</option>
                        <option value="lowercase">Lowercase</option>
                        <option value="removeextraspaces">Remove Extra Spaces</option>
                        <option value="removespecialcharacters">Remove Special Characters</option>
                    </select>
                </div>
                <div class="col-auto">
                    <button type="button" class="btn btn-danger remove-column mt-4">Remove</button>
                </div>
            </div>
        `;
        $('#columns').append(columnHtml);
    });

    // Remove column functionality
    $(document).on('click', '.remove-column', function () {
        $(this).closest('.column').remove();
    });

    // Form submission
    $('#csv-form').on('submit', async function (e) {
        e.preventDefault();

        const formData = new FormData();
        formData.append('databaseUniqueId', $('#databaseUniqueId').val());
        formData.append('file', $('#file')[0].files[0]);

        // Collect column definitions and transformations
        const columnDefinitions = {};
        const transformations = {};
        $('.column').each(function () {
            const columnName = $(this).find('input[name="column_name"]').val();
            const dataType = $(this).find('select[name="data_type"]').val();
            const transformation = $(this).find('select[name="transformation"]').val();

            columnDefinitions[columnName] = dataType;
            if (transformation) transformations[columnName] = transformation;
        });

        formData.append('columnDefinitions', JSON.stringify(columnDefinitions));
        formData.append('transformations', JSON.stringify(transformations));

        // Submit to backend
        const response = await fetch('http://localhost:8080/api/data/import/csv', {
            method: 'POST',
            body: formData
        });

        const result = await response.json();
        alert(result.message);
    });
});

document.getElementById('submitButton').addEventListener('click', async () => {
    const dbConnectionId = document.getElementById('dbConnectionId').value.trim();
    const jsonDataInput = document.getElementById('jsonData').value.trim();

    // Validate inputs
    if (!dbConnectionId || !jsonDataInput) {
        showResponse('Please fill out all fields.', 'danger');
        return;
    }

    try {
        const jsonData = JSON.parse(jsonDataInput); // Parse the JSON string

        // Prepare the request payload
        const payload = {
            dbConnectionId: dbConnectionId,
            jsonData: jsonData
        };

        // Make the POST request
        const response = await fetch('http://localhost:8080/api/data/import/json', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        const result = await response.json();

        if (response.ok) {
            showResponse(result.message + ` Records inserted: ${result.recordsInserted}`, 'success');
        } else {
            showResponse(result.message || 'Failed to insert JSON data.', 'danger');
        }
    } catch (error) {
        console.error('Error:', error);
        showResponse('Invalid JSON format or server error.', 'danger');
    }
});

// Function to display response messages
function showResponse(message, type) {
    const messageDiv = document.getElementById('responseMessage');

    // Set the alert message with the appropriate class based on the type
    messageDiv.innerHTML = `<div class="alert alert-${type}">${message}</div>`;

    // Optionally, you can hide the message after 5 seconds
    setTimeout(() => {
        messageDiv.innerHTML = '';
    }, 5000);
}