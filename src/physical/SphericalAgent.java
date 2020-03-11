package physical;

import math.Vec3;
import processing.core.PApplet;
import tools.Vertex;
import tools.configurationspace.ConfigurationSpace;

import java.util.ArrayList;
import java.util.List;

public class SphericalAgent {
    final PApplet parent;
    final SphericalAgentDescription description;
    final ConfigurationSpace configurationSpace;
    final float speed;
    final Vec3 color;

    Vec3 center;
    List<Vertex> path = new ArrayList<>();
    int currentMilestone = 0;
    public boolean isPaused = false;

    public SphericalAgent(final PApplet parent, final SphericalAgentDescription description, final ConfigurationSpace configurationSpace, float speed, Vec3 color) {
        this.parent = parent;
        this.description = description;
        this.configurationSpace = configurationSpace;
        this.speed = speed;
        this.color = color;

        this.center = Vec3.of(description.startPosition);
    }

    public void update(float dt) {
        if (isPaused) {
            return;
        }
        if (currentMilestone < path.size() - 1) {
            // reached next milestone
            if (path.get(currentMilestone + 1).position.minus(center).norm() < 2) {
                currentMilestone++;
                return;
            }
            // move towards next milestone
            Vec3 velocityDir =
                    path.get(currentMilestone + 1).position
                            .minus(center)
                            .normalizeInPlace();
            center.plusInPlace(velocityDir.scale(speed * dt));
        }
    }

    public void draw() {
        // path
        parent.stroke(color.x, color.y, color.z);
        for (int i = 0; i < path.size() - 1; i++) {
            Vec3 v1 = path.get(i).position;
            Vec3 v2 = path.get(i + 1).position;
            parent.line(v1.x, v1.y, v1.z, v2.x, v2.y, v2.z);
        }
        parent.noStroke();
        // agent
        parent.pushMatrix();
        parent.fill(color.x, color.y, color.z);
        parent.translate(center.x, center.y, center.z);
        parent.sphere(description.radius);
        parent.popMatrix();
    }

    public void setPath(List<Vertex> path) {
        this.path = path;
        currentMilestone = 0;
        center.set(description.startPosition);
    }

    public void stepForward() {
        if (path.size() == 0) {
            return;
        }
        center.set(path.get(currentMilestone).position);
        if (currentMilestone < path.size() - 1) {
            currentMilestone++;
        }
    }

    public void stepBackward() {
        if (path.size() == 0) {
            return;
        }
        center.set(path.get(currentMilestone).position);
        if (currentMilestone > 0) {
            currentMilestone--;
        }
    }

}
