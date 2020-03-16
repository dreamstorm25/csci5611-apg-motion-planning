package demos;

import camera.QueasyCam;
import math.Vec3;
import robot.acting.SphericalAgent;
import robot.input.SphericalAgentDescription;
import fixed.SphericalObstacle;
import processing.core.PApplet;
import robot.sensing.PlainConfigurationSpace;
import robot.planning.rrt.RapidlyExploringRandomTree;

import java.util.ArrayList;
import java.util.List;

public class RRT2 extends PApplet {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 800;
    public static final int SIDE = 100;
    final Vec3 minCorner = Vec3.of(0, -SIDE, -SIDE);
    final Vec3 maxCorner = Vec3.of(0, SIDE, SIDE);

    final Vec3 startPosition = Vec3.of(0, SIDE * 0.9f, -SIDE * 0.9f);
    final Vec3 finishPosition = Vec3.of(0, -SIDE * 0.9f, SIDE * 0.9f);
    SphericalAgentDescription sphericalAgentDescription;
    SphericalAgent sphericalAgent;
    List<SphericalObstacle> sphericalObstacles = new ArrayList<>();
    PlainConfigurationSpace configurationSpace;
    RapidlyExploringRandomTree rrt;

    QueasyCam cam;

    static boolean DRAW_OBSTACLES = true;
    static boolean SMOOTH_PATH = false;

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
                    Vec3.of(0, SIDE * 0.9f - 20, SIDE * -0.9f + 20 * i - 10),
                    SIDE * 0.1f,
                    Vec3.of(1, 0, 1)
            ));
        }
        for (int i = 0; i < 9; i++) {
            sphericalObstacles.add(new SphericalObstacle(
                    this,
                    Vec3.of(0, SIDE * 0.9f - 60, SIDE * -0.5f + 20 * i - 10),
                    SIDE * 0.1f,
                    Vec3.of(1, 0, 1)
            ));
        }
        for (int i = 0; i < 9; i++) {
            sphericalObstacles.add(new SphericalObstacle(
                    this,
                    Vec3.of(0, SIDE * -0.9f + 20, SIDE * -0.5f + 20 * i - 10),
                    SIDE * 0.1f,
                    Vec3.of(1, 0, 1)
            ));
        }
        for (int i = 0; i < 9; i++) {
            sphericalObstacles.add(new SphericalObstacle(
                    this,
                    Vec3.of(0, SIDE * -0.9f + 60, SIDE * -0.9f + 20 * i - 10),
                    SIDE * 0.1f,
                    Vec3.of(1, 0, 1)
            ));
        }
        sphericalAgentDescription = new SphericalAgentDescription(
                startPosition,
                SIDE * (0.5f / 20)
        );
        configurationSpace = new PlainConfigurationSpace(this, sphericalAgentDescription, sphericalObstacles, minCorner, maxCorner);
        sphericalAgent = new SphericalAgent(this, sphericalAgentDescription, configurationSpace, 20f, Vec3.of(1));
        rrt = new RapidlyExploringRandomTree(this, startPosition, finishPosition);
        rrt.growTree(1000, configurationSpace);
    }

    public void draw() {
        if (keyPressed) {
            if (key == 'n') {
                rrt.growTree(10, configurationSpace);
            }
        }
        long start = millis();
        // update
        if (SMOOTH_PATH) {
            sphericalAgent.smoothUpdate(0.1f);
        } else {
            sphericalAgent.update(0.1f);
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
        // configuration space
        configurationSpace.draw();
        // rrt
        rrt.draw();
        // agent
        sphericalAgent.draw();
        long draw = millis();

        surface.setTitle("Processing - FPS: " + Math.round(frameRate) + " Update: " + (update - start) + "ms Draw " + (draw - update) + "ms" + " smooth-path: " + SMOOTH_PATH);
    }

    public void keyPressed() {
        if (key == 'h') {
            DRAW_OBSTACLES = !DRAW_OBSTACLES;
        }
        if (key == '1') {
            sphericalAgent.setPath(rrt.search());
        }
        if (key == 'p') {
            sphericalAgent.isPaused = !sphericalAgent.isPaused;
        }
        if (key == 'x') {
            SMOOTH_PATH = !SMOOTH_PATH;
        }
        if (keyCode == RIGHT) {
            sphericalAgent.stepForward();
        }
        if (keyCode == LEFT) {
            sphericalAgent.stepBackward();
        }
        if (key == 'j') {
            RapidlyExploringRandomTree.DRAW_TREE = !RapidlyExploringRandomTree.DRAW_TREE;
        }
    }

    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[]{"demos.RRT2"};
        if (passedArgs != null) {
            PApplet.main(concat(appletArgs, passedArgs));
        } else {
            PApplet.main(appletArgs);
        }
    }
}
