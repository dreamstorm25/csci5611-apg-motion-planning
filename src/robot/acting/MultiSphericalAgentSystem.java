package robot.acting;

import fixed.SphericalObstacle;
import math.Vec3;
import processing.core.PApplet;
import processing.core.PShape;
import robot.input.SphericalAgentDescription;
import robot.planning.multiagentgraph.MultiAgentGraph;
import robot.sensing.ConfigurationSpace;

import java.util.ArrayList;
import java.util.List;

public class MultiSphericalAgentSystem {
    public static boolean COLOR_SPLIT = false;
    public static float INITIAL_AGENT_SPEED = 20f;
    public static float MAX_EDGE_LEN = 10f;
    public static int NUM_VERTEX_SAMPLES = 10000;
    public static float TTC_K = 10;
    public static float TTC_T0 = 1;
    public static float TTC_MAX_FORCE = 500f;
    public static float TTC_POWER = 2;

    final PApplet parent;
    final ConfigurationSpace configurationSpace;
    final MultiAgentGraph multiAgentGraph;
    public List<SphericalAgent> sphericalAgents = new ArrayList<>();

    public MultiSphericalAgentSystem(PApplet parent, List<SphericalAgentDescription> sphericalAgentDescriptions, ConfigurationSpace configurationSpace, Vec3 minCorner, Vec3 maxCorner) {
        this.parent = parent;
        // At least one spherical agent is required
        assert (sphericalAgentDescriptions.size() > 0);
        if (COLOR_SPLIT) {
            for (int i = 0; i < sphericalAgentDescriptions.size(); ++i) {
                SphericalAgentDescription description = sphericalAgentDescriptions.get(i);
                sphericalAgents.add(
                        new SphericalAgent(parent,
                                description,
                                configurationSpace,
                                minCorner, maxCorner,
                                INITIAL_AGENT_SPEED,
                                i < sphericalAgentDescriptions.size() / 2 ? Vec3.of(1, 1, 0) : Vec3.of(0, 1, 1))
                );
            }
        } else {
            for (SphericalAgentDescription sphericalAgentDescription : sphericalAgentDescriptions) {
                sphericalAgents.add(
                        new SphericalAgent(parent,
                                sphericalAgentDescription,
                                configurationSpace,
                                minCorner, maxCorner,
                                INITIAL_AGENT_SPEED,
                                Vec3.of(parent.random(1), parent.random(1), parent.random(1))
                        )
                );
            }
        }
        this.configurationSpace = configurationSpace;
        this.multiAgentGraph = new MultiAgentGraph(parent, sphericalAgentDescriptions);
        this.multiAgentGraph.generateVertices(sphericalAgents.get(0).samplePoints(NUM_VERTEX_SAMPLES), configurationSpace);
        this.multiAgentGraph.generateAdjacencies(MAX_EDGE_LEN, configurationSpace);
    }

    public void update(float dt) {
        for (SphericalAgent agent : sphericalAgents) {
            agent.update(dt);
        }
    }

    public void smoothUpdate(float dt) {
        for (SphericalAgent agent : sphericalAgents) {
            agent.smoothUpdate(dt);
        }
    }

    public void updateBoid(List<SphericalObstacle> obstacles, float dt) {
        for (SphericalAgent agent : sphericalAgents) {
            agent.boidUpdate(sphericalAgents, obstacles, dt);
        }
    }

    private Vec3 getTTCForceOnI(Vec3 xj, Vec3 xi, Vec3 vj, Vec3 vi, float rj, float ri) {
        Vec3 xji = xj.minus(xi);
        Vec3 vji = vj.minus(vi);
        float distance = xji.norm();
        float impactRadius = ri + rj + 8;
        if (distance < impactRadius) {
            // Avoid collision (which could be happening probably due to slow incoming agents)
            Vec3 separationForce = xji.normalize().scaleInPlace(20 * (impactRadius - distance));
            return separationForce.scaleInPlace(-1);
        }

        // Collision detection
        final float a = vji.dot(vji);
        if (a < 1e-6) {
            // Almost relatively stationary
            return Vec3.zero();
        }
        final float b = xji.dot(vji);
        final float c = xji.dot(xji) - (ri + rj) * (ri + rj);
        float desc = b * b - a * c;
        if (desc < 0) {
            // No collision
            return Vec3.zero();
        }
        double t1 = (-b - Math.sqrt(desc)) / a;
        double t2 = (-b + Math.sqrt(desc)) / a;
        if (t1 < 0 && t2 < 0) {
            // (-, -)
            // No collision
            return Vec3.zero();
        }
        double timeToCollision = -1;
        if ((t1 > 0 && t2 < 0) || (t2 > 0 && t1 < 0)) {
            // (+, -) (-, +) case
            // collision occurs
            timeToCollision = Math.max(t1, t2);
        } else {
            // (+, +) case
            // collision occurs
            timeToCollision = Math.min(t1, t2);
        }
        // Calculate ttcForceOnI
        double firstTerm = (TTC_K * Math.exp(-timeToCollision / TTC_T0)) / (Math.pow(timeToCollision, TTC_POWER) * a);
        double secondTerm = (TTC_POWER / timeToCollision + 1 / TTC_T0);
        Vec3 thirdTerm = xji.scale(a).minus(vji.scale(b)).scaleInPlace(1 / (float) Math.sqrt(desc));
        Vec3 fourthTerm = vji;
        Vec3 ttcForceOnI = thirdTerm.minus(fourthTerm).scaleInPlace((float) (-1 * firstTerm * secondTerm));
        return ttcForceOnI;
    }

    public void updateTTC(List<SphericalObstacle> sphericalObstacles, float dt) {
        // Get isolated velocities
        List<Vec3> isolatedVelocities = new ArrayList<>();
        for (SphericalAgent agent : sphericalAgents) {
            isolatedVelocities.add(agent.getIsolatedVelocity());
        }
        // Compute ttc forces
        List<Vec3> totalTTCForces = new ArrayList<>();
        for (int i = 0; i < sphericalAgents.size(); i++) {
            totalTTCForces.add(Vec3.of(0));
        }
        // Agent-agent interaction
        for (int i = 0; i < sphericalAgents.size() - 1; i++) {
            SphericalAgent agentI = sphericalAgents.get(i);
            Vec3 agentIVel = isolatedVelocities.get(i);
            for (int j = i + 1; j < sphericalAgents.size(); j++) {
                SphericalAgent agentJ = sphericalAgents.get(j);
                Vec3 agentJVel = isolatedVelocities.get(j);
                // Newtons 3rd law
                Vec3 agentI_Force = getTTCForceOnI(agentJ.center, agentI.center, agentJVel, agentIVel, agentJ.description.radius, agentI.description.radius);
                Vec3 agentJ_Force = agentI_Force.scale(-1);
                // Adding to existing ttc forces
                totalTTCForces.set(i, totalTTCForces.get(i).plusInPlace(agentI_Force));
                totalTTCForces.set(j, totalTTCForces.get(j).plusInPlace(agentJ_Force));
            }
        }

        // Agent obstacle interaction
        for (int i = 0; i < sphericalAgents.size(); i++) {
            SphericalAgent agentI = sphericalAgents.get(i);
            Vec3 agentIVel = isolatedVelocities.get(i);
            for (SphericalObstacle obstacleJ : sphericalObstacles) {
                Vec3 ttcForceOnI = getTTCForceOnI(obstacleJ.center, agentI.center, Vec3.of(0), agentIVel, obstacleJ.radius, agentI.description.radius);
                // Adding to existing ttc forces
                totalTTCForces.set(i, totalTTCForces.get(i).plusInPlace(ttcForceOnI));
            }
        }

        // Adding ttc force and prm guided force
        for (int i = 0; i < sphericalAgents.size(); i++) {
            SphericalAgent agent = sphericalAgents.get(i);
            Vec3 isolatedVelocity = isolatedVelocities.get(i);
            Vec3 ttcForce = totalTTCForces.get(i);
            if (ttcForce.norm() > TTC_MAX_FORCE) {
                PApplet.println(ttcForce.norm());
                ttcForce = ttcForce.normalizeInPlace().scaleInPlace(TTC_MAX_FORCE);
            }
            Vec3 ttcVelocity = ttcForce.scale(dt);
            Vec3 displacement = isolatedVelocity.plusInPlace(ttcVelocity).scale(dt);
            agent.ttcUpdate(displacement);
        }
    }

    public void draw() {
        // agents
        for (SphericalAgent agent : sphericalAgents) {
            agent.draw();
        }
        // graph
        multiAgentGraph.draw();
    }

    public void draw(List<PShape> agentWalkCycleShapes, float size) {
        // agents
        for (SphericalAgent agent : sphericalAgents) {
            agent.draw(agentWalkCycleShapes, size);
        }
        // graph
        multiAgentGraph.draw();
    }

    public void stepForward() {
        for (SphericalAgent agent : sphericalAgents) {
            agent.stepForward();
        }
    }

    public void stepBackward() {
        for (SphericalAgent agent : sphericalAgents) {
            agent.stepBackward();
        }
    }

    public void togglePause() {
        for (SphericalAgent agent : sphericalAgents) {
            agent.isPaused = !agent.isPaused;
        }
    }

    public void dfs() {
        for (int i = 0; i < sphericalAgents.size(); i++) {
            SphericalAgent agent = sphericalAgents.get(i);
            agent.setPath(multiAgentGraph.dfs(i));
        }
    }

    public void bfs() {
        for (int i = 0; i < sphericalAgents.size(); i++) {
            SphericalAgent agent = sphericalAgents.get(i);
            agent.setPath(multiAgentGraph.bfs(i));
        }
    }

    public void ucs() {
        for (int i = 0; i < sphericalAgents.size(); i++) {
            SphericalAgent agent = sphericalAgents.get(i);
            agent.setPath(multiAgentGraph.ucs(i));
        }
    }

    public void aStar() {
        for (int i = 0; i < sphericalAgents.size(); i++) {
            SphericalAgent agent = sphericalAgents.get(i);
            agent.setPath(multiAgentGraph.aStar(i));
        }
    }

    public void weightedAStar(float epsilon) {
        for (int i = 0; i < sphericalAgents.size(); i++) {
            SphericalAgent agent = sphericalAgents.get(i);
            agent.setPath(multiAgentGraph.weightedAStar(epsilon, i));
        }
    }

}
