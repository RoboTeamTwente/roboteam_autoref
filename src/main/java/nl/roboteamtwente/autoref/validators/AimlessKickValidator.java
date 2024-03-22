package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;


public class AimlessKickValidator implements RuleValidator {
    private static final double GRACE_PERIOD = 2.0;
    private double lastViolation = Double.NEGATIVE_INFINITY;

    @Override
    public RuleViolation validate(Game game) {
        //Get the last touch made before the ball exited the field.
        Touch touch = game.getLastFinishedTouch();
        if (touch == null || (touch.equals(game.getKickIntoPlay()) && game.getKickType() == KickType.KICKOFF)) {
            return null;
        }

        //Get the left and right goal lines to see if they were crossed
        FieldLine rightGoalLine = game.getField().getLineByName("RightGoalLine");
        FieldLine leftGoalLine = game.getField().getLineByName("LeftGoalLine");

        //Sometimes getting the lines returns null, this is an error handling statement.
        if (rightGoalLine == null || leftGoalLine == null) {
            return null;
        }

        //Get the last robot who made the last touch
        Robot byBot = game.getRobot(touch.getBy());

        //Check if the bot retrieved was in their own half before the ball went out.
        if (game.getField().isInOwnHalf(byBot.getTeam().getSide(), touch.getEndLocation().xy())
                && game.getTime() - lastViolation > GRACE_PERIOD) {

            //If the ball left the right goal line and the side of the bot was left then an aimless kick happened.
            if (game.getBall().getPosition().getX() > rightGoalLine.p1().getX()
                    && byBot.getTeam().getSide() == Side.LEFT) {
                lastViolation = game.getTime();
                return new Violation(byBot.getTeam().getColor(), byBot.getId(), game.getBall().getPosition().xy(), touch.getEndLocation().xy());
            }

            //If the ball left the left goal line and the side of the bot was right then an aimless kick happened.
            if (game.getBall().getPosition().getX() < leftGoalLine.p1().getX()
                    && byBot.getTeam().getSide() == Side.RIGHT) {
                lastViolation = game.getTime();
                return new Violation(byBot.getTeam().getColor(), byBot.getId(), game.getBall().getPosition().xy(), touch.getEndLocation().xy());
            }
        }

        return null;
    }

    @Override
    public void reset(Game game) {
        lastViolation = Double.NEGATIVE_INFINITY;
    }

    @Override
    public boolean isActive(Game game) {
        return game.isBallInPlay();
    }


    /**
     * Violation record which is used to flag who did the violation and where.
     *
     * @param byTeam   the team the robot is in that performed the violation.
     * @param byBot    the robot that performed the violation.
     * @param ballLocation the location on the field where the ball left the goal line.
     * @param kickLocation the location on the field where the aimless kick was made.
     */
    record Violation(TeamColor byTeam, int byBot, Vector2 ballLocation, Vector2 kickLocation) implements RuleViolation {
        @Override
        public String toString() {
            return "Aimless kick (by: " + byTeam + ", by bot #" + byBot + ", at kick location: " + kickLocation + ", ball left at " + ballLocation + " )";
        }

        /**
         * Function that formats the violation into a packet to send to the GameController.
         *
         * @return a GameEvent packet of type AimlessKick to be handled by the GameController.
         */
        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.AIMLESS_KICK)
                    .setAimlessKick(SslGcGameEvent.GameEvent.AimlessKick.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(byBot)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(ballLocation.getX()).setY(ballLocation.getY()))
                            .setKickLocation(SslGcGeometry.Vector2.newBuilder().setX(kickLocation.getX()).setY(kickLocation.getY()))
                    )
                    .build();
        }
    }
}
