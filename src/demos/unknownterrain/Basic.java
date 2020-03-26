package demos.unknownterrain;

import camera.QueasyCam;
import fixed.SphericalObstacle;
import math.Vec3;
import processing.core.PApplet;
import robot.acting.AnytimeSphericalAgent;
import robot.input.SphericalAgentDescription;
import robot.planning.anytimegraph.AnytimeGraph;
import robot.sensing.PlainConfigurationSpace;

import java.util.ArrayList;
import java.util.List;

public class Basic extends PApplet {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 800;
    public static final int SIDE = 100;
    final Vec3 minCorner = Vec3.of(0, -SIDE, -SIDE);
    final Vec3 maxCorner = Vec3.of(0, SIDE, SIDE);

    final Vec3 startPosition = Vec3.of(0, SIDE * 0.9f, SIDE * -0.9f);
    final Vec3 finishPosition = Vec3.of(0, SIDE * -0.9f, SIDE * 0.9f);
    SphericalAgentDescription sphericalAgentDescription;
    AnytimeSphericalAgent anytimeSphericalAgent;
    List<SphericalObstacle> sphericalObstacles = new ArrayList<>();
    PlainConfigurationSpace configurationSpace;

    QueasyCam cam;

    static boolean DRAW_OBSTACLES = true;
    static boolean SMOOTH_PATH = false;
    static String ALGORITHM = "";

    public void settings() {
        size(WIDTH, HEIGHT, P3D);
    }

    public void setup() {
        surface.setTitle("Processing");
        colorMode(RGB, 1.0f);
        rectMode(CENTER);
        noStroke();

        cam = new QueasyCam(this);
        for (int i = 0; i < 9; i++) {
            sphericalObstacles.add(new SphericalObstacle(
                    this,
                    Vec3.of(0, (i - 5) * SIDE * 0.2f, 0),
                    SIDE * 0.1f,
                    Vec3.of(1, 0, 1)
            ));
        }
        sphericalAgentDescription = new SphericalAgentDescription(
                startPosition,
                finishPosition,
                SIDE * (0.5f / 20)
        );
        configurationSpace = new PlainConfigurationSpace(this, sphericalAgentDescription, sphericalObstacles);
        reset();
    }

    private void reset() {
        anytimeSphericalAgent = new AnytimeSphericalAgent(
                this,
                sphericalAgentDescription,
                configurationSpace,
                minCorner, maxCorner,
                10f,
                Vec3.of(1),
                5000,
                5,
                AnytimeSphericalAgent.Algorithm.AStar);
        ALGORITHM = "A*";
        anytimeSphericalAgent.isPaused = true;
    }

    public void draw() {
        if (keyPressed) {
            if (keyCode == RIGHT) {
                anytimeSphericalAgent.stepForward();
            }
            if (keyCode == LEFT) {
                anytimeSphericalAgent.stepBackward();
            }
        }
        long start = millis();
        // update
        if (SMOOTH_PATH) {
            anytimeSphericalAgent.smoothUpdate(0.1f);
        } else {
            anytimeSphericalAgent.update(0.1f);
        }
        long update = millis();
        // draw
        background(0);
        // obstacles
        if (DRAW_OBSTACLES) {
            for (SphericalObstacle sphericalObstacle : sphericalObstacles) {
                sphericalObstacle.draw();
            }
        }
        // agent
        anytimeSphericalAgent.draw();
        // configuration space
        configurationSpace.draw();
        long draw = millis();

        surface.setTitle("Processing - FPS: " + Math.round(frameRate) + " Update: " + (update - start) + "ms Draw " + (draw - update) + "ms" + " search: " + ALGORITHM);
    }

    public void keyPressed() {
        if (key == 'x') {
            SMOOTH_PATH = !SMOOTH_PATH;
        }
        if (key == 'h') {
            DRAW_OBSTACLES = !DRAW_OBSTACLES;
        }
        if (key == 'k') {
            AnytimeGraph.DRAW_VERTICES = !AnytimeGraph.DRAW_VERTICES;
        }
        if (key == 'j') {
            AnytimeGraph.DRAW_EDGES = !AnytimeGraph.DRAW_EDGES;
        }
        if (key == 'p') {
            anytimeSphericalAgent.isPaused = !anytimeSphericalAgent.isPaused;
        }
        if (key == 'r') {
            reset();
        }
        if (key == '1') {
            anytimeSphericalAgent.algorithm = AnytimeSphericalAgent.Algorithm.DFS;
            ALGORITHM = "DFS";
        }
        if (key == '2') {
            anytimeSphericalAgent.algorithm = AnytimeSphericalAgent.Algorithm.BFS;
            ALGORITHM = "BFS";
        }
        if (key == '3') {
            anytimeSphericalAgent.algorithm = AnytimeSphericalAgent.Algorithm.UCS;
            ALGORITHM = "UCS";
        }
        if (key == '4') {
            anytimeSphericalAgent.algorithm = AnytimeSphericalAgent.Algorithm.AStar;
            ALGORITHM = "A*";
        }
        if (key == '5') {
            anytimeSphericalAgent.algorithm = AnytimeSphericalAgent.Algorithm.WeightedAStar;
            ALGORITHM = "weighted A*";
        }
    }

    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[]{"demos.unknownterrain.Basic"};
        if (passedArgs != null) {
            PApplet.main(concat(appletArgs, passedArgs));
        } else {
            PApplet.main(appletArgs);
        }
    }
}
