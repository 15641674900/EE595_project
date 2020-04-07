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
package aim4.driver.pilot;

import aim4.config.Debug;
import aim4.config.SimConfig;
import aim4.driver.AutoDriver;
import aim4.driver.DriverUtil;
import aim4.map.lane.Lane;
import aim4.msg.i2v.Confirm;
import aim4.vehicle.AutoVehicleDriverView;
import aim4.vehicle.VehicleUtil;

/**
 * An agent that pilots a {@link AutoVehicleDriverView} autonomously. This agent
 * attempts to emulate the behavior of a real-world autonomous driver agent in
 * terms of physically controlling the Vehicle.
 */
public class V2IPilot extends BasicPilot {

  public static class ReservationParameter {

    /**
     * The Lane in which the Vehicle will depart the intersection.
     */
    private Lane departureLane;

    /////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////

    /**
     * Create a reservation parameter object
     */
    public ReservationParameter(Confirm msg) {
      // The Lane in which the Vehicle should arrive at the intersection.
      Debug.currentMap.getLaneRegistry().get(msg.getArrivalLaneID());
      this.departureLane = Debug.currentMap.getLaneRegistry().get(msg.getDepartureLaneID());
    }

    /**
     * Get the Lane in which this driver agent's Vehicle should
     * arrive to comply with the reservation this driver agent is holding. If
     * the driver agent is not holding a reservation, the return value is not
     * defined.
     *
     * @return the departure Lane for the reservation this driver agent is
     *         holding
     */
    public Lane getDepartureLane() {
      return departureLane;
    }

  }

  // ///////////////////////////////
  // CONSTANTS
  // ///////////////////////////////

  /**
   * The minimum distance to maintain between the Vehicle controlled by this
   * AutonomousPilot and the one in front of it. {@value} meters.
   */
  public static final double MINIMUM_FOLLOWING_DISTANCE = 0.5; // meters

  /**
   * The default shortest distance before an intersection at which the vehicle
   * stops if the vehicle can't enter the intersection immediately.
   */
  public static double DEFAULT_STOP_DISTANCE_BEFORE_INTERSECTION = 1.0;

  /**
   * The distance, expressed in units of the Vehicle's velocity, at which to
   * switch to a new lane when turning. {@value} seconds.
   */
  public static final double TRAVERSING_LANE_CHANGE_LEAD_TIME = 1.5; // sec

  // ///////////////////////////////
  // PRIVATE FIELDS
  // ///////////////////////////////

  private AutoVehicleDriverView vehicle;

  private AutoDriver driver;


  // ///////////////////////////////
  // CONSTRUCTORS
  // ///////////////////////////////

  /**
   * Create an pilot to control a vehicle.
   *
   * @param vehicle      the vehicle to control
   * @param driver       the driver
   */
  public V2IPilot(AutoVehicleDriverView vehicle, AutoDriver driver) {
    this.vehicle = vehicle;
    this.driver = driver;
  }

  // ///////////////////////////////
  // PUBLIC METHODS
  // ///////////////////////////////


  /**
   * Get the vehicle this pilot controls.
   */
  @Override
  public AutoVehicleDriverView getVehicle() {
    return vehicle;
  }

  /**
   * Get the driver this pilot controls.
   */
  @Override
  public AutoDriver getDriver() {
    return driver;
  }

  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  // steering

  /**
   * Set the steering action when the vehicle is traversing an intersection.
   */
  public void takeSteeringActionForTraversing(ReservationParameter rp) {
    // If we're not already in the departure lane
    if (driver.getCurrentLane() != rp.getDepartureLane()) {
      // If we're changing to a different Road
      if (Debug.currentMap.getRoad(driver.getCurrentLane()) !=
        Debug.currentMap.getRoad(rp.getDepartureLane())) {
        // Find out how far from it we are
        double distToLane =
          rp.getDepartureLane().nearestDistance(vehicle.gaugePosition());
        // If we're close enough...
        double traversingLaneChangeDistance =
          TRAVERSING_LANE_CHANGE_LEAD_TIME * vehicle.gaugeVelocity();
        if (distToLane < traversingLaneChangeDistance) {
          // Change to it
          driver.setCurrentLane(rp.getDepartureLane());
        }
      }
    } // else, we're changing to the same Road, so we need a
      // different criterion... in this case none
    followCurrentLane();
    // Use the basic lane-following behavior
  }

  // throttle actions

  /**
   * The simple throttle action.
   */
  public void simpleThrottleAction() {
    cruise();
    dontHitVehicleInFront();
    dontEnterIntersection();

  }

  /**
   * Stop before hitting the car in front of us.
   *
   */
  private void dontHitVehicleInFront() {
    double stoppingDistance =
      VehicleUtil.calcDistanceToStop(vehicle.gaugeVelocity(),
                                     vehicle.getSpec().getMaxDeceleration());
    double followingDistance = stoppingDistance + MINIMUM_FOLLOWING_DISTANCE;
    if (VehicleUtil.distanceToCarInFront(vehicle) < followingDistance) {
      vehicle.slowToStop();
    }
  }

  /**
   * Stop before entering the intersection.
   */
  private void dontEnterIntersection() {
    double stoppingDistance = distIfStopNextTimeStep();
    double minDistanceToIntersection =
      stoppingDistance + DEFAULT_STOP_DISTANCE_BEFORE_INTERSECTION;
    if (vehicle.getDriver().distanceToNextIntersection() <
        minDistanceToIntersection) {
      vehicle.slowToStop();
    }
  }

  /**
   * Determine how far the vehicle will go if it waits until the next time
   * step to stop.
   *
   * e
   * @return How far the vehicle will go if it waits until the next time
   *         step to stop
   */
  private double distIfStopNextTimeStep() {
    double distIfAccel = VehicleUtil.calcDistanceIfAccel(
        vehicle.gaugeVelocity(),
        vehicle.getSpec().getMaxAcceleration(),  // TODO: why max accel here?
        DriverUtil.calculateMaxFeasibleVelocity(vehicle),
        SimConfig.TIME_STEP);
    double distToStop = VehicleUtil.calcDistanceToStop(
        speedNextTimeStepIfAccel(),
        vehicle.getSpec().getMaxDeceleration());
    return distIfAccel + distToStop;
  }


  /**
   * Calculate the velocity of the vehicle at the next time step, if we choose
   * to accelerate at this time step.
   *
   *
   * @return the velocity of the vehicle at the next time step, if we choose
   *         to accelerate at this time step
   */
  private double speedNextTimeStepIfAccel(){
    // Our speed at the next time step will be either the target speed or as fast as we can go, whichever is smaller
    return Math.min(DriverUtil.calculateMaxFeasibleVelocity(vehicle),
                    vehicle.gaugeVelocity() +
                      vehicle.getSpec().getMaxAcceleration() *
                      SimConfig.TIME_STEP);
  }

}
