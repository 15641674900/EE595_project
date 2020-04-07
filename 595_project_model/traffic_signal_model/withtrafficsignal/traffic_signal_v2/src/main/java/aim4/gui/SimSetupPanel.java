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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JPanel;

import aim4.sim.setup.ApproxNPhasesTrafficSignalSimSetup;
import aim4.sim.setup.BasicSimSetup;

/**
 * The simulation setup panel.
 */
public class SimSetupPanel extends JPanel implements ItemListener {

  private static final long serialVersionUID = 1L;

  final static String TRAFFIC_SIGNAL_SETUP_PANEL = "Traffic Signals";

  /** The card panel */
  private JPanel cards; //a panel that uses CardLayout
  /** The card layout */
  private CardLayout cardLayout;
  /** The traffic signal setup panel */
  private TrafficSignalParamPanel trafficSignalSetupPanel;
  /** The simulation setup panel */
  private BasicSimSetup simSetup;

  /**
   * Create a simulation setup panel
   *
   * @param initSimSetup  the initial simulation setup
   */
  public SimSetupPanel(BasicSimSetup initSimSetup) {
    this.simSetup = initSimSetup;

    // create the combo box pane
    JPanel comboBoxPane = new JPanel(); //use FlowLayout
    comboBoxPane.setBackground(Color.WHITE);


    // create the cards pane
    cardLayout = new CardLayout();
    cards = new JPanel(cardLayout);

    trafficSignalSetupPanel = new TrafficSignalParamPanel();
    cards.add(trafficSignalSetupPanel, TRAFFIC_SIGNAL_SETUP_PANEL);

    // add the combo box pane and cards pane
    setLayout(new BorderLayout());
    add(comboBoxPane, BorderLayout.PAGE_START);
    add(cards, BorderLayout.CENTER);
  }

  /**
   * Create and return a simulation setup object.
   *
   * @return the simulation setup object
   */
  public BasicSimSetup getSimSetup() {
      ApproxNPhasesTrafficSignalSimSetup simSetup2 =
        new ApproxNPhasesTrafficSignalSimSetup(simSetup,
                                               "/SignalPhases/AIM4Phases.csv");
      simSetup2.setTrafficVolume("/SignalPhases/AIM4Volumes.csv");

      simSetup2.setLanesPerRoad(trafficSignalSetupPanel.getLanesPerRoad());
      simSetup2.setStopDistBeforeIntersection(1.0);
      return simSetup2;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void itemStateChanged(ItemEvent evt) {
    cardLayout.show(cards, (String)evt.getItem());
  }
}