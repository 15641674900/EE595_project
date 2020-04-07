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
package aim4.gui;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import aim4.gui.component.LabeledSlider;

/**
 * The traffic signal parameter panel.
 */
public class TrafficSignalParamPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  LabeledSlider trafficRateSlider;
  LabeledSlider lanesPerRoadSlider;

  /**
   * Create a traffic signal parameter panel.
   */
  public TrafficSignalParamPanel() {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    // create the components

    trafficRateSlider =
      new LabeledSlider(0.0, 2500.0,
                        800.0,
                        500.0, 100.0,
                        "Traffic Level: %.0f vehicles/hour/lane",
                        "%.0f");
    add(trafficRateSlider);

    lanesPerRoadSlider =
      new LabeledSlider(0.0, 2.0, 1.0, 0.5, 0.5,
                        "Total simulation time: %.1f hours",
                        "%.1f");
    add(lanesPerRoadSlider);


  }


  /**
   * Get the number of lanes per road.
   *
   * @return the number of lanes per road
   */
  public int getLanesPerRoad() {
    return (int)lanesPerRoadSlider.getValue();
  }
}