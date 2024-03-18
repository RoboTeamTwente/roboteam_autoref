package nl.roboteamtwente.autoref.model;

/**
 * This is the robot class which defines a robot object of a game.
 * It extends the Entity class as it is a physical object and has
 * the properties of an Entity.
 */
public class Robot extends Entity {
    /**
     * Each robot has a unique ID.
     */
    private final int id;

    /**
     * Maximum radius of the robot in meters
     */
    private float radius;

    /**
     * A robot has an angle that it is facing during any point of the game.
     */
    private float angle;

    /**
     * A robot is part of a team.
     */
    private Team team;

    private Touch touch;

    /**
     * This variable keeps track of whether a robot has touched the ball in the previous frame.
     */
    private boolean justTouchedBall = false;

    /**
     *
     * @param id is the ID that the robot is constructed with, it is unique for every team.
     */
    public Robot(int id) {
        this.id = id;
    }

    /**
     *
     * @return the robot ID.
     */
    public int getId() {
        return id;
    }

    /**
     *
     * @return the team of the robot.
     */
    public Team getTeam() {
        return team;
    }


    /**
     *
     * @param team is the team the robot is assigned.
     */
    public void setTeam(Team team) {
        this.team = team;
    }


    /**
     *
     * @return the angle the robot is facing.
     */
    public float getAngle() {
        return angle;
    }


    /**
     *
     * @param angle sets the angle of the robot.
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }

    /**
     *
     * @return a boolean of whether the robot object is a goalkeeper or not based on the ID.
     */
    public boolean isGoalkeeper() {
        return this.team.getGoalkeeperId() == this.id;
    }

    /**
     *
     * @return a boolean if the robot is touching the ball.
     */
    public boolean isTouchingBall() {
        return touch != null;
    }

    public Touch getTouch() {
        return touch;
    }

    public void setTouch(Touch touch) {
        this.touch = touch;
    }

    /**
     *
     * @return a boolean if the robot has just touched the ball.
     */
    public boolean hasJustTouchedBall() {
        return justTouchedBall;
    }

    /**
     *
     * @param justTouchedBall sets the current robot object to whether it has just touched the ball.
     */
    public void setJustTouchedBall(boolean justTouchedBall) {
        this.justTouchedBall = justTouchedBall;
    }

    public RobotIdentifier getIdentifier() {
        return new RobotIdentifier(team.getColor(), id);
    }

    public void setRadius(float r) {
        this.radius = r;
    }

    /**
     * 
     * @return radius of robot
     */
    public float getRadius() {
        return radius;
    }

    /**
     *
     * @return a string value of the robot object.
     */
    @Override
    public String toString() {
        return "Robot{" +
                "id=" + id +
                ", orientation=" + angle +
                ", position=" + position +
                ", velocity=" + velocity +
                '}';
    }
}
