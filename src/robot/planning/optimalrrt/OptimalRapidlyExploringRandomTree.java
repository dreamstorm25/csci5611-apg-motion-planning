package robot.planning.optimalrrt;

import math.Vec3;
import processing.core.PApplet;
import robot.sensing.ConfigurationSpace;

import java.util.*;

public class OptimalRapidlyExploringRandomTree {
    public static float GROWTH_LIMIT = 10f;
    public static float END_POINT_HINT_SIZE = 2f;
    public static float FINISH_SLACK_RADIUS = 5f;
    public static float NEIGHBOUR_RADIUS = 10f;
    public static boolean DRAW_TREE = true;

    final PApplet applet;
    final Vec3 startPosition;
    final Vec3 finishPosition;
    final Vertex root;

    public OptimalRapidlyExploringRandomTree(PApplet applet, Vec3 startPosition, Vec3 finishPosition) {
        this.applet = applet;
        this.startPosition = Vec3.of(startPosition);
        this.finishPosition = Vec3.of(finishPosition);
        this.root = Vertex.of(applet, startPosition, 0);
    }

    private Vertex getNearestVertexFrom(final Vec3 position) {
        Stack<Vertex> fringe = new Stack<>();

        fringe.add(root);
        float minDistance = position.minus(root.position).norm();
        Vertex nearestVertex = root;
        while (fringe.size() > 0) {
            Vertex node = fringe.pop();
            float distance = position.minus(node.position).norm();
            if (distance < minDistance) {
                minDistance = distance;
                nearestVertex = node;
            }
            fringe.addAll(node.getChildren());
        }
        return nearestVertex;
    }

    public void generateNextNode(ConfigurationSpace configurationSpace) {
        Vec3 newPosition = configurationSpace.samplePoint();

        // nearest vertex search
        Stack<Vertex> fringe = new Stack<>();
        List<Vertex> neighbours = new ArrayList<>();
        fringe.add(root);
        float minDistance = newPosition.minus(root.position).norm();
        Vertex nearestVertex = root;
        while (fringe.size() > 0) {
            Vertex node = fringe.pop();
            if (node.position.minus(newPosition).norm() < NEIGHBOUR_RADIUS) {
                neighbours.add(node);
            }
            float distance = newPosition.minus(node.position).norm();
            if (distance < minDistance) {
                minDistance = distance;
                nearestVertex = node;
            }
            fringe.addAll(node.getChildren());
        }
        // if no neighbours exist within given range at least we will have the nearest neighbour
        if (neighbours.size() == 0) {
            neighbours.add(nearestVertex);
        }
        // min cost vertex search
        Vertex minCostVertex = nearestVertex;
        float minCost = nearestVertex.costFromStart + nearestVertex.position.minus(newPosition).norm();
        for (Vertex neighbour: neighbours) {
            float cost = neighbour.costFromStart + neighbour.position.minus(newPosition).norm();
            if (cost < minCost) {
                minCostVertex = neighbour;
                minCost = cost;
            }
        }
        // growth limit
        Vec3 growth = newPosition.minus(minCostVertex.position);
        if (growth.norm() > GROWTH_LIMIT) {
            newPosition = minCostVertex.position.plus(growth.normalize().scale(GROWTH_LIMIT));
        }
        // collision detection
        if (configurationSpace.doesEdgeIntersectSomeObstacle(minCostVertex.position, newPosition)) {
            return;
        }
        // linking min cost vertex and new vertex
        float distanceFromStart = minCostVertex.costFromStart + minCostVertex.position.minus(newPosition).norm();
        Vertex newVertex = Vertex.of(applet, newPosition, distanceFromStart);
        minCostVertex.addChild(newVertex);
        // rewiring
        for (Vertex neighbour: neighbours) {
            float cost = newVertex.costFromStart + newVertex.position.minus(neighbour.position).norm();
            if (cost < neighbour.costFromStart) {
                neighbour.costFromStart = cost;
                neighbour.parent.removeChild(neighbour);
                newVertex.addChild(neighbour);
            }
        }
    }

    public void growTree(int numNodes, ConfigurationSpace configurationSpace) {
        for (int i = 0; i < numNodes; i++) {
            generateNextNode(configurationSpace);
        }
    }

    public void draw() {
        if (DRAW_TREE) {
            // tree
            Stack<Vertex> fringe = new Stack<>();
            fringe.add(root);

            while (fringe.size() > 0) {
                Vertex node = fringe.pop();
                node.draw();
                fringe.addAll(node.getChildren());
            }

            // finish slack sphere
            applet.pushMatrix();
            applet.stroke(0, 1, 0);
            applet.noFill();
            applet.translate(finishPosition.x, finishPosition.y, finishPosition.z);
            applet.sphere(FINISH_SLACK_RADIUS);
            applet.popMatrix();
        }
        // start
        applet.pushMatrix();
        applet.fill(0, 0, 1);
        applet.noStroke();
        applet.translate(startPosition.x, startPosition.y, startPosition.z);
        applet.box(END_POINT_HINT_SIZE);
        applet.popMatrix();
        // finish
        applet.pushMatrix();
        applet.fill(0, 1, 0);
        applet.noStroke();
        applet.translate(finishPosition.x, finishPosition.y, finishPosition.z);
        applet.box(END_POINT_HINT_SIZE);
        applet.popMatrix();
    }

    public List<Vec3> search() {
        Vertex nearestVertex = getNearestVertexFrom(finishPosition);
        if (finishPosition.minus(nearestVertex.position).norm() < FINISH_SLACK_RADIUS) {
            List<Vec3> path = new ArrayList<>();
            path.add(0, nearestVertex.position);
            Vertex node = nearestVertex.parent;
            while (node != null) {
                path.add(0, node.position);
                node = node.parent;
            }
            return path;
        }
        PApplet.println("Could not find path to finish position");
        return Collections.singletonList(startPosition);
    }
}
