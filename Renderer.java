import org.lwjgl.BufferUtils;
import java.nio.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

public class Renderer {
    private int computeProg, displayProg, vao, vbo, ebo;
    private int outputTex, overlayTex;
    private int uPos, uDir, uRight, uUp, uFov, uSize, uMaxDist;
    private int winW, winH;
    private int renderW = 960, renderH = 600;

    public void setWindowSize(int w, int h) { winW = w; winH = h; }

    public void resizeOverlay(int w, int h) {
        winW = w; winH = h;
        if (overlayTex != 0) glDeleteTextures(overlayTex);
        overlayTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, overlayTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, winW, winH, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        ByteBuffer initBuf = BufferUtils.createByteBuffer(winW * winH * 4);
        for (int i = 0; i < winW * winH * 4; i++) initBuf.put((byte)0);
        initBuf.flip();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, winW, winH, GL_RGBA, GL_UNSIGNED_BYTE, initBuf);
    }
    public int getWinW() { return winW; }
    public int getWinH() { return winH; }

    public int[] craftBtnBounds;

    public void setupShaders() {
        String computeSrc =
        "#version 430 core\n" +
        "layout(local_size_x = 16, local_size_y = 16) in;\n" +
        "layout(rgba8, binding = 0) uniform image2D outImg;\n" +
        "layout(binding = 1) uniform usampler3D voxTex;\n" +
        "layout(binding = 2) uniform sampler2DArray uBlockTex;\n" +
        "layout(binding = 3) uniform usampler3D shapeTex;\n" +
        "uniform vec3 camPos;\n" +
        "uniform vec3 camDir;\n" +
        "uniform vec3 camRight;\n" +
        "uniform vec3 camUp;\n" +
        "uniform float fovTan;\n" +
        "uniform ivec3 worldSize;\n" +
        "uniform float maxDist;\n" +
        "void main() {\n" +
        "  ivec2 p = ivec2(gl_GlobalInvocationID.xy);\n" +
        "  ivec2 sz = imageSize(outImg);\n" +
        "  if (p.x >= sz.x || p.y >= sz.y) return;\n" +
        "  vec2 ndc = (vec2(p) + 0.5) / vec2(sz) * 2.0 - 1.0;\n" +
        "  float aspect = float(sz.x) / float(sz.y);\n" +
        "  vec3 rd = normalize(camDir + camRight * ndc.x * fovTan * aspect + camUp * ndc.y * fovTan);\n" +
        "  vec3 ro = camPos;\n" +
        "  ivec3 mp = ivec3(floor(ro));\n" +
        "  vec3 stp = sign(rd);\n" +
        "  vec3 td = 1.0 / abs(rd);\n" +
        "  vec3 tm = vec3(\n" +
        "    rd.x > 0.0 ? (float(mp.x) + 1.0 - ro.x) / rd.x : (rd.x < 0.0 ? (ro.x - float(mp.x)) / (-rd.x) : 1.0e30),\n" +
        "    rd.y > 0.0 ? (float(mp.y) + 1.0 - ro.y) / rd.y : (rd.y < 0.0 ? (ro.y - float(mp.y)) / (-rd.y) : 1.0e30),\n" +
        "    rd.z > 0.0 ? (float(mp.z) + 1.0 - ro.z) / rd.z : (rd.z < 0.0 ? (ro.z - float(mp.z)) / (-rd.z) : 1.0e30)\n" +
        "  );\n" +
        "  int type = 0; float dist = 0; ivec3 norm = ivec3(0);\n" +
        "  for (int i = 0; i < 200; i++) {\n" +
        "    if (mp.x < 0 || mp.x >= worldSize.x || mp.y < 0 || mp.y >= worldSize.y || mp.z < 0 || mp.z >= worldSize.z) break;\n" +
        "    uint v = texelFetch(voxTex, mp, 0).r;\n" +
        "    if (v > 0u) {\n" +
        "      vec3 entryLocal = (ro + rd * dist) - vec3(mp);\n" +
        "      ivec3 sp = ivec3(clamp(entryLocal * 16.0, 0.0, 15.0));\n" +
        "      uint sv = texelFetch(shapeTex, ivec3(sp.x, sp.y, int(v - 1u) * 16 + sp.z), 0).r;\n" +
        "      if (sv > 0u) { type = int(v); break; }\n" +
        "      ivec3 sstp = ivec3(stp);\n" +
        "      vec3 s_td = td / 16.0;\n" +
        "      vec3 s_tm;\n" +
        "      if (rd.x > 0.0) s_tm.x = (mp.x + (sp.x + 1.0) / 16.0 - ro.x) / rd.x;\n" +
        "      else if (rd.x < 0.0) s_tm.x = (ro.x - (mp.x + sp.x / 16.0)) / (-rd.x);\n" +
        "      else s_tm.x = 1e30;\n" +
        "      if (rd.y > 0.0) s_tm.y = (mp.y + (sp.y + 1.0) / 16.0 - ro.y) / rd.y;\n" +
        "      else if (rd.y < 0.0) s_tm.y = (ro.y - (mp.y + sp.y / 16.0)) / (-rd.y);\n" +
        "      else s_tm.y = 1e30;\n" +
        "      if (rd.z > 0.0) s_tm.z = (mp.z + (sp.z + 1.0) / 16.0 - ro.z) / rd.z;\n" +
        "      else if (rd.z < 0.0) s_tm.z = (ro.z - (mp.z + sp.z / 16.0)) / (-rd.z);\n" +
        "      else s_tm.z = 1e30;\n" +
        "      bool found = false;\n" +
        "      for (int si = 0; si < 48; si++) {\n" +
        "        if (s_tm.x < s_tm.y) {\n" +
        "          if (s_tm.x < s_tm.z) { dist = s_tm.x; s_tm.x += s_td.x; sp.x += int(sstp.x); norm = ivec3(int(-sstp.x), 0, 0); }\n" +
        "          else { dist = s_tm.z; s_tm.z += s_td.z; sp.z += int(sstp.z); norm = ivec3(0, 0, int(-sstp.z)); }\n" +
        "        } else {\n" +
        "          if (s_tm.y < s_tm.z) { dist = s_tm.y; s_tm.y += s_td.y; sp.y += int(sstp.y); norm = ivec3(0, int(-sstp.y), 0); }\n" +
        "          else { dist = s_tm.z; s_tm.z += s_td.z; sp.z += int(sstp.z); norm = ivec3(0, 0, int(-sstp.z)); }\n" +
        "        }\n" +
        "        if (sp.x < 0 || sp.x >= 16 || sp.y < 0 || sp.y >= 16 || sp.z < 0 || sp.z >= 16) break;\n" +
        "        sv = texelFetch(shapeTex, ivec3(sp.x, sp.y, int(v - 1u) * 16 + sp.z), 0).r;\n" +
        "        if (sv > 0u) { found = true; break; }\n" +
        "      }\n" +
        "      if (found) { type = int(v); break; }\n" +
        "      vec3 exitPos = ro + rd * dist;\n" +
        "      vec3 nextRo = exitPos + rd * 0.001;\n" +
        "      mp = ivec3(floor(nextRo));\n" +
        "      tm = vec3(\n" +
        "        rd.x > 0.0 ? (float(mp.x) + 1.0 - nextRo.x) / rd.x : (rd.x < 0.0 ? (nextRo.x - float(mp.x)) / (-rd.x) : 1e30),\n" +
        "        rd.y > 0.0 ? (float(mp.y) + 1.0 - nextRo.y) / rd.y : (rd.y < 0.0 ? (nextRo.y - float(mp.y)) / (-rd.y) : 1e30),\n" +
        "        rd.z > 0.0 ? (float(mp.z) + 1.0 - nextRo.z) / rd.z : (rd.z < 0.0 ? (nextRo.z - float(mp.z)) / (-rd.z) : 1e30)\n" +
        "      );\n" +
        "      continue;\n" +
        "    }\n" +
        "    if (tm.x < tm.y) {\n" +
        "      if (tm.x < tm.z) { dist = tm.x; tm.x += td.x; mp.x += int(stp.x); norm = ivec3(int(-stp.x), 0, 0); }\n" +
        "      else { dist = tm.z; tm.z += td.z; mp.z += int(stp.z); norm = ivec3(0, 0, int(-stp.z)); }\n" +
        "    } else {\n" +
        "      if (tm.y < tm.z) { dist = tm.y; tm.y += td.y; mp.y += int(stp.y); norm = ivec3(0, int(-stp.y), 0); }\n" +
        "      else { dist = tm.z; tm.z += td.z; mp.z += int(stp.z); norm = ivec3(0, 0, int(-stp.z)); }\n" +
        "    }\n" +
        "  }\n" +
        "  vec3 col = vec3(0.157, 0.314, 0.627);\n" +
        "  if (type != 0) {\n" +
        "    float light = norm.y > 0 ? 1.0 : (norm.y < 0 ? 0.35 : 0.6);\n" +
        "    float fog = min(dist / maxDist, 1.0); fog = fog * fog;\n" +
        "    light *= (1.0 - fog);\n" +
        "    vec3 f = fract((ro + rd * dist) - vec3(mp));\n" +
        "    vec2 uv;\n" +
        "    if (norm.x > 0) uv = vec2(1.0 - f.z, 1.0 - f.y);\n" +
        "    else if (norm.x < 0) uv = vec2(f.z, 1.0 - f.y);\n" +
        "    else if (norm.y > 0) uv = vec2(f.x, 1.0 - f.z);\n" +
        "    else if (norm.y < 0) uv = vec2(f.x, 1.0 - f.z);\n" +
        "    else if (norm.z > 0) uv = vec2(1.0 - f.x, 1.0 - f.y);\n" +
        "    else uv = vec2(f.x, 1.0 - f.y);\n" +
        "    vec3 texCol = texelFetch(uBlockTex, ivec3(uv * 16.0, type - 1), 0).rgb;\n" +
        "    col = texCol * light + vec3(0.7, 0.78, 0.9) * fog;\n" +
        "  }\n" +
        "  imageStore(outImg, p, vec4(col, 1.0));\n" +
        "}";

        int cs = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(cs, computeSrc);
        glCompileShader(cs);
        checkShader(cs, "compute");

        computeProg = glCreateProgram();
        glAttachShader(computeProg, cs);
        glLinkProgram(computeProg);
        checkProgram(computeProg, "compute");

        uPos = glGetUniformLocation(computeProg, "camPos");
        uDir = glGetUniformLocation(computeProg, "camDir");
        uRight = glGetUniformLocation(computeProg, "camRight");
        uUp = glGetUniformLocation(computeProg, "camUp");
        uFov = glGetUniformLocation(computeProg, "fovTan");
        uSize = glGetUniformLocation(computeProg, "worldSize");
        uMaxDist = glGetUniformLocation(computeProg, "maxDist");

        String vsSrc =
        "#version 330 core\n" +
        "layout(location = 0) in vec2 pos;\n" +
        "layout(location = 1) in vec2 tex;\n" +
        "out vec2 uv;\n" +
        "void main() { gl_Position = vec4(pos, 0, 1); uv = tex; }";

        String fsSrc =
        "#version 330 core\n" +
        "in vec2 uv;\n" +
        "out vec4 color;\n" +
        "uniform sampler2D screenTex;\n" +
        "void main() { color = texture(screenTex, uv); }";

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vsSrc);
        glCompileShader(vs);
        checkShader(vs, "vertex");

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fsSrc);
        glCompileShader(fs);
        checkShader(fs, "fragment");

        displayProg = glCreateProgram();
        glAttachShader(displayProg, vs);
        glAttachShader(displayProg, fs);
        glLinkProgram(displayProg);
        checkProgram(displayProg, "display");
    }

    private void checkShader(int shader, String name) {
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException(name + " shader compile error: " + glGetShaderInfoLog(shader));
    }

    private void checkProgram(int prog, String name) {
        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException(name + " program link error: " + glGetProgramInfoLog(prog));
    }

    public void setupBuffers(BlockRegistry reg, World world) {
        outputTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, outputTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, renderW, renderH, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        overlayTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, overlayTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, winW, winH, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        ByteBuffer initBuf = BufferUtils.createByteBuffer(winW * winH * 4);
        for (int i = 0; i < winW * winH * 4; i++) initBuf.put((byte)0);
        initBuf.flip();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, winW, winH, GL_RGBA, GL_UNSIGNED_BYTE, initBuf);

        reg.upload();
        reg.uploadShapes();

        world.tex3D = glGenTextures();
        glBindTexture(GL_TEXTURE_3D, world.tex3D);
        glTexImage3D(GL_TEXTURE_3D, 0, GL_R8UI, World.WX, World.WY, World.WZ, 0, GL_RED_INTEGER, GL_UNSIGNED_BYTE, world.buffer);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_3D, 0);

        float[] verts = {-1,-1,0,0, 1,-1,1,0, 1,1,1,1, -1,1,0,1};
        int[] idxs = {0,1,2, 2,3,0};
        ByteBuffer vb = BufferUtils.createByteBuffer(verts.length * 4);
        FloatBuffer fvb = vb.asFloatBuffer();
        fvb.put(verts); fvb.flip();

        ByteBuffer ib = BufferUtils.createByteBuffer(idxs.length * 4);
        IntBuffer iib = ib.asIntBuffer();
        iib.put(idxs); iib.flip();

        vao = glGenVertexArrays();
        glBindVertexArray(vao);
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, fvb, GL_STATIC_DRAW);
        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, iib, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
    }

    public void render(World world, Player player, Camera camera, EntityManager entities, ChatSystem chat, BlockRegistry reg, boolean selectorOpen, long seed, java.util.Map<Integer, float[]> otherPlayers, int selfId) {
        double fwdX = camera.forwardX(), fwdY = camera.forwardY(), fwdZ = camera.forwardZ();
        double rightX = camera.rightX(), rightZ = camera.rightZ();
        double upX = camera.upX(), upY = camera.upY(), upZ = camera.upZ();

        glUseProgram(computeProg);
        glUniform3f(uPos, (float)player.px, (float)player.py, (float)player.pz);
        glUniform3f(uDir, (float)fwdX, (float)fwdY, (float)fwdZ);
        glUniform3f(uRight, (float)rightX, 0, (float)rightZ);
        glUniform3f(uUp, (float)upX, (float)upY, (float)upZ);
        glUniform1f(uFov, (float)Math.tan(camera.fov / 2));
        glUniform3i(uSize, World.WX, World.WY, World.WZ);
        glUniform1f(uMaxDist, (float)player.getReach());

        glBindImageTexture(0, outputTex, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_3D, world.tex3D);
        glUniform1i(glGetUniformLocation(computeProg, "voxTex"), 1);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D_ARRAY, reg.texArray);
        glUniform1i(reg.uBlockTexLoc, 2);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_3D, reg.shapeTex);
        glUniform1i(glGetUniformLocation(computeProg, "shapeTex"), 3);

        glDispatchCompute((renderW + 15) / 16, (renderH + 15) / 16, 1);
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        glUseProgram(displayProg);
        glBindVertexArray(vao);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, outputTex);
        glUniform1i(glGetUniformLocation(displayProg, "screenTex"), 0);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

        updateOverlay(world, player, camera, entities, chat, reg, selectorOpen, seed, otherPlayers, selfId);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, overlayTex);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glDisable(GL_BLEND);

        glBindVertexArray(0);
        glUseProgram(0);
    }

    public void updateOverlay(World world, Player player, Camera camera, EntityManager entities, ChatSystem chat, BlockRegistry reg, boolean selectorOpen, long seed, java.util.Map<Integer, float[]> otherPlayers, int selfId) {
        BufferedImage bi = new BufferedImage(winW, winH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (chat.isActive() || chat.hasMessages())
            chat.render(g, winW, winH);
        int cx = winW / 2, cy = winH / 2;

        if (!selectorOpen) {
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRect(cx - 8, cy - 1, 17, 3);
            g.fillRect(cx - 1, cy - 8, 3, 17);
        }

        if (selectorOpen) {
            int n = reg.size();
            int slotSize = 36, gap = 6, totalW = n * (slotSize + gap);
            int startX = winW / 2 - totalW / 2;
            int barY = winH / 2 - slotSize / 2;
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, winW, winH);
            g.setColor(new Color(30, 30, 60, 220));
            g.fillRect(startX - 8, barY - 8, totalW + 16, slotSize + 56);
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(new Color(200, 200, 255));
            g.drawString("Select Block (click to choose, E/ESC to close)", cx - 140, barY - 14);
            for (int i = 1; i <= n; i++) {
                int sx = startX + (i - 1) * (slotSize + gap);
                BlockTexture tex = reg.textures.get(i - 1);
                BufferedImage timg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                timg.setRGB(0, 0, 16, 16, tex.pixels, 0, 16);
                g.drawImage(timg, sx, barY, slotSize, slotSize, null);
                if (i == player.selectedBlock) {
                    g.setColor(new Color(255, 255, 255, 220));
                    g.drawRect(sx - 2, barY - 2, slotSize + 3, slotSize + 3);
                    g.drawRect(sx - 3, barY - 3, slotSize + 5, slotSize + 5);
                }
                g.setFont(new Font("Monospaced", Font.PLAIN, 10));
                g.setColor(new Color(220, 220, 220));
                g.drawString(tex.name.substring(0, Math.min(4, tex.name.length())), sx + 2, barY + slotSize + 12);
            }
            int btnX = startX - 8, btnY = barY + slotSize + 30;
            int btnW = totalW + 16, btnH = 28;
            g.setColor(new Color(60, 60, 120, 220));
            g.fillRect(btnX, btnY, btnW, btnH);
            g.setColor(new Color(100, 100, 180));
            g.drawRect(btnX, btnY, btnW, btnH);
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(200, 200, 255));
            g.drawString("[ Crafting Table ]", cx - 55, btnY + 19);
            craftBtnBounds = new int[]{btnX, btnY, btnW, btnH};
        } else {
            craftBtnBounds = null;
        }

        {
            double fwdX = camera.forwardX(), fwdY = camera.forwardY(), fwdZ = camera.forwardZ();
            double rX = camera.rightX(), rZ = camera.rightZ();
            double uX = camera.upX(), uY = camera.upY(), uZ = camera.upZ();
            double aspect = (double) winW / winH;
            double fovTan = Math.tan(camera.fov / 2);
            for (float[] d : entities.itemDrops) {
                double dx = d[0] - player.px, dy = d[1] - player.py, dz = d[2] - player.pz;
                double fDot = dx * fwdX + dy * fwdY + dz * fwdZ;
                if (fDot <= 0) continue;
                double rDot = dx * rX + dy * 0 + dz * rZ;
                double uDot = dx * uX + dy * uY + dz * uZ;
                int sx = (int) (winW / 2 + (rDot / fDot) * (winW / 2) / (fovTan * aspect));
                int sy = (int) (winH / 2 - (uDot / fDot) * (winH / 2) / fovTan);
                if (sx >= 0 && sx < winW && sy >= 0 && sy < winH) {
                    int type = (int) d[3];
                    g.setColor(new Color(reg.color(type)));
                    g.fillRect(sx - 3, sy - 3, 7, 7);
                    g.setColor(new Color(255, 255, 255, 180));
                    g.drawRect(sx - 3, sy - 3, 6, 6);
                }
            }
            for (float[] n : entities.npcs) {
                double ncx = n[0], ncy = n[1] + n[6] * 0.5, ncz = n[2];
                double dx = ncx - player.px, dy = ncy - player.py, dz = ncz - player.pz;
                double fDot = dx * fwdX + dy * fwdY + dz * fwdZ;
                if (fDot <= 0) continue;
                double rDot = dx * rX + dy * 0 + dz * rZ;
                double uDot = dx * uX + dy * uY + dz * uZ;
                int sx = (int) (winW / 2 + (rDot / fDot) * (winW / 2) / (fovTan * aspect));
                int sy = (int) (winH / 2 - (uDot / fDot) * (winH / 2) / fovTan);
                if (sx >= 0 && sx < winW && sy >= 0 && sy < winH) {
                    double sc = 20.0 / fDot;
                    int hh = (int)(n[6] * 40 * sc);
                    int hw2 = (int)(n[5] * 40 * sc);
                    g.setColor(new Color(200, 80, 200, 200));
                    g.fillRect(sx - hw2, sy - hh / 2, hw2 * 2, hh);
                    g.setColor(new Color(255, 200, 255, 180));
                    g.drawRect(sx - hw2, sy - hh / 2, hw2 * 2, hh);
                    g.setColor(new Color(255, 255, 255, 200));
                    g.fillOval(sx - 3, sy - hh / 2 - 2, 6, 6);
                }
            }
            if (otherPlayers != null) {
                for (java.util.Map.Entry<Integer, float[]> e : otherPlayers.entrySet()) {
                    if (e.getKey() == selfId) continue;
                    float[] p = e.getValue();
                    double dx = p[0] - player.px, dy = p[1] + 1.0 - player.py, dz = p[2] - player.pz;
                    double fDot = dx * fwdX + dy * fwdY + dz * fwdZ;
                    if (fDot <= 0) continue;
                    double rDot = dx * rX + dy * 0 + dz * rZ;
                    double uDot = dx * uX + dy * uY + dz * uZ;
                    int sx = (int) (winW / 2 + (rDot / fDot) * (winW / 2) / (fovTan * aspect));
                    int sy = (int) (winH / 2 - (uDot / fDot) * (winH / 2) / fovTan);
                    if (sx >= 0 && sx < winW && sy >= 0 && sy < winH) {
                        double sc = 20.0 / fDot;
                        int hh = (int)(40 * sc);
                        int hw2 = (int)(15 * sc);
                        g.setColor(new Color(100, 200, 255, 200));
                        g.fillRect(sx - hw2, sy - hh / 2, hw2 * 2, hh);
                        g.setColor(new Color(200, 255, 255, 180));
                        g.drawRect(sx - hw2, sy - hh / 2, hw2 * 2, hh);
                        g.setColor(new Color(255, 255, 100, 220));
                        g.fillOval(sx - 3, sy - hh / 2 - 2, 6, 6);
                    }
                }
            }
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(255, 255, 255, 160));
        g.drawString(player.gameMode == 1 ? "SURVIVAL" : "CREATIVE", 10, 18);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(180, 180, 255, 180));
        g.drawString(String.format("XYZ: %.1f %.1f %.1f", player.px, player.py, player.pz), 10, 34);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(150, 150, 200, 140));
        g.drawString("Seed: " + seed, 10, 48);
        if (player.gameMode == 1) {
            int iy = 62;
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            for (int i = 1; i <= reg.size(); i++) {
                int cnt = player.inventory[i];
                if (cnt > 0) {
                    int col = reg.color(i);
                    g.setColor(new Color(col));
                    g.fillRect(10, iy, 10, 10);
                    g.setColor(new Color(col >> 16 & 0xFF, col >> 8 & 0xFF, col & 0xFF, 200));
                    g.drawString(reg.name(i) + ": " + cnt, 24, iy + 9);
                    if (i == player.selectedBlock) {
                        g.setColor(new Color(255, 255, 100, 180));
                        g.drawRect(9, iy - 1, 12, 12);
                    }
                    iy += 14;
                }
            }
        }
        g.dispose();

        int[] pixels = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
        ByteBuffer buf = BufferUtils.createByteBuffer(winW * winH * 4);
        for (int row = winH - 1; row >= 0; row--) {
            for (int col = 0; col < winW; col++) {
                int argb = pixels[row * winW + col];
                buf.put((byte) ((argb >> 16) & 0xFF));
                buf.put((byte) ((argb >> 8) & 0xFF));
                buf.put((byte) (argb & 0xFF));
                buf.put((byte) ((argb >> 24) & 0xFF));
            }
        }
        buf.flip();

        glBindTexture(GL_TEXTURE_2D, overlayTex);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, winW, winH, GL_RGBA, GL_UNSIGNED_BYTE, buf);
    }
}
