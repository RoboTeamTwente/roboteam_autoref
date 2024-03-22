package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

import java.util.HashMap;
import java.util.Map;

public class BotDribbledBallTooFarValidator implements RuleValidator {

    private static final double GRACE_PERIOD = 2.0;

    /**
     * Violations map to determine who did the violation and when.
     */
    private final Map<RobotIdentifier, Double> lastViolations = new HashMap<>();

    @Override
    public RuleViolation validate(Game game) {
        if (game.getFinishedTouches().isEmpty()) {
            return null;
        }

        for (Touch touch : game.getFinishedTouches()) {
            Vector2 startLocation = touch.getStartLocation().xy();
            Vector2 endLocation = touch.getEndLocation().xy();
            Robot robot = game.getRobot(touch.getBy());
            float dist = startLocation.distance(endLocation);

            if (dist <= 1) {
                continue;
            }

            if (!lastViolations.containsKey(robot.getIdentifier()) || lastViolations.get(robot.getIdentifier()) + GRACE_PERIOD < game.getTime()) {
                lastViolations.put(robot.getIdentifier(), game.getTime());
                return new Violation(robot.getTeam().getColor(), robot.getId(), startLocation, endLocation);
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


    record Violation(TeamColor byTeam, int byBot, Vector2 start, Vector2 end) implements RuleViolation {
        @Override
        public String toString() {
            return "Bot dribbled ball too far (by: " + byTeam + ", by bot #" + byBot + ", at start location: " + start + ", ended at: " + end + ")";
        }

        /**
         * Function that formats the violation into a packet to send to the GameController.
         *
         * @return a GameEvent packet of type BotDribbledBallTooFar to be handled by the GameController.
         */
        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.BOT_DRIBBLED_BALL_TOO_FAR)
                    .setBotDribbledBallTooFar(SslGcGameEvent.GameEvent.BotDribbledBallTooFar.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(byBot)
                            .setStart(SslGcGeometry.Vector2.newBuilder().setX(start.getX()).setY(start.getY()))
                            .setEnd(SslGcGeometry.Vector2.newBuilder().setX(end.getX()).setY(end.getY()))
                    )
                    .build();
        }
    }
}
