package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

import java.util.HashMap;
import java.util.Map;

public class DefenderInDefenseAreaValidator implements RuleValidator {

    private final Map<RobotIdentifier, Double> lastViolations = new HashMap<>();

    private static final double GRACE_PERIOD = 2.0;

    /**
     * Check if the violation is still in GRACE_PERIOD
     *
     * @param bot              - identifier of the bot
     * @param currentTimeStamp - the current time that detect violation again
     * @return true if bot still under GRACE_PERIOD
     */
    private boolean botStillOnCoolDown(RobotIdentifier bot, double currentTimeStamp) {
        if (lastViolations.containsKey(bot)) {
            Double timestampLastViolation = lastViolations.get(bot);
            if (currentTimeStamp <= timestampLastViolation + GRACE_PERIOD) {
                return true;
            } else {
                lastViolations.remove(bot);
                return false;
            }
        }
        return false;
    }


    /**
     * The validate method of this class determines whether a defender is in their own
     * defense area. The location of all bots is checked, if the location of the bot
     * is between the coordinates of their defense area, the violation is raised.
     *
     * @param game the game object being validated
     * @return a violation when a defender is in the defense area.
     */
    @Override
    public RuleViolation validate(Game game) {
        for (Robot robot : game.getBall().getRobotsTouching()) {
            Side side = game.getTeam(robot.getTeam().getColor()).getSide();
            String sideString = side == Side.LEFT ? "Left" : "Right";

            if (robot.isGoalkeeper()) {
                continue;
            }

            if (!game.getField().isFullyInDefenseArea(robot.getTeam().getSide(), robot.getPosition().xy())) {
                continue;
            }

            FieldLine penaltyStretch = game.getField().getLineByName(sideString + "PenaltyStretch");
            FieldLine rightPenaltyStretch = game.getField().getLineByName(sideString + "FieldRightPenaltyStretch");
            FieldLine leftPenaltyStretch = game.getField().getLineByName(sideString + "FieldLeftPenaltyStretch");
            float dist = Math.min(Math.abs(game.getLastStartedTouch().getStartLocation().getX() - penaltyStretch.p1().getX()),
                    Math.min(Math.abs( game.getLastStartedTouch().getStartLocation().getY() - rightPenaltyStretch.p1().getY()),
                    Math.abs(game.getLastStartedTouch().getStartLocation().getY() - leftPenaltyStretch.p1().getY())));
            if (!botStillOnCoolDown(robot.getIdentifier(), game.getTime())) {
                lastViolations.put(robot.getIdentifier(), game.getTime());
                return new Violation(robot.getTeam().getColor(), robot.getId(), robot.getPosition().xy(), dist);
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
     * @param byTeam   the team the robot is in that performed the violation.
     * @param byBot    the robot that performed the violation.
     * @param location the location on the field where the violation was made.
     * @param distance the minimum distance from the lines of the defense area to the bot.
     */
    record Violation(TeamColor byTeam, int byBot, Vector2 location, float distance) implements RuleViolation {
        @Override
        public String toString() {
            return "Defender in defense area (by: " + byTeam + ", by bot #" + byBot + ", at " + location + ", distance: " + distance + ")";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.DEFENDER_IN_DEFENSE_AREA)
                    .setDefenderInDefenseArea(SslGcGameEvent.GameEvent.DefenderInDefenseArea.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setByBot(byBot)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY()))
                            .setDistance(distance))
                    .build();
        }
    }
}
