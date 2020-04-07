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
package aim4.im;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import aim4.map.Road;
import aim4.map.lane.Lane;
import aim4.util.Registry;
import aim4.util.Util;
import aim4.vehicle.VehicleSimView;

/**
 * An agent to manage an intersection. This is an abstract class
 * that sets up the properties of the intersection when it is created.
 */
public class IntersectionManager {

  /////////////////////////////////
  // PRIVATE FIELDS
  /////////////////////////////////

  /** The ID number of this intersection manager. */
  protected int id;

  /** the current time of the intersection manager */
  protected double currentTime;

  /**
   * The intersection managed by this intersection manager.
   */
  private Intersection intersection;
  /**
   * The path model of the intersection.
   */
  private TrackModel trackModel;


  /////////////////////////////////
  // CLASS CONSTRUCTORS
  /////////////////////////////////

  /**
   * Create an intersection manager.
   *
   * @param intersection  an intersection
   * @param trackModel    a path model of the intersection
   * @param currentTime   the current time
   * @param imRegistry    an intersection manager registry
   */
  public IntersectionManager(Intersection intersection,
                             TrackModel trackModel,
                             double currentTime,
                             Registry<IntersectionManager> imRegistry) {
    assert(trackModel.getIntersection() == intersection);
    this.intersection = intersection;
    this.trackModel = trackModel;
    this.currentTime = currentTime;
    this.id = imRegistry.register(this);

    // Register the intersection manager with the lanes
    registerWithLanes();
  }

  /**
   * Register this IntersectionManager with each of the Lanes that it manages.
   */
  private void registerWithLanes() {
    for(Lane lane : intersection.getLanes()) {
      lane.getLaneIM().registerIntersectionManager(this);
    }
  }

  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  /**
   * Take any actions for a certain period of time.
   *
   * @param timeStep  the size of the time step to simulate, in seconds
   */
  public void act(double timeStep) {
    currentTime += timeStep;
  }

  /**
   * Get the unique ID number of this IntersectionManager.
   *
   * @return the ID number of this IntersectionManager
   */
  public int getId() {
    return id;
  }

  /**
   * Get the current time.
   *
   * @return the current time.
   */
  public double getCurrentTime() {
    return currentTime;
  }

  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  // intersection

  /**
   * Get the intersection managed by this intersection manager.
   *
   * @return the intersection managed by this intersection manager
   */
  public Intersection getIntersection() {
    return intersection;
  }

  /**
   * Get the track model.
   *
   * @return the track model
   */
  public TrackModel getTrackModel() {
    return trackModel;
  }


  /**
   * Whether or not this IntersectionManager manages the given Lane.
   *
   * @param l the Lane
   * @return  whether this IntersectionManager manages the given Lane
   */
  public boolean manages(Lane l) {
    return intersection.getLanes().contains(l);
  }


  /**
   * Determine whether the given Vehicle is currently entirely contained
   * within the Area governed by this IntersectionManager.
   *
   * @param vehicle the Vehicle
   * @return        whether the Vehicle is currently entirely contained within
   *                the Area governed by this IntersectionManager
   */
  public boolean contains(VehicleSimView vehicle) {
    // Get all corners of the vehicle and make sure they are inside the
    // intersection.
    for(Point2D corner : vehicle.getCornerPoints()) {
      if (!intersection.getArea().contains(corner)) {
        return false;
      }
    }
    // If all corners are inside, the whole thing is considered inside.
    return true;
  }

  /**
   * Determine whether the given Rectangle intersects the Area governed
   * by this IntersectionManager.
   *
   * @param rectangle the Rectangle
   * @return          whether the Rectangle intersects the Area governed by
   *                  this IntersectionManager
   */
  public boolean intersects(Rectangle2D rectangle) {
    // Just call the Area method, so we don't have to clone the area.
    // Make sure not to use "intersect" which is destructive.
    return intersection.getArea().intersects(rectangle);
  }


  /**
   * Get the distance from the entry of the given Lane, to the departure of
   * the other given Lane, if traveling along segments through their point
   * of intersection.
   *
   * @param arrival   the arrival Lane
   * @param departure the departure Lane
   * @return          the distance from the entry of the arrival Lane to the
   *                  exit of the departure Lane through their intersection
   */
  public double traversalDistance(Lane arrival, Lane departure) {
    return trackModel.traversalDistance(arrival, departure);
  }



}