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
