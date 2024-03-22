package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

public class BallLeftFieldTouchLineValidator implements RuleValidator {


    private static final double GRACE_PERIOD = 2.0;
    private double lastViolations = Double.NEGATIVE_INFINITY;

    /**
     * The validate method of this class determines whether any robot has caused the ball
     * to exit the upper or lower touch lines. The Y coordinate of the ball is compared against
     * the Y coordinates of the upper and/or lower lines to determine if the ball did indeed cross
     * the lines. The robot that last touched the ball is the one responsible for the exiting of
     * the ball and hence the violation.
     *
     * @param game the game object being validated
     * @return a violation when the ball leaves the touch line.
     */
    @Override
    public RuleViolation validate(Game game) {
        Vector3 ball = game.getBall().getPosition();
        FieldLine bottomTouchLine = game.getField().getLineByName("BottomTouchLine");
        FieldLine topTouchLine = game.getField().getLineByName("TopTouchLine");

        // Lines are sometimes not present for some reason with some log-file tests not related to this rule
        if (bottomTouchLine == null || topTouchLine == null) {
            return null;
        }

        if (ball.getY() > topTouchLine.p1().getY() || ball.getY() < bottomTouchLine.p1().getY()) {
            if (game.getLastStartedTouch() == null) {
                return null;
            }

            RobotIdentifier byBot = game.getLastStartedTouch().getBy();
            if (game.getTime() - lastViolations > GRACE_PERIOD) {
                lastViolations = game.getTime();
                return new Violation(byBot.teamColor(), byBot.id(), ball.xy());
            }
        } else {
            lastViolations = Double.NEGATIVE_INFINITY;
        }
        return null;
    }

    @Override
    public void reset(Game game) {
        lastViolations = Double.NEGATIVE_INFINITY;
    }

    @Override
    public boolean isActive(Game game) {
        return game.isBallInPlay();
    }


    /**
     * Violation record which is used to flag who did the violation and where.
     *
     * @param byTeam   the team the robot is on that made the violation.
     * @param byBot    the robot that did the violation.
     * @param location the location on the field where the violation was made.
     */
    record Violation(TeamColor byTeam, int byBot, Vector2 location) implements RuleViolation {
        @Override
        public String toString() {
            return "Ball left the touch line (by: " + byTeam + ", by bot #" + byBot + ", at " + location + " )";
        }

        /**
         * Function that formats the violation into a packet to send to the GameController.
         *
         * @return a GameEvent packet of type BallLeftFieldTouchLine to be handled by the GameController.
         */
        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.BALL_LEFT_FIELD_TOUCH_LINE)
                    .setBallLeftFieldTouchLine(SslGcGameEvent.GameEvent.BallLeftField.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(byBot)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY())))
                    .build();

        }
    }
}
