import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;


public class Resolver {
    public static String extractSumElements(String input, ArrayList<String[]> assignments) {
        String result = "";
        String startRegex = "\\+\\s*\\\\sum\\_\\{([^}]*)\\}\\^\\{([^}]*)\\}\\(";
        String endRegex = "\\)";
        StringBuilder input_bulder = new StringBuilder(input);
        Pattern pattern = Pattern.compile(startRegex + "((?:(?!" + startRegex + ").|\n)*?)" + endRegex);
        Matcher matcher = pattern.matcher(input_bulder);

        while (matcher.find()) {
            String substring = matcher.group(1);
            int openParentheses = 1;
            int endIndex = matcher.end();

            for (int i = matcher.start() + matcher.group().indexOf('(') + 1; i < input_bulder.length(); i++) {
                if (input_bulder.charAt(i) == '(') {
                    openParentheses++;
                } else if (input_bulder.charAt(i) == ')') {
                    openParentheses--;
                }

                if (openParentheses == 0) {
                    endIndex = i + 1;
                    break;
                }
            }

            substring = input_bulder.substring(matcher.start(), endIndex);
            substring = substring.replaceFirst("^" + startRegex, "").replaceFirst(endRegex + "$", "");
            String subresult = extractSumElements(substring.trim(), assignments);
            subresult += ", " + matcher.group().replaceFirst(".*\\_\\{", "").replaceFirst("\\}.*", "");
            subresult += varChange(", " + matcher.group().replaceFirst(".*\\^\\{", "").replaceFirst("\\}.*", ""), assignments);
            String solveSum = solveSummation(subresult.split(","));
            input_bulder.replace(matcher.start(), endIndex, "+ " + solveSum);
            matcher.reset(input_bulder);
        }

        result += input_bulder.toString();
        return result;
    }

    private static String varChange(String expression, ArrayList<String[]> assignments) {
        for (String[] assignment : assignments) {
            String variable = assignment[0];
            String value = assignment[1];
            expression = expression.replaceAll("\\b" + Pattern.quote(variable) + "\\b", value);
        }
        return expression;
    }

    private static String solveSummation(String[] input) {
        if (input.length != 3) {
            return "Invalid input format";
        }

        String expression = input[0];
        String indexRange = input[1];
        String upperLimit = input[2];

        // Extract index and limits
        Pattern pattern = Pattern.compile("(\\w+)=(\\d+)");
        Matcher matcher = pattern.matcher(indexRange);
        if (!matcher.find()) {
            return "Invalid index range format";
        }

        String index = matcher.group(1);
        int lowerLimit = Integer.parseInt(matcher.group(2));

        // Replace index with 'j' for easier parsing
        expression = expression.replace(index, "j");

        // Solve the summation
        String result = solveExpression(expression, lowerLimit, upperLimit);

        return result;
    }

    private static String solveExpression(String expression, int lowerLimit, String upperLimit) {
        if (expression.contains("j")) {
            // Case with variable index
            if (expression.equals("j")) {
                return String.format("((%s * (%s + 1)) / 2)", upperLimit, upperLimit);
            } else if (expression.matches("\\d+\\s*\\+\\s*j")) {
                String[] parts = expression.split("\\+");
                int constant = Integer.parseInt(parts[0].trim());
                return String.format("(%d * (%s - %d + 1) + (%s * (%s + 1)) / 2)",
                        constant, upperLimit, lowerLimit, upperLimit, upperLimit);
            } else {
                // For more complex expressions, return a general form
                return String.format("\\sum_{%s}^{%s}(%s)", lowerLimit, upperLimit, expression);
            }
        } else {
            // Case with constant expression
            return String.format("((%s) * (%s - %d + 1))", expression, upperLimit, lowerLimit);
        }
    }
}