package nl.roboteamtwente.autoref.model;

public class Touch {
    int id;
    private Vector3 startLocation;
    private Vector3 endLocation;
    private double startTime;
    private Double endTime;
    private Vector3 startVelocity;
    private Vector3 endVelocity;
    private float percentageBallSeen;
    private float averageNumberOfRobotsCloseBy;
    private int nOfDataPoints;
    private RobotIdentifier by;

    public Touch(int id, Vector3 startLocation, double startTime,
                 Vector3 startVelocity, int numberOfRobotsCloseBy, RobotIdentifier by) {
        this.id = id;
        this.startLocation = startLocation;
        this.endLocation = null;
        this.startTime = startTime;
        this.endTime = null;
        this.startVelocity = startVelocity;
        this.endVelocity = null;
        this.percentageBallSeen = 1.0f;
        this.averageNumberOfRobotsCloseBy = numberOfRobotsCloseBy;
        this.nOfDataPoints = 1;
        this.by = by;
    }

    public boolean isFinished() {
        return this.endLocation != null;
    }

    public void updatePercentages(boolean ballVisible, int numberOfRobotsCloseBy) {
        int value = ballVisible ? 1 : 0;
        this.percentageBallSeen = (this.percentageBallSeen * this.nOfDataPoints + value) / (nOfDataPoints + 1);
        this.averageNumberOfRobotsCloseBy = (this.averageNumberOfRobotsCloseBy * this.nOfDataPoints
                + numberOfRobotsCloseBy) / (nOfDataPoints + 1);
        this.nOfDataPoints++;
    }

    public float deflectionAngle() {
        float angle = startVelocity.xy().angle(endVelocity.xy());
        return Math.min(angle, 360 - angle);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Touch other) {
            return id == other.id;
        } else {
            return false;
        }
    }

    public Vector3 getStartLocation() {
        return startLocation;
    }

    public void setEndLocation(Vector3 endLocation) {
        this.endLocation = endLocation;
    }

    public Vector3 getEndLocation() {
        return endLocation;
    }

    public void setEndTime(Double endTime) {
        this.endTime = endTime;
    }

    public Double getEndTime() {
        return endTime;
    }

    public void setEndVelocity(Vector3 endVelocity) {
        this.endVelocity = endVelocity;
    }

    public int getId() {
        return id;
    }

    public RobotIdentifier getBy() {
        return by;
    }

    public double getStartTime() {
        return this.startTime;
    }
}
