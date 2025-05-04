async function calculateResult() {
  const expression = display.value;
  if (!expression) {
    return; // Empty expression is prohibited
  }

  console.log('Sending expression to the backend side', expressionToSend);

  try {
    // fetch is for POST request to the backend API endpoints
    const response = await fetch('/api/calculate', {
      // Match @PostMapping path
      method: 'POST', // use POST to send the data in the request body
      headers: {
        // Telling the server that we are sending the JSON data
        'Content-Type': 'application/json',
        // Telling the server, we want the JSON back
        Accept: 'application/json',
      },
      // Converting the js data {expression: "..."} into JSON string
      body: JSON.stringify({ expression: expression }), // Sending expression in the JSON body
    });

    // waiting for the server's response and parse it as a JSON file
    const data = await response.json();

    console.log('Received response from the backend side: ', data);

    if (response.ok) {
      // if server HTTP 2xx status, the display will be updated with the result from the server JSON response
      // Rounding the display value to avoid floating point inaccuracies
      display.value = parseFloat(data.result.toFixed(10));
    } else {
      // if the server responded with an error (HTTP 4xx or 5xx status), it will display the error message form the server's JSON response
      // Display the error message from the server side
      display.value = data.error || 'Error'; // assume backend sends {"error": ...}
      console.error('Calculation error:', data.error);
    }
  } catch (error) {
    display.value = 'Error';
    console.error('Fetch error:', error);
  }
}
