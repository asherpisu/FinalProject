import org.lwjgl.*; // core LWJGL stuff
// import org.lwjgl.glfw.*; // unused right now
import org.lwjgl.opengl.*; // for OpenGL rendering
// import org.lwjgl.system.*; // not needed here

import java.nio.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*; // GLFW functions (window, input, etc.)
import static org.lwjgl.opengl.GL11.*; // legacy OpenGL stuff
import static org.lwjgl.opengl.GL15.*; // buffer stuff
import static org.lwjgl.opengl.GL20.*; // shader stuff
import static org.lwjgl.opengl.GL30.*; // VAOs and more modern OpenGL
import static org.lwjgl.system.MemoryUtil.*; // memory util helpers

public class SandSim {
    long window; // our app window

    final int WIDTH = 800;  // window width
    final int HEIGHT = 600; // window height
    final int CELL_SIZE = 1; // each "pixel" of sand is 2x2 screen pixels, I wanted  
                             // them to be somewhat discrete, so they're not just 1x1

    // how many simulation cells across and down
    final int GRID_WIDTH = WIDTH / CELL_SIZE;
    final int GRID_HEIGHT = HEIGHT / CELL_SIZE;

    // possible cell types
    //I chose not to use classes for these after a bunch of experimentation
    //cause it turns out its actually a lot easier to just make them a part 
    //of the main file w/OpenGl
    enum Type {EMPTY, SAND, WATER, WOOD}

    // the main simulation grid
    Type[][] grid = new Type[GRID_WIDTH][GRID_HEIGHT];

    // pixel color data for rendering
    int[] pixels = new int[GRID_WIDTH * GRID_HEIGHT];

    int textureID, shaderProgram, vao;

    boolean mouseDown = false; // whether mouse is pressed
    Type currentType = Type.SAND; // what we’re placing with mouse (sand is default)

    void run() {
        init(); // setup everything
        loop(); // enter the main loop
        glfwDestroyWindow(window); // clean up after window is closed
        glfwTerminate(); // shut down GLFW
    }

    void init() {
        // initialize GLFW, crash if it fails
        if (!glfwInit()) throw new RuntimeException("GLFW Init Failed");

        // requesting OpenGL 3.3 core profile (I need this for the shaders to work, 
        //because they are running on version #3.30 core profile)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // for Mac

        // create window
        window = glfwCreateWindow(WIDTH, HEIGHT, "SandSim", NULL, NULL);
        glfwMakeContextCurrent(window);
        GL.createCapabilities(); // enables OpenGL commands
        glfwSwapInterval(1); // enable vsync

        // initialize grid to all empty
        for (int x = 0; x < GRID_WIDTH; x++)
            for (int y = 0; y < GRID_HEIGHT; y++)
                grid[x][y] = Type.EMPTY;

        // mouse input
        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT)
                mouseDown = (action == GLFW_PRESS);
        });

        // key input to switch particle type
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_1) currentType = Type.SAND;
                if (key == GLFW_KEY_2) currentType = Type.WATER;
                if (key == GLFW_KEY_3) currentType = Type.WOOD;
            }
        });

        // load and compile shaders
        shaderProgram = createShader("src/vertex.glsl", "src/fragment.glsl");

        // setting up VAO and buffers for full-screen quad (quad is just referring to the setup of a basic square here
        // as the particle mesh, (in the vertices array, you can see that its just two triangles, cause OpenGL loves triangles))
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        float[] vertices = {
            -1f,  1f, 0f, 1f,
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
             1f,  1f, 1f, 1f
        };

        // This array tell OpenGL what order in which to render the vertices (it doesnt super matter in this case
        // because im not rendering some complex 3d mesh, only simple 2d)
        int[] indices = {0, 1, 2, 2, 3, 0};

        //VBO=vertex buffer object - basically the data being sent to the GPU
        //EBO=element buffere arrays - optimize the ordering of the vertices when
        //rendering to maximize performance
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // vertex position
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * 4, 0);
        glEnableVertexAttribArray(0);
        // texture coords
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * 4, 2 * 4);
        glEnableVertexAttribArray(1);

        // creating texture for the pixel grid
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, GRID_WIDTH, GRID_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    void loop() {
        // main update-render loop
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents(); // check input
            if (mouseDown) placeParticle(); // drop stuff where mouse is
            update(); // run simulation step
            render(); // draw it
            glfwSwapBuffers(window); // display result
        }
    }

    void placeParticle() {
    DoubleBuffer xb = BufferUtils.createDoubleBuffer(1);
    DoubleBuffer yb = BufferUtils.createDoubleBuffer(1);
    glfwGetCursorPos(window, xb, yb);
    int centerX = (int)(xb.get(0) / CELL_SIZE);
    int centerY = (int)(yb.get(0) / CELL_SIZE);

    Random rand = new Random();
    int brushRadius = 5; // Radius in grid cells
    int particlesPerFrame = 30; // Number of particles to place

    for (int i = 0; i < particlesPerFrame; i++) {
        // Use polar coordinates to bias distribution toward center
        double angle = rand.nextDouble() * 2 * Math.PI;
        double radius = brushRadius * Math.sqrt(rand.nextDouble()); // square root for center-bias

        int dx = (int)(Math.cos(angle) * radius);
        int dy = (int)(Math.sin(angle) * radius);

        int gx = centerX + dx;
        int gy = centerY + dy;

        if (gx >= 0 && gx < GRID_WIDTH && gy >= 0 && gy < GRID_HEIGHT) {
            grid[gx][gy] = currentType;
        }
    }
}

    //The state machine/cellular automata that is the bulk of the logic/interactions
    //behind the collisions of each particle
    void update() {
        for (int y = GRID_HEIGHT - 2; y >= 0; y--) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                Type t = grid[x][y];
                Random rand = new Random();

                // SAND LOGIC
                if (t == Type.SAND) {
                    // fall down
                    if (grid[x][y + 1] == Type.EMPTY || grid[x][y + 1] == Type.WATER) {
                        grid[x][y + 1] = Type.SAND;
                        grid[x][y] = (grid[x][y + 1] == Type.WATER) ? Type.WATER : Type.EMPTY;
                    }
                    // try to fall down-left
                    else if (x > 0 && (grid[x - 1][y + 1] == Type.EMPTY || grid[x - 1][y + 1] == Type.WATER)) {
                        grid[x - 1][y + 1] = Type.SAND;
                        grid[x][y] = (grid[x - 1][y + 1] == Type.WATER) ? Type.WATER : Type.EMPTY;
                    }
                    // fall down-right
                    else if (x < GRID_WIDTH - 1 && (grid[x + 1][y + 1] == Type.EMPTY || grid[x + 1][y + 1] == Type.WATER)) {
                        grid[x + 1][y + 1] = Type.SAND;
                        grid[x][y] = (grid[x + 1][y + 1] == Type.WATER) ? Type.WATER : Type.EMPTY;
                    }
                }

                // WATER LOGIC
                else if (t == Type.WATER) {
                    boolean moved = false;

                    // try falling straight down
                    if (grid[x][y + 1] == Type.EMPTY) {
                        grid[x][y + 1] = Type.WATER;
                        grid[x][y] = Type.EMPTY;
                        continue;
                    }

                    // diagonal falling, random left/right
                    if ((x > 0 && grid[x - 1][y + 1] == Type.EMPTY) && (x < GRID_WIDTH - 1 && grid[x + 1][y + 1] == Type.EMPTY)) {
                        if (rand.nextDouble() < 0.5) {
                            grid[x - 1][y + 1] = Type.WATER;
                        } else {
                            grid[x + 1][y + 1] = Type.WATER;
                        }
                        grid[x][y] = Type.EMPTY;
                        continue;
                    }

                    // diagonal fallback options
                    else if (x > 0 && grid[x - 1][y + 1] == Type.EMPTY) {
                        grid[x - 1][y + 1] = Type.WATER;
                        grid[x][y] = Type.EMPTY;
                        continue;
                    } else if (x < GRID_WIDTH - 1 && grid[x + 1][y + 1] == Type.EMPTY) {
                        grid[x + 1][y + 1] = Type.WATER;
                        grid[x][y] = Type.EMPTY;
                        continue;
                    }

                    // sideways spread (random if both)
                    else if ((x > 0 && grid[x - 1][y] == Type.EMPTY) && (x < GRID_WIDTH - 1 && grid[x + 1][y] == Type.EMPTY)) {
                        if (rand.nextDouble() < 0.5) {
                            grid[x - 1][y] = Type.WATER;
                        } else {
                            grid[x + 1][y] = Type.WATER;
                        }
                        grid[x][y] = Type.EMPTY;
                    } else if (x > 0 && grid[x - 1][y] == Type.EMPTY) {
                        grid[x - 1][y] = Type.WATER;
                        grid[x][y] = Type.EMPTY;
                    } else if (x < GRID_WIDTH - 1 && grid[x + 1][y] == Type.EMPTY) {
                        grid[x + 1][y] = Type.WATER;
                        grid[x][y] = Type.EMPTY;
                    }
                }

                // WOOD doesn't move, no logic needed, it'll just be placed where the mouse clicks and stay there
            }
        }

        // convert grid to pixel colors
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                int index = y * GRID_WIDTH + x;
                switch (grid[x][y]) {
                    case SAND:
                        pixels[index] = 0xFFC8A000; // orange-ish
                        break;
                    case WATER:
                        pixels[index] = 0xFF4060FF; // bluish
                        break;
                    case WOOD:
                        pixels[index] = 0xFF804000; // brown
                        break;
                    default:
                        pixels[index] = 0xFF000000; // black for empty
                        break;
                }
            }
        }

        // send pixel buffer to GPU
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
        glClear(GL_COLOR_BUFFER_BIT); // clear screen
        glUseProgram(shaderProgram);
        glBindVertexArray(vao);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0); // draw the quad
    }

    int createShader(String vertPath, String fragPath) {
        try {
            // read files
            String vertSrc = new String(Files.readAllBytes(Paths.get(vertPath)));
            String fragSrc = new String(Files.readAllBytes(Paths.get(fragPath)));

            // compile vertex shader
            int vert = glCreateShader(GL_VERTEX_SHADER);
            glShaderSource(vert, vertSrc);
            glCompileShader(vert);
            if (glGetShaderi(vert, GL_COMPILE_STATUS) == GL_FALSE)
                throw new RuntimeException("Vertex Shader Error: " + glGetShaderInfoLog(vert));

            // compile fragment shader
            int frag = glCreateShader(GL_FRAGMENT_SHADER);
            glShaderSource(frag, fragSrc);
            glCompileShader(frag);
            if (glGetShaderi(frag, GL_COMPILE_STATUS) == GL_FALSE)
                throw new RuntimeException("Fragment Shader Error: " + glGetShaderInfoLog(frag));

            // link both into a program
            int program = glCreateProgram();
            glAttachShader(program, vert);
            glAttachShader(program, frag);
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
                throw new RuntimeException("Shader Linking Error: " + glGetProgramInfoLog(program));

            // cleanup
            glDeleteShader(vert);
            glDeleteShader(frag);

            return program;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new SandSim().run(); // start the show
    }
}
