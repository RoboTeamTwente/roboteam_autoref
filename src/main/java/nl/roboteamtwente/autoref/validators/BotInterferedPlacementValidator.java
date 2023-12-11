package nl.roboteamtwente.autoref.validators;

import nl.roboteamtwente.autoref.RuleValidator;
import nl.roboteamtwente.autoref.RuleViolation;
import nl.roboteamtwente.autoref.model.*;
import org.robocup.ssl.proto.SslGcCommon;
import org.robocup.ssl.proto.SslGcGameEvent;
import org.robocup.ssl.proto.SslGcGeometry;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class BotInterferedPlacementValidator implements RuleValidator {
    private static final double GRACE_PERIOD = 2.0;

    private static final float MIN_DISTANCE_BETWEEN_ROBOT_AND_PLACEMENT = 0.5f;

    //Map from robotId -> last enter the distance <= MIN_DISTANCE_BETWEEN_ROBOT_AND_PLACEMENT
    private final Map<RobotIdentifier, Double> lastEnterForbiddenArea = new HashMap<>();

    /**
     * Check if the distance between a point to
     * the stadium shape formed by the line capped by point 1 and point 2
     * smaller than the distance threshold
     *
     * @param p1 - first point of the line
     * @param p2 - second point of the line
     * @param p3 - the point to calculate distance to the line
     * @param robot - robot
     * @return the distance between the point 3 and the line defined by point 1 and 2
     */
    public boolean calculateDistancePointToLine(Vector2 p1, Vector2 p2, Vector2 p3, Robot robot) {
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();

        // If distance from p3 to p1 or p2 < threshold then it's a violation
        // Step1: Circle check
        if ((p3.distance(p1) - robot.getRadius() < MIN_DISTANCE_BETWEEN_ROBOT_AND_PLACEMENT) || (p3.distance(p2) - robot.getRadius() < MIN_DISTANCE_BETWEEN_ROBOT_AND_PLACEMENT)) {
            return true;
        }


        //Step 2: Rectangular formed by 2 lines parallel to the lined formed by p1, p2 and have the distance of 0.5 to the formed line

        // Find the coefficients a1, b1, and c1 of the equation d1: a1x + b1y + c1 = 0 that
        // defines the line passing through points p1 and p2
        double a1 = y2 - y1;
        double b1 = x1 - x2;
        double c1 = x2 * y1 - x1 * y2;

        // Find the coefficients a2, b2, and c2 of the equation d2: a2x + b2y + c2 = 0 that
        // defines the line passing through p3 and perpendicular to d1
        double a2 = b1;
        double b2 = -a1;
        double c2 = b1*(-x3) + a1*y3;

        //The intersection point of d1 and d2 is xi, yi
        //The intersection point is also the projection of p3 onto d1
        double xi = ((b1*c2)-(b2*c1))/((a1*b2)-(a2*b1));
        double yi = ((a2*c1)-(a1*c2))/((a1*b2)-(a2*b1));

        // Check if the projection point is capped by p1, p2
        if ((xi <= Math.max(x1,x2) && xi >= Math.min(x1,x2)) && (yi <= Math.max(y1,y2) && yi >= Math.min(y1,y2))) {
            // Calculate the distance between point p3 and the line defined by the equation ax + by + c = 0 using the formula:
            // |ax3 + by3 + c| / sqrt(a^2 + b^2)
            double numerator = Math.abs(a1 * x3 + b1 * y3 + c1);
            double denominator = Math.sqrt(a1 * a1 + b1 * b1);
            double distance = numerator / denominator;
            if (distance - robot.getRadius() < MIN_DISTANCE_BETWEEN_ROBOT_AND_PLACEMENT) {
                return true;
            }
        }

        return false;
    }

    /**
     * Round float number to 1 decimal place
     *
     * @param number
     * @return rounded float number
     */
    public float roundFloatTo1DecimalPlace(float number) {
        DecimalFormat df = new DecimalFormat("#.#"); // Creates a decimal format object with one decimal place
        String roundedFloatStr = df.format(number); // Formats the float as a string with one decimal place
        return Float.parseFloat(roundedFloatStr); // Parses the rounded string back into a float
    }

    /**
     * Check if time a robot enter forbidden area is more than 2 seconds
     *
     * @param bot              - identifier of the robot
     * @param currentTimeStamp - the time robot enter forbidden area
     * @return true if robot enter more than 2 seconds else update the lastEnterForbidden Area
     */

    public boolean checkViolation(RobotIdentifier bot, double currentTimeStamp) {
        if (lastEnterForbiddenArea.containsKey(bot)) {
            Double timestampLastViolation = lastEnterForbiddenArea.get(bot);
            // if enter forbidden area more than 2 second => return fault and reset enter forbidden area time
            if (currentTimeStamp > timestampLastViolation + GRACE_PERIOD) {
                lastEnterForbiddenArea.put(bot, currentTimeStamp);
                return true;
            }
        } else {
            // if the first time => add into enter forbidden area
            lastEnterForbiddenArea.put(bot, currentTimeStamp);
        }
        return false;
    }

    @Override
    public void reset(Game game) {
        lastEnterForbiddenArea.clear();
    }

    @Override
    public RuleViolation validate(Game game) {
        if (game.getState() == GameState.BALL_PLACEMENT) {

            Team opponentTeam = game.getTeam(game.getStateForTeam().getOpponentColor());
            for (Robot robot : opponentTeam.getRobots()) {
                Vector2 robotPos = robot.getPosition().xy();
                Vector2 placementPos = game.getDesignatedPosition();
                Vector2 ballPos = game.getBall().getPosition().xy();
                if (calculateDistancePointToLine(ballPos, placementPos, robotPos, robot)) {
                    if (checkViolation(robot.getIdentifier(), game.getTime())) {
                        Vector2 roundRobotPos = new Vector2(roundFloatTo1DecimalPlace(robot.getPosition().getX()), roundFloatTo1DecimalPlace(robot.getPosition().getY()));
                        return new BotInterferedPlacementValidator.BotInterferedPlacementViolation(robot.getTeam().getColor(), robot.getId(), roundRobotPos, ballPos, placementPos);
                    }
                } else if (lastEnterForbiddenArea.containsKey(robot.getIdentifier())) {
                    lastEnterForbiddenArea.remove(robot.getIdentifier());
                }
            }
        }
        return null;
    }

    @Override
    public boolean isActive(Game game) {
        return game.getState() == GameState.BALL_PLACEMENT;
    }

    record BotInterferedPlacementViolation(TeamColor byTeam, int byBot, Vector2 location, Vector2 ballPos, Vector2 placementPos) implements RuleViolation {
        @Override
        public String toString() {
            return "Bot interfered placement (by: " + byTeam + ", bot #" + byBot + " location: " + location + " ballPos" + ballPos + " placementPos: " + placementPos + " )";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.BOT_INTERFERED_PLACEMENT)
                    .setBotInterferedPlacement(SslGcGameEvent.GameEvent.BotInterferedPlacement.newBuilder()
                            .setByBot(byBot)
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY())))
                    .build();
        }
    }
}
