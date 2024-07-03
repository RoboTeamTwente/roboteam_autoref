package nl.roboteamtwente.autoref.model;

import org.robocup.ssl.proto.SslGcRefereeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.Optional;


/**
 * This is the game object which controls different aspects of a RoboCup game.
 */
public class Game {

    /**
     * List of robot objects that are in the game and are part of the playing field.
     */
    private final List<Robot> robots;

    /**
     * The game keeps track of the ball object at all times
     */
    private final Ball ball;

    /**
     * The ball placement position when game state is PLACEMENT
     */
    private final Vector2 designatedPosition;

    /**
     * The team which is mentioned in GameState
     */
    private TeamColor stateForTeam;

    /**
     * The received next command from referee packet
     */
    private SslGcRefereeMessage.SSL_Referee.Command nextCommand;

    /**
     * The received command from referee packet
     */
    private SslGcRefereeMessage.SSL_Referee.Command command;

    /**
     * The game consists of 2 teams blue and yellow
     */
    private final Team blue;
    private final Team yellow;


    /**
     * The game consists of 2 teams playing on a field.
     */
    private final Field field;

    private double timeLastGameStateChange;

    private GameState state;
    private double time;

    private Game previous;

    private Touch kickIntoPlay;
    private KickType kickType;
    private final List<Touch> touches;
    private final Comparator<Touch> touchComparator = Comparator.comparing(Touch::getEndTime);

    private boolean forceStarted;

    public Game() {
        this.robots = new ArrayList<>();
        this.ball = new Ball();
        this.field = new Field();

        this.designatedPosition = new Vector2(0, 0);
        this.stateForTeam = null;

        this.blue = new Team(TeamColor.BLUE);
        this.yellow = new Team(TeamColor.YELLOW);

        this.state = GameState.HALT;
        this.time = 0.0;
        this.previous = this;

        this.touches = new ArrayList<>();

        this.forceStarted = false;
    }

    /**
     * @return the ball object of the game
     */
    public Ball getBall() {
        return ball;
    }

    /**
     * @param color is an object TeamColor which we want the Team for
     * @return the Team object (blue || yellow) based on the color given to the method.
     */
    public Team getTeam(TeamColor color) {
        if (color == TeamColor.BLUE) {
            return blue;
        } else {
            return yellow;
        }
    }

    /**
     * @return the ball placement position
     */
    public Vector2 getDesignatedPosition() {
        return designatedPosition;
    }

    public TeamColor getStateForTeam() {
        return this.stateForTeam;
    }

    public void setStateForTeam(TeamColor stateForTeam) {
        this.stateForTeam = stateForTeam;
    }

    /**
     * @return the list of robots playing the game.
     */
    public List<Robot> getRobots() {
        return robots;
    }

    /**
     * @param robot is added to the list of robots on the playing field.
     */
    public void addRobot(Robot robot) {
        this.robots.add(robot);
    }

    /**
     * Get the robot corresponding to the identifier.
     *
     * @param identifier the identifier to search.
     * @return the matching robot.
     */
    public Robot getRobot(RobotIdentifier identifier) {
        return this.robots.stream().filter((robot) -> robot.getIdentifier().equals(identifier)).findAny().orElse(null);
    }

    /**
     * @return a string value for the game objects with all robot objects, ball and the teams.
     */
    @Override
    public String toString() {
        return "Game{" +
                "robots=" + robots +
                ", ball=" + ball +
                ", blue=" + blue +
                ", yellow=" + yellow +
                '}';
    }

    public SslGcRefereeMessage.SSL_Referee.Command getCommand() {
        return this.command;
    }

    public SslGcRefereeMessage.SSL_Referee.Command getNextCommand() {
        return this.nextCommand;
    }

    public void setCommand(SslGcRefereeMessage.SSL_Referee.Command command) {
        this.command = command;
    }

    public void setNextCommand(SslGcRefereeMessage.SSL_Referee.Command command) {
        this.nextCommand = command;
    }

    /**
     * @return the field the game is played at.
     */
    public Field getField() {
        return field;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public void setPrevious(Game previous) {
        this.previous = previous;
    }

    public Game getPrevious() {
        return previous;
    }

    public List<Touch> getTouches() {
        return touches;
    }

    public List<Touch> getCurrentTouches() {
        return touches.stream().filter((it) -> !it.isFinished()).collect(Collectors.toList());
    }

    public List<Touch> getFinishedTouches() {
        return touches.stream().filter(Touch::isFinished).collect(Collectors.toList());
    }

    public Touch getLastStartedTouch() {
        return touches.isEmpty() ? null : touches.get(touches.size() - 1);
    }

    public Touch getLastFinishedTouch() {
        Optional<Touch> optionalTouch = touches.stream().filter(Touch::isFinished).max(touchComparator);
        return optionalTouch.isPresent() ? optionalTouch.get() : null;
    }

    public Touch getKickIntoPlay() {
        return kickIntoPlay;
    }

    public void setKickIntoPlay(Touch kickIntoPlay) {
        this.kickIntoPlay = kickIntoPlay;
    }

    public KickType getKickType() {
        return kickType;
    }

    public void setKickType(KickType kickType) {
        this.kickType = kickType;
    }

    public boolean isForceStarted() {
        return forceStarted;
    }

    public void setForceStarted(boolean forceStarted) {
        this.forceStarted = forceStarted;
    }

    public void setTimeLastGameStateChange(double timeLastGameStateChange) {
        this.timeLastGameStateChange = timeLastGameStateChange;
    }

    public double getTimeLastGameStateChange() {
        return timeLastGameStateChange;
    }

    public boolean isBallInPlay() {
        return getState() == GameState.RUN || (getState() == GameState.PENALTY && getKickIntoPlay() != null);
    }

}
