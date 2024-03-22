package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

public class BoundaryCrossingValidator implements RuleValidator {
    // We add a margin to correct for the small position error when the ball bounces off the boundary.
    private static final float MARGIN = 0.7f + 0.05f;

    private boolean triggered = false;

    @Override
    public RuleViolation validate(Game game) {
        if (triggered) {
            return null;
        }

        Vector2 location;
        Vector3 ball = game.getBall().getPosition();

        if (ball.getY() > game.getField().getPosition().getY() + game.getField().getSize().getY() + MARGIN
                || ball.getY() < game.getField().getPosition().getY() - MARGIN
                || ball.getX() > game.getField().getPosition().getX() + game.getField().getSize().getX() + MARGIN
                || ball.getX() < game.getField().getPosition().getX() - MARGIN) {

            Touch touch = game.getLastFinishedTouch();
            location = ball.xy();

            triggered = true;
            if (touch != null) {
                Robot byBot = game.getRobot(touch.getBy());
                return new Violation(byBot.getTeam().getColor(), location);
            } else {
                return new Violation(null, location);
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
        triggered = false;
    }


    record Violation(TeamColor byTeam, Vector2 location) implements RuleViolation {
        @Override
        public String toString() {
            return "Ball left the Boundary Crossing (by: " + byTeam + ", at " + location + ")";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.BOUNDARY_CROSSING)
                    .setBoundaryCrossing(SslGcGameEvent.GameEvent.BoundaryCrossing.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY())))
                    .build();

        }
    }
}
