package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

public class PenaltyKickFailedValidator implements RuleValidator {
    private boolean valid = false;

    @Override
    public RuleViolation validate(Game game) {
        // make sure this only gets called if the goalkeeper touches the ball while the opponent is taking a penalty
        if (!valid) {
            if (game.getTeam(game.getStateForTeam().getOpponentColor()).getGoalkeeper().hasJustTouchedBall()) {
                valid = true;
            } else {
                return null;
            }
        }

        // if last finished touch was by goalkeeper
        Touch touch = game.getLastFinishedTouch();
        if (touch.getBy().teamColor() != game.getStateForTeam() && touch.deflectionAngle() >= 90.0f &&
                game.getTeam(game.getStateForTeam().getOpponentColor()).getGoalkeeperId() == touch.getBy().id()) {
            valid = false;
            return new Violation(touch.getBy().teamColor(), game.getBall().getPosition().xy(),
                    "defending goalkeeper changed angle of velocity of ball by " + touch.deflectionAngle() + " degrees");

        }

        return null;
    }

    @Override
    public boolean isActive(Game game) {
        return game.getState() == GameState.PENALTY;
    }

    @Override
    public void reset(Game game) {
        valid = false;
    }

    record Violation(TeamColor byTeam, Vector2 location, String reason) implements RuleViolation {
        @Override
        public String toString() {
            return "Penalty kick failed (by: " + byTeam + ", at " + location + ", reason: " + reason + ")";
        }


        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.PENALTY_KICK_FAILED)
                    .setPenaltyKickFailed(SslGcGameEvent.GameEvent.PenaltyKickFailed.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY()))
                            .setReason(reason))
                    .build();
        }
    }
}
