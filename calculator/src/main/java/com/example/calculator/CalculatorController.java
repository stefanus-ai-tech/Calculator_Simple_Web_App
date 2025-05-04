package com.example.calculator;


import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api") // Base path for all endpoint in this controller
public class CalculatorController {
    
    public static class CalculationRequest {
        private String expression;

        public String getExpression(){
            return expression;
        }

        public void setExpression(String expression){
            this.expression = expression;
        }

        // getter and setter is for Spring to deserialize JSON into this object
    }

    // Listen for POST request specifically to "/api/calculate"

    // Inside CalculatorController.java

    @PostMapping("/calculate")
    @SuppressWarnings("CallToPrintStackTrace")
    public ResponseEntity<?> calculate(@RequestBody CalculationRequest request) {
        String expression = request.getExpression();
        System.out.println("Received expression (single operation attempt): " + expression);

        if (expression == null || expression.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Expression cannot be empty"));
        }

        expression = expression.trim();
        char operator = ' ';
        int operatorIndex = -1;
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (operatorIndex != -1) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Only single operation supported"));
                }
                // Basic check for operator position (not first or last char)
                // Note: This prevents starting with '-' for negative numbers. A more complex check is needed for that.
                if (i == 0 || i == expression.length() - 1) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid operator position"));
                }
                operator = c;
                operatorIndex = i;
            }
        }

        if (operatorIndex == -1) {
            return ResponseEntity.badRequest().body(Map.of("error", "No valid operator found for single operation"));
        }

        try {
            // Split the string into two parts based on the operator's position
            String leftPart = expression.substring(0, operatorIndex).trim(); // Gets everything BEFORE the operator
            String rightPart = expression.substring(operatorIndex + 1).trim(); // Gets everything AFTER the operator

            // Handling the multidigit
            // Double.parseDouble() correctly converts strings like "12321", "-50", "3.14" into numbers.
            double leftOperand = Double.parseDouble(leftPart);
            double rightOperand = Double.parseDouble(rightPart);
            // *** ***

            double result;

            // Perform the single operation
            switch (operator) {
                case '+' : 
                    result = leftOperand + rightOperand;
                    break;
                case '-' : 
                    result = leftOperand - rightOperand;
                    break;
                case '*' : 
                    result = leftOperand * rightOperand;
                    break;
                case '/' : {
                    if (rightOperand == 0) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Division by zero"));
                    }
                    result = leftOperand / rightOperand;
                    break; // Added break
                }
                default : {
                    // This case should ideally not be reached if operator validation is robust earlier
                    // But if it is, it indicates an unexpected operator char slipped through
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid or unsupported operator")); 
                }
            }

            if (!Double.isFinite(result)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Result is not a finite number"));
            }

            System.out.println("Calculated result (single op): " + result);
            return ResponseEntity.ok(Map.of("result", result));

        } catch (NumberFormatException e) {
            // This error occurs if Double.parseDouble fails (e.g., "12a" or empty string after split)
            System.err.println("Error parsing numbers: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid number format"));
        } catch (Exception e) {
            System.err.println("Unexpected calculation error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Server calculation error"));
        }
    }

}
