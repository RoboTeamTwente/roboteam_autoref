package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

public class AttackerDoubleTouchedBallValidator implements RuleValidator {
    private boolean triggered = false;


    /**
     * The validate method of this class determines whether an attacker has double touched the ball.
     *
     * @param game the game object being validated
     * @return a violation when an attacker has double touched the ball, else return null.
     */
    @Override
    public RuleViolation validate(Game game) {
        Touch kickIntoPlay = game.getKickIntoPlay();
        if (kickIntoPlay == null || !kickIntoPlay.equals(game.getLastFinishedTouch())) {
            return null;
        }

        Robot robot = game.getRobot(kickIntoPlay.getBy());
        Touch currentTouch = robot.getTouch();

        // Ball should move 0.05 meters before "in play", then another 0.05 meters before it's a violation.
        float distance = kickIntoPlay.isFinished() ? 0.05f : 0.10f;

        if (!triggered && currentTouch != null && game.getBall().getPosition().distance(kickIntoPlay.getStartLocation()) >= distance) {
            triggered = true;
            return new Violation(robot.getTeam().getColor(),robot.getIdentifier(), game.getKickIntoPlay().getStartLocation().xy());
        }

        return null;
    }

    @Override
    public boolean isActive(Game game) {
        return game.isBallInPlay();
    }

    @Override
    public void reset(Game game) {
        triggered = false;
    }

    /**
     * Violation record which is used to flag who did the violation and where.
     *
     * @param by       the robot that performed the violation.
     * @param location the location on the field where the violation was made.
     */
    record Violation(TeamColor byTeam, RobotIdentifier by, Vector2 location) implements RuleViolation {


        @Override
        public String toString() {
            return "Attacker double touched ball (by: " + by.teamColor() + " #" + by.id() + ", at " + location + ")";
        }


        /**
         * Function that formats the violation into a packet to send to the GameController.
         *
         * @return a GameEvent packet of type AttackerDoubleTouchedBall to be handled by the GameController.
         */
        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.ATTACKER_DOUBLE_TOUCHED_BALL)
                    .setAttackerDoubleTouchedBall(SslGcGameEvent.GameEvent.AttackerDoubleTouchedBall.newBuilder()
                            .setByTeam(by.teamColor() == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(by.id())
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY())))
                    .build();
        }
    }
}
