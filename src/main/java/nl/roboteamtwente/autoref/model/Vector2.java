package nl.roboteamtwente.autoref.model;

/**
 * A Vector2 class is used to define a 2D object on the playing field.
 */
public class Vector2 {

    /**
     * X-coordinate of an object.
     */
    private float x;

    /**
     * Y-coordinate of an object.
     */
    private float y;

    /**
     * Constructor of a Vector2.
     * @param x, X-coordinate
     * @param y, Y-coordinate
     */
    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return an identical copy of this vector.
     */
    public Vector2 copy() {
        return new Vector2(x, y);
    }

    /**
     *
     * @return X-coordinate of an object
     */
    public float getX() {
        return x;
    }

    /**
     * Sets the X-coordinate of an object
     */
    public void setX(float x) {
        this.x = x;
    }

    /**
     *
     * @return Y-coordinate of an object
     */
    public float getY() {
        return y;
    }

    /**
     *
     * Sets the Y-coordinate of an object
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * Adds the dimensions of another vector to the one in the current object.
     * @param other the vector to add.
     * @return the new Vector object.
     */
    public Vector2 add(Vector2 other) {
        return new Vector2(this.getX() + other.getX(), this.getY() + other.getY());
    }

    /**
     * Subtract the dimensions of another vector to the one in the current object.
     * @param other the vector to substract.
     * @return the new Vector object.
     */
    public Vector2 subtract(Vector2 other) {
        return new Vector2(this.getX() - other.getX(), this.getY() - other.getY());
    }

    /**
     * Multiply vector by a constant
     * @param constant the constant the vertor needs to be multiplied with
     * @return the new Vector object
     */
    public Vector2 multiply(float constant) {
        return new Vector2(constant * this.getX(), constant * this.getY());
    }

    /**
     * Calculate the dot product of the current object with other object.
     * @param other the second vector.
     * @return the new Vector object.
     */
    public float dotProduct(Vector2 other) {
        return (this.getX() *other.getX() + this.getY()* other.getY());
    }

    /**
     * Calculate the length/magnitude of the current vector.
     * @return the length of vector.
     */
    public float magnitude() {
        return ((float) Math.sqrt(this.getX()*this.getX() + this.getY()* this.getY()));
    }

    /**
     * Method used to change the orientation of the robot.
     * @param angle which the orientation should be made in.
     * @return the updated rotated value.
     */
    public Vector2 rotate(float angle) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        return new Vector2(cos * x - sin * y, sin * x - cos * y);
    }

    /**
     * Method used to detect the distance between two vectors.
     * @param other, the other vector to find the distance to.
     * @return the distance between two vectors.
     */
    public float distance(Vector2 other) {
        return (float) Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
    }

    /**
     * Get the angle between two vectors
     * @param other The other vector.
     * @return the angle in degrees.
     */
    public float angle(Vector2 other) {
        return (float) Math.toDegrees(Math.acos(dotProduct(other) / (magnitude() * other.magnitude())));
    }

    /**
     * @return the string value of the Vector2 object.
     */
    @Override
    public String toString() {
        return "{" + x + ", " + y + '}';
    }
}
