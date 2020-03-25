package robot.acting;

import fixed.SphericalObstacle;
import math.Vec3;
import processing.core.PApplet;
import processing.core.PConstants;

import java.util.List;

public class Boid {
    final PApplet parent;
    Vec3 velocity ;
    Vec3 acceleration ;
    float radius ;
    Vec3 color;
    Vec3 minCorner;
    Vec3 maxCorner;
    public Vec3 center ;
    public float impactRadius ;
    List<SphericalObstacle> obstacles ;

    public Boid(PApplet parent, float radius, Vec3 minCorner, Vec3 maxCorner, Vec3 center, float impactRadius, List<SphericalObstacle> obstacles) {
        this.parent = parent;
        this.radius = radius;
        this.minCorner = minCorner;
        this.maxCorner = maxCorner;
        this.center = center;
        this.impactRadius = impactRadius;
        this.obstacles = obstacles;
        this.velocity = Vec3.of(0,parent.random(-50, 50),parent.random(-30, 70));
        this.acceleration = Vec3.zero();
    }

    public void update(List<Boid> flock, float dt, Vec3 goal){
        Vec3 path = goal.minus(this.center);
        Vec3 seperationforce = Vec3.zero() ;
        Vec3 centroid = Vec3.zero() ;
        Vec3 alignment = Vec3.zero() ;
        int neighbors = 0 ;
        for(Boid boid : flock){
            Vec3 force = this.center.minus(boid.center);
            float distance = force.norm() ;
            if( distance < this.impactRadius && distance > 0){
                neighbors += 1 ;
                force.normalizeInPlace() ;
                seperationforce.plusInPlace(force.scaleInPlace(2000f*(this.impactRadius-distance)));
                centroid.plusInPlace((this.center.plus(boid.center)).normalizeInPlace().scaleInPlace(10.5f));
                alignment.plusInPlace((boid.velocity.minus(this.velocity)).normalizeInPlace().scaleInPlace(15f));
            }
        }


        Vec3 finalForce = seperationforce.plus(centroid.plus(alignment)) ;
        this.velocity.plusInPlace(finalForce.scale(dt));
        this.center.plusInPlace(this.velocity.scale(dt));
        this.velocity = path.scaleInPlace(4);

        for(SphericalObstacle obstacle : obstacles){
            Vec3 distance = obstacle.center.minus(this.center);
            if(distance.norm() < obstacle.radius+this.radius){
                distance.normalizeInPlace();
                this.center = distance.scaleInPlace(-1*(obstacle.radius+this.radius));
                distance = obstacle.center.minus(this.center);
                distance.normalizeInPlace();
                this.velocity.minusInPlace(this.velocity.plus(distance.scaleInPlace(this.velocity.dot(distance))).scaleInPlace(1.5f));
            }
        }

//        if(center.y < minCorner.y || center.y > maxCorner.y){
//            velocity.y = -velocity.y ;
//        }
//        if(center.z < minCorner.z || center.z > maxCorner.z){
//            velocity.z = -velocity.z ;
//        }
    }

    public void draw(){
        parent.pushMatrix();
        parent.translate(center.x, center.y, center.z);
//        parent.stroke(200,0,0);
//        parent.noFill();
        parent.rotateY(PConstants.PI/2);
//        parent.circle(0,0,impactRadius);
        parent.noStroke();
        parent.fill(color.x, color.y, color.z);
        parent.ellipse(0,0 , radius, radius/2);
        parent.popMatrix();
    }
}