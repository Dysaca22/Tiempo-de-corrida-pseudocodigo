import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import java.util.regex.Pattern;
import java.io.BufferedReader;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.io.FileReader;
import java.util.Map;
import java.io.File;

public class App {
    static Map<String, String> STRUCTURE;
    static int[] if_count = { 0, 0 };
    static int if_pos = 0;
    static boolean in_if = false;
    static ArrayList<String[]> assignments = new ArrayList<>();

    public static boolean regexMatch(String regex, String input) {
        return Pattern.compile(regex).matcher(input).find();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> readStructure(String pathname) {
        File structureFile = new File(pathname);
        try (FileReader fileReader = new FileReader(structureFile)) {
            Gson gson = new Gson();
            return gson.fromJson(fileReader, Map.class);
        } catch (java.io.IOException e) {
            System.err.println("Error al leer el archivo structure.json: " + e.getMessage());
        }
        return null;
    }

    public static String detectInstruction(String line) {
        String key_founded = "";

        if (in_if) {
            key_founded = regexMatch(STRUCTURE.get("endif"), line) ? "endif"
                    : regexMatch(STRUCTURE.get("else"), line) ? "else" : "inif";
        } else {
            for (String key : STRUCTURE.keySet()) {
                if (regexMatch(STRUCTURE.get(key), line)) {
                    key_founded = key;
                    break;
                }
            }
        }

        switch (key_founded) {
            case "for":
                String[] elems = line.replaceFirst("^(?i)para", "").trim().split("=");
                String[] parts = elems[1].split(",");
                String increment = parts[2];
                String limit_inf = increment.contains("-") ? parts[1] : parts[0],
                        limit_sup = increment.contains("-") ? parts[0] : parts[1];
                Integer add_assig = 0;
                if (!increment.equals("+1") & !increment.equals("-1") & !limit_inf.equals("1")) {
                    limit_sup = String.format("⌊(%s - %s) / %s⌋ + 1", limit_sup, limit_inf,
                            increment.replace("-", "").replace("+", ""));
                    limit_inf = "1";
                } else if (!limit_inf.equals("1")) {
                    limit_sup = limit_inf.contains("-") ? String.format("%s + %s + 1", limit_sup, limit_inf)
                            : String.format("%s - %s + 1", limit_sup, limit_inf);
                    add_assig = Integer.parseInt(limit_inf) - 1;
                    limit_inf = "1";
                }
                assignments.add(new String[] { elems[0], elems[0] + (add_assig != 0 ? " + " + add_assig : "") });
                return String.format("+ 2 + \\sum_{%s=%s}^{%s}( 2", elems[0], limit_inf, limit_sup);
            case "endfor":
                return " )";
            case "if":
                in_if = true;
                return "+ 1";
            case "else":
                if_pos = 1;
                return "";
            case "endif":
                int if_amount = if_count[0] > if_count[1] ? if_count[0] : if_count[1];
                if_count = new int[] { 0, 0 };
                in_if = false;
                return " + 1".repeat(if_amount);
            case "inif":
                if_count[if_pos]++;
                return "";
            case "assignment":
                assignments.add(line.split("="));
                return "+ 1";
            default:
                return "+ 1";
        }
    }

    public static String process(BufferedReader reader) throws java.io.IOException {
        System.out.println("Procesando...");
        String line = reader.readLine();
        boolean startFound = false;

        String result = "";
        while (line != null && !(startFound && regexMatch(STRUCTURE.get("end"), line))) {
            if (regexMatch(STRUCTURE.get("start"), line)) {
                startFound = true;
            } else {
                result += detectInstruction(line) + " ";
            }
            line = reader.readLine();
        }
        return result.replaceAll("\\s+", " ");
    }

    public static void main(String[] args) {
        STRUCTURE = readStructure("src/structure.json");
        String first_result = "";

        JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                first_result = process(reader);
            } catch (java.io.IOException e) {
                System.err.println("Error al leer el archivo: " + e.getMessage());
            }
        }

        System.out.println("T(n) = " + first_result + "\n");
        String output = Resolver.extractSumElements(first_result, assignments);
        System.out.println("Reducción...");
        System.out.println("T(n) = " + output);

        JOptionPane.showMessageDialog(null,
                "Resultados:\n\n" +
                        "T(n) = " + first_result + "\n\n" +
                        "Reducción:\n" +
                        "T(n) = " + output,
                "Resultados del Análisis",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
