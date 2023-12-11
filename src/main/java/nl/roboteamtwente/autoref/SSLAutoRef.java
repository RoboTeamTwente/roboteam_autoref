package nl.roboteamtwente.autoref;

import nl.roboteamtwente.autoref.model.*;
import nl.roboteamtwente.proto.StateOuterClass;
import nl.roboteamtwente.proto.WorldOuterClass;
import nl.roboteamtwente.proto.WorldRobotOuterClass;
import org.robocup.ssl.proto.SslVisionGeometry;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SSLAutoRef {
    private static final float BALL_TOUCHING_DISTANCE = 0.025f;

    private final Referee referee;

    private Thread worldThread;
    private GameControllerConnection gcConnection;
    private Thread gcThread;

    private WorldConnection worldConnection;

    private Consumer<RuleViolation> onViolation;
    private boolean autoConnect = false;

    private int commands = 0;
    private int nextTouchId = 0;

    public SSLAutoRef() {
        this.referee = new Referee();
    }

    /**
     * Process packet AutoRef received from World
     *
     * @param statePacket packet AutoRef got from World
     */
    public void processWorldState(StateOuterClass.State statePacket) {
        //build game
        Game game = new Game();
        if (referee.getGame() != null) {
            referee.getGame().setPrevious(null);
            game.setPrevious(referee.getGame());
        }

        WorldOuterClass.World world = statePacket.getCommandExtrapolatedWorld();

        game.setTime(world.getTime() / 1_000_000_000.0);
        game.setForceStarted(game.getPrevious().isForceStarted());

        //derive World packet
        deriveRefereeMessage(game, statePacket);
        deriveBall(game, world);
        for (WorldRobotOuterClass.WorldRobot robot : world.getBlueList()) {
            deriveRobot(game, TeamColor.BLUE, robot);
        }

        for (WorldRobotOuterClass.WorldRobot robot : world.getYellowList()) {
            deriveRobot(game, TeamColor.YELLOW, robot);
        }
        deriveTeamData(game, statePacket);
        deriveField(game, statePacket);
        deriveTouch(game);

        gameStateChanges(game);

        referee.setGame(game);
    }

    /**
     * From the RefereeMessage, derive the command and the designated position
     *
     * @param game        game
     * @param statePacket packet AutoRef got from World
     */
    private void deriveRefereeMessage(Game game, StateOuterClass.State statePacket) {
        if (game.getState() == null || statePacket.getReferee().getCommandCounter() != commands) {
            game.setState(game.getPrevious().getState());

            commands = statePacket.getReferee().getCommandCounter();

            switch (statePacket.getReferee().getCommand()) {
                case HALT -> {
                    // Any state can lead to halt
                    game.setState(GameState.HALT);
                }
                case STOP -> {
                    // Stop command always stops the game
                    game.setState(GameState.STOP);
                }
                case BALL_PLACEMENT_BLUE, BALL_PLACEMENT_YELLOW -> {
                    // Ball placement is always triggered.
                    game.setState(GameState.BALL_PLACEMENT);
                }
                case FORCE_START -> {
                    // Force starts makes the game jump to start.
                    game.setState(GameState.RUN);
                }
                case NORMAL_START -> {
                    // Normal start starts the current stage of the game.
                    if (game.getPrevious().getState() == GameState.PREPARE_KICKOFF) {
                        game.setState(GameState.KICKOFF);
                    } else if (game.getPrevious().getState() == GameState.PREPARE_PENALTY) {
                        game.setState(GameState.PENALTY);
                    }
                }
                //noinspection deprecation
                case INDIRECT_FREE_YELLOW, INDIRECT_FREE_BLUE, DIRECT_FREE_YELLOW, DIRECT_FREE_BLUE -> {
                    // Free kick is always triggered.
                    game.setState(GameState.FREE_KICK);
                }
                case PREPARE_KICKOFF_YELLOW, PREPARE_KICKOFF_BLUE -> {
                    // Prepare kickoff is always triggered.
                    game.setState(GameState.PREPARE_KICKOFF);
                }
                case PREPARE_PENALTY_YELLOW, PREPARE_PENALTY_BLUE -> {
                    // Prepare penalty is always triggered.
                    game.setState(GameState.PREPARE_PENALTY);
                }
                case TIMEOUT_YELLOW, TIMEOUT_BLUE -> {
                    // Timeouts are always triggered.
                    game.setState(GameState.TIMEOUT);
                }
            }
        } else {
            game.setState(game.getPrevious().getState());
        }

        //check if game state needs to be set to running
        if (statePacket.getReferee().hasCurrentActionTimeRemaining()) {
            int timeRemaining = statePacket.getReferee().getCurrentActionTimeRemaining();
            if (timeRemaining < 0) {
                if (game.getState() == GameState.KICKOFF || game.getState() == GameState.FREE_KICK) {
                    game.setState(GameState.RUN);
                }

                if (game.getState() == GameState.PENALTY) {
                    game.setState(GameState.STOP);
                }
            }
        }

        game.setCommand(statePacket.getReferee().getCommand());
        game.setNextCommand(statePacket.getReferee().getNextCommand());

        //set stateForTeam
        game.setStateForTeam(switch (statePacket.getReferee().getCommand()) {
            //noinspection deprecation
            case GOAL_YELLOW, PREPARE_KICKOFF_YELLOW, PREPARE_PENALTY_YELLOW, INDIRECT_FREE_YELLOW, TIMEOUT_YELLOW, BALL_PLACEMENT_YELLOW, DIRECT_FREE_YELLOW ->
                    TeamColor.YELLOW;
            //noinspection deprecation
            case GOAL_BLUE, PREPARE_KICKOFF_BLUE, PREPARE_PENALTY_BLUE, INDIRECT_FREE_BLUE, TIMEOUT_BLUE, BALL_PLACEMENT_BLUE, DIRECT_FREE_BLUE ->
                    TeamColor.BLUE;
            default -> game.getPrevious().getStateForTeam();
        });

        //TODO divide by / 1000.0f if the scale is not correct
        game.getDesignatedPosition().setX(statePacket.getReferee().getDesignatedPositionOrBuilder().getX() / 1000.0f);
        game.getDesignatedPosition().setY(statePacket.getReferee().getDesignatedPositionOrBuilder().getY() / 1000.0f);

    }

    /**
     * Set all values for the ball
     *
     * @param game  game
     * @param world filtered data from World
     */
    private void deriveBall(Game game, WorldOuterClass.World world) {
        // Sometimes world reports the ball as being at 0,0
        if (world.getBall().getPos().getX() == 0.0f || world.getBall().getPos().getY() == 0.0f) {
            game.getBall().getPosition().setX(game.getPrevious().getBall().getPosition().getX());
            game.getBall().getPosition().setY(game.getPrevious().getBall().getPosition().getY());
            game.getBall().getPosition().setZ(game.getPrevious().getBall().getPosition().getZ());
        } else {
            game.getBall().getPosition().setX(world.getBall().getPos().getX());
            game.getBall().getPosition().setY(world.getBall().getPos().getY());
            game.getBall().getPosition().setZ(world.getBall().getZ());
        }

        game.getBall().getVelocity().setX(world.getBall().getVel().getX());
        game.getBall().getVelocity().setY(world.getBall().getVel().getY());
        game.getBall().getVelocity().setZ(world.getBall().getZVel());
        game.getBall().setVisible(world.getBall().getVisible());
    }

    /**
     * If robot does not exist in Game, add robot to Game.
     * Derive robot data
     *
     * @param game       game
     * @param teamColor  team color
     * @param worldRobot robot
     */
    private void deriveRobot(Game game, TeamColor teamColor, WorldRobotOuterClass.WorldRobot worldRobot) {
        Robot robot = game.getTeam(teamColor).getRobotById(worldRobot.getId());
        if (robot == null) {
            robot = new Robot(worldRobot.getId());
            game.addRobot(robot);
            game.getTeam(teamColor).addRobot(robot);
        }

        robot.getPosition().setX(worldRobot.getPos().getX());
        robot.getPosition().setY(worldRobot.getPos().getY());
        robot.getVelocity().setX(worldRobot.getVel().getX());
        robot.getVelocity().setY(worldRobot.getVel().getY());
        robot.setAngle(worldRobot.getAngle());
    }

    /**
     * Derive data for the Team class
     *
     * @param game        game
     * @param statePacket packet AutoRef got from World
     */
    private void deriveTeamData(Game game, StateOuterClass.State statePacket) {
        game.getTeam(TeamColor.BLUE).setRobotRadius(statePacket.getBlueRobotParameters().getParameters().getRadius());
        game.getTeam(TeamColor.YELLOW).setRobotRadius(statePacket.getYellowRobotParameters().getParameters().getRadius());
        game.getTeam(TeamColor.BLUE).setRobotHeight(statePacket.getBlueRobotParameters().getParameters().getHeight());
        game.getTeam(TeamColor.YELLOW).setRobotHeight(statePacket.getYellowRobotParameters().getParameters().getHeight());

        game.getTeam(TeamColor.BLUE).setGoalkeeperId(statePacket.getReferee().getBlue().getGoalkeeper());
        game.getTeam(TeamColor.YELLOW).setGoalkeeperId(statePacket.getReferee().getYellow().getGoalkeeper());

        game.getTeam(TeamColor.BLUE).setSide(statePacket.getReferee().getBlueTeamOnPositiveHalf() ? Side.RIGHT : Side.LEFT);
        game.getTeam(TeamColor.YELLOW).setSide(statePacket.getReferee().getBlueTeamOnPositiveHalf() ? Side.LEFT : Side.RIGHT);
    }

    /**
     * Derive all lines on the field
     *
     * @param game        game
     * @param statePacket packet AutoRef got from World
     */
    private void deriveField(Game game, StateOuterClass.State statePacket) {
        game.getField().setBoundaryWidth(statePacket.getField().getField().getBoundaryWidth() / 1000.0f);
        game.getField().getSize().setX(statePacket.getField().getField().getFieldLength() / 1000.0f);
        game.getField().getSize().setY(statePacket.getField().getField().getFieldWidth() / 1000.0f);
        game.getField().getPosition().setX(-statePacket.getField().getField().getFieldLength() / 2.0f / 1000.0f);
        game.getField().getPosition().setY(-statePacket.getField().getField().getFieldWidth() / 2.0f / 1000.0f);
        game.getField().getGoal().setWidth(statePacket.getField().getField().getGoalWidth() / 1000.0f);
        game.getField().getGoal().setDepth(statePacket.getField().getField().getGoalDepth() / 1000.0f);

        for (SslVisionGeometry.SSL_FieldLineSegment lineSegment : statePacket.getField().getField().getFieldLinesList()) {
            Vector2 p1 = new Vector2(lineSegment.getP1().getX() / 1000.0f, lineSegment.getP1().getY() / 1000.0f);
            Vector2 p2 = new Vector2(lineSegment.getP2().getX() / 1000.0f, lineSegment.getP2().getY() / 1000.0f);
            FieldLine fieldLine = new FieldLine(lineSegment.getName(), p1, p2, lineSegment.getThickness() / 1000.0f);

            game.getField().addLine(fieldLine);
        }
    }

    /**
     * Check if robots are touching the ball
     *
     * @param game game
     */
    private void deriveTouch(Game game) {
        // copy variables from previous game
        game.getBall().setLastTouchStarted(game.getPrevious().getBall().getLastTouchStarted());

        // restore all finished touches, in-progress touches will be evaluated next
        game.getTouches().addAll(game.getPrevious().getFinishedTouches());
        game.setKickType(game.getPrevious().getKickType());
        game.setKickIntoPlay(game.getPrevious().getKickIntoPlay());

        Ball ball = game.getBall();
        Vector3 ballPosition = ball.getPosition();

        for (Robot robot : game.getRobots()) {
            Robot oldRobot = game.getPrevious().getRobot(robot.getIdentifier());

            // copy over old values
            if (oldRobot != null) {
                robot.setTouch(oldRobot.getTouch());
                robot.setJustTouchedBall(oldRobot.hasJustTouchedBall());
            }

            Touch touch = robot.getTouch();

            float distance = robot.getPosition().xy().distance(ballPosition.xy());

            // detect if there's a touch
            if (distance <= robot.getTeam().getRobotRadius() + BALL_TOUCHING_DISTANCE && ball.getPosition().getZ() <= robot.getTeam().getRobotHeight() + BALL_TOUCHING_DISTANCE) {
                ball.getRobotsTouching().add(robot);

                // it just started touching ball, either when its the first frame or when
                // in the previous frame the robot was not touching the ball.
                robot.setJustTouchedBall(oldRobot == null || !oldRobot.isTouchingBall());
            } else {
                // robot is not touching ball
                robot.setJustTouchedBall(false);
                robot.setTouch(null);

                if (touch != null) {
                    // we update the touch to include the end position
                    touch = new Touch(touch.id(), touch.startLocation(), ballPosition, touch.startTime(), game.getTime(), touch.startVelocity(), ball.getVelocity(), robot.getIdentifier());


                    // if this touch is the kick into play, we update that too
                    if (Objects.equals(touch, game.getKickIntoPlay())) {
                        game.setKickIntoPlay(touch);
                    }
                }
            }

            if (robot.hasJustTouchedBall()) {
                // we create a new partial touch
                touch = new Touch(nextTouchId++, ballPosition, null, game.getTime(), null, ball.getVelocity(), null, robot.getIdentifier());
                ball.setLastTouchStarted(touch);
                robot.setTouch(touch);

                System.out.print("touch #" + touch.id() + " by " + robot.getIdentifier());

                // if this happened during kickoff or a free kick, this is the kick into play
                if (game.getState() == GameState.KICKOFF || game.getState() == GameState.FREE_KICK) {
                    game.setKickType(game.getState() == GameState.KICKOFF ? KickType.KICKOFF : KickType.FREE_KICK);
                    game.setKickIntoPlay(touch);

                    // we change the state to running
                    game.setState(GameState.RUN);

                    System.out.print(" (kick into play)");
                }

                System.out.println();
            }

            // to conclude, we add the touch to the game
            if (touch != null) {
                game.getTouches().add(touch);
            }
        }
    }

    /**
     * Check for any GameState changes and take.
     * If there is a change, store the time of the change (current time).
     * If state changed and previous was HALT or STOP, reset touches
     *
     * @param game game
     */
    private void gameStateChanges(Game game) {
        //set TimeLastGameStateChange
        if (game.getState() != game.getPrevious().getState()) {
            System.out.println("game state: " + game.getPrevious().getState() + " -> " + game.getState());
            game.setTimeLastGameStateChange(game.getTime());
        } else {
            game.setTimeLastGameStateChange(game.getPrevious().getTimeLastGameStateChange());
        }

        //reset touches if previous game state was HALT or STOP and game state changed
        if (game.getPrevious().getState() != game.getState() && EnumSet.of(GameState.STOP, GameState.HALT).contains(game.getPrevious().getState())) {
            System.out.println("reset");

            game.getBall().setLastTouchStarted(null);
            game.setKickType(null);
            game.setKickIntoPlay(null);
            game.getTouches().clear();

            for (Robot robot : game.getRobots()) {
                robot.setTouch(null);
            }
        }
    }


    /**
     * Setup connections with all other software
     *
     * @param portGameController port GameContoller
     * @param portWorld          port World
     */
    public void start(String ipWorld, String ipGameController, int portWorld, int portGameController) {
        //setup connection with GameControl
        gcConnection = new GameControllerConnection();
        gcConnection.setIp(ipGameController);
        gcConnection.setPort(portGameController);
        gcConnection.setAutoConnect(autoConnect);
        gcThread = new Thread(gcConnection);
        gcThread.start();

        //setup connection with World
        worldConnection = new WorldConnection(ipWorld, portWorld, this);
        worldThread = new Thread(worldConnection);
        worldThread.start();
    }

    /**
     * Process received packet and check for violations
     *
     * @param packet
     */
    public void checkViolations(StateOuterClass.State packet) {
        processWorldState(packet);
        //check for any violations
        List<RuleViolation> violations = getReferee().validate();
        for (RuleViolation violation : violations) {
            //violation to ui/AutoRefController.java
            if (onViolation != null) {
                onViolation.accept(violation);
            }

            if (isAutoConnect()) {
                gcConnection.addToQueue(violation.toPacket());
            }
        }
    }

    public void stop() {
        gcConnection.setAutoConnect(false);
        try {
            //make sure sleep is longer than any sleep in GameControllerConnection.java
            Thread.sleep(gcConnection.getReconnectSleep() + 3000);
        } catch (InterruptedException e) {
        }
        gcConnection.disconnect();
        gcThread.interrupt();
        worldConnection.close();
        worldThread.interrupt();
    }

    public void setOnViolation(Consumer<RuleViolation> onViolation) {
        this.onViolation = onViolation;
    }

    public void setAutoConnect(boolean autoConnect) {
        if (gcConnection != null) {
            gcConnection.setAutoConnect(autoConnect);
        }

        this.autoConnect = autoConnect;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public Referee getReferee() {
        return referee;
    }

    public boolean isWorldConnected() {
        // FIXME: There is no way to check a ZMQ socket if its connected.
        return true;
    }

    public boolean isGCConnected() {
        return gcConnection.isConnected();
    }
}
