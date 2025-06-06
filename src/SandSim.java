import org.lwjgl.*;
// import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
// import org.lwjgl.system.*;

import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*; 

public class SandSim {
    long window;
    final int WIDTH = 800;
    final int HEIGHT = 600;
    final int CELL_SIZE = 2;
    final int GRID_WIDTH = WIDTH / CELL_SIZE;
    final int GRID_HEIGHT = HEIGHT / CELL_SIZE;

    enum Type {EMPTY, SAND, WATER, WOOD}

    Type[][] grid = new Type[GRID_WIDTH][GRID_HEIGHT];
    int[] pixels = new int[GRID_WIDTH * GRID_HEIGHT];

    int textureID, shaderProgram, vao;

    boolean mouseDown = false;
    Type currentType = Type.SAND;

    void run() {
        init();
        loop();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    void init() {
        if (!glfwInit()) throw new RuntimeException("GLFW Init Failed");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "SandSim", NULL, NULL);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                grid[x][y] = Type.EMPTY;

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT)
                mouseDown = (action == GLFW_PRESS);
        });

        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_1) currentType = Type.SAND;
                if (key == GLFW_KEY_2) currentType = Type.WATER;
                if (key == GLFW_KEY_3) currentType = Type.WOOD;
            }
        });

        shaderProgram = createShader("src/vertex.glsl", "src/fragment.glsl");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        float[] vertices = {
            -1f,  1f, 0f, 1f,
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
             1f,  1f, 1f, 1f
        };
        int[] indices = {0, 1, 2, 2, 3, 0};

        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 2 * 4);
        glEnableVertexAttribArray(1);

        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, GRID_WIDTH, GRID_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            if (mouseDown) placeParticle();
            update();
            render();
            glfwSwapBuffers(window);
        }
    }

    void placeParticle() {
        DoubleBuffer xb = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer yb = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, xb, yb);
        int gx = (int)(xb.get(0) / CELL_SIZE);
        int gy = (int)((yb.get(0)) / CELL_SIZE);
        if (gx >= 0 && gx < GRID_WIDTH && gy >= 0 && gy < GRID_HEIGHT) {
            grid[gx][gy] = currentType;
        }
    }

        void update() {
        for (int y = GRID_HEIGHT - 2; y >= 0; y--) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                Type t = grid[x][y];
                Random rand = new Random();
                if (t == Type.SAND) {
                    if (grid[x][y + 1] == Type.EMPTY || grid[x][y + 1] == Type.WATER) {
                        grid[x][y + 1] = Type.SAND;
                        grid[x][y] = (grid[x][y + 1] == Type.WATER) ? Type.WATER : Type.EMPTY;
                    } else if (x > 0 && (grid[x - 1][y + 1] == Type.EMPTY || grid[x - 1][y + 1] == Type.WATER)) {
                        grid[x - 1][y + 1] = Type.SAND;
                        grid[x][y] = (grid[x - 1][y + 1] == Type.WATER) ? Type.WATER : Type.EMPTY;
                    } else if (x < GRID_WIDTH - 1 && (grid[x + 1][y + 1] == Type.EMPTY || grid[x + 1][y + 1] == Type.WATER)) {
                        grid[x + 1][y + 1] = Type.SAND;
                        grid[x][y] = (grid[x + 1][y + 1] == Type.WATER) ? Type.WATER : Type.EMPTY;
                    }
                } else if (t == Type.WATER) {
                    if (grid[x][y + 1] == Type.EMPTY) {
                        grid[x][y + 1] = Type.WATER;
                        grid[x][y] = Type.EMPTY;
                    } 
                    else {
                        boolean moved = false;
                        if ((x > 0 && grid[x - 1][y + 1] == Type.EMPTY) && (x < GRID_WIDTH - 1 && grid[x + 1][y + 1] == Type.EMPTY)) {
                            double randNum = rand.nextDouble();
                            if (randNum<0.5) {
                                grid[x - 1][y + 1] = Type.WATER;
                                grid[x][y] = Type.EMPTY;
                                moved = true;
                            }
                            else {
                                grid[x + 1][y + 1] = Type.WATER;
                                grid[x][y] = Type.EMPTY;
                                moved = true;
                            }
                        } else if (x > 0 && grid[x - 1][y + 1] == Type.EMPTY) {
                            grid[x - 1][y + 1] = Type.WATER;
                            grid[x][y] = Type.EMPTY;
                            moved = true;
                        } else if (x < GRID_WIDTH - 1 && grid[x + 1][y + 1] == Type.EMPTY) {
                            grid[x + 1][y + 1] = Type.WATER;
                            grid[x][y] = Type.EMPTY;
                            moved = true;
                        } 
                         else if ((x > 0 && grid[x - 1][y] == Type.EMPTY) && (x < GRID_WIDTH - 1 && grid[x + 1][y] == Type.EMPTY)) {
                            double randNum = rand.nextDouble();
                            if (randNum<0.5) {
                                grid[x - 1][y] = Type.WATER;
                                grid[x][y] = Type.EMPTY;
                                moved = true;
                            }
                            else {
                                grid[x + 1][y] = Type.WATER;
                                grid[x][y] = Type.EMPTY;
                                moved = true;
                            }
                        } 
                        else if (x > 0 && grid[x - 1][y] == Type.EMPTY) {
                            grid[x - 1][y] = Type.WATER;
                            grid[x][y] = Type.EMPTY;
                            moved = true;
                        } else if (x < GRID_WIDTH - 1 && grid[x + 1][y] == Type.EMPTY) {
                            grid[x + 1][y] = Type.WATER;
                            grid[x][y] = Type.EMPTY;
                            moved = true;
                        }
                        if (moved) continue;
                    }
                }
                
                // Wood does not move
            }
        }

        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                int index = y * GRID_WIDTH + x;
                switch (grid[x][y]) {
                    case SAND:
                        pixels[index] = 0xFFC8A000;
                        break;
                    case WATER:
                        pixels[index] = 0xFF4060FF;
                        break;
                    case WOOD:
                        pixels[index] = 0xFF804000;
                        break;
                    default:
                        pixels[index] = 0xFF000000;
                        break;
                }
            }
        }

        ByteBuffer buffer = BufferUtils.createByteBuffer(GRID_WIDTH * GRID_HEIGHT * 4);
        for (int p : pixels) {
            buffer.put((byte)((p >> 16) & 0xFF));
            buffer.put((byte)((p >> 8) & 0xFF));
            buffer.put((byte)(p & 0xFF));
            buffer.put((byte)((p >> 24) & 0xFF));
        }
        buffer.flip();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, GRID_WIDTH, GRID_HEIGHT, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    }

    void render() {
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(shaderProgram);
        glBindVertexArray(vao);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }

    int createShader(String vertPath, String fragPath) {
        try {
            String vertSrc = new String(Files.readAllBytes(Paths.get(vertPath)));
            String fragSrc = new String(Files.readAllBytes(Paths.get(fragPath)));
            int vert = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vert, vertSrc);
            glCompileShader(vert);
            if (glGetShaderi(vert, GL_COMPILE_STATUS) == GL_FALSE)
                throw new RuntimeException("Vertex Shader Error: " + glGetShaderInfoLog(vert));

            int frag = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(frag, fragSrc);
            glCompileShader(frag);
            if (glGetShaderi(frag, GL_COMPILE_STATUS) == GL_FALSE)
                throw new RuntimeException("Fragment Shader Error: " + glGetShaderInfoLog(frag));

            int program = glCreateProgram();
            glAttachShader(program, vert);
            glAttachShader(program, frag);
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
                throw new RuntimeException("Shader Linking Error: " + glGetProgramInfoLog(program));

            glDeleteShader(vert);
            glDeleteShader(frag);
            return program;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new SandSim().run();
    }
}