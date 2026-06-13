import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    public boolean[] keys = new boolean[512];
    public boolean mouseGrabbed;
    public boolean selectorOpen;
    private Game game;

    public void setupCallbacks(Game game, long window) {
        this.game = game;
        glfwSetKeyCallback(window, (w, key, sc, act, mods) -> {
            if (act == GLFW_RELEASE && key == GLFW_KEY_ESCAPE) {
                if (game.chat.isActive()) { game.chat.toggle(); return; }
                if (selectorOpen) { selectorOpen = false; return; }
                if (mouseGrabbed) { releaseMouse(window); return; }
                glfwSetWindowShouldClose(w, true);
                return;
            }
            if (act == GLFW_PRESS && key == GLFW_KEY_T) {
                if (game.chat.isActive()) {
                    game.chat.suppressNextChar = true;
                    game.chat.onChar('t');
                    return;
                } else {
                    game.chat.toggle();
                    if (mouseGrabbed) releaseMouse(window);
                    return;
                }
            }
            if (act == GLFW_PRESS && key == GLFW_KEY_E && !game.chat.isActive()) {
                selectorOpen = !selectorOpen;
                if (selectorOpen && mouseGrabbed) releaseMouse(window);
                return;
            }
            if (game.chat.isActive()) {
                if (act == GLFW_PRESS) {
                    if (key == GLFW_KEY_ENTER) game.chat.onEnter();
                    else if (key == GLFW_KEY_BACKSPACE) game.chat.onBackspace();
                    else if (key == GLFW_KEY_UP) game.chat.scrollUp();
                    else if (key == GLFW_KEY_DOWN) game.chat.scrollDown();
                    else if (key == GLFW_KEY_TAB) game.chat.onTab();
                }
                return;
            }
            if (key >= 0 && key < 512) keys[key] = act != GLFW_RELEASE;
        });
        glfwSetCharCallback(window, (w, cp) -> {
            if (game.chat.isActive()) game.chat.onChar((char)cp);
        });
        glfwSetCursorPosCallback(window, (w, xpos, ypos) -> {
            if (mouseGrabbed) {
                double dx = xpos - game.lastMouseX;
                double dy = ypos - game.lastMouseY;
                game.camera.rotate(dx * 0.003, dy * 0.003);
                glfwSetCursorPos(window, game.winW / 2.0, game.winH / 2.0);
                game.lastMouseX = game.winW / 2.0;
                game.lastMouseY = game.winH / 2.0;
            } else {
                game.lastMouseX = xpos;
                game.lastMouseY = ypos;
            }
        });
        glfwSetMouseButtonCallback(window, (w, button, act, mods) -> {
            if (act == GLFW_PRESS && !game.chat.isActive()) {
                if (selectorOpen) {
                    int n = game.reg.size();
                    int slotSize = 36, gap = 6, startX = game.winW / 2 - (n * (slotSize + gap)) / 2;
                    int barY = game.winH / 2 - slotSize / 2;
                    double mx = game.lastMouseX, my = game.lastMouseY;
                    for (int i = 1; i <= n; i++) {
                        int sx = startX + (i - 1) * (slotSize + gap);
                        if (mx >= sx && mx <= sx + slotSize && my >= barY && my <= barY + slotSize) {
                            game.player.selectedBlock = i;
                            selectorOpen = false;
                            return;
                        }
                    }
                    if (game.craftBtnBounds != null && mx >= game.craftBtnBounds[0] && mx <= game.craftBtnBounds[0] + game.craftBtnBounds[2] &&
                        my >= game.craftBtnBounds[1] && my <= game.craftBtnBounds[1] + game.craftBtnBounds[3]) {
                        selectorOpen = false;
                        game.openCraftingTable();
                        return;
                    }
                    return;
                }
                if (!mouseGrabbed && button == GLFW_MOUSE_BUTTON_LEFT) grabMouse(window);
                else if (mouseGrabbed) {
                    if (button == GLFW_MOUSE_BUTTON_LEFT) game.onLeftClick();
                    else if (button == GLFW_MOUSE_BUTTON_RIGHT) game.onRightClick();
                }
            }
        });
        glfwSetScrollCallback(window, (w, xoff, yoff) -> {
            int dir = yoff > 0 ? 1 : -1;
            if (game.chat.isActive()) {
                if (yoff > 0) game.chat.scrollUp();
                else game.chat.scrollDown();
                return;
            }
            game.player.selectedBlock += dir;
            int n = game.reg.size();
            if (game.player.selectedBlock < 1) game.player.selectedBlock = n;
            if (game.player.selectedBlock > n) game.player.selectedBlock = 1;
        });
    }

    public void grabMouse(long window) {
        if (!mouseGrabbed) {
            mouseGrabbed = true;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }
    }

    public void releaseMouse(long window) {
        if (mouseGrabbed) {
            mouseGrabbed = false;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }
}
