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
package aim4.config;

import aim4.im.v2i.RequestHandler.TrafficSignalRequestHandler;
import aim4.im.v2i.RequestHandler.TrafficSignalRequestHandler.
    CyclicSignalController;
import aim4.map.GridMap;
import aim4.map.Road;
import aim4.util.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The traffic signal phases.
 */
public class TrafficSignalPhase {

  /** The number of phases */
  private int phaseNum;

  /** The list of the names of the roads that are active in the phase */
  private List<List<Road>> activeRoads;

  /** The duration of the green signals */
  private List<Double> greenDurations;

  /** The duration of the yellow signals */
  private List<Double> yellowDurations;

  /** The duration of the red signals */
  private List<Double> redDurations;


  /////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////


  /**
   * Construct a traffic signal phases.
   *
   * @param map   the map of the simulation
   * @param strs  the description of the direction based on the name of the
   *              roads.
   */
  public TrafficSignalPhase(GridMap map, List<String> strs) {
    this.phaseNum = strs.size() - 1;
    this.activeRoads = new LinkedList<List<Road>>();
    this.greenDurations = new LinkedList<Double>();
    this.yellowDurations = new LinkedList<Double>();
    this.redDurations = new LinkedList<Double>();
    // read from the second line
    for (String str : strs) {
      String[] tokens = str.split(",");
      ArrayList<Road> roads = new ArrayList<Road>();
      // active roads for the phase
      if (tokens[0].contains("N")) {
        for (Road road : map.getRoads()) {
          if (road.getName().equals("1st Avenue N")) {
            roads.add(road);
          }
        }
      }
      if (tokens[0].contains("S")) {
        for (Road road : map.getRoads()) {
          if (road.getName().equals("1st Avenue S")) {
            roads.add(road);
          }
        }
      }
      if (tokens[0].contains("E")) {
        for (Road road : map.getRoads()) {
          if (road.getName().equals("1st Street E")) {
            roads.add(road);
          }
        }
      }
      if (tokens[0].contains("W")) {
        for (Road road : map.getRoads()) {
          if (road.getName().equals("1st Street W")) {
            roads.add(road);
          }
        }
      }
      activeRoads.add(roads);
      // the duration of the green signal
      greenDurations.add(Double.parseDouble(tokens[1])); //cast the string value to double type
      // the duration of the yellow signal
      yellowDurations.add(Double.parseDouble(tokens[2]));
      // the duration of the red signal
      redDurations.add(Double.parseDouble(tokens[3]));
    }
  }


  /////////////////////////////////
  // PUBLIC STATIC METHODS
  /////////////////////////////////

  /**
   * Create a new traffic signal phase object from file
   *
   * @param map          the map
   * @param csvFileName  the file name of the CSV file
   * @return the traffic signal phase object
   */
  public static TrafficSignalPhase makeFromFile(GridMap map, String csvFileName) {
    List<String> strs = null;
    try {
      strs = Util.readFileToStrArray(csvFileName);
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage());
    }
    if (strs != null) {
      return new TrafficSignalPhase(map, strs);
    } else {
      return null;
    }
  }

  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  /**
   * Get the duration of the green signals.
   *
   * @param phaseId  the phase ID
   * @return the green duration
   */
  public double getGreenDurations(int phaseId) {
    return greenDurations.get(phaseId);
  }

  /**
   * Get the duration of the yellow signals.
   *
   * @param phaseId  the phase ID
   * @return the yellow duration
   */
  public double getYellowDurations(int phaseId) {
    return yellowDurations.get(phaseId);
  }

  /**
   * Get the duration of the red signals.
   *
   * @param phaseId  the phase ID
   * @return the red duration
   */
  public double getRedDurations(int phaseId) {
    return redDurations.get(phaseId);
  }

  /**
   * Generate a signal controller for a road.
   *
   * @param road  the road
   * @return a signal controller for the road
   */
  public CyclicSignalController calcCyclicSignalController(Road road) {
    double[] durations = new double[phaseNum * 3];
    TrafficSignal[] signals = new TrafficSignal[phaseNum * 3];

    int j = 0;
    for(int i=0; i<phaseNum; i++) {
      // check whether the road is active in ths current phase
      boolean isActive = false;
      //check the active road whether exist
      for(Road r: activeRoads.get(i)) {
        if (r.getName().equals(road.getName())) {
          isActive = true;
          break;
        }
      }
      if (isActive) {
        durations[j] = greenDurations.get(i);
        signals[j] = TrafficSignal.GREEN;
//        System.out.print("duration from green");
//        System.out.println(Arrays.toString(durations));
        j++;
        durations[j] = yellowDurations.get(i);
        signals[j] = TrafficSignal.YELLOW;
        j++;
        durations[j] = redDurations.get(i);
        signals[j] = TrafficSignal.RED;
      } else {
        durations[j] += greenDurations.get(i) + yellowDurations.get(i) + redDurations.get(i);
        signals[j] = TrafficSignal.RED;
      }

      j++;
    }
    System.out.print("Duration: ");
    System.out.println(Arrays.toString(durations));
    System.out.print("signal: ");
    System.out.println(Arrays.toString(signals));
    return new TrafficSignalRequestHandler.CyclicSignalController(durations, signals);
  }

  /////////////////////////////////
  // DEBUG
  /////////////////////////////////

  /**
   * Get a string description of the signal phases.
   *
   * @return the string description of the signal phases
   */
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    for(int i=0; i<phaseNum; i++) {
      s.append("Phase ").append(i).append(":");
      for(Road r : activeRoads.get(i)) {
        s.append(" \"").append(r.getName()).append("\"");
      }
      s.append(" g=").append(Constants.TWO_DEC.format(greenDurations.get(i))).append(", ");
      s.append("y=").append(Constants.TWO_DEC.format(yellowDurations.get(i))).append(", ");
      s.append("r=").append(Constants.TWO_DEC.format(redDurations.get(i))).append("\n");
    }
    return s.toString();
  }

}
