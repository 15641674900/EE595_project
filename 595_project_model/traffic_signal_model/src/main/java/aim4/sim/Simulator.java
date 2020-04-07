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
package aim4.sim;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import aim4.config.Debug;
import aim4.config.DebugPoint;
import aim4.config.SimConfig;
import aim4.driver.AutoDriver;
import aim4.driver.DriverSimView;
import aim4.gui.Viewer;
import aim4.im.IntersectionManager;
import aim4.im.v2i.V2IManager;
import aim4.map.DataCollectionLine;
import aim4.map.BasicMap;
import aim4.map.Road;
import aim4.map.SpawnPoint;
import aim4.map.SpawnPoint.SpawnSpec;
import aim4.map.lane.Lane;
import aim4.msg.i2v.I2VMessage;
import aim4.msg.v2i.V2IMessage;
import aim4.vehicle.AutoVehicleSimView;
import aim4.vehicle.BasicAutoVehicle;
import aim4.vehicle.VehicleSpec;
import aim4.vehicle.VinRegistry;
import aim4.vehicle.VehicleSimView;

/**
 * The autonomous drivers only simulator.
 */
public class Simulator {

  /////////////////////////////////
  // NESTED CLASSES
  /////////////////////////////////

  /**
   * The result of a simulation step.
   */
  public static class SimStepResult  {

    /** The VIN of the completed vehicles in this time step */
    List<Integer> completedVINs;

    /**
     * Create a result of a simulation step
     *
     * @param completedVINs  the VINs of completed vehicles.
     */
    public SimStepResult(List<Integer> completedVINs) {
      this.completedVINs = completedVINs;
    }

    /**
     * Get the list of VINs of completed vehicles.
     *
     * @return the list of VINs of completed vehicles.
     */
    public List<Integer> getCompletedVINs() {
      return completedVINs;
    }
  }

  /////////////////////////////////
  // PRIVATE FIELDS
  /////////////////////////////////

  /** The map */
  private BasicMap basicMap;
  /** All active vehicles, in form of a map from VINs to vehicle objects. */
  private Map<Integer,VehicleSimView> vinToVehicles;
  /** The current time */
  private double currentTime;
  /** The number of completed vehicles */
  private int numOfCompletedVehicles;
  /** The total number of bits transmitted by the completed vehicles */
  private int totalBitsTransmittedByCompletedVehicles;
  /** The total number of bits received by the completed vehicles */
  private int totalBitsReceivedByCompletedVehicles;

  private Viewer viewer;


  /////////////////////////////////
  // CLASS CONSTRUCTORS
  /////////////////////////////////

  /**
   * Create an instance of the simulator.
   *
   * @param basicMap             the map of the simulation
   */
  public Simulator(BasicMap basicMap, Viewer viewer) {
    this.basicMap = basicMap;
    this.vinToVehicles = new HashMap<Integer,VehicleSimView>();
    currentTime = 0.0;
    this.viewer = viewer;
    numOfCompletedVehicles = 0;
    totalBitsTransmittedByCompletedVehicles = 0;
    totalBitsReceivedByCompletedVehicles = 0;
  }

  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  // the main loop

  public synchronized SimStepResult step(double timeStep) throws IOException {
    detectepisode();
    spawnVehicles(timeStep);
    provideSensorInput();
    letDriversAct();
    letIntersectionManagersAct(timeStep);
    communication();
    moveVehicles(timeStep);
    List<Integer> completedVINs = cleanUpCompletedVehicles();
    currentTime += timeStep;
    checkClocks();     // debug
    outputInfToFile(timeStep);
    return new SimStepResult(completedVINs);
  }


  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  // information retrieval


  public synchronized BasicMap getMap() {
    return basicMap;
  }


  public synchronized double getSimulationTime() {
    return (int)(currentTime * SimConfig.CYCLES_PER_SECOND);
  }


  public synchronized int getNumCompletedVehicles() {
    return numOfCompletedVehicles;
  }


  public synchronized double getAvgBitsTransmittedByCompletedVehicles() {
    if (numOfCompletedVehicles > 0) {
      return ((double)totalBitsTransmittedByCompletedVehicles)
             / numOfCompletedVehicles;
    } else {
      return 0.0;
    }
  }

  public synchronized double getAvgBitsReceivedByCompletedVehicles() {
    if (numOfCompletedVehicles > 0) {
      return ((double)totalBitsReceivedByCompletedVehicles)
             / numOfCompletedVehicles;
    } else {
      return 0.0;
    }
  }

  public synchronized Set<VehicleSimView> getActiveVehicles() {
    return new HashSet<VehicleSimView>(vinToVehicles.values());
  }

  /////////////////////////////////
  // PRIVATE METHODS
  /////////////////////////////////

  /////////////////////////////////
  // STEP 1
  /////////////////////////////////

  /**
   * Spawn vehicles.
   *
   * @param timeStep  the time step
   */
  private void spawnVehicles(double timeStep) {
    for(SpawnPoint spawnPoint : basicMap.getSpawnPoints()) {
//    SpawnPoint spawnPoint = basicMap.getSpawnPoints().get(0);
      List<SpawnSpec> spawnSpecs = spawnPoint.act(timeStep);
      if (!spawnSpecs.isEmpty()) {
        if (canSpawnVehicle(spawnPoint)) {
          for(SpawnSpec spawnSpec : spawnSpecs) {
            VehicleSimView vehicle = makeVehicle(spawnPoint, spawnSpec);
            VinRegistry.registerVehicle(vehicle); // Get vehicle a VIN number
            vinToVehicles.put(vehicle.getVIN(), vehicle);
            break; // only handle the first spawn vehicle
                   // TODO: need to fix this
          }
        } // else ignore the spawnSpecs and do nothing
      }
    }
  }


  /**
   * Whether a spawn point can spawn any vehicle
   *
   * @param spawnPoint  the spawn point
   * @return Whether the spawn point can spawn any vehicle
   */
  private boolean canSpawnVehicle(SpawnPoint spawnPoint) {
    // TODO: can be made much faster.
    Rectangle2D noVehicleZone = spawnPoint.getNoVehicleZone();
    for(VehicleSimView vehicle : vinToVehicles.values()) {
      if (vehicle.getShape().intersects(noVehicleZone)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Create a vehicle at a spawn point.
   *
   * @param spawnPoint  the spawn point
   * @param spawnSpec   the spawn specification
   * @return the vehicle
   */
  private VehicleSimView makeVehicle(SpawnPoint spawnPoint,
                                     SpawnSpec spawnSpec) {
    VehicleSpec spec = spawnSpec.getVehicleSpec();
    Lane lane = spawnPoint.getLane();
    Road road = spawnPoint.getRoad();
    // Now just take the minimum of the max velocity of the vehicle, and
    // the speed limit in the lane
    double initVelocity = Math.min(spec.getMaxVelocity(), lane.getSpeedLimit());
    // Obtain a Vehicle
    AutoVehicleSimView vehicle = new BasicAutoVehicle(spec,
                           spawnPoint.getPosition(),
                           spawnPoint.getHeading(),
                           spawnPoint.getSteeringAngle(),
                           initVelocity, // velocity
                           initVelocity,  // target velocity
                           spawnPoint.getAcceleration(),
                           spawnSpec.getSpawnTime());
    // Set the driver
    AutoDriver driver = new AutoDriver(vehicle, basicMap);
    driver.setCurrentLane(lane);
    driver.setCurrentRoad(road);
    driver.setSpawnPoint(spawnPoint);
    driver.setDestination(spawnSpec.getDestinationRoad());
    vehicle.setDriver(driver);

    return vehicle;
  }


  /////////////////////////////////
  // STEP 2
  /////////////////////////////////

  /**
   * Compute the lists of vehicles of all lanes.
   *
   * @return a mapping from lanes to lists of vehicles sorted by their
   *         distance on their lanes
   */
  private Map<Lane,SortedMap<Double,VehicleSimView>> computeVehicleLists() {
    // Set up the structure that will hold all the Vehicles as they are
    // currently ordered in the Lanes
    Map<Lane,SortedMap<Double,VehicleSimView>> vehicleLists = new HashMap<Lane,SortedMap<Double,VehicleSimView>>();
    for(Road road : basicMap.getRoads()) {
      for(Lane lane : road.getLanes()) {
        vehicleLists.put(lane, new TreeMap<Double,VehicleSimView>());
      }
    }
    // Now add each of the Vehicles, but make sure to exclude those that are
    // already inside (partially or entirely) the intersection
    for(VehicleSimView vehicle : vinToVehicles.values()) {
      // Find out what lanes it is in.
      Set<Lane> lanes = vehicle.getDriver().getCurrentlyOccupiedLanes();
      for(Lane lane : lanes) {
        // Find out what IntersectionManager is coming up for this vehicle
        IntersectionManager im =
          lane.getLaneIM().nextIntersectionManager(vehicle.getPosition());
        // Only include this Vehicle if it is not in the intersection.
        if(lane.getLaneIM().distanceToNextIntersection(vehicle.getPosition())>0
            || im == null || !im.intersects(vehicle.getShape().getBounds2D())) {
          // Now find how far along the lane it is.
          double dst = lane.distanceAlongLane(vehicle.getPosition());
          // Now add it to the map.
          vehicleLists.get(lane).put(dst, vehicle);
        }
      }
    }
    // Now consolidate the lists based on lanes
    for(Road road : basicMap.getRoads()) {
      for(Lane lane : road.getLanes()) {
        // We may have already removed this Lane from the map
        if(vehicleLists.containsKey(lane)) {
          Lane currLane = lane;
          // Now run through the lanes
          while(currLane.hasNextLane()) {
            currLane = currLane.getNextLane();
            // Put everything from the next lane into the original lane
            // and remove the mapping for the next lane
            vehicleLists.get(lane).putAll(vehicleLists.remove(currLane));
          }
        }
      }
    }

    return vehicleLists;
  }

  /**
   * Compute the next vehicles of all vehicles.
   *
   * @param vehicleLists  a mapping from lanes to lists of vehicles sorted by
   *                      their distance on their lanes
   * @return a mapping from vehicles to next vehicles
   */
  private Map<VehicleSimView, VehicleSimView> computeNextVehicle(
    Map<Lane,SortedMap<Double,VehicleSimView>> vehicleLists) {
    // At this point we should only have mappings for start Lanes, and they
    // should include all the Lanes they run into.  Now we need to turn this
    // into a hash map that maps Vehicles to the next vehicle in the Lane
    // or any Lane the Lane runs into
    Map<VehicleSimView, VehicleSimView> nextVehicle =
      new HashMap<VehicleSimView,VehicleSimView>();
    // For each of the ordered lists of vehicles
    for(SortedMap<Double,VehicleSimView> vehicleList : vehicleLists.values()) {
      VehicleSimView lastVehicle = null;
      // Go through the Vehicles in order of their position in the Lane
      for(VehicleSimView currVehicle : vehicleList.values()) {
        if(lastVehicle != null) {
          // Create the mapping from the previous Vehicle to the current one
          nextVehicle.put(lastVehicle,currVehicle);
        }
        lastVehicle = currVehicle;
      }
    }

    return nextVehicle;
  }

  /**
   * Provide each vehicle with sensor information to allow it to make
   * decisions.  This works first by making an ordered list for each Lane of
   * all the vehicles in that Lane, in order from the start of the Lane to
   * the end of the Lane.  We must make sure to leave out all vehicles that
   * are in the intersection.  We must also concatenate the lists for lanes
   * that feed into one another.  Then, for each vehicle, depending on the
   * state of its sensors, we provide it with the appropriate sensor input.
   */
  private void provideSensorInput() {
    Map<Lane,SortedMap<Double,VehicleSimView>> vehicleLists =
      computeVehicleLists();
    Map<VehicleSimView, VehicleSimView> nextVehicle =
      computeNextVehicle(vehicleLists);

    provideIntervalInfo(nextVehicle);
    provideVehicleTrackingInfo(vehicleLists);
  }

  /**
   * Provide sensing information to the intervalometers of all vehicles.
   *
   * @param nextVehicle  a mapping from vehicles to next vehicles
   */
  private void provideIntervalInfo(
    Map<VehicleSimView, VehicleSimView> nextVehicle) {

    // Now that we have this list set up, let's provide input to all the
    // Vehicles.
    for(VehicleSimView vehicle: vinToVehicles.values()) {
      // If the vehicle is autonomous
      if (vehicle instanceof AutoVehicleSimView) {
        AutoVehicleSimView autoVehicle = (AutoVehicleSimView)vehicle;

        switch(autoVehicle.getLRFMode()) {
        case DISABLED:
          // Find the interval to the next vehicle
          double interval;
          // If there is a next vehicle, then calculate it
          if(nextVehicle.containsKey(autoVehicle)) {
            // It's the distance from the front of this Vehicle to the point
            // at the rear of the Vehicle in front of it
            interval = calcInterval(autoVehicle, nextVehicle.get(autoVehicle));
          } else { // Otherwise, just set it to the maximum possible value
            interval = Double.MAX_VALUE;
          }
          // Now actually record it in the vehicle
          autoVehicle.getIntervalometer().record(interval);
          autoVehicle.setLRFSensing(false); // Vehicle is not using
                                            // the LRF sensor
          break;
        case LIMITED:
          // FIXME
          autoVehicle.setLRFSensing(true); // Vehicle is using the LRF sensor
          break;
        case ENABLED:
          // FIXME
          autoVehicle.setLRFSensing(true); // Vehicle is using the LRF sensor
          break;
        default:
          throw new RuntimeException("Unknown LRF Mode: " +
                                     autoVehicle.getLRFMode().toString());
        }
      }
    }
  }

  /**
   * Provide tracking information to vehicles.
   *
   * @param vehicleLists  a mapping from lanes to lists of vehicles sorted by
   *                      their distance on their lanes
   */
  private void provideVehicleTrackingInfo(
    Map<Lane, SortedMap<Double, VehicleSimView>> vehicleLists) {
    // Vehicle Tracking
    for(VehicleSimView vehicle: vinToVehicles.values()) {
      // If the vehicle is autonomous
      if (vehicle instanceof AutoVehicleSimView) {
        AutoVehicleSimView autoVehicle = (AutoVehicleSimView)vehicle;

        if (autoVehicle.isVehicleTracking()) {
          DriverSimView driver = autoVehicle.getDriver();
          Lane targetLane = autoVehicle.getTargetLaneForVehicleTracking();
          Point2D pos = autoVehicle.getPosition();
          double dst = targetLane.distanceAlongLane(pos);

          // initialize the distances to infinity
          double frontDst = Double.MAX_VALUE;
          double rearDst = Double.MAX_VALUE;
          VehicleSimView frontVehicle = null ;
          VehicleSimView rearVehicle = null ;

          // only consider the vehicles on the target lane
          SortedMap<Double,VehicleSimView> vehiclesOnTargetLane =
            vehicleLists.get(targetLane);

          // compute the distances and the corresponding vehicles
          try {
            double d = vehiclesOnTargetLane.tailMap(dst).firstKey();
            frontVehicle = vehiclesOnTargetLane.get(d);
            frontDst = (d-dst)-frontVehicle.getSpec().getLength();
          } catch(NoSuchElementException e) {
            frontVehicle = null;
          }
          try {
            double d = vehiclesOnTargetLane.headMap(dst).lastKey();
            rearVehicle = vehiclesOnTargetLane.get(d);
            rearDst = dst-d;
          } catch(NoSuchElementException ignored) {
          }

          // assign the sensor readings

          autoVehicle.getFrontVehicleDistanceSensor().record(frontDst);
          autoVehicle.getRearVehicleDistanceSensor().record(rearDst);

          // assign the vehicles' velocities

          if(frontVehicle!=null) {
            autoVehicle.getFrontVehicleSpeedSensor().record(
                frontVehicle.getVelocity());
          } else {
            autoVehicle.getFrontVehicleSpeedSensor().record(Double.MAX_VALUE);
          }
          if(rearVehicle!=null) {
            autoVehicle.getRearVehicleSpeedSensor().record(
                rearVehicle.getVelocity());
          } else {
            autoVehicle.getRearVehicleSpeedSensor().record(Double.MAX_VALUE);
          }

          // show the section on the viewer
          if (Debug.isTargetVIN(driver.getVehicle().getVIN())) {
            Point2D p1 = targetLane.getPointAtNormalizedDistance(
                Math.max((dst-rearDst)/targetLane.getLength(),0.0));
            Point2D p2 = targetLane.getPointAtNormalizedDistance(
                Math.min((frontDst+dst)/targetLane.getLength(),1.0));
            Debug.addLongTermDebugPoint(
              new DebugPoint(p2, p1, "cl", Color.RED.brighter()));
          }
        }
      }
    }

  }


  /**
   * Calculate the distance between vehicle and the next vehicle on a lane.
   *
   * @param vehicle      the vehicle
   * @param nextVehicle  the next vehicle
   * @return the distance between vehicle and the next vehicle on a lane
   */
  private double calcInterval(VehicleSimView vehicle,
                              VehicleSimView nextVehicle) {
    // From Chiu: Kurt, if you think this function is not okay, probably
    // we should talk to see what to do.
    Point2D pos = vehicle.getPosition();
    if(nextVehicle.getShape().contains(pos)) {
      return 0.0;
    } else {
      // TODO: make it more efficient
      double interval = Double.MAX_VALUE ;
      for(Line2D edge : nextVehicle.getEdges()) {
        double dst = edge.ptSegDist(pos);
        if(dst < interval){
          interval = dst;
        }
      }
      return interval;
    }
  }


  /////////////////////////////////
  // STEP 3
  /////////////////////////////////

  /**
   * Allow each driver to act.
   */
  private void letDriversAct() {
    for(VehicleSimView vehicle : vinToVehicles.values()) {
      vehicle.getDriver().act();
    }
  }

  /////////////////////////////////
  // STEP 4
  /////////////////////////////////

  /**
   * Allow each intersection manager to act.
   *
   * @param timeStep  the time step
   */
  private void letIntersectionManagersAct(double timeStep) {
    for(IntersectionManager im : basicMap.getIntersectionManagers()) {
      im.act(timeStep);
    }
  }

  /////////////////////////////////
  // STEP 5
  /////////////////////////////////

  /**
   * Deliver the V2I and I2V messages.
   */
  private void communication() {
    deliverV2IMessages();
    deliverI2VMessages();
  }

  /**
   * Deliver the V2I messages.
   */
  private void deliverV2IMessages() {
    // Go through each vehicle and deliver each of its messages
    for(VehicleSimView vehicle : vinToVehicles.values()) {
      // Start with V2I messages
      if (vehicle instanceof AutoVehicleSimView) {
        AutoVehicleSimView sender = (AutoVehicleSimView)vehicle;
        Queue<V2IMessage> v2iOutbox = sender.getV2IOutbox();
        while(!v2iOutbox.isEmpty()) {
          V2IMessage msg = v2iOutbox.poll();
          V2IManager receiver =
            (V2IManager)basicMap.getImRegistry().get(msg.getImId());
          // Calculate the distance the message must travel
          double txDistance =
            sender.getPosition().distance(
                receiver.getIntersection().getCentroid());
          // Find out if the message will make it that far
          if(transmit(txDistance, sender.getTransmissionPower())) {
            // Actually deliver the message
            receiver.receive(msg);
            // Add the delivery to the debugging information
          }
          // Either way, we increment the number of transmitted messages
        }
      }
    }
  }

  /**
   * Deliver the I2V messages.
   */
  private void deliverI2VMessages() {
    // Now deliver all the I2V messages
    for(IntersectionManager im : basicMap.getIntersectionManagers()) {
      V2IManager senderIM = (V2IManager)im;
      for(Iterator<I2VMessage> i2vIter = senderIM.outboxIterator();
          i2vIter.hasNext();) {
        I2VMessage msg = i2vIter.next();
        AutoVehicleSimView vehicle =
          (AutoVehicleSimView)VinRegistry.getVehicleFromVIN(
            msg.getVin());
        // Calculate the distance the message must travel
        assert vehicle != null;
        double txDistance = senderIM.getIntersection().getCentroid().distance(
            vehicle.getPosition());
        // Find out if the message will make it that far
        if(transmit(txDistance, senderIM.getTransmissionPower())) {
          // Actually deliver the message
          vehicle.receive(msg);
        }
      }
      // Done delivering the IntersectionManager's messages, so clear the outbox.
      senderIM.clearOutbox();
    }
  }



  /**
   * Whether the transmission of a message is successful
   *
   * @param distance  the distance of the transmission
   * @param power     the power of the transmission
   * @return whether the transmission of a messsage is successful
   */
  private boolean transmit(double distance, double power) {
    // Simple for now
    return distance <= power;
  }


  /////////////////////////////////
  // STEP 6
  /////////////////////////////////

  /**
   * Move all the vehicles.
   *
   * @param timeStep  the time step
   */
  private void moveVehicles(double timeStep) {
    for(VehicleSimView vehicle : vinToVehicles.values()) {
      Point2D p1 = vehicle.getPosition();
      vehicle.move(timeStep);
      Point2D p2 = vehicle.getPosition();
      for(DataCollectionLine line : basicMap.getDataCollectionLines()) {
        line.intersect(vehicle, currentTime, p1, p2);
      }
      if (Debug.isPrintVehicleStateOfVIN(vehicle.getVIN())) {
        vehicle.printState();
      }
    }
  }


  //          System.out.print("VIN: ");
//          System.out.println(vehicle.getVIN());
//          System.out.print("distance: ");
//          System.out.println(vehicle.getDriver().distanceToNextIntersection());
//          System.out.print("time: ");
//          System.out.println(vehicle.getWaitTime(timeStep, vehicle.getDriver().distanceToNextIntersection()));
//          Road r = basicMap.getRoad(vehicle.getDriver().getCurrentLane());
//          System.out.print("Road name: ");
//          System.out.println(r.getName());
//          System.out.print("Spawn name: ");
//          System.out.println(vehicle.getDriver().getSpawnPoint().getRoad());
//          System.out.print("destination name: ");
//          System.out.println(vehicle.getDriver().getDestination());
//          System.out.print("lane index: ");
//          System.out.println(r.getSpecificLaneIndex(vehicle.getDriver().getCurrentLane()));
  private void outputInfToFile(double timeStep) throws IOException {
    DecimalFormat fnum = new DecimalFormat("##0.00");
    File writename = new File("output.txt");
    BufferedWriter out = new BufferedWriter(new FileWriter(writename, false));
    int time = viewer.time;
    out.write(time+"\n");
    for (VehicleSimView vehicle : vinToVehicles.values()) {
      try {
        Road r = basicMap.getRoad(vehicle.getDriver().getCurrentLane());
//        int time = (int)(currentTime * SimConfig.CYCLES_PER_SECOND);
        String dist=fnum.format(vehicle.getDriver().distanceToNextIntersection());
        if(vehicle.getDriver().distanceToNextIntersection()>146){
          dist = "146.00";
        } else if (vehicle.getDriver().distanceToNextIntersection()<0.9){
          dist = "0.00";
        }
        String beforeIntercetionTime = fnum.format(25*vehicle.getWaitTime(timeStep,
                vehicle.getDriver().distanceToNextIntersection()));
        out.write(time+"\t"+vehicle.getVIN()+"\t");
        out.write(dist+"\t"+ beforeIntercetionTime +"\t");
        out.write(vehicle.getDriver().getSpawnPoint().getRoad()+"\t");
        out.write(vehicle.getDriver().getDestination()+"\t");
        out.write(r.getSpecificLaneIndex(vehicle.getDriver().getCurrentLane())+"\n");
        out.flush();
//        out.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }
  /////////////////////////////////
  // STEP 7
  /////////////////////////////////

  /**
   * Remove all completed vehicles.
   *
   * @return the VINs of the completed vehicles
   */
  private List<Integer> cleanUpCompletedVehicles() {
    List<Integer> completedVINs = new LinkedList<Integer>();

    Rectangle2D mapBoundary = basicMap.getDimensions();

    List<Integer> removedVINs = new ArrayList<Integer>(vinToVehicles.size());
    for(int vin : vinToVehicles.keySet()) {
      VehicleSimView v = vinToVehicles.get(vin);
      // If the vehicle is no longer in the layout
      // TODO: this should be replaced with destination zone.
      if(!v.getShape().intersects(mapBoundary)) {
        // Process all the things we need to from this vehicle
        if (v instanceof AutoVehicleSimView) {
          AutoVehicleSimView v2 = (AutoVehicleSimView)v;
          totalBitsTransmittedByCompletedVehicles += v2.getBitsTransmitted();
          totalBitsReceivedByCompletedVehicles += v2.getBitsReceived();
        }
        removedVINs.add(vin);
      }
    }
    // Remove the marked vehicles
    for(int vin : removedVINs) {
      vinToVehicles.remove(vin);
      completedVINs.add(vin);
      numOfCompletedVehicles++;
    }

    return completedVINs;
  }

  /////////////////////////////////
  // DEBUG
  /////////////////////////////////

  /**
   * Check whether the clocks are in sync.
   */
  private void checkClocks() {
    // Check the clocks for all autonomous vehicles.
    for(VehicleSimView vehicle: vinToVehicles.values()) {
      vehicle.checkCurrentTime(currentTime);
    }
    // Check the clocks for all the intersection managers.
    for(IntersectionManager im : basicMap.getIntersectionManagers()) {
      im.checkCurrentTime(currentTime);
    }
  }


  private void detectepisode() throws IOException {
    if(viewer.time == 3600){
      VinRegistry.reset();   // TODO: should make it part of the simulator
      System.gc();
    }
  }

}
