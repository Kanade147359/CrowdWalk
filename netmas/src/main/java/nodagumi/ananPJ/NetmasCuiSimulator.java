// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ;

import java.io.*;
import java.util.*;

import nodagumi.ananPJ.*;
import nodagumi.ananPJ.Agents.*;
import nodagumi.ananPJ.Agents.WalkAgent.*;
import nodagumi.ananPJ.Simulator.*;
import nodagumi.ananPJ.misc.*;

import nodagumi.Itk.*;

public class NetmasCuiSimulator extends BasicSimulationLauncher {

    protected String agentMovementHistoryPath = null;
    protected String individualPedestriansLogDir = null;

    public NetmasCuiSimulator(Random _random) {
        super(_random);
    }

    public NetmasCuiSimulator(String _propertiesPath) {
        super(null) ;
        // load properties
        setPropertiesFromFile(_propertiesPath) ;

        // check property options
        if (mapPath == null) {
            System.err.println("NetmasCuiSimulator: map file is " +
                    "required.");
            return;
        } else if (!((File) new File(mapPath)).exists()) {
            System.err.println("NetmasCuiSimulator: specified map file does " +
                    "not exist.");
            return;
        }
        try {
            networkMap = readMapWithName(mapPath) ;
            networkMap.setHasDisplay(false);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        try {
            agentMovementHistoryPath = properties.getFilePath("agent_movement_history_file", null, false);
            individualPedestriansLogDir = properties.getDirectoryPath("individual_pedestrians_log_dir", null);
            if (individualPedestriansLogDir != null) {
                individualPedestriansLogDir = individualPedestriansLogDir.replaceFirst("[/\\\\]+$", "");
            }
        } catch(Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        // プロパティファイルで指定されたパスを使用する(以下が無いとマップファイルの設定が使われる)
        setupNetworkMap() ;
    }

    public void initialize() {
        initializeSimulatorEntity(false) ;
    }

    public void start() {
        if (agentMovementHistoryPath != null) {
            simulator.getAgentHandler().initAgentMovementHistorLogger("agent_movement_history", agentMovementHistoryPath);
        }
        if (individualPedestriansLogDir != null) {
            simulator.getAgentHandler().initIndividualPedestriansLogger("individual_pedestrians_log", individualPedestriansLogDir);
        }
	Itk.logDebug("NetmasCuiSimulator start!");
        while (!finished) {
            simulateOneStepBare() ;
            if (exitCount > 0 && counter > exitCount) {
                finished = true;
                break;
            }
        }
        if (isTimeSeriesLog) {
            // flush log file
            simulator.saveGoalLog(timeSeriesLogPath, true);
        }
        if (individualPedestriansLogDir != null) {
            simulator.getAgentHandler().closeIndividualPedestriansLogger();
        }
        if (isTimerEnabled) {
            timer.writeElapsed();
            timer.stop();
        }
    }

}

