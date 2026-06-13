import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class ChatSystem {
    private final LinkedList<String> messages = new LinkedList<>();
    private final StringBuilder input = new StringBuilder();
    private boolean active = false;
    private boolean waiting = false;
    private int scrollOffset = 0;
    private static final int MAX_HISTORY = 100;
    private static final int CLOSED_LINES = 4;
    private static final int OPEN_LINES = 8;
    boolean suppressNextChar = false;
    private static final String MODEL = "qwen3.6-12b-iq-ultra-heretic-uncensored-thinking-v2-hightop";
    private static final String SYSTEM = "You are inside a voxel 3D world with terrain, trees, water. Be concise (max 2 sentences). Stay in character as an AI in a block world.";

    public void toggle() {
        active = !active;
        if (active) {
            input.setLength(0);
            scrollOffset = 0;
            tabIndex = -1;
            suppressNextChar = true;
        }
    }
    public boolean isActive() { return active; }
    public boolean hasMessages() { return !messages.isEmpty(); }

    private static final String[] COMMANDS = {"/new", "/mode 1", "/mode 2", "/create ", "/blockadd ", "/blockshape ", "/blockedit ", "/craft", "/seed", "/help", "/mod", "/server", "/client "};
    private int tabIndex;

    public void onChar(char c) {
        if (suppressNextChar) { suppressNextChar = false; return; }
        if (active && !waiting && c >= ' ' && c <= '~') { input.append(c); tabIndex = -1; }
    }

    public void onBackspace() {
        if (active && !waiting && input.length() > 0) {
            input.setLength(input.length() - 1);
            tabIndex = -1;
        }
    }

    public void onTab() {
        if (!active || waiting) return;
        String cur = input.toString();
        java.util.List<String> matches = new java.util.ArrayList<>();
        for (String cmd : COMMANDS)
            if (cmd.startsWith(cur) || cur.isEmpty()) matches.add(cmd);
        if (matches.isEmpty()) return;
        tabIndex = (tabIndex + 1) % matches.size();
        input.setLength(0);
        input.append(matches.get(tabIndex));
    }

    public void scrollUp() { scrollOffset++; }
    public void scrollDown() { if (scrollOffset > 0) scrollOffset--; }

    private Runnable terrainCallback;
    private Consumer<String> createTextureCallback;
    private Consumer<String> addBlockCallback;
    private Consumer<String> blockShapeCallback;
    private Consumer<String> blockEditCallback;
    private Consumer<Integer> modeCallback;
    private Runnable craftCallback;
    private Runnable modCallback;
    private Consumer<String> serverCallback;
    private Consumer<String> clientCallback;
    private Consumer<Long> seedCallback;
    public void setTerrainCallback(Runnable r) { terrainCallback = r; }
    public void setCreateTextureCallback(Consumer<String> cb) { createTextureCallback = cb; }
    public void setAddBlockCallback(Consumer<String> cb) { addBlockCallback = cb; }
    public void setBlockShapeCallback(Consumer<String> cb) { blockShapeCallback = cb; }
    public void setBlockEditCallback(Consumer<String> cb) { blockEditCallback = cb; }
    public void setModeCallback(Consumer<Integer> cb) { modeCallback = cb; }
    public void setCraftCallback(Runnable r) { craftCallback = r; }
    public void setModCallback(Runnable r) { modCallback = r; }
    public void setServerCallback(Consumer<String> cb) { serverCallback = cb; }
    public void setClientCallback(Consumer<String> cb) { clientCallback = cb; }
    public void setSeedCallback(Consumer<Long> cb) { seedCallback = cb; }

    public void onEnter() {
        if (!active || waiting) return;
        tabIndex = -1;
        String msg = input.toString().trim();
        if (msg.isEmpty()) return;
        if (msg.toLowerCase().startsWith("/new") && terrainCallback != null) {
            input.setLength(0);
            scrollOffset = 0;
            addMessage("> " + msg);
            terrainCallback.run();
            return;
        }
        if (msg.toLowerCase().startsWith("/create ")) {
            String name = msg.substring(8).trim();
            if (!name.isEmpty() && createTextureCallback != null) {
                input.setLength(0);
                addMessage("> " + msg);
                createTextureCallback.accept(name);
                return;
            }
        }
        if (msg.toLowerCase().startsWith("/blockadd ")) {
            String name = msg.substring(10).trim();
            if (!name.isEmpty() && addBlockCallback != null) {
                input.setLength(0);
                addMessage("> " + msg);
                addBlockCallback.accept(name);
                return;
            }
        }
        if (msg.toLowerCase().startsWith("/blockshape ")) {
            String name = msg.substring(12).trim();
            if (!name.isEmpty() && blockShapeCallback != null) {
                input.setLength(0);
                addMessage("> " + msg);
                blockShapeCallback.accept(name);
                return;
            }
        }
        if (msg.toLowerCase().startsWith("/blockedit ")) {
            String name = msg.substring(11).trim();
            if (!name.isEmpty() && blockEditCallback != null) {
                input.setLength(0);
                addMessage("> " + msg);
                blockEditCallback.accept(name);
                return;
            }
        }
        if (msg.toLowerCase().startsWith("/mode ")) {
            String arg = msg.substring(6).trim();
            if (modeCallback != null) {
                input.setLength(0);
                addMessage("> " + msg);
                try { modeCallback.accept(Integer.parseInt(arg)); }
                catch (NumberFormatException e) { addMessage("Usage: /mode 1 (survival) | /mode 2 (creative)"); }
                return;
            }
        }
        if (msg.toLowerCase().startsWith("/craft")) {
            input.setLength(0);
            addMessage("> " + msg);
            if (craftCallback != null) craftCallback.run();
            return;
        }
        if (msg.toLowerCase().startsWith("/mod")) {
            input.setLength(0);
            addMessage("> " + msg);
            if (modCallback != null) modCallback.run();
            return;
        }
        if (msg.toLowerCase().startsWith("/seed")) {
            String arg = msg.length() > 5 ? msg.substring(5).trim() : "";
            input.setLength(0);
            addMessage("> " + msg);
            if (seedCallback != null) {
                if (arg.isEmpty()) {
                    seedCallback.accept(System.currentTimeMillis());
                } else {
                    try { seedCallback.accept(Long.parseLong(arg)); }
                    catch (NumberFormatException e) { addMessage("Usage: /seed [number]"); }
                }
            }
            return;
        }
        if (msg.toLowerCase().startsWith("/server")) {
            input.setLength(0);
            addMessage("> " + msg);
            String arg = msg.length() > 8 ? msg.substring(8).trim() : "";
            if (serverCallback != null) serverCallback.accept(arg);
            return;
        }
        if (msg.toLowerCase().startsWith("/client ")) {
            input.setLength(0);
            addMessage("> " + msg);
            String arg = msg.substring(8).trim();
            if (clientCallback != null) clientCallback.accept(arg);
            return;
        }
        if (msg.toLowerCase().startsWith("/help")) {
            input.setLength(0);
            addMessage("> " + msg);
            addMessage("/new - world | /seed [n] - set seed | /mode 1/2 - survival/creative | /create - pixel editor | /blockadd - add block | /blockshape - shape editor | /blockedit - block editor | /craft - crafting table | /mod - list mods | /server [port] - host | /client <ip:port> - connect | /help - this");
            return;
        }
        addMessage("> " + msg);
        input.setLength(0);
        waiting = true;
        scrollOffset = 0;
        new Thread(() -> {
            addMessage(query(msg));
            waiting = false;
        }).start();
    }

    private void addMessage(String msg) {
        messages.add(msg);
        while (messages.size() > MAX_HISTORY) messages.removeFirst();
        scrollOffset = 0;
    }

    private String query(String prompt) {
        try {
            URL url = new URL("http://127.0.0.1:1234/v1/chat/completions");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setDoOutput(true);
            c.setConnectTimeout(5000);
            c.setReadTimeout(60000);
            String body = "{\"model\":\"" + MODEL + "\",\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escape(SYSTEM) + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escape(prompt) + "\"}]," +
                "\"stream\":false}";
            OutputStream os = c.getOutputStream();
            os.write(body.getBytes(StandardCharsets.UTF_8));
            os.flush();
            int status = c.getResponseCode();
            if (status != 200) {
                BufferedReader er = new BufferedReader(
                    new InputStreamReader(c.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder eb = new StringBuilder();
                String l; while ((l = er.readLine()) != null) eb.append(l);
                er.close();
                return "HTTP " + status + ": " + eb.toString();
            }
            BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line; while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return parseChatResponse(sb.toString());
        } catch (ConnectException e) {
            return "Can't reach LM Studio at 127.0.0.1:1234";
        } catch (Exception e) {
            return "Error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private String parseChatResponse(String json) {
        int idx = json.indexOf("\"content\"");
        if (idx < 0) return "Parse error";
        idx = json.indexOf(':', idx + 9);
        if (idx < 0) return "Parse error";
        idx = json.indexOf('"', idx + 1);
        if (idx < 0) return "Parse error";
        idx++;
        StringBuilder out = new StringBuilder();
        for (; idx < json.length(); idx++) {
            char ch = json.charAt(idx);
            if (ch == '\\' && idx + 1 < json.length()) {
                char n = json.charAt(++idx);
                if (n == 'n') out.append('\n');
                else if (n == '"') out.append('"');
                else if (n == '\\') out.append('\\');
                else out.append(ch).append(n);
            } else if (ch == '"') break;
            else out.append(ch);
        }
        return out.toString();
    }

    public void render(Graphics2D g, int w, int h) {
        int lineCount = active ? OPEN_LINES : CLOSED_LINES;
        int totalMsgLines = countMessageLines(g, w);
        int maxScroll = Math.max(0, totalMsgLines - lineCount);

        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        int boxH = active ? 200 : 90;
        int boxY = h - boxH;
        int inputH = active ? 32 : 0;

        if (active || !messages.isEmpty()) {
            int alpha = active ? 150 : 100;
            g.setColor(new Color(10, 10, 30, alpha));
            g.fillRect(0, boxY, w, boxH);
            g.setColor(new Color(60, 60, 255, alpha / 2));
            g.drawLine(0, boxY, w, boxY);
        }

        Font font = new Font("Monospaced", Font.BOLD, 16);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int y = boxY + 20;

        int startLine = Math.max(0, totalMsgLines - lineCount - scrollOffset);
        int line = 0;

        List<String> allLines = buildMessageLines(g, w);
        for (int li = 0; li < allLines.size() && line < lineCount; li++) {
            if (li < startLine) continue;
            String txt = allLines.get(li);
            Color tc = txt.startsWith("> ") ? new Color(100, 255, 100) : new Color(255, 200, 80);
            g.setColor(new Color(0, 0, 0, 140));
            g.drawString(txt, 12, y + 2);
            g.setColor(tc);
            g.drawString(txt, 10, y);
            y += 22;
            line++;
        }

        if (line < lineCount && waiting) {
            String think = "Thinking" + ".".repeat((int)(System.currentTimeMillis() / 400 % 4));
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(think, 12, y + 2);
            g.setColor(new Color(0, 200, 255));
            g.drawString(think, 10, y);
            y += 22;
            line++;
        }

        if (line < lineCount && active && y < h - inputH - 5) {
            g.setFont(new Font("Monospaced", Font.ITALIC, 13));
            g.setColor(new Color(150, 150, 200, 180));
            g.drawString("/help - commands  |  UP/DOWN scroll  |  ESC close", 10, y);
        }

        if (active) {
            g.setFont(font);
            g.setColor(new Color(0, 0, 20, 200));
            g.fillRect(0, h - inputH, w, inputH);
            g.setColor(new Color(100, 100, 255, 150));
            g.drawLine(0, h - inputH, w, h - inputH);
            String cursor = System.currentTimeMillis() / 500 % 2 == 0 ? "\u2588" : " ";
            g.setColor(new Color(255, 200, 100));
            g.drawString("> " + input.toString() + cursor, 10, h - 8);
        }
    }

    private int countMessageLines(Graphics2D g, int w) {
        FontMetrics fm = g.getFontMetrics();
        int count = 0;
        for (String msg : messages) {
            if (fm.stringWidth(msg) > w - 20)
                count += wrap(msg, fm, w - 20).size();
            else
                count++;
        }
        return count;
    }

    private List<String> buildMessageLines(Graphics2D g, int w) {
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        for (String msg : messages) {
            if (fm.stringWidth(msg) > w - 20) {
                lines.addAll(wrap(msg, fm, w - 20));
            } else {
                lines.add(msg);
            }
        }
        return lines;
    }

    private List<String> wrap(String text, FontMetrics fm, int maxW) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if (!line.isEmpty() && fm.stringWidth(line + " " + w) > maxW) {
                lines.add(line.toString());
                line = new StringBuilder(w);
            } else {
                if (!line.isEmpty()) line.append(" ");
                line.append(w);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }
}
