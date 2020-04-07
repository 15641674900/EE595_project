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
package aim4.map;

import java.util.List;

import aim4.config.Debug;
import aim4.map.BasicMap;
import aim4.map.Road;
import aim4.map.lane.Lane;
import aim4.util.Util;

/**
 * The RandomDestinationSelector selects Roads uniformly at random, but will
 * not select a Road that is the dual of the starting Road.  This is to
 * prevent Vehicles from simply going back from whence they came.
 */
public class DestinationSelector {

  /////////////////////////////////
  // PRIVATE FIELDS
  /////////////////////////////////

  /////////////////////////////////
  // CLASS CONSTRUCTORS
  /////////////////////////////////

  /**
   * Create a new RandomDestinationSelector from the given Layout.
   *
   * @param layout the Layout from which to create the
   *               RandomDestinationSelector
   */
  public DestinationSelector(BasicMap layout) {
    layout.getDestinationRoads();
  }

  /////////////////////////////////
  // PUBLIC METHODS
  /////////////////////////////////

  public Road selectDestination(Lane currentLane) {

    Road currentRoad = Debug.currentMap.getRoad(currentLane);

    if (currentRoad.getName().equals("E")){
      if(1==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("E");
      } else if(2==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("N");
      } else if(0==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("S");
      } else {
        System.err.print("E destination selected failed, keep forward");
        return Debug.currentMap.getRoad("E");
      }
    } else if (currentRoad.getName().equals("W")){
      if(1==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("W");
      } else if(2==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("S");
      } else if(0==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("N");
      } else {
        System.err.print("W destination selected failed, keep forward");
        return Debug.currentMap.getRoad("W");
      }
    } else if (currentRoad.getName().equals("N")){
      if(1==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("N");
      } else if(2==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("W");
      } else if(0==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("E");
      } else {
        System.err.print("N destination selected failed, keep forward");
        return Debug.currentMap.getRoad("N");
      }
    } else if (currentRoad.getName().equals("S")){
      if(1==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("S");
      } else if(2==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("E");
      } else if(0==currentRoad.getSpecificLaneIndex(currentLane)){
        return Debug.currentMap.getRoad("W");
      } else {
        System.err.print("S destination selected failed, keep forward");
        return Debug.currentMap.getRoad("S");
      }
    } else {
      System.err.print("destination selected failed, keep forward");
      return currentRoad;
    }
  }
}
