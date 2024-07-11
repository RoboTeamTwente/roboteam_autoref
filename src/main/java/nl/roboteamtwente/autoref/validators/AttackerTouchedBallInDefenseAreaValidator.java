package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

import java.util.HashMap;
import java.util.Map;

public class AttackerTouchedBallInDefenseAreaValidator implements RuleValidator {

    /**
     * The grace period allowed for the robot to be in the defense area.
     */
    private static final double GRACE_PERIOD = 2.0;

    /**
     * Violations map to determine who did the violation and when.
     */
    private final Map<RobotIdentifier, Double> lastViolations = new HashMap<>();

    /**
     * The validate method of this class determines whether an attacker has touched the ball
     * in the defense area. The position of the robot is checked to see if they are inside
     * the defense area of the oppositions team and if the robot has came into contact
     * with the ball while being in the defense area.
     *
     * @param game the game object being validated
     * @return a violation when an attacker has touched the ball in the opponents defense area,
     * else return null.
     */
    @Override
    public RuleViolation validate(Game game) {
        for (Robot robot : game.getBall().getRobotsTouching()) {
            if (!game.getField().isPartiallyInDefenseArea(robot.getTeam().getSide().getOpposite(), robot.getPosition().xy()) &&
            !game.getField().isInDefenseArea(robot.getTeam().getSide().getOpposite(), game.getBall().getPosition().xy())) {
                continue;
            }

            if (!lastViolations.containsKey(robot.getIdentifier()) || lastViolations.get(robot.getIdentifier()) + GRACE_PERIOD < game.getTime()) {
                lastViolations.put(robot.getIdentifier(), game.getTime());
                return new Violation(robot.getTeam().getColor(),robot.getIdentifier(), robot.getPosition().xy());
            }
        }

        return null;
    }

    @Override
    public boolean isActive(Game game) {
        return game.isBallInPlay();
    }

    @Override
    public void reset(Game game) {
        lastViolations.clear();
    }

    /**
     * Violation record which is used to flag who did the violation and where.
     *
     * @param robot    the robot that performed the violation.
     * @param location the location on the field where the violation was made.
     */
    record Violation(TeamColor byTeam, RobotIdentifier robot, Vector2 location) implements RuleViolation {
        @Override
        public String toString() {
            return "Attacker touched ball in defense area (by: " + robot.teamColor() + " #" + robot.id() + ", at " + location + ")";
        }


        /**
         * Function that formats the violation into a packet to send to the GameController.
         *
         * @return a GameEvent packet of type AttackerTouchedBallInDefenseArea to be handled by the GameController.
         */
        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.ATTACKER_TOUCHED_BALL_IN_DEFENSE_AREA)
                    .setAttackerTouchedBallInDefenseArea(SslGcGameEvent.GameEvent.AttackerTouchedBallInDefenseArea.newBuilder()
                            .setByTeam(robot.teamColor() == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(robot.id())
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY())))
                    .build();
        }
    }
}
