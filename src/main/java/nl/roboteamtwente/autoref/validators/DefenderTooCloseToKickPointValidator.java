package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;


public class DefenderTooCloseToKickPointValidator implements RuleValidator {

    private static final double GRACE_PERIOD = 2.0;

    private double lastViolation = Double.NEGATIVE_INFINITY;

    @Override
    public RuleViolation validate(Game game) {

        // Check if team color is not null
        if (game.getStateForTeam() == null) {
            return null;
        }

        // Height is not important in this case:
        // ball will always remain on the ground during these game states
        Vector2 ball = game.getBall().getPosition().xy();

        // Get defending teamColor from the game state
        TeamColor defendingTeamColor = game.getStateForTeam() == TeamColor.YELLOW ? TeamColor.BLUE : TeamColor.YELLOW;

        // Check if defender robots are too close to the ball (within 0.5m)
        for (Robot robot : game.getTeam(defendingTeamColor).getRobots()) {
            Vector2 robotPos = robot.getPosition().xy();

            // Calculate distance to ball
            float distanceToBall = ball.distance(robotPos);

            // If robot is within 0.5m of the ball, it is too close
            if (distanceToBall < 0.5) {
                if (lastViolation + GRACE_PERIOD < game.getTime()) {
                    lastViolation = game.getTime();
                    return new Violation(robot.getTeam().getColor(), robot.getId(), robotPos, distanceToBall);
                }
            }

        }

        return null;
    }

    @Override
    public boolean isActive(Game game) {
        // FIXME: Change later
        return game.isBallInPlay();
    }

    @Override
    public void reset(Game game) {
        lastViolation = Double.NEGATIVE_INFINITY;
    }

    record Violation(TeamColor byTeam, int byBot, Vector2 location, float distance) implements RuleViolation {
        @Override
        public String toString() {
            return "Defender too close to kick point (by: " + byTeam + ", by bot #" + byBot + ", at " + location + ", distance: " + distance + ")";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.DEFENDER_TOO_CLOSE_TO_KICK_POINT)
                    .setDefenderTooCloseToKickPoint(SslGcGameEvent.GameEvent.DefenderTooCloseToKickPoint.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(byBot)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY()))
                            .setDistance(distance))
                    .build();
        }
    }
}
