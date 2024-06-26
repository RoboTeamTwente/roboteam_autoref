package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

public class PossibleGoalValidator implements RuleValidator {

    // Check if possible goal is raised
    private boolean isEventRaised = false;

    /**
     * Check if the ball position is inside the goal post
     * Division A and B have different goal size
     * @param game - the game that is currently taking place
     * @param side - the side of the ball when enter the goal
     * @param ballPos - the position of the ball
     * @return true if ball is inside the goal otherwise false
     */
    boolean checkBallInsideGoal(Game game, Side side, Vector2 ballPos) {
        float goalDepthLength = game.getField().getGoal().getDepth();
        float goalWidthLength = game.getField().getGoal().getWidth();

        String fieldLineName;
        if (side.equals(Side.RIGHT)) {
            fieldLineName = "RightFieldLeftPenaltyStretch";
        } else {
            fieldLineName = "LeftFieldLeftPenaltyStretch";
        }
        FieldLine fieldLine = game.getField().getLineByName(fieldLineName);
        if (fieldLine != null) {
            // LeftToRightCoefficient if leftPenaltyStretch is positive otherwise negative
            float leftToRightCoefficient = 1;
            if (fieldLine.p1().getY() >= 0) {
                leftToRightCoefficient = 1;
            } else {
                leftToRightCoefficient = -1;
            }
            float leftPostP1x = fieldLine.p1().getX();
            float leftPostP1y = (goalWidthLength/2) * leftToRightCoefficient;

            float leftPostP2x = leftPostP1x + side.getCardinality()*goalDepthLength;

            float rightPostP1y = (goalWidthLength/2) * leftToRightCoefficient * -1;

            // Check if ball inside right goal
            if ((ballPos.getY() >= Math.min(rightPostP1y, leftPostP1y)) && (ballPos.getY() <= Math.max(rightPostP1y, leftPostP1y))
                    && (ballPos.getX() >= Math.min(leftPostP1x,leftPostP2x)) && (ballPos.getX() <= Math.max(leftPostP1x,leftPostP2x))) {
                System.out.println("Inside goal " + side);
                return true;
            }
        }
        return false;
    }

    @Override
    public RuleViolation validate(Game game) {
        if (isEventRaised) {
            return null;
        }
        Vector2 ballPos = game.getBall().getPosition().xy();

        Touch touch = game.getLastFinishedTouch();
        if (touch == null) {
            return null;
        }
        Side ballSide = ballPos.getX() < 0 ? Side.LEFT : Side.RIGHT;
        if (checkBallInsideGoal(game, ballSide, ballPos)) {
            Vector2 kickLocation = touch.getEndLocation().xy();
            RobotIdentifier kickBot = touch.getBy();
            TeamColor kickingTeam = kickBot.teamColor();
            double lastTouchTimeStampByTeam = touch.getEndTime();
            TeamColor byTeam;

            // Scoring team is the opposite team of the team owns the side
            if (game.getTeam(TeamColor.BLUE).getSide().equals(ballSide)) {
                byTeam = TeamColor.YELLOW;
            } else {
                byTeam = TeamColor.BLUE;
            }

            int numRobotsByTeam = game.getTeam(byTeam).getRobots().size();
            isEventRaised = true;
            return new PossibleGoalValidator.PossibleGoal(byTeam, kickingTeam, kickBot.id(), ballPos, kickLocation, 0f, numRobotsByTeam, (int) lastTouchTimeStampByTeam);
        }
        return null;
    }


    @Override
    public void reset(Game game) {
        isEventRaised = false;
    }

    @Override
    public boolean isActive(Game game) {
        return game.isBallInPlay();
    }

    record PossibleGoal(TeamColor byTeam, TeamColor kickingTeam, int kickingBot, Vector2 location, Vector2 kickLocation, Float maxBallHeight, int numRobotsByTeam, int lastTouchTimestampByTeam) implements RuleViolation {
        @Override
        public String toString() {
            return "Possible goal (by: " + byTeam + ", kicking team: " + kickingTeam + ", bot #" + kickingBot + ", location:" + location + ", kick location:" + kickLocation
                    + ", num robots: "+ numRobotsByTeam + ")";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.POSSIBLE_GOAL)
                    .setPossibleGoal(SslGcGameEvent.GameEvent.Goal.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setKickingTeam(kickingTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setKickingBot(kickingBot)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY()))
                            .setMaxBallHeight(maxBallHeight)
                            .setKickLocation(SslGcGeometry.Vector2.newBuilder().setX(kickLocation.getX()).setY(kickLocation.getY()))
                            .setNumRobotsByTeam(numRobotsByTeam)
                    )
                    .build();
        }
    }
}
