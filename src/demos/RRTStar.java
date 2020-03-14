package demos;

import camera.QueasyCam;
import math.Vec3;
import physical.SphericalAgent;
import physical.SphericalAgentDescription;
import physical.SphericalObstacle;
import processing.core.PApplet;
import tools.configurationspace.PlainConfigurationSpace;
import tools.optimalrrt.OptimalRapidlyExploringRandomTree;

import java.util.ArrayList;
import java.util.List;

public class RRTStar extends PApplet {
    public static final int WIDTH = 800;
    public static final int HEIGHT = 800;
    public static final int SIDE = 100;
    final Vec3 minCorner = Vec3.of(0, -SIDE, -SIDE);
    final Vec3 maxCorner = Vec3.of(0, SIDE, SIDE);

    final Vec3 startPosition = Vec3.of(0, SIDE * (7f / 10), SIDE * (-6f / 10));
    final Vec3 finishPosition = Vec3.of(0, SIDE * (-1f / 10), SIDE * (1f / 10));
    SphericalAgentDescription sphericalAgentDescription;
    SphericalAgent sphericalAgent;
    List<SphericalObstacle> sphericalObstacles = new ArrayList<>();
    PlainConfigurationSpace configurationSpace;
    OptimalRapidlyExploringRandomTree rrt;

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
        sphericalObstacles.add(new SphericalObstacle(
                this,
                Vec3.of(0, 0, 0),
                SIDE * (2f / 20),
                Vec3.of(1, 0, 0)
        ));
        sphericalAgentDescription = new SphericalAgentDescription(
                startPosition,
                SIDE * (0.5f / 20)
        );
        configurationSpace = new PlainConfigurationSpace(this, sphericalAgentDescription, sphericalObstacles, minCorner, maxCorner);
        sphericalAgent = new SphericalAgent(this, sphericalAgentDescription, configurationSpace, 20f, Vec3.of(1));
        rrt = new OptimalRapidlyExploringRandomTree(this, startPosition, finishPosition);
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
        // agent
        sphericalAgent.draw();
        // configuration space
        configurationSpace.draw();
        // rrt
        rrt.draw();
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
            OptimalRapidlyExploringRandomTree.DRAW_TREE = !OptimalRapidlyExploringRandomTree.DRAW_TREE;
        }
    }

    static public void main(String[] passedArgs) {
        String[] appletArgs = new String[]{"demos.RRTStar"};
        if (passedArgs != null) {
            PApplet.main(concat(appletArgs, passedArgs));
        } else {
            PApplet.main(appletArgs);
        }
    }
}