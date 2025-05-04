package com.example.calculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.springframework.http.ResponseEntity;

// import net.objecthunter.exp4j.Expression;
// import net.objecthunter.exp4j.ExpressionBuilder;

import org.springframework.web.bind.annotation.*;


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

    @PostMapping("/calculate")
    // @RequestBody is telling the Spring to take the JSON from the request body and then convert it into a CalculationRequest object

    @SuppressWarnings({"CallToPrintStackTrace", "UseSpecificCatch"})
    public ResponseEntity<?> calculate(@RequestBody CalculationRequest request){
        String expressionFromFrontend = request.getExpression();
        System.out.println("Received expression from the frontend: " + expressionFromFrontend);

        try {
            double result = evaluateSimpleExpression(expressionFromFrontend);
            System.out.println("Calculated: " + result);


            // for infinite and NaN result check
            if (!Double.isFinite(result)){
                ResponseEntity.badRequest().body(Map.of("error", "Invalid operation or division by zero"));  
            }

            // return the calculation result as a json
            return ResponseEntity.ok(Map.of("result", result));

        }   catch (IllegalArgumentException | ArithmeticException e){

            System.err.println("Calculation error" + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        }catch (Exception e){
            System.err.println("Calculation error: " + e.getMessage());
            e.printStackTrace(); // for debugging on the server-side
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid expression format"));
        }
    }

    private static boolean isOperator(char c){
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private static boolean isNumber(String s){
        if (s == null || s.isEmpty()) return false; 
        try {
            Double.valueOf(s);
            return true;
        } catch (NumberFormatException e){
            return false;
        }
    }
    private static double evaluateSimpleExpression(String expression) throws IllegalArgumentException, ArithmeticException{
        if (expression == null || expression.trim().isEmpty()){
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        String expressionStr = expression.trim();

        // basic tokenization

        List<String> tokens = new ArrayList<>();
        StringBuilder currentNumber = new StringBuilder();
        for (char c : expressionStr.toCharArray()){
            if (Character.isDigit(c) || c == '.'){
                currentNumber.append(c);
            } else if (isOperator(c)) {
                if (currentNumber.length() > 0 ){
                    tokens.add(currentNumber.toString());
                    currentNumber.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else if (!Character.isWhitespace(c)) {
                throw new IllegalArgumentException("Invalid character in expression "+c);
            }
        }

        if (currentNumber.length() > 0) {
            tokens.add(currentNumber.toString());
        }

        if (tokens.isEmpty()) throw new IllegalArgumentException("The result of the expression is no token");
        
        // precedence handling of multiplication and dividing first

        List<String> pass1 = new ArrayList<>();
        Stack<Double> operandStack = new Stack<>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (isNumber(token)){
                operandStack.push(Double.valueOf(token));
            } else if (token.equals("*") || token.equals("/")){
                if (operandStack.isEmpty() || i + 1 >= tokens.size() || !isNumber(tokens.get(i+1))){
                    throw new IllegalArgumentException("Invalid expression format near " + token);
                }

                double left = operandStack.pop();
                double right = Double.parseDouble(tokens.get(i+1));

                if (token.equals("/") && right == 0.0) {
                    throw new ArithmeticException("Division by zero");
                }

                double result = token.equals("*") ? left * right : left / right;
                if (Double.isInfinite(result) || Double.isNaN(result)) {
                    throw new ArithmeticException("Invalid operation result");
                }

                operandStack.push(result);
                i++;
            } else {
                if (!operandStack.isEmpty()){
                    pass1.add(String.valueOf(operandStack.pop()));
                    operandStack.clear();
                }
                pass1.add(token);
            }
        }

  

        if (!operandStack.isEmpty()) {
            pass1.add(String.valueOf(operandStack.pop()));
        }

        // secondpass for addition and subtraction

        double finalResult = 0;
        char currentOperator = '+';

        if (!pass1.isEmpty() && isNumber(pass1.get(0))){
            finalResult = Double.parseDouble(pass1.get(0));
        } else if (pass1.isEmpty()){
            return 0;
        } else {
            throw new IllegalArgumentException("Expression can't start with operator");
        }

        for (int i = 0; i < pass1.size(); i++) {
            String token = pass1.get(i);
            if (token.equals("+") || token.equals("-")) {
                currentOperator = token.charAt(0);
                if (i + 1 >= pass1.size() || !isNumber(pass1.get(i+1))){
                    throw new IllegalArgumentException("Operator must be followed by a number");

                } 
            } else if (isNumber(token)){
                double operand = Double.parseDouble(token);
                if (currentOperator == '+'){
                    finalResult += operand;
                } else {
                    finalResult -= operand;
                }
            } else {
                throw new IllegalArgumentException("Unexpected token in second pass: " + token);
            }

        }
        
        return finalResult;
    }
}


