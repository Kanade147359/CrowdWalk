// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;

public class AgentsPerFloor extends JFrame {
	/**
	 * Show how many agent are at each floor.
	 */
	double minDepth = Double.MAX_VALUE, maxDepth = Double.MIN_VALUE;
	int agentCount;
	HashMap<Double, Integer> agentsPerFloor;
	ArrayList<Double> floors;
	
	GraphPane gpane;
	JLabel status;

	public AgentsPerFloor(EvacuationSimulator simulator) {
		super("Agents per floor");
		setLayout(new BorderLayout());
		agentsPerFloor = new HashMap<Double, Integer>();
		floors = new ArrayList<Double>();
		gpane = new GraphPane();

		for (final MapNode node : simulator.getNodes()) {
			if (!agentsPerFloor.containsKey(node.getHeight())) {
				agentsPerFloor.put(node.getHeight(), 0);
				floors.add(node.getHeight());
			}
			minDepth = Math.min(minDepth, node.getHeight());
			maxDepth = Math.max(maxDepth, node.getHeight());
		}
		agentCount = simulator.getAgents().size();
		Collections.sort(floors);

		gpane.setSize(600, 600);
		add(gpane, BorderLayout.CENTER);

    	setLocation(322, 0);
    	setSize(600, 600);

    	status = new JLabel("");
    	status.setBorder(BorderFactory.createLoweredBevelBorder());
    	add(status, BorderLayout.SOUTH);

    	pack();
		update(simulator.getAgents());
	}
	
	public void update(List<AgentBase> list) {
		for (Double height : agentsPerFloor.keySet()) {
			agentsPerFloor.put(height, 0);
		}

		for (final AgentBase agent : list) {
			if (agent.isEvacuated()) continue;
			final double height = agent.getHeight();
			double minDiff = Double.MAX_VALUE;
			double theFloor = 0.0;
			for (Double floorHeight : floors) {
				double diff = Math.abs(height - floorHeight);
				if (diff < minDiff) {
					minDiff = diff;
					theFloor = floorHeight;
				}
			}
			agentsPerFloor.put(theFloor, agentsPerFloor.get(theFloor) + 1);
		}
		gpane.repaint();
	}

	class GraphPane extends JPanel {
		/**
		 * graph
		 */

		@Override
		public void paintComponent(Graphics g0) {
			super.paintComponents(g0);
			
			for (Double height : agentsPerFloor.keySet()) {
				int count = agentsPerFloor.get(height);
				if (agentCount > 0) {
					int x = 400 * count / agentCount + 100;
					int y = (int)(450 - (350 * (height - minDepth) / (maxDepth - minDepth)));
			
					g0.drawLine(100, y, x, y);
				}
			}
		}
	}
}
//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
