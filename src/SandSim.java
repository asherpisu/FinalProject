import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.*;

public class SandSim {
    long window;
    final int WIDTH = 800;
    final int HEIGHT = 600;
    final int CELL_SIZE = 2;
    final int GRID_WIDTH = WIDTH / CELL_SIZE;
    final int GRID_HEIGHT = HEIGHT / CELL_SIZE;

    enum Type { EMPTY, SAND }

    Type[][] grid = new Type[GRID_WIDTH][GRID_HEIGHT];
    int[] pixels = new int[GRID_WIDTH * GRID_HEIGHT];

    int textureID, shaderProgram, vao;

	boolean mouseDown = false;

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
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Note: Mr. Scheffel, if you are trying to run on windows,
																// remove this line if there are any problems
	

        window = glfwCreateWindow(WIDTH, HEIGHT, "SandSim", NULL, NULL);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        // Initialize grid
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                grid[x][y] = Type.EMPTY;

        // Mouse input
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                DoubleBuffer xb = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yb = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xb, yb);
                int gx = (int)(xb.get(0) / CELL_SIZE);
                int gy = (int)((HEIGHT - yb.get(0)) / CELL_SIZE);
                if (gx >= 0 && gx < GRID_WIDTH && gy >= 0 && gy < GRID_HEIGHT)
                    grid[gx][gy] = Type.SAND;
            }
        });

        // Shaders
        shaderProgram = createShader("/Users/aashrithpisupati/Library/CloudStorage/OneDrive-LakeWashingtonSchoolDistrict/2024-2025 10th grade/AP CSA/FinalProjectWorkspace/FinalProject/src/vertex.glsl"
									,"/Users/aashrithpisupati/Library/CloudStorage/OneDrive-LakeWashingtonSchoolDistrict/2024-2025 10th grade/AP CSA/FinalProjectWorkspace/FinalProject/src/fragment.glsl");

        // VAO + Quad
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

        // Texture
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, GRID_WIDTH, GRID_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            update();
            render();
            glfwSwapBuffers(window);
        }
    }

    void update() {
        for (int y = GRID_HEIGHT - 2; y >= 0; y--) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (grid[x][y] == Type.SAND) {
                    if (grid[x][y + 1] == Type.EMPTY) {
                        grid[x][y + 1] = Type.SAND;
                        grid[x][y] = Type.EMPTY;
                    } else if (x > 0 && grid[x - 1][y + 1] == Type.EMPTY) {
                        grid[x - 1][y + 1] = Type.SAND;
                        grid[x][y] = Type.EMPTY;
                    } else if (x < GRID_WIDTH - 1 && grid[x + 1][y + 1] == Type.EMPTY) {
                        grid[x + 1][y + 1] = Type.SAND;
                        grid[x][y] = Type.EMPTY;
                    }
                }
            }
        }

        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (grid[x][y] == Type.SAND)
                    pixels[y * GRID_WIDTH + x] = 0xFFC8A000;
                else
                    pixels[y * GRID_WIDTH + x] = 0xFF000000;
            }
        }

        ByteBuffer buffer = BufferUtils.createByteBuffer(GRID_WIDTH * GRID_HEIGHT * 4);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
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
