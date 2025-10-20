package com.demo.game.network.messages;

import java.io.Serializable;
import javafx.geometry.Point2D;

/**
 * A simple, Serializable replacement for Point2D to be sent over the network.
 * The standard javafx.geometry.Point2D class does not implement Serializable
 * and will cause a NotSerializableException if sent in an ObjectOutputStream.
 */
public class SPoint2D implements Serializable {
    private static final long serialVersionUID = 1L; // Version for serialization
    public final double x;
    public final double y;

    /**
     * Constructs a new SPoint2D with specified coordinates.
     * @param x the x-coordinate.
     * @param y the y-coordinate.
     */
    public SPoint2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructs a new SPoint2D from an existing JavaFX Point2D object.
     * @param point the JavaFX Point2D to copy coordinates from.
     */
    public SPoint2D(Point2D point) {
        this.x = point.getX();
        this.y = point.getY();
    }

    /**
     * Converts this SPoint2D object back into a JavaFX Point2D object.
     * @return A new Point2D object with the same coordinates.
     */
    public Point2D toPoint2D() {
        return new Point2D(x, y);
    }

    @Override
    public String toString() {
        return "SPoint2D[x=" + x + ", y=" + y + "]";
    }
}
