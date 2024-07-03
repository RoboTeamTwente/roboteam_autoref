package nl.roboteamtwente.autoref.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The ball class represents a ball object in a RoboCup game. It extends
 * the Entity class, which the common properties of a physical object in
 * this game.
 */
public class Ball extends Entity {

    /**
     * List or robots currently touching the ball
     */
    private final List<Robot> robotsTouching = new ArrayList<>();

    private Touch lastTouchStarted;

    private boolean visible;

    /**
     * The velocity calculated by comparing the previous and current position of the ball
     * mainly used for determining the angle between 2 positions
     */
    private Vector2 velocityByPosition;

    public Touch getLastTouchStarted() {
        return lastTouchStarted;
    }

    public void setLastTouchStarted(Touch lastTouchStarted) {
        this.lastTouchStarted = lastTouchStarted;
    }

    /**
     *
     * @return a list of robots which are currently touching the ball.
     */
    public List<Robot> getRobotsTouching() {
        return this.robotsTouching;
    }

    public boolean isVisible(){
        return this.visible;
    }

    public void setVisible(boolean vis){
        this.visible = vis;
    }

    /**
     * Calculate the velocity of the ball by comparing the current and old position of the ball
     * Multiplied by 80Hz (speed of the camera) to get to m/s
     * Less acurate magnitude than observer/world data
     */
    public void calculateVelocityByPosition(Vector2 oldPosition) {
        this.velocityByPosition = this.getPosition().xy().subtract(oldPosition);
        this.velocityByPosition.setX(this.velocityByPosition.getX()*80.0f);
        this.velocityByPosition.setY(this.velocityByPosition.getY()*80.0f);
    }

    public Vector2 getVelocityByPosition() {
        return this.velocityByPosition;
    }


    /**
     *
     * @return string value of the ball object.
     */
    @Override
    public String toString() {
        return "Ball{" +
                "position=" + position +
                ", velocity=" + velocity +
                '}';
    }
}
