package nl.roboteamtwente.autoref.model;

/**
 * A Vector3 class is used to define a 3D object on the playing field.
 */
public class Vector3 {
    /**
     * X-coordinate of an object.
     */
    private float x;

    /**
     * Y-coordinate of an object.
     */
    private float y;

    /**
     * Z-coordinate of an object.
     */
    private float z;


    /**
     * Constructor of a Vector3.
     * @param x, X-coordinate
     * @param y, Y-coordinate
     * @param z, Z-coordinate
     */
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * @return an identical copy of this vector.
     */
    public Vector3 copy() {
        return new Vector3(x, y, z);
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
     * Sets the Y-coordinate of an object
     */
    public void setY(float y) {
        this.y = y;
    }


    /**
     *
     * @return Z-coordinate of an object
     */
    public float getZ() {
        return z;
    }


    /**
     * Sets the Z-coordinate of an object
     */
    public void setZ(float z) {
        this.z = z;
    }


    /**
     *
     * @return the xy coordinates of a Vector3.
     */
    public Vector2 xy() {
        return new Vector2(getX(), getY());
    }

    /**
     * Method used to detect the distance between two vectors.
     * @param other, the other vector to find the distance to.
     * @return the distance between two vectors.
     */
    public float distance(Vector3 other) {
        return (float) Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }

    /**
     * Calculate the length/magnitude of the current vector.
     * @return the length of vector.
     */
    public float magnitude() {
        return ((float) Math.sqrt(this.getX()*this.getX() + this.getY()*this.getY() + this.getZ()*this.getZ()));
    }


    /**
     * @return the string value of the Vector3 object.
     */
    @Override
    public String toString() {
        return "{" + x + ", " + y + ", " + z + '}';
    }
}
