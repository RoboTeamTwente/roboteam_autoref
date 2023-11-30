package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcRefereeMessage;

public class PlacementSucceededValidator implements RuleValidator {

    private static final float FREE_KICK_PLACEMENT_DISTANCE = 0.05f;

    private static final float FORCE_START_PLACEMENT_DISTANCE = 0.5f;

    private static final float MAXIMUM_PLACEMENT_DISTANCE_BETWEEN_BALL_AND_DESIGNATED_POS = 0.15f;

    private static final float MIN_PLACEMENT_TIME = 2;

    private static final float STATIONARY_THRESHOLD = 0.005f;

    private static Vector3 initialBallPosition;

    //Rule states: validator only raised 1 per ball placement
    private static boolean issueValidator = false;

    /**
     * Check if the ball is stationary with its velocity
     * Check the distance constraint for robot
     *
     * @param game
     * @return true if it meets 2 requirements
     */
    public boolean isConsideredPlacedSuccessfully(Game game) {
        // Ball must be stationary during placement
        if (game.getBall().getVelocity().xy().magnitude() > STATIONARY_THRESHOLD) {
            return false;
        }

        //All robot must keep distance to ball during the placement
        double minDistance = isNextCommandForPlacingTeam(game) ? FREE_KICK_PLACEMENT_DISTANCE : FORCE_START_PLACEMENT_DISTANCE;
        Vector2 ballPosition = game.getBall().getPosition().xy();

        for (Robot robot : game.getRobots()) {
            if (robot.getPosition().xy().distance(ballPosition) < minDistance) {
                return false;
            }
        }

        return true;
    }

    /**
     * Based on the game command and next command to check if the next command is for the placing team
     *
     * @param game - Game command and next command
     * @return true if the next command is for the placing team
     */
    private boolean isNextCommandForPlacingTeam(Game game) {
        if (game.getCommand() == SslGcRefereeMessage.Referee.Command.BALL_PLACEMENT_BLUE) {
            return game.getNextCommand() == SslGcRefereeMessage.Referee.Command.DIRECT_FREE_BLUE
                    || game.getNextCommand() == SslGcRefereeMessage.Referee.Command.INDIRECT_FREE_BLUE;
        } else if (game.getCommand() == SslGcRefereeMessage.Referee.Command.BALL_PLACEMENT_YELLOW) {
            return game.getNextCommand() == SslGcRefereeMessage.Referee.Command.DIRECT_FREE_YELLOW
                    || game.getNextCommand() == SslGcRefereeMessage.Referee.Command.INDIRECT_FREE_YELLOW;
        }
        return false;
    }

    @Override
    public RuleViolation validate(Game game) {

        if (issueValidator || game.getDesignatedPosition() == null) {
            return null;
        }

        Vector3 currentBallPos = game.getBall().getPosition();
        TeamColor forTeam = game.getStateForTeam();
        float precision = game.getDesignatedPosition().distance(currentBallPos.xy());
        double timeTaken = game.getTime() - game.getTimeLastGameStateChange();

        //Check the constraint for distance between ball and designated position and the ball placement cannot perform earlier than 2 seconds after the ball placement command has been issued
        if (precision <= MAXIMUM_PLACEMENT_DISTANCE_BETWEEN_BALL_AND_DESIGNATED_POS && timeTaken >= MIN_PLACEMENT_TIME) {
            if (isConsideredPlacedSuccessfully(game)) {
                issueValidator = true;
                float distance = initialBallPosition.xy().distance(currentBallPos.xy());

                return new PlacementSucceededValidator.PlacementSucceededViolation(forTeam, (float) timeTaken, precision, distance);
            }
        }

        return null;
    }

    @Override
    public void reset(Game game) {
        initialBallPosition = game.getBall().getPosition();
        issueValidator = false;
    }

    record PlacementSucceededViolation(TeamColor byTeam, float time_taken, float precision, float distance) implements RuleViolation {
        @Override
        public String toString() {
            return "Placement succeeded (by: " + byTeam + " time taken: " + time_taken + ", precision: " + precision + ", distance: " + distance + " )";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.PLACEMENT_SUCCEEDED)
                    .setPlacementSucceeded(SslGcGameEvent.GameEvent.PlacementSucceeded.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setTimeTaken(time_taken)
                            .setPrecision(precision)
                            .setDistance(distance))
                    .build();

        }
    }


    record PlacementFailedViolation(TeamColor byTeam, float distance) implements RuleViolation {
        @Override
        public String toString() {
            return "Placement failed (by: " + byTeam + ", remaining distance: " + distance + " )";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.PLACEMENT_FAILED)
                    .setPlacementFailed(SslGcGameEvent.GameEvent.PlacementFailed.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setRemainingDistance(distance))
                    .build();

        }
    }

    @Override
    public boolean isActive(Game game) {
        return game.getState() == GameState.BALL_PLACEMENT;
    }
}
