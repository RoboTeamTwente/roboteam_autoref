package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

import java.util.HashMap;
import java.util.Map;

public class BotKickedBallTooFastValidator implements RuleValidator {

    // Hashmap of previous violations
    private final Map<RobotIdentifier, Double> lastViolations = new HashMap<>();

    // Grace period in seconds
    private static final double GRACE_PERIOD = 2.0;

    /**
     * The validate method of this class determines whether the ball was kicked too fast.
     *
     * @param game the game object being validated
     * @return a violation for when the ball was kicked too fast by a bot.
     */
    @Override
    public RuleViolation validate(Game game) {
        // Ball speed in m/s
        Ball ball = game.getBall();
        float speed = ball.getVelocity().magnitude();

        // If speed in one frame is higher than 6.5 m/s, ball was kicked too fast by the bot.
        if (speed > 6.5) {
            RobotIdentifier robotID = game.getLastStartedTouch().getBy();
            TeamColor team = robotID.teamColor();
            Vector2 location = ball.getPosition().xy();

            // Only if this violation has not been sent in the last 2 seconds, raise it
            if (!lastViolations.containsKey(robotID) || lastViolations.get(robotID) + GRACE_PERIOD < game.getTime()) {
                lastViolations.put(robotID, game.getTime());
                return new Violation(team, robotID.id(), location, speed);
            }
        }
        return null;
    }

    // Rule should only be checked when the ball is in play
    @Override
    public boolean isActive(Game game) {
        return game.isBallInPlay();
    }

    @Override
    public void reset(Game game) {
        lastViolations.clear();
    }

    record Violation(TeamColor byTeam, int byBot, Vector2 location, float speed) implements RuleViolation {
        @Override
        public String toString() {
            return "Bot kicked ball too fast (by: " + byTeam + ", by bot #" + byBot + ", at " + location + ", ball speed: " + speed + ")";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.BOT_KICKED_BALL_TOO_FAST)
                    .setBotKickedBallTooFast(SslGcGameEvent.GameEvent.BotKickedBallTooFast.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(byBot)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY()))
                            .setInitialBallSpeed(speed))
                    .build();
        }
    }

}
