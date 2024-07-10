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

public class BotCrashingValidator implements RuleValidator {

    private static final float BOT_CRASH_DISTANCE = 0.005f;
    private static final float SPEED_VECTOR_THRESHOLD = 1.5f;
    private static final float MIN_SPEED_DIFFERENCE = 0.3f;
    private static final double GRACE_PERIOD = 2.0;
    //Map from robotId -> last violation time
    private final Map<RobotIdentifier, Double> lastViolations = new HashMap<>();

    /**
     * Calculate the angle between 2 vectors
     *
     * @param velocity1 - speed vector of bot 1
     * @param velocity2 - speed vector of bot 2
     * @return the angle in rad
     */
    public float angleBetweenVectors(Vector2 velocity1, Vector2 velocity2) {
        float epsilon = 0.000001f;
        float dotProduct = velocity1.dotProduct(velocity2);
        float cosTheta = dotProduct / (velocity1.magnitude() * velocity2.magnitude());
        if (Math.abs(dotProduct - 0) < epsilon) {
            cosTheta = 0.0f;
        }
        return (float) Math.acos(cosTheta);
    }

    /**
     * The difference of the speed vectors of both robots is taken and projected onto the line that is defined by the position of both robots
     *
     * @param position1 - position of bot 1
     * @param velocity1 - velocity vector of bot 1
     * @param position2 - position of bot 2
     * @param velocity2 - velocity of bot 2
     * @return the projection of speed difference
     */
    public static float calculateCollisionVelocity(Vector2 position1, Vector2 velocity1, Vector2 position2, Vector2 velocity2) {
        // Calculate velocity difference vector
        Vector2 velocityDifference = velocity2.subtract(velocity1);

        // Calculate position difference vector
        Vector2 positionDifference = position2.subtract(position1);

        // Calculate projection of velocity difference vector onto position difference vector
        float scalar = velocityDifference.dotProduct(positionDifference) / (positionDifference.magnitude() * positionDifference.magnitude());

        Vector2 projection = new Vector2(positionDifference.getX() * scalar, positionDifference.getY() * scalar);

        // Return the result
        return projection.magnitude();
    }


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

    @Override
    public RuleViolation validate(Game game) {
        for (Robot robotYellow : game.getTeam(TeamColor.YELLOW).getRobots()) {
            if (botStillOnCoolDown(robotYellow.getIdentifier(), game.getTime())) {
                continue;
            }
            for (Robot robotBlue : game.getTeam(TeamColor.BLUE).getRobots()) {
                if (botStillOnCoolDown(robotBlue.getIdentifier(), game.getTime())) {
                    continue;
                }
                Vector2 robotYellowPos = robotYellow.getPosition().xy();
                Vector2 robotBluePos = robotBlue.getPosition().xy();
                Vector2 robotYellowVel = robotYellow.getVelocity().xy();
                Vector2 robotBlueVel = robotBlue.getVelocity().xy();
                float distanceBetweenRobots = robotYellowPos.distance(robotBluePos);

                if (distanceBetweenRobots > robotYellow.getRadius() + robotBlue.getRadius() + BOT_CRASH_DISTANCE) {
                    continue;
                }
                // projection length of difference between speed vector
                float crashSpeed = calculateCollisionVelocity(robotBluePos, robotBlueVel, robotYellowPos, robotYellowVel);

                if (crashSpeed < SPEED_VECTOR_THRESHOLD) {
                    continue;
                }
                //speed difference
                float speedDiff = robotBlueVel.magnitude() - robotYellowVel.magnitude();
                speedDiff = roundFloatTo1DecimalPlace(speedDiff);
                //center position of 2 robots
                Vector2 location = new Vector2(roundFloatTo1DecimalPlace((float) ((robotBluePos.getX() + robotYellowPos.getX()) * 0.5))
                        , roundFloatTo1DecimalPlace((float) ((robotBluePos.getY() + robotYellowPos.getY()) * 0.5)));
                float crashAngle = angleBetweenVectors(robotBlueVel, robotYellowVel);
                crashAngle = roundFloatTo1DecimalPlace(crashAngle);
                if (Math.abs(speedDiff) < MIN_SPEED_DIFFERENCE) {
                    //crash drawn case
                    int botBlue = robotBlue.getId();
                    int botYellow = robotYellow.getId();
                    lastViolations.put(robotBlue.getIdentifier(), game.getTime());
                    lastViolations.put(robotYellow.getIdentifier(), game.getTime());
                    return new BotCrashingValidator.CrashDrawnViolation(botBlue, botYellow, location, crashSpeed, speedDiff, crashAngle);
                } else {
                    //crash unique case
                    int violator;
                    int victim;
                    TeamColor byTeam;
                    if (speedDiff > 0) {
                        byTeam = TeamColor.BLUE;
                        violator = robotBlue.getId();
                        victim = robotYellow.getId();
                    } else {
                        byTeam = TeamColor.YELLOW;
                        violator = robotYellow.getId();
                        victim = robotBlue.getId();
                    }
                    lastViolations.put(robotBlue.getIdentifier(), game.getTime());
                    lastViolations.put(robotYellow.getIdentifier(), game.getTime());
                    return new CrashUniqueViolation(distanceBetweenRobots, byTeam, violator, victim, location, crashSpeed, speedDiff, crashAngle);
                }
            }
        }
        return null;
    }

    @Override
    public void reset(Game game) {
        lastViolations.clear();
    }

    @Override
    public boolean isActive(Game game) {
        return game.getState() != GameState.HALT;
    }

    record CrashDrawnViolation(int botBlue, int botYellow, Vector2 location, float crash_speed, float speed_diff, float crash_angle) implements RuleViolation {
        @Override
        public String toString() {
            return "Bot crash drawn (bot blue #" + botBlue + " , bot yellow #" + botYellow + ", at " + location + ", crash speed :" + crash_speed + ", speed diff: " + speed_diff + ", angle:" + crash_angle + " )";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.BOT_CRASH_DRAWN)
                    .setBotCrashDrawn(SslGcGameEvent.GameEvent.BotCrashDrawn.newBuilder()
                            .setBotBlue(botBlue)
                            .setBotYellow(botYellow)
                            .setCrashSpeed(crash_speed)
                            .setSpeedDiff(speed_diff)
                            .setCrashAngle(crash_angle)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY())))
                    .build();
        }
    }

    record CrashUniqueViolation(float distance, TeamColor byTeam, int violator, int victim, Vector2 location, float crash_speed, float speed_diff, float crash_angle) implements RuleViolation {
        @Override
        public String toString() {
            return "Bot crash unique (by: " + byTeam + ", main violator #" + violator + " , victim #" + victim + ", distance: " + distance + ", at " + location + ", crash speed :" + crash_speed + ", speed diff: " + speed_diff + ", angle:" + crash_angle + " )";
        }

        @Override
        public SslGcGameEvent.GameEvent toPacket() {
            return SslGcGameEvent.GameEvent.newBuilder()
                    .setType(SslGcGameEvent.GameEvent.Type.BOT_CRASH_UNIQUE)
                    .setBotCrashUnique(SslGcGameEvent.GameEvent.BotCrashUnique.newBuilder()
                            .setByTeam(byTeam == TeamColor.BLUE ? SslGcCommon.Team.BLUE : SslGcCommon.Team.YELLOW)
                            .setViolator(violator)
                            .setVictim(victim)
                            .setCrashSpeed(crash_speed)
                            .setSpeedDiff(speed_diff)
                            .setCrashAngle(crash_angle)
                            .setLocation(SslGcGeometry.Vector2.newBuilder().setX(location.getX()).setY(location.getY())))
                    .build();

        }
    }
}
