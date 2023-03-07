package nl.roboteamtwente.autoref;

import nl.roboteamtwente.autoref.model.*;
import nl.roboteamtwente.proto.StateOuterClass;
import nl.roboteamtwente.proto.WorldOuterClass;
import nl.roboteamtwente.proto.WorldRobotOuterClass;
import org.robocup.ssl.proto.SslVisionGeometry;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.List;

public class SSLAutoRef {
    private static final float BALL_TOUCHING_DISTANCE = 125.0f;

    private final Referee referee;

    private ZMQ.Socket worldSocket;
    private GameControllerConnection gcConnection;

    public SSLAutoRef() {
        this.referee = new Referee();
    }

    public void processWorldState(StateOuterClass.State statePacket) {
        Game game = new Game();
        if (referee.getGame() != null) {
            game.setPrevious(referee.getGame());
        }

        WorldOuterClass.World world = statePacket.getLastSeenWorld();

        game.setTime(world.getTime() / 1000000000.0);

        game.setState(switch (statePacket.getReferee().getCommand()) {
            case HALT -> GameState.HALT;
            case STOP -> GameState.STOP;
            //noinspection deprecation
            case NORMAL_START, FORCE_START, GOAL_YELLOW, GOAL_BLUE -> GameState.RUNNING;
            case PREPARE_KICKOFF_YELLOW, PREPARE_KICKOFF_BLUE -> GameState.PREPARE_KICKOFF;
            case PREPARE_PENALTY_YELLOW, PREPARE_PENALTY_BLUE -> GameState.PREPARE_PENALTY;
            case DIRECT_FREE_YELLOW, DIRECT_FREE_BLUE -> GameState.DIRECT_FREE;
            //noinspection deprecation
            case INDIRECT_FREE_YELLOW, INDIRECT_FREE_BLUE -> GameState.INDIRECT_FREE;
            case TIMEOUT_YELLOW, TIMEOUT_BLUE -> GameState.TIMEOUT;
            case BALL_PLACEMENT_YELLOW, BALL_PLACEMENT_BLUE -> GameState.BALL_PLACEMENT;
        });

        game.getBall().getPosition().setX(world.getBall().getPos().getX() * 1000.0f);
        game.getBall().getPosition().setY(world.getBall().getPos().getY() * 1000.0f);
        game.getBall().getPosition().setZ(world.getBall().getZ() * 1000.0f);
        game.getBall().getVelocity().setX(world.getBall().getVel().getX());
        game.getBall().getVelocity().setY(world.getBall().getVel().getY());
        game.getBall().getVelocity().setZ(world.getBall().getZVel());

        for (WorldRobotOuterClass.WorldRobot robot : world.getBlueList()) {
            processRobotState(game, TeamColor.BLUE, robot);
        }

        for (WorldRobotOuterClass.WorldRobot robot : world.getYellowList()) {
            processRobotState(game, TeamColor.YELLOW, robot);
        }

        game.getTeam(TeamColor.BLUE).setRobotRadius(statePacket.getBlueRobotParameters().getParameters().getRadius() * 1000.0f);
        game.getTeam(TeamColor.YELLOW).setRobotRadius(statePacket.getYellowRobotParameters().getParameters().getRadius() * 1000.0f);

        game.getTeam(TeamColor.BLUE).setGoalkeeperId(statePacket.getReferee().getBlue().getGoalkeeper());
        game.getTeam(TeamColor.YELLOW).setGoalkeeperId(statePacket.getReferee().getYellow().getGoalkeeper());

        game.getTeam(TeamColor.BLUE).setSide(statePacket.getReferee().getBlueTeamOnPositiveHalf() ? Side.RIGHT : Side.LEFT);
        game.getTeam(TeamColor.YELLOW).setSide(statePacket.getReferee().getBlueTeamOnPositiveHalf() ? Side.LEFT : Side.RIGHT);

        game.getField().setBoundaryWidth(statePacket.getField().getField().getBoundaryWidth());
        game.getField().getSize().setX(statePacket.getField().getField().getFieldLength());
        game.getField().getSize().setY(statePacket.getField().getField().getFieldWidth());
        game.getField().getPosition().setX(-statePacket.getField().getField().getFieldLength() / 2.0f);
        game.getField().getPosition().setY(-statePacket.getField().getField().getFieldWidth() / 2.0f);

        for (SslVisionGeometry.SSL_FieldLineSegment lineSegment : statePacket.getField().getField().getFieldLinesList()) {
            Vector2 p1 = new Vector2(lineSegment.getP1().getX(), lineSegment.getP1().getY());
            Vector2 p2 = new Vector2(lineSegment.getP2().getX(), lineSegment.getP2().getY());
            FieldLine fieldLine = new FieldLine(lineSegment.getName(), p1, p2, lineSegment.getThickness());

            game.getField().addLine(fieldLine);
        }

        deriveWorldState(game);
        referee.setGame(game);
    }

    private void deriveWorldState(Game game) {
        // FIXME: When ball goes out of play, reset state variables.

        Ball ball = game.getBall();
        Vector3 ballPosition = ball.getPosition();

        game.getBall().getRobotsTouching().clear();
        for (Robot robot : game.getRobots()) {
            // FIXME: is this a good way to detect if a robot is touching the ball?
            float distance = robot.getPosition().xy().distance(ballPosition.xy());
            if (distance <= BALL_TOUCHING_DISTANCE) {
                ball.getRobotsTouching().add(robot);

                robot.setJustTouchedBall(!robot.isTouchingBall());
                robot.setTouchingBall(true);
            } else {
                robot.setJustTouchedBall(false);
                robot.setTouchingBall(false);
            }

            if (robot.hasJustTouchedBall()) {
                ball.setLastTouchedBy(robot);
                ball.setLastTouchedAt(ball.getPosition());
            }
        }
    }

    private void processRobotState(Game game, TeamColor teamColor, WorldRobotOuterClass.WorldRobot worldRobot) {
        Robot robot = game.getTeam(teamColor).getRobotById(worldRobot.getId());
        if (robot == null) {
            robot = new Robot(worldRobot.getId());
            game.addRobot(robot);
            game.getTeam(teamColor).addRobot(robot);
        }

        robot.getPosition().setX(worldRobot.getPos().getX() * 1000.0f);
        robot.getPosition().setY(worldRobot.getPos().getY() * 1000.0f);
        robot.getVelocity().setX(worldRobot.getVel().getX() * 1000.0f);
        robot.getVelocity().setY(worldRobot.getVel().getY() * 1000.0f);
        robot.setAngle(worldRobot.getAngle());
    }

    public void start() {
        // FIXME: All still pretty temporary.
        try {
            gcConnection = new GameControllerConnection();
            gcConnection.connect("localhost", 10007);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try (ZContext context = new ZContext()) {
                this.worldSocket = context.createSocket(SocketType.SUB);

                this.worldSocket.subscribe("");
                this.worldSocket.connect("tcp://127.0.0.1:5558");

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        byte[] buffer = this.worldSocket.recv();
                        StateOuterClass.State packet = StateOuterClass.State.parseFrom(buffer);
                        processWorldState(packet);

                        List<RuleViolation> violations = referee.validate();
                        for (RuleViolation violation : violations) {
                            System.out.println("[" + referee.getGame().getTime() + "] " + violation);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "World Connection").start();
    }

    public Referee getReferee() {
        return referee;
    }
}
