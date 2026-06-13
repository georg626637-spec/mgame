import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatTest {
    static final String MODEL = "qwen3.6-12b-iq-ultra-heretic-uncensored-thinking-v2-hightop";
    static final String SYSTEM = "You are inside a voxel 3D world with terrain, trees, water. Be concise (max 2 sentences). Stay in character as an AI in a block world.";

    public static void main(String[] args) throws Exception {
        String endpoint = args.length > 0 ? args[0] : "http://127.0.0.1:1234/v1/chat/completions";
        System.out.println("Testing API: " + endpoint);
        System.out.println("Model: " + MODEL);
        System.out.println("Type messages (or 'quit' to exit)\n");

        Scanner sc = new Scanner(System.in);
        try {
            while (true) {
                System.out.print("> ");
                if (!sc.hasNextLine()) break;
                String msg = sc.nextLine();
                if (msg.equals("quit")) break;

                String body = "{\"model\":\"" + MODEL + "\",\"messages\":[" +
                    "{\"role\":\"system\",\"content\":\"" + escape(SYSTEM) + "\"}," +
                    "{\"role\":\"user\",\"content\":\"" + escape(msg) + "\"}]," +
                    "\"stream\":false}";

                System.out.println("--- Request ---");
                System.out.println(body);
                System.out.println("--- Response ---");

                try {
                    URL url = new URL(endpoint);
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST");
                    c.setRequestProperty("Content-Type", "application/json");
                    c.setDoOutput(true);
                    c.setConnectTimeout(5000);
                    c.setReadTimeout(60000);

                    try (OutputStream os = c.getOutputStream()) {
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }

                    int status = c.getResponseCode();
                    System.out.println("HTTP " + status);

                    BufferedReader r;
                    if (status == 200) {
                        r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                    } else {
                        r = new BufferedReader(new InputStreamReader(c.getErrorStream(), StandardCharsets.UTF_8));
                    }

                    StringBuilder sb = new StringBuilder();
                    String line;
                    try (BufferedReader br = r) {
                        while ((line = br.readLine()) != null) sb.append(line);
                    }

                    String raw = sb.toString();
                    System.out.println(raw);

                    if (status == 200) {
                        System.out.println("\n--- Parsed ---");
                        System.out.println(parseContent(raw));
                    }
                    c.disconnect();
                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println();
            }
        } finally {
            sc.close();
        }
    }

    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    static String parseContent(String json) {
        if (json == null || json.isEmpty()) return "NOT FOUND";
        int idx = json.indexOf("\"content\"");
        if (idx < 0) return "NOT FOUND";
        idx = json.indexOf(':', idx + 9);
        if (idx < 0) return "NOT FOUND";
        idx = json.indexOf('"', idx + 1);
        if (idx < 0) return "NOT FOUND";
        idx++;
        StringBuilder out = new StringBuilder();
        for (; idx < json.length(); idx++) {
            char ch = json.charAt(idx);
            if (ch == '\\' && idx + 1 < json.length()) {
                char n = json.charAt(++idx);
                if (n == 'n') out.append('\n');
                else if (n == '"') out.append('"');
                else if (n == '\\') out.append('\\');
                else if (n == 't') out.append('\t');
                else out.append(ch).append(n);
            } else if (ch == '"') break;
            else out.append(ch);
        }
        return out.toString();
    }
}
