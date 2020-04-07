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
package aim4.im.v2i;

import aim4.config.SimConfig;
import aim4.config.TrafficSignal;

import java.io.*;
import java.util.List;

import aim4.gui.Viewer;
import aim4.im.v2i.policy.BasePolicy;
import aim4.im.v2i.policy.BasePolicyCallback;
import aim4.im.v2i.policy.BasePolicy.ProposalFilterResult;
import aim4.im.v2i.policy.BasePolicy.ReserveParam;
import aim4.map.Road;
import aim4.map.lane.Lane;
import aim4.msg.i2v.Reject;
import aim4.msg.v2i.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * The approximate N-Phases traffic signal request handler.
 */
public class RequestHandler {

  /////////////////////////////////
  // NESTED CLASSES
  /////////////////////////////////

  /**
   * The interface of signal controllers.
   */
  public interface SignalController {

    /**
     * Get the signal at the given time
     *
     * @param time  the given time
     * @return the signal
     */
    TrafficSignal getSignal(double time);
  }

  /**
   * The cyclic signal controller.
   */
  public static class pythonSignalController implements SignalController, Runnable{

    /** The durations of the signals */
    private long duration;
    /** The list of signals */
    public TrafficSignal signal;
    public Road road;
    public Lane lane;
    private Viewer viewer;
    private String EastSignal;
    private String WestSignal;
    private String NorthSignal;
    private String SouthSignal;

    /** Create a cyclic signal controller.*/
    public pythonSignalController (Road road, Lane lane, Viewer viewer){
      this.duration = 1000;
      this.signal = TrafficSignal.RED;
      this.road = road;
      this.lane = lane;
      this.viewer = viewer;
    }

    public TrafficSignal getSignal(double time) {
          return signal;
    }

    @Override
    public void run() {
          double time = 0;
        if (road.getName().equals("E")){
          while(true) {
            try {
               time = readFromFile(time);
            } catch (IOException e) {e.printStackTrace();}
              if(duration>0){
                char c = EastSignal.charAt(road.getSpecificLaneIndex(lane));
                signal = charToTrafficSignal(c);
                    try {
                      Thread.sleep((long) SimConfig.TIME_STEP_IN_MILI*duration);
                    } catch (InterruptedException e) {e.printStackTrace();}
              }else {
                try {
                  Thread.sleep(10L);
                } catch (InterruptedException e) {e.printStackTrace();}
              }
          }
        }else if (road.getName().equals("W")){
          while(true) {
            try {
              time = readFromFile(time);
            } catch (IOException e) {e.printStackTrace();}
            if(duration>0){
              char c = WestSignal.charAt(road.getSpecificLaneIndex(lane));
              signal = charToTrafficSignal(c);
                  try {
                    Thread.sleep((long) SimConfig.TIME_STEP_IN_MILI*duration);
                  } catch (InterruptedException e) {e.printStackTrace();}
            }else {
              try {
                Thread.sleep(10L);
              } catch (InterruptedException e) {e.printStackTrace();}
            }
          }
        }else if (road.getName().equals("N")){
          while(true) {
            try {
              time = readFromFile(time);
            } catch (IOException e) {e.printStackTrace();}
            if(duration>0){
              char c = NorthSignal.charAt(road.getSpecificLaneIndex(lane));
              signal = charToTrafficSignal(c);
                  try {
                    Thread.sleep((long) SimConfig.TIME_STEP_IN_MILI*duration);
                  } catch (InterruptedException e) {e.printStackTrace();}

            }else {
              try {
                Thread.sleep(10L);
              } catch (InterruptedException e) {e.printStackTrace();}
            }
          }
        }else if (road.getName().equals("S")){
          while(true) {
            try {
              time = readFromFile(time);
            } catch (IOException e) {e.printStackTrace();}
            if(duration>0){
              char c = SouthSignal.charAt(road.getSpecificLaneIndex(lane));
              signal = charToTrafficSignal(c);
              for (int i = 0; i< duration; i++){
                try {
                  viewer.threadflag = true;
                  if (road.getSpecificLaneIndex(lane)==0){
                    viewer.time+=1;
                  }
                  Thread.sleep((long) SimConfig.TIME_STEP_IN_MILI);
                } catch (InterruptedException e) {e.printStackTrace();}
              }
            }else {
              try {
                  Thread.sleep((long) SimConfig.TIME_STEP_IN_MILI);
                  viewer.threadflag = false;
                Thread.sleep(10L);
              } catch (InterruptedException e) {e.printStackTrace();}
            }
          }
        }else {
          System.err.print("This lane was not valid");
        }
    }

    private double readFromFile(double time) throws IOException {
      FileInputStream inputStream = new FileInputStream("src/main/resources/SignalPhases/input.txt");
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      String readtime =  bufferedReader.readLine();
      try {
        Double.valueOf(readtime);
      } catch (Exception ex) {
        return time;
      }
      double timeFromFile = Double.parseDouble(readtime);
      String signalFromFile = bufferedReader.readLine();
      EastSignal = signalFromFile.substring(0,3);
      WestSignal =signalFromFile.substring(3,6);
      NorthSignal = signalFromFile.substring(6,9);
      SouthSignal = signalFromFile.substring(9,12);
      duration = (long) (timeFromFile-time);
      if (timeFromFile>time){
        return timeFromFile;
      }{
        return time;
      }
    }

    private TrafficSignal charToTrafficSignal(char c){
      TrafficSignal t = null;
      if (c == 'R'){
        t = TrafficSignal.RED;
      }else if (c == 'G'){
        t = TrafficSignal.GREEN;
      }else if (c == 'Y'){
        t = TrafficSignal.YELLOW;
      }else {
        System.err.print("Traffic signal read problem");
      }
      return t;
    }

  }



  /////////////////////////////////
  // PRIVATE FIELDS
  /////////////////////////////////

  /**
   * A mapping from lane ID to the traffic signal controllers on the lane.
   */
  private Map<Integer,SignalController> signalControllers;
  /** The base policy */
  private BasePolicyCallback basePolicy;


  /////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////

  /**
   * Create the approximate N-Phases traffic signal request handler.
   */
  public RequestHandler() {
    signalControllers = new HashMap<Integer,SignalController>();
  }

  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  public void act() {
    // do nothing
  }


  public void setBasePolicyCallback(BasePolicyCallback basePolicy) {
    this.basePolicy = basePolicy;
  }

  /**
   * Set the traffic signal controller of a lane
   *
   * @param laneId            the lane ID
   * @param signalController  the signal controller
   */
  public void setSignalControllers(int laneId, SignalController signalController) {
    signalControllers.put(laneId, signalController);
  }


  public void processRequestMsg(Request msg) {
    int vin = msg.getVin();

    // If the vehicle has got a reservation already, reject it.
    if (basePolicy.hasReservation(vin)) {
      basePolicy.sendRejectMsg(vin, msg.getRequestId(), Reject.Reason.CONFIRMED_ANOTHER_REQUEST);
      return;
    }
    // filter the proposals
    ProposalFilterResult filterResult =
        BasePolicy.standardProposalsFilter(msg.getProposals(), basePolicy.getCurrentTime());
    if (filterResult.isNoProposalLeft()) {
      basePolicy.sendRejectMsg(vin, msg.getRequestId(), filterResult.getReason());
    }

    List<Request.Proposal> proposals = filterResult.getProposals();

    // If cannot enter from lane according to canEnterFromLane(), reject it.
    if (!canEnterFromLane(proposals.get(0).getArrivalLaneID())) {
      basePolicy.sendRejectMsg(vin, msg.getRequestId(), Reject.Reason.NO_CLEAR_PATH);
      return;
    }
    // try to see if reservation is possible for the remaining proposals.
    ReserveParam reserveParam = basePolicy.findReserveParam(msg, proposals);
    if (reserveParam != null) {
      basePolicy.sendComfirmMsg(msg.getRequestId(), reserveParam);
    } else {
      basePolicy.sendRejectMsg(vin, msg.getRequestId(), Reject.Reason.NO_CLEAR_PATH);
    }
  }

///add synchronized
  public synchronized TrafficSignal getSignal(int laneId) {
//    System.out.print(laneId);
    return signalControllers.get(laneId).getSignal(basePolicy.getCurrentTime());
  }


  /////////////////////////////////
  // PRIVATE METHODS
  /////////////////////////////////

  /**
   * Check whether the vehicle can enter the intersection from a lane at
   * the current time.  This method is intended to be overridden by superclass.
   *
   * @param arrivalLaneId  the id of the lane from which the vehicle enters
   *                the intersection.
   * @return whether the vehicle can enter the intersection
   */
  private boolean canEnterFromLane(int arrivalLaneId) {
    return getSignal(arrivalLaneId) == TrafficSignal.GREEN;
  }

}
