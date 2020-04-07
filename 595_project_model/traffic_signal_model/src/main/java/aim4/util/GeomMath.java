/*
Copyright (c) 2011 Tsz-Chiu Au, Peter Stone
University of Texas at Austin
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the University of Texas at Austin nor the names of its
contributors may be used to endorse or promote products derived from this
software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package aim4.util;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

/**
 * Class of static utility methods for geometric computation.
 */
public final class GeomMath {

  /////////////////////////////////
  // CONSTANTS
  /////////////////////////////////

  /**
   * Math.PI / 2.0
   */
  public static final double HALF_PI = Math.PI / 2.0;

  /**
   * Math.PI * 2.0
   */
  public static final double TWO_PI = 2.0 * Math.PI;


  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  /**
   * Turn a cardinal number into an ordinal number.
   *
   * @param num the cardinal number
   * @return    the ordinal version of the given number
   */
  public static String ordinalize(int num) {
    String suffix;
    if(num % 100 == 11 || num % 100 == 12 || num % 100 == 13) {
      suffix = "th";
    } else {
      switch(num % 10) {
      case 1:
        suffix = "st";
        break;
      case 2:
        suffix = "nd";
        break;
      case 3:
        suffix = "rd";
        break;
      default:
        suffix = "th";
        break;
      }
    }
    return num + suffix;
  }

  /**
   * Get the "canonical" angle. This is used for recentering angles in
   * [0.0, 2*Pi).
   *
   * @param angle  the angle
   * @return an equivalent angle that is greater than or equal to 0.0
   *         and less than 2*PI
   */
  public static double canonicalAngle(double angle) {
    return angle - Math.floor(angle/TWO_PI) * TWO_PI;
  }

  /**
   * Find the angle of the heading to a point from a given starting point.
   *
   * @param p          the point to which to find the angle
   * @param startPoint the point from which to start
   * @return           the angle of the heading to the first point when
   *                   starting at the second point
   */
  public static double angleToPoint(Point2D p, Point2D startPoint) {
    return Math.atan2(p.getY() - startPoint.getY(),
                      p.getX() - startPoint.getX());
  }

  /**
   * Find a point displaced by the given distance in the given direction.
   *
   * @param p     the starting point
   * @param r     the distance to move from the point
   * @param theta the angle at which to move from the point
   * @return      the point displaced the given distance in the given
   *              direction from the original point
   */
  public static Point2D polarAdd(Point2D p, double r, double theta) {
    return new Point2D.Double(p.getX() + Math.cos(theta) * r,
                              p.getY() + Math.sin(theta) * r);
  }

  /**
   * Subtract two 2D vectors.
   *
   * @param p1 the first vector
   * @param p2 the second vector
   * @return   a Point representing the subtraction of the second vector from
   *           the first
   */
  public static Point2D subtract(Point2D p1, Point2D p2) {
     return new Point2D.Double(p1.getX() - p2.getX(), p1.getY() - p2.getY());
  }

  /**
   * Compute the dot product of two 2D vectors.
   *
   * @param p1 the first vector
   * @param p2 the second vector
   * @return the dot product of the two vectors
   */
  public static double dotProduct(Point2D p1, Point2D p2) {
     return (p1.getX() * p2.getX()) + (p1.getY() * p2.getY());
  }


  /**
   * Compute the intersection of two lines.
   *
   * @param l1 the first line
   * @param l2 the second line
   * @return   the intersection point of the two lines
   */
  public static Point2D findLineLineIntersection(Line2D l1, Line2D l2) {
    return findLineLineIntersection(l1.getX1(), l1.getY1(),
                                    l1.getX2(), l1.getY2(),
                                    l2.getX1(), l2.getY1(),
                                    l2.getX2(), l2.getY2());
  }

  /**
   * Compute the intersection of two lines defined by two points each.
   * Undefined behavior if the lines are parallel.
   *
   * @param x1 the x coordinate of the first point of the first line
   * @param y1 the y coordinate of the first point of the first line
   * @param x2 the x coordinate of the second point of the first line
   * @param y2 the y coordinate of the second point of the first line
   * @param x3 the x coordinate of the first point of the second line
   * @param y3 the y coordinate of the first point of the second line
   * @param x4 the x coordinate of the second point of the second line
   * @param y4 the y coordinate of the second point of the second line
   * @return   the intersection point of the two lines.
   */
  public static Point2D findLineLineIntersection(double x1, double y1,
                                                 double x2, double y2,
                                                 double x3, double y3,
                                                 double x4, double y4) {
    double d12 = determinant(x1, y1, x2, y2);
    double d34 = determinant(x3, y3, x4, y4);
    double diffx1x2 = x1 - x2;
    double diffy1y2 = y1 - y2;
    double diffx3x4 = x3 - x4;
    double diffy3y4 = y3 - y4;
    double d1234 = determinant(diffx1x2, diffy1y2, diffx3x4, diffy3y4);
    if(d1234 == 0) {
      // This is tricky: it means that one of the endpoints of one of the
      // lines is on the other line.  So, let's find which one it is and
      // if it is more than one, find the one closest to x1,y1.
      List<Point2D> candidates = new ArrayList<Point2D>(4);
      candidates.add(new Point2D.Double(x1,y1));
      candidates.add(new Point2D.Double(x2,y2));
      candidates.add(new Point2D.Double(x3,y3));
      candidates.add(new Point2D.Double(x4,y4));
      Line2D l1 = new Line2D.Double(x1,y1,x2,y2);
      Line2D l2 = new Line2D.Double(x3,y3,x4,y4);
      Point2D retval = null;
      double dist = Double.MAX_VALUE;
      // Go through all the Points
      for(Point2D p : candidates) {
        // If this Point is on both segments
        if(l1.ptSegDist(p) == 0 && l2.ptSegDist(p) == 0) {
          // And it is closer to x1,y1 than any of the other ones so far
          if(l1.getP1().distance(p) < dist) {
            // Then save it
            retval = p;
            // and its distance for future comparisons
            dist = l1.getP1().distance(p);
          }
        }
      }
      return retval;
    } else {
      double x = determinant(d12, diffx1x2, d34, diffx3x4) / d1234;
      double y = determinant(d12, diffy1y2, d34, diffy3y4) / d1234;
      return new Point2D.Double(x, y);
    }
  }


  /**
   * Given a polygonal Shape, return a list of lists of points that are the
   * vertices of the closed polygonal sub-shapes.
   *
   * @param s the polygonal Shape
   * @return  the lists of sub-shape vertices
   * @throws IllegalArgumentException if the Shape is not polygonal
   */
  public static List<List<Point2D>> polygonalSubShapeVertices(Shape s) {
    List<List<Point2D>> answ = new ArrayList<List<Point2D>>();
    List<Point2D> currList = new ArrayList<Point2D>();
    int lastMoveType = PathIterator.SEG_MOVETO;
    double[] pts = new double[6];
    for(PathIterator iter = s.getPathIterator(new AffineTransform());
        !iter.isDone(); iter.next()) {
      int moveType = iter.currentSegment(pts);
      switch(moveType) {
      case PathIterator.SEG_MOVETO:
        if(lastMoveType == PathIterator.SEG_CLOSE) {
          currList.remove(0);
        }
        break;
      case PathIterator.SEG_LINETO:
        break;
      case PathIterator.SEG_CLOSE:
        answ.add(currList);
        currList = new ArrayList<Point2D>();
        break;
      default:
        throw new IllegalArgumentException("Shape is not polygonal!");
      }
      currList.add(new Point2D.Double(pts[0], pts[1]));
      lastMoveType = moveType;
    }
    return answ;
  }

  /**
   * Given a polygonal Shape, return a list of segments describing its
   * perimeter.
   *
   * @param s the polygonal Shape
   * @return  a list of segments describing the perimeter of the Shape
   * @throws IllegalArgumentException if the Shape is not polygonal
   */
  public static List<Line2D> polygonalShapePerimeterSegments(Shape s) {
    List<Line2D> perimeterSegments = new ArrayList<Line2D>();
    for(List<Point2D> vtcs: polygonalSubShapeVertices(s)) {
      for(int i = 0; i < vtcs.size(); i++) {
        Point2D p1 = vtcs.get(i);
        Point2D p2 = vtcs.get((i + 1) % vtcs.size());
        perimeterSegments.add(new Line2D.Double(p1, p2));
      }
    }
    return perimeterSegments;
  }

  /**
   * Given a polygonal, non-overlapping Shape, return the areas of the closed
   * portions of the shape.  If the Shape is not polygonal or it overlaps
   * itself, behavior is undefined.
   *
   * @param s the polygonal Shape
   * @return  the areas of the closed portions of the Shape
   */
  public static List<Double> polygonalShapeAreas(Shape s) {
    List<Double> answ = new ArrayList<Double>();
    for(List<Point2D> vtcs: polygonalSubShapeVertices(s)) {
      double twiceCurrArea = 0;
      for(int i = 0; i < vtcs.size(); i++) {
        Point2D p1 = vtcs.get(i);
        Point2D p2 = vtcs.get((i + 1) % vtcs.size());
        twiceCurrArea += (p1.getX() * p2.getY() - p2.getX() * p1.getY());
      }
      answ.add(twiceCurrArea/2);
    }
    return answ;
  }

  /**
   * Given a polygonal Shape, return the centroid of the shape. If the
   * Shape is not polygonal, behavior is undefined.
   *
   * @param s the polygonal Shape
   * @return  the centroid of the Shape
   */
  public static Point2D polygonalShapeCentroid(Shape s) {
    List<Double> pAreas = polygonalShapeAreas(s);
    List<List<Point2D>> pVtcs = polygonalSubShapeVertices(s);
    double totalArea = 0;
    // Weighted x coordinate of centroid
    double cx = 0;
    // Weighted y coordinate of centroid
    double cy = 0;
    // For each subshape, get its centroid and add its weighted coordinates
    // too our totals (cx and cy).
    for(int i = 0; i < pAreas.size(); i++) {
      totalArea += Math.abs(pAreas.get(i));
      // Find the centroid of each sub-area
      double cxi = 0;
      double cyi = 0;
      List<Point2D> vtcs = pVtcs.get(i);
      for(int j = 0; j < vtcs.size(); j++) {
        Point2D p1 = vtcs.get(j);
        Point2D p2 = vtcs.get((j + 1) % vtcs.size());
        cxi += (p1.getX() + p2.getX()) *
                     (p1.getX() * p2.getY() - p2.getX() * p1.getY());
        cyi += (p1.getY() + p2.getY()) *
                     (p1.getX() * p2.getY() - p2.getX() * p1.getY());
      }
      // Centroid is (cxi/(6 * area), cyi/(6 * area)), but
      // we want to weight by area, so we are not going to divide by it.  We
      // do, however, need the sign to make sure it comes out with the correct
      // sign.
      cx += Math.signum(pAreas.get(i)) * cxi / 6;
      cy += Math.signum(pAreas.get(i)) * cyi / 6;
    }
    return new Point2D.Double(cx / totalArea, cy / totalArea);
  }


  /**
   * Find the Area corresponding to the provided Shape with all
   * holes filled in.  Takes the union of all the subareas according
   * to {@link #subareas(Shape s)}.
   *
   * @param s the Shape to fill in
   * @return  an Area representing the Shape with all holes filled in
   */
  public static Area filledArea(Shape s) {
    Area answ = new Area();
    for(Area a : subareas(s)) {
      answ.add(a);
    }
    return answ;
  }

  /**
   * Find all the subareas of a Shape.  This means that if a Shape
   * is defined as a Shape with a piece missing from it, this will
   * return both an Area for the full Shape as well as one for
   * the hole in the middle.  If multiple pieces exist, they will
   * each be returned individually.
   *
   * @param s the Shape to deconstruct.
   * @return  a List of subareas that make up the Shape
   */
  public static List<Area> subareas(Shape s) {
    List<Area> answ = new ArrayList<Area>();
    for(List<Point2D> vtcs: polygonalSubShapeVertices(s)) {
      GeneralPath path = new GeneralPath();
      path.moveTo((float)vtcs.get(0).getX(), (float)vtcs.get(0).getY());
      for(int i = 1; i < vtcs.size(); i++) {
        path.lineTo((float)vtcs.get(i).getX(), (float)vtcs.get(i).getY());
      }
      path.closePath();
      answ.add(new Area(path));
    }
    return answ;
  }

  /**
   * Determine the angle between two angles.  This includes the cases where
   * the shortest angle between the two crosses the positive X axis.
   *
   * @param ang1 the first angle
   * @param ang2 the second angle
   * @return     the angle between the two given angles
   */
  public static double angleDiff(double ang1, double ang2) {
    double absoluteDifference = Math.abs(ang1 - ang2);
    return Math.min(absoluteDifference, 2 * Math.PI - absoluteDifference);
  }


  /////////////////////////////////
  // PRIVATE STATIC METHODS
  /////////////////////////////////

  /**
   * Compute the determinant of a 2x2 matrix.
   * | a b |
   * | c d |
   */
  private static double determinant(double a, double b, double c, double d) {
    return a * d - b * c;
  }


  /////////////////////////////////
  // CLASS CONSTRUCTORS
  /////////////////////////////////

  /** This class should never be instantiated. */
  private GeomMath(){};

}
