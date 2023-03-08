package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class AttackerTouchedBallInDefenseAreaValidator implements RuleValidator {
    private static final double GRACE_PERIOD = 2.0;

    private final Map<RobotIdentifier, Double> lastViolations = new HashMap<>();

    @Override
    public RuleViolation validate(Game game) {
        // FIXME: This doesn't work for non-straight lines
        for (Robot robot : game.getBall().getRobotsTouching()) {
            Team team = robot.getTeam();
            Side oppositeSide = team.getSide().getOpposite();
            String oppositeSideString = oppositeSide == Side.LEFT ? "Left" : "Right";

            FieldLine penaltyStretch = game.getField().getLineByName(oppositeSideString + "PenaltyStretch");
            if (robot.getPosition().getX() * oppositeSide.getCardinality() < penaltyStretch.p1().getX() * oppositeSide.getCardinality()) {
                continue;
            }

            FieldLine rightPenaltyStretch = game.getField().getLineByName(oppositeSideString + "FieldRightPenaltyStretch");
            FieldLine leftPenaltyStretch = game.getField().getLineByName(oppositeSideString + "FieldLeftPenaltyStretch");

            FieldLine topPenaltyStretch = rightPenaltyStretch.p1().getY() > leftPenaltyStretch.p1().getY() ? rightPenaltyStretch : leftPenaltyStretch;
            FieldLine bottomPenaltyStretch = topPenaltyStretch == rightPenaltyStretch ? leftPenaltyStretch : rightPenaltyStretch;

            if (robot.getPosition().getY() < bottomPenaltyStretch.p1().getY()) {
                continue;
            }

            if (robot.getPosition().getY() > topPenaltyStretch.p1().getY()) {
                continue;
            }

            if (!lastViolations.containsKey(robot.getIdentifier()) || lastViolations.get(robot.getIdentifier()) + GRACE_PERIOD < game.getTime()) {
                lastViolations.put(robot.getIdentifier(), game.getTime());
                return new Violation(team.getColor(), robot.getId(), robot.getPosition().xy(), 0.0f);
            }
        }

        return null;
    }

    @Override
    public EnumSet<GameState> activeStates() {
        return EnumSet.of(GameState.RUNNING);
    }

    @Override
    public void reset() {
        lastViolations.clear();
    }

    record Violation(TeamColor byTeam, int byBot, Vector2 location, float distance) implements RuleViolation {
        @Override
        public String toString() {
            return "Attacker touched ball in defense area (by: " + byTeam + " #" + byBot + ", at " + location + ", distance: " + distance + ")";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.ATTACKER_TOUCHED_BALL_IN_DEFENSE_AREA)
                    .setAttackerTouchedBallInDefenseArea(SslGcGameEvent.GameEvent.AttackerTouchedBallInDefenseArea.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(byBot)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY()))
                            .setDistance(distance))
                    .build();
        }
    }
}
