// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.geom.Point2D;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.*;
import java.lang.ClassNotFoundException;
//import java.lang.System;
import java.text.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Formatter;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;

import javax.vecmath.Vector3d;

import org.w3c.dom.Document;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.*;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.AgentGenerationFile;
import nodagumi.ananPJ.Agents.AgentFactory;
import nodagumi.ananPJ.Scenario.*;
import nodagumi.ananPJ.misc.SimClock.SimTime;

import nodagumi.Itk.CsvFormatter;
import nodagumi.Itk.*;

public class AgentHandler {
    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * agent の queue を処理する際に、先頭・末尾のどちらから処理するか
     * を示すフラグ。
     * MapLink の中でのソートでは、エージェントの進んだ距離に対して、
     * 昇順になるようになっている。
     * つまり、進んでいないエージェント（列の末尾にいるエージェント）が
     * queue (agents という変数)の先頭に来る。
     * このフラグは、update, preupdate において、
     * この queue を前から処理するか、後ろから処理するかを指定する。
     * true の場合、後ろから（つまり agent の列として先頭から）処理する。
     * 本来、これはかならず true であるべきだが、
     * 2014年12月までの実装での状態を残すため、
     * フラグで切り替えられるようにしておく。
     */
    // static private boolean usingFrontFirstOrderQueue = false ;
    static private boolean isUsingFrontFirstOrderQueue = true ;

    //============================================================
    //------------------------------------------------------------
    /**
     * switching queue order
     * @param flag : if true, use front_first order.
     *               else, use rear_first order.
     */
    static public void useFrontFirstOrderQueue(boolean flag) {
        isUsingFrontFirstOrderQueue = flag ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 乱数生成器。
     */
    private Random random = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 所属するSimulator へのリンク。
     */
    private EvacuationSimulator simulator;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェント生成ファイル
     */
    private AgentGenerationFile generate_agent = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 地図。
     */
    private NetworkMap networkMap;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * NetworkMapParts の prefix の規定値
     */
    public static String DefaultAgentIdPrefix = "ag" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントテーブル。
     * エージェントの id とエージェントを結びつけるもの。
     */
    private UniqIdObjectTable<AgentBase> agentTable =
        new UniqIdObjectTable<AgentBase>(DefaultAgentIdPrefix) ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 生成した全エージェントリスト。
     */
    private ArrayList<AgentBase> generatedAgents;

    /**
     * 避難完了したエージェントリスト。
     */
    private ArrayList<AgentBase> evacuatedAgents;

    /**
     * スタックしたエージェントリスト。
     */
    private ArrayList<AgentBase> stuckAgents;

    /**
     * 今動いているエージェントの集まり
     */
    private HashMap<String, AgentBase> walkingAgentTable ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * EXIT ごとの、避難エージェント数の統計。
     */
    private HashMap<MapNode, Integer> evacuatedAgentCountByExit;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントが存在するリンクのリスト
     */
    private HashSet<MapLink> effectiveLinkSet = new HashSet<MapLink>() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 平均速度。
     */
    private double averageSpeed = 0.0;

    /**
     * 生成されるはずの全エージェント数。
     */
    private int maxAgentCount = 0;

    /**
     */
    private boolean isAllAgentSpeedZeroBreak = false;

    /**
     */
    private boolean isAllAgentSpeedZero = false;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    // ログ出力定義関連
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * individualPedestriansLog の出力先 dir
     */
    private String individualPedestriansLogDir = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * agentMovementHistoryLogger
     */
    private Logger agentMovementHistoryLogger = null;

    /**
     * agentMovementHistoryLogger の CSV フォーマットは以下の通り。
     * 各行のカラムの順：
     *		"GenerationFileの情報"
     *		"エージェントID"
     *		"発生時刻1"
     *		"発生時刻2"
     *		"到着時刻1"
     *		"到着時刻2"
     *		"移動時間1"
     *		"移動時間2"
     */
    private static CsvFormatter<AgentBase> agentMovementHistoryLoggerFormatter =
        new CsvFormatter<AgentBase>() ;
    static {
        CsvFormatter<AgentBase> formatter = agentMovementHistoryLoggerFormatter ;
        formatter
            .addColumn(formatter.new Column("GenerationFileの情報") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.getConfigLine().replaceAll(",", " ") ;}})
            .addColumn(formatter.new Column("エージェントID") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.ID ;}})
            .addColumn(formatter.new Column("発生時刻1") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return agent.generatedTime.getAbsoluteTimeString() ;}})
            .addColumn(formatter.new Column("発生時刻2") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return "" + (int)agent.generatedTime.getRelativeTime() ;}})
            .addColumn(formatter.new Column("到着時刻1") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return currentTime.getAbsoluteTimeString();}})
            .addColumn(formatter.new Column("到着時刻2") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return "" + (int)currentTime.getRelativeTime();}})
            .addColumn(formatter.new Column("移動時間1") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return
                            Itk.formatSecTime((int)currentTime.calcDifferenceFrom(agent.generatedTime)) ;}})
            .addColumn(formatter.new Column("移動時間2") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj;
                        return "" + (int)(currentTime.calcDifferenceFrom(agent.generatedTime)) ;}})
            ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * individualPedestriansLogger
     */
    private Logger individualPedestriansLogger = null;

    /**
     * individualPedestriansLogger のCSVフォーマットは以下の通り。
     * 各行のカラムの順：
     *		"pedestrianID"
     *		"current_position_in_model_x"
     *		"current_position_in_model_y"
     *		"current_position_in_model_z"
     *		"current_position_for_drawing_x"
     *		"current_position_for_drawing_y"
     *		"current_position_for_drawing_z"
     *		"current_acceleration"
     *		"current_velocity"
     *		"current_linkID"
     *		"current_nodeID_of_forward_movement"
     *		"current_nodeID_of_backward_movement"
     *		"current_distance_from_node_of_forward_movement"
     *		"current_moving_direction"
     *		"generated_time"
     *		"current_traveling_period"
     *		"current_exposure"
     *		"amount_exposure"
     *		"current_status_by_exposure"
     *		"next_assigned_passage_node"
     */
    private static CsvFormatter<AgentBase> individualPedestriansLoggerFormatter =
        new CsvFormatter<AgentBase>() ;
    static {
        CsvFormatter<AgentBase> formatter = individualPedestriansLoggerFormatter ;
        formatter
            .addColumn(formatter.new Column("pedestrianID") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return agent.ID ;}})
            .addColumn(formatter.new Column("current_position_in_model_x") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getPos().getX())) ;}})
            .addColumn(formatter.new Column("current_position_in_model_y") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getPos().getY())) ;}})
            .addColumn(formatter.new Column("current_position_in_model_z") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getHeight())) ;}})
            .addColumn(formatter.new Column("current_position_for_drawing_x") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getPos().getX() 
                                      + agent.getSwing().x)) ;}})
            .addColumn(formatter.new Column("current_position_for_drawing_y") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getPos().getY() 
                                      + agent.getSwing().y)) ;}})
            .addColumn(formatter.new Column("current_position_for_drawing_z") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + ((agent.getHeight() /
                                       ((MapPartGroup)agent.getCurrentLink().getParent()).getScale())
                                      + agent.getSwing().z)) ;}})
            .addColumn(formatter.new Column("current_acceleration") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getAcceleration())) ;}})
            .addColumn(formatter.new Column("current_velocity") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getSpeed())) ;}})
            .addColumn(formatter.new Column("current_linkID") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + -1 :
                                agent.getCurrentLink().ID ) ;}})
            .addColumn(formatter.new Column("current_nodeID_of_forward_movement") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + -1 :
                                agent.getNextNode().ID ) ;}})
            .addColumn(formatter.new Column("current_nodeID_of_backward_movement") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                agent.getLastNode().ID :
                                agent.getPrevNode().ID ) ;}})
            .addColumn(formatter.new Column("current_distance_from_node_of_forward_movement") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0.0 :
                                "" + (agent.getRemainingDistance())) ;}})
            .addColumn(formatter.new Column("current_moving_direction") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return (agent.isEvacuated() ?
                                "" + 0 :
                                "" + ((int)agent.getDirection().value())) ;}})
            .addColumn(formatter.new Column("generated_time") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        return "" + ((int)agent.generatedTime.getRelativeTime()) ;}})
            .addColumn(formatter.new Column("current_traveling_period") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return "" + ((int)currentTime.getRelativeTime()) ;}})
            .addColumn(formatter.new Column("current_exposure") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return "" + (agent.obstructer.currentValueForLog()) ;}})
            .addColumn(formatter.new Column("amount_exposure") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        return "" + (agent.obstructer.accumulatedValueForLog()) ;}})
            .addColumn(formatter.new Column("current_status_by_exposure") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj ;
                        return agent.getTriageName();}})
            .addColumn(formatter.new Column("next_assigned_passage_node") {
                    public String value(AgentBase agent, Object timeObj,
                                        Object agentHandlerObj) {
                        SimTime currentTime = (SimTime)timeObj ;
                        AgentHandler handler = (AgentHandler)agentHandlerObj ;
                        return (agent.isEvacuated() ?
                                "" :
                                agent.getNextCandidateString()) ;}})
            ;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 画面パーツ類。
     */
    private transient JButton start_button = null;
    private transient JButton pause_button = null;
    private transient JButton step_button = null;

    private transient JScrollBar simulationDeferFactorControl;
    private transient JLabel simulationDeferFactorValue;

    protected transient JPanel control_panel = null;
    private transient JLabel clock_label = new JLabel("NOT STARTED1");
    private transient JLabel time_label = new JLabel("NOT STARTED2");
    private transient JLabel evacuatedCount_label = new JLabel("NOT STARTED3");
    private ArrayList<ButtonGroup> toggle_scenario_button_groups = new
        ArrayList<ButtonGroup>();
    private transient JTextArea message = new JTextArea("UNMaps Version 1.9.5\n") {
        @Override
        public void append(String str) {
            super.append(str);
            message.setCaretPosition(message.getDocument().getLength());
        }
    };

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public AgentHandler (EvacuationSimulator _simulator) {
        simulator = _simulator;

        // simulatorから必須パラメータ取り出し。
        random = simulator.getRandom();
        networkMap = simulator.getMap() ;

        // ファイル類の読み込み
        loadAgentGenerationFile(simulator.getSetupFileInfo().getGenerationFile()) ;

        // 各種配列初期化。
        evacuatedAgentCountByExit = new LinkedHashMap<MapNode, Integer>();
        generatedAgents = new ArrayList<AgentBase>();
        evacuatedAgents = new ArrayList<AgentBase>();
        stuckAgents = new ArrayList<AgentBase>();
        walkingAgentTable = new HashMap<String, AgentBase>() ;

        // 表示初期化。
        if (hasDisplay()) {
            setup_control_panel(simulator.getSetupFileInfo().getGenerationFile(),
                                simulator.getSetupFileInfo().getScenarioFile(),
                                networkMap);
        }

    }

    //------------------------------------------------------------
    // 読み込み関連
    //------------------------------------------------------------
    /**
     * エージェント生成ファイル。
     */
    private void loadAgentGenerationFile(String generationFile) {
        try {
            /* [I.Noda] generation file の読み込みはここ */
             generate_agent =
                 new AgentGenerationFile(generationFile,
                                         networkMap,
                                         simulator.getFallbackParameters(),
                                         hasDisplay(),
                                         simulator.getLinerGenerateAgentRatio(),
                                         random);
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("Illegal AgentGenerationFile",
                         simulator.getSetupFileInfo().getGenerationFile(),
                         ex.getMessage());
            System.exit(1);
        }
        if (generate_agent != null) {
            for (AgentFactory factory : generate_agent) {
                maxAgentCount += factory.getMaxGeneration();
            }
        }
    }

    //------------------------------------------------------------
    // シミュレーション関連
    //------------------------------------------------------------
    /**
     * シミュレーション開始準備
     */
    public void prepareForSimulation() {
        for (AgentBase agent : getAllAgentCollection()) {
            agent.prepareForSimulation() ;
        }
        // 初回は全リンクを対象とする
        clearEffectiveLinkSet() ;
        for(MapLink link : simulator.getLinks()) {
            addEffectiveLink(link) ;
        }
    }

    //------------------------------------------------------------
    /**
     * リンクの preprocess 処理
     */
    private void preUpdateLinks(SimTime currentTime) {
        synchronized (simulator) {
            //            for (MapLink link : simulator.getLinks()) {
            // Iterator をちゃんと動かすために、remove は別のところへ退避。
            ArrayList<MapLink> emptyLinkList = new ArrayList<MapLink>() ;
            for(MapLink link : getEffectiveLinkSet()) {
                if(link.agents.isEmpty()) {
                    emptyLinkList.add(link) ;
                } else {
                    link.preUpdate(currentTime);
                }
            }
            for(MapLink link : emptyLinkList) {
                removeEffectiveLink(link) ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * エージェントの preprocess 処理
     */
    private void preUpdateAgents(SimTime currentTime) {
        if (true) {
            synchronized (simulator) {
                for (MapLink link : getEffectiveLinkSet()) {
                    ArrayList<AgentBase> forwardAgents
                        = link.getLane(Direction.Forward);
                    for(int i = 0 ; i < forwardAgents.size() ; i++) {
                        AgentBase agent =
                            (isUsingFrontFirstOrderQueue ?
                             forwardAgents.get(forwardAgents.size() - i - 1) :
                             forwardAgents.get(i)) ;

                        agent.preUpdate(currentTime);
                    }
                    ArrayList<AgentBase> backwardAgents =
                        link.getLane(Direction.Backward);
                    for (int i = backwardAgents.size() - 1; i >= 0; --i) {
                        AgentBase agent = backwardAgents.get(i);
                        agent.preUpdate(currentTime);
                    }
                }
            }
        }
    }

    //------------------------------------------------------------
    /**
     * シミュレーションサイクル
     */
    public void update(SimTime currentTime) {
        update_buttons();

        ArrayList<AgentBase> generatedAgentsInStep
            = generateAgentsAndSetup(currentTime) ;

        preUpdateLinks(currentTime);
        preUpdateAgents(currentTime);
        updateAgents(currentTime);
        updatePollution();
        updateLinks(currentTime);

        updateAgentViews();

        /* the following must be here, as direction etc. are
         * calculated in the methods call above, such as updateAgents.
         * [2015.05.25 I.Noda] おそらく表示のために、ここにないといけない。
         */
        registerGeneratedAgentsInStep(generatedAgentsInStep) ;
        updateEffectiveLinkSetAndRemoveEvacuatedAgents() ;
        notifyMovedAgents() ;
    }

    //------------------------------------------------------------
    /**
     * generation 定義による新規エージェントの生成
     */
    private ArrayList<AgentBase> generateAgentsAndSetup(SimTime currentTime) {
        ArrayList<AgentBase> generatedAgentsInStep = new ArrayList<AgentBase>();

        // generate_agent による生成。
        if (generate_agent != null) {
            for (AgentFactory factory : generate_agent) {
                factory.tryUpdateAndGenerate(simulator, currentTime,
                                             generatedAgentsInStep) ;
            }
        }

        // Agent の初期設定。
        if (! generatedAgentsInStep.isEmpty()) {
            generatedAgents.addAll(generatedAgentsInStep);
            for (AgentBase agent : generatedAgentsInStep) {
                /* [2015.05.25 I.Noda] 
                 * registerAgent から切り離して、Agent に ID をふる。
                 */
                assignUniqIdForAgent(agent) ;
                addWalkingAgent(agent) ;
                if(!agent.isEvacuated())
                    addEffectiveLink(agent.getCurrentLink()) ;
            }
        }
        return generatedAgentsInStep ;
    }

    //------------------------------------------------------------
    /**
     * 新規エージェントの登録
     */
    private void registerGeneratedAgentsInStep(ArrayList<AgentBase>  agentList) {
        for (AgentBase agent : agentList) {
            simulator.registerAgent(agent);
        }
    }

    //------------------------------------------------------------
    /**
     * エージェントが存在するリンクのリストを更新。
     * さらに、
     * evacuate したエージェントを、walkingAgentTable から除く。
     */
    private void updateEffectiveLinkSetAndRemoveEvacuatedAgents() {
        // エージェントが存在するリンクのリストを更新
        /** [2015.07.05 I.Noda]
         * effectiveLinkSet を、HashSet としたので、
         * いちいちクリアする必要はない。
         * エージェントがいなくなったリンクは、preUpdateLinks() で削除される。
         */
        //clearEffectiveLinkSet() ;
        ArrayList<AgentBase> evacuatedAgentsInStep = new ArrayList<AgentBase>() ;
        for (AgentBase agent : getWalkingAgentCollection()) {
            if(agent.isEvacuated()) {
                evacuatedAgentsInStep.add(agent) ;
                continue ;
            } 
            addEffectiveLink(agent.getCurrentLink()) ;
        }
        for(AgentBase agent : evacuatedAgentsInStep) {
            removeWalkingAgent(agent) ;
        }
    }

    //------------------------------------------------------------
    /**
     * エージェント位置がアップデートされたことを nofity
     */
    private void notifyMovedAgents() {
        if(!hasDisplay()) return ;

        // 位置が変化したエージェントを通知する
        for (AgentBase agent : getWalkingAgentCollection()) {
            if (agent.isEvacuated()) {
                continue;
            }
            boolean agentMoved = false;

            Point2D currentPosition = agent.getPos();
            Point2D lastPosition = agent.lastPosition ;
            if (lastPosition == null || ! currentPosition.equals(lastPosition)) {
                agent.lastPosition = currentPosition ;
                agentMoved = true;
            }

            Vector3d currentSwing = agent.getSwing();
            Vector3d lastSwing = agent.lastSwing ;
            if (lastSwing == null || ! currentSwing.equals(lastSwing)) {
                agent.lastSwing = currentSwing ;
                agentMoved = true;
            }

            if (agentMoved) {
                networkMap.getNotifier().agentMoved(agent);
            }
        }
    }

    //------------------------------------------------------------
    /**
     * エージェントの update 処理
     */
    private void updateAgents(SimTime currentTime) {
        if (hasDisplay()) displayClock(currentTime) ;

        int count = 0;
        double speedTotal = 0.0;
        isAllAgentSpeedZero = true;

        for(AgentBase agent : getWalkingAgentCollection()) {
            if (!agent.isEvacuated()) {
                agent.update(currentTime);
                if (agent.isEvacuated()) {// この回に evacuate した場合。
                    updateNewlyEvacuatedAgent(agent, currentTime) ;
                } else { // まだ歩いている場合。
                    ++count;
                    speedTotal += agent.getSpeed();
                    isAllAgentSpeedZero &= (agent.getSpeed() == 0.0) ;
                }
                logIndividualPedestrians(currentTime, agent);
            }
        }
        averageSpeed = speedTotal / count;

        if (hasDisplay()) updateEvacuatedCount();
    }

    //------------------------------------------------------------
    /**
     * このサイクルで evacuateしたエージェントの処理
     */
    private void updateNewlyEvacuatedAgent(AgentBase agent, SimTime currentTime) {
        final MapNode exit = agent.getLastNode() ;
        Integer i = evacuatedAgentCountByExit.get(exit);
        if (i == null)
            i = new Integer(0);
        i += 1;
        evacuatedAgentCountByExit.put(exit, i);
        evacuatedAgents.add(agent);
        if (agent.isStuck()) {
            stuckAgents.add(agent);
        }
        if (agentMovementHistoryLogger != null) {
            agentMovementHistoryLoggerFormatter
                .outputValueToLoggerInfo(agentMovementHistoryLogger,
                                         agent, currentTime, this);
        }
    }

    //------------------------------------------------------------
    /**
     * 時計表示
     */
    private void displayClock(SimTime currentTime) {
        String time_string = String.format("Elapsed: %5.2fsec",
                                           currentTime.getRelativeTime());
        time_label.setText(time_string);
        String clock_string = currentTime.getAbsoluteTimeString() ;
        clock_label.setText(clock_string);
    }

    //------------------------------------------------------------
    /**
     * エージェントの見え計算
     */
    private void updateAgentViews() {
        if(hasDisplay()) {
            for (AgentBase agent : getWalkingAgentCollection()){
                if (agent.isEvacuated())
                    continue;
                agent.updateViews();
            }
        }
    }

    //------------------------------------------------------------
    /**
     * Pollution のアップデート
     */
    private void updatePollution() {
        /* pollution */
        for (final AgentBase agent : getWalkingAgentCollection()) {
            if (!agent.isEvacuated()) {
                // (do nothing)
                // evacuatedAgentCount++;
            } else {
                double damage = agent.obstructer.accumulatedValueForLog() ;
            }
        }
    }

    //------------------------------------------------------------
    /**
     * リンクの update 処理。
     */
    private void updateLinks(SimTime currentTime) {
        /* [2015.07.04 I.Noda]
         * 現状で、表示関係の処理しかしていないので、
         * Display を持っている場合のみ、処理。
         */
        if(hasDisplay()) {
            synchronized (simulator) {
                for (MapLink link : simulator.getLinks()) {
                    link.update(currentTime);
                }
            }
        }
    }

    //------------------------------------------------------------
    // シミュレーション周辺
    //------------------------------------------------------------
    /**
     * エージェントを追加。
     * @param agent : 追加するエージェント。ID は自動生成。
     * @return agent がそのまま変える。
     */
    public AgentBase assignUniqIdForAgent(AgentBase agent) {
        String id = agentTable.putWithUniqId(agent) ;
        agent.ID = id;
        return agent ;
    }

    //------------------------------------------------------------
    /**
     * 終了チェック
     */
    public boolean isFinished() {
        if (isAllAgentSpeedZeroBreak) {
            for (AgentFactory factory : generate_agent) {
                if (factory.enabled) return false;
            }
            boolean existNotFinished = false;
            for (final AgentBase agent : getWalkingAgentCollection()) {
                if (!agent.finished()) {
                    existNotFinished = true;
                    break;
                }
            }
            if (existNotFinished) {
                if (isAllAgentSpeedZero) {
                    Itk.logInfo("finished", "all agents speed zero");
                    return true;
                }
                return false;
            }
        } else {
            for (final AgentBase agent : getWalkingAgentCollection()) {
                if (!agent.finished()) return false;
            }
            for (AgentFactory factory : generate_agent) {
                if (factory.enabled) return false;
            }
        }
        Itk.logInfo("finished","no more agents to generate");
        return true;
    }

    //------------------------------------------------------------
    /**
     * すべてのゴールタグを集める。
     */
    public ArrayList<String> getAllGoalTags() {
        ArrayList<String> all_goal_tags = new ArrayList<String>();

        for (AgentFactory factory : generate_agent) {
            Term goal_tag = factory.goal;
            if (goal_tag != null &&
                !all_goal_tags.contains(goal_tag.getString())) {
                all_goal_tags.add(goal_tag.getString());
            }
            for (Term mid_goal : factory.getPlannedRoute()) {
                if (!all_goal_tags.contains(mid_goal.getString())) {
                    all_goal_tags.add(mid_goal.getString());
                }
            }
        }
        return all_goal_tags;
    }

    //------------------------------------------------------------
    // アクセス関連 access
    //------------------------------------------------------------
    /**
     * 画面を持つかどうか
     */
    public boolean hasDisplay() {
        return simulator.hasDisplay() ;
    }

    //------------------------------------------------------------
    /**
     * 乱数シード設定
     */
    public void setRandom(Random _random) {
        random = _random;
        if (generate_agent != null)
            generate_agent.setRandom(_random);
    }

    //------------------------------------------------------------
    /**
     */
    public void setLinerGenerateAgentRatio(double _ratio) {
        generate_agent.setLinerGenerateAgentRatio(_ratio);
    }

    //------------------------------------------------------------
    /**
     */
    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
    }

    //------------------------------------------------------------
    /**
     */
    public void setIsAllAgentSpeedZeroBreak(boolean _isAllAgentSpeedZeroBreak)
    {
        this.isAllAgentSpeedZeroBreak = _isAllAgentSpeedZeroBreak;
    }

    //------------------------------------------------------------
    /**
     * 歩いているエージェントのCollectionを返す。
     * @return エージェントのCollection。
     */
    public Collection<AgentBase> getWalkingAgentCollection() {
        return walkingAgentTable.values() ;
    }

    //------------------------------------------------------------
    /**
     * 生成されるはずの全エージェント数。
     */
    public int getMaxAgentCount() {
        return maxAgentCount;
    }

    //------------------------------------------------------------
    /**
     * 平均速度。
     */
    public double getAverageSpeed() {
        return averageSpeed;
    }

    //------------------------------------------------------------
    /**
     * 歩いているエージェントの数
     * @return エージェントの数
     */
    public int numOfWalkingAgents() {
        return walkingAgentTable.size() ;
    }

    //------------------------------------------------------------
    /**
     * 避難完了したエージェントの数
     * @return エージェントの数
     */
    public int numOfEvacuatedAgents() {
        return evacuatedAgents.size() ;
    }

    //------------------------------------------------------------
    /**
     * スタック中のエージェントの数
     * @return エージェントの数
     */
    public int numOfStuckAgents() {
        return stuckAgents.size() ;
    }

    //------------------------------------------------------------
    /**
     * 指定した node の evacuation count を取得
     */
    public int getEvacuatedCount(MapNode node) {
        return evacuatedAgentCountByExit.get(node);
    }

    //------------------------------------------------------------
    /**
     * evacuation count のテーブル取得
     */
    public HashMap<MapNode, Integer> getExitNodesMap() {
        return evacuatedAgentCountByExit;
    }

    //------------------------------------------------------------
    /**
     * 生成されたすべてのエージェントの Collection を返す。
     * @return 全エージェントのリスト
     */
    public final Collection<AgentBase> getAllAgentCollection() {
        return agentTable.values() ;
    }

    //------------------------------------------------------------
    /**
     * 生成されたすべてのエージェントの数
     * @return 全エージェントの数
     */
    public int numOfAllAgents() {
        return agentTable.size() ;
    }


    //------------------------------------------------------------
    /**
     * 歩いているエージェントをテーブルに追加
     * @param agent : 追加するエージェント
     */
    public void addWalkingAgent(AgentBase agent) {
        walkingAgentTable.put(agent.ID, agent) ;
    }

    //------------------------------------------------------------
    /**
     * 歩いているエージェントテーブルより一人取り除く。
     * @return agent : 取り除くエージェント
     */
    public boolean removeWalkingAgent(AgentBase agent) {
        if(walkingAgentTable.containsKey(agent.ID)) {
            walkingAgentTable.remove(agent.ID) ;
            return true ;
        } else {
            Itk.logWarn("agent is not in walkingTable:", agent) ;
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * effective link のクリア
     */
    private void clearEffectiveLinkSet() {
        effectiveLinkSet.clear() ;
    }

    //------------------------------------------------------------
    /**
     * effective link のクリア
     */
    private Set<MapLink> getEffectiveLinkSet() {
        return effectiveLinkSet ;
    }

    //------------------------------------------------------------
    /**
     * effective link の登録
     * @param link : 追加するリンク。
     */
    private void addEffectiveLink(MapLink link) {
        effectiveLinkSet.add(link) ;
    }

    //------------------------------------------------------------
    /**
     * effective link の削除
     * @param link : 削除するリンク。
     */
    private void removeEffectiveLink(MapLink link) {
        effectiveLinkSet.remove(link) ;
    }

    //------------------------------------------------------------
    // ログ関連
    //------------------------------------------------------------
    /**
     * エージェント状態のダンプ
     */
    public void dumpAgentResult(PrintStream out) {
        for (final AgentBase agent : getAllAgentCollection()) {
            agent.dumpResult(out);
        }
    }

    //------------------------------------------------------------
    /**
     * individualPedestriansLogger への出力。
     */
    private void logIndividualPedestrians(SimTime currentTime, AgentBase agent) {
        if (individualPedestriansLogger != null) {
            individualPedestriansLoggerFormatter
                .outputValueToLoggerInfo(individualPedestriansLogger,
                                         agent, currentTime, this);
        }
    }


    //------------------------------------------------------------
    /**
     * 統計的結果説明
     */
    public String getStatisticsDescription() {
        /* 各被害レベルの人数 (0, 1, 2, 3)
         * 平均避難時間　　
         * 避難終了時間
         */
        int[] each_level = new int[4];
        double finish_total = 0.0 ;
        SimTime finish_max = null ;
        int count_all = 0, count_evacuated = 0;
        for (AgentBase agent : getAllAgentCollection()) {
            count_all++;
            each_level[agent.getTriageInt()]++;
            SimTime t = agent.finishedTime;
            if (t == null) continue;
            count_evacuated++;
            finish_total += t.getRelativeTime();
            if (finish_max == null || t.isAfter(finish_max)) finish_max = t;
        }
        return (""
                + each_level[0] + ","
                + each_level[1] + ","
                + each_level[2] + ","
                + each_level[3] + ","
                + count_evacuated + ","
                + count_all + ","
                + finish_total / count_evacuated + ","
                + finish_max.getRelativeTime()
                );
    }

    //------------------------------------------------------------
    /**
     * ロガーの初期化
     */
    public Logger initLogger(String name, Level level, java.util.logging.Formatter formatter, String filePath) {
        Logger logger = Logger.getLogger(name);
        try {
            // 「ロックファイルが残っているとログファイルに連番が振られて増え続けてしまう」不具合を回避する
            File file = new File(filePath + ".lck");
            if (file.exists()) {
                Itk.logWarn("delete lock file", filePath + ".lck");
                file.delete();
            }

            FileHandler handler = new FileHandler(filePath);
            handler.setFormatter(formatter);
            logger.addHandler(handler);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        logger.setLevel(level);
        return logger;
    }

    //------------------------------------------------------------
    /**
     * ロガーの終了処理
     */
    public void closeLogger(Logger logger) {
        if (logger != null) {
            for (Handler handler : logger.getHandlers()) {
                handler.close();
            }
        }
    }

    //------------------------------------------------------------
    /**
     * AgentMovementHistoryLogger の初期化
     */
    public void initAgentMovementHistoryLogger(String name, String filePath) {
        agentMovementHistoryLogger = initLogger(name, Level.INFO, new java.util.logging.Formatter() {
            public String format(final LogRecord record) {
                return formatMessage(record) + "\n";
            }
        }, filePath);
        agentMovementHistoryLogger.setUseParentHandlers(false); // コンソールには出力しない
        agentMovementHistoryLoggerFormatter
            .outputHeaderToLoggerInfo(agentMovementHistoryLogger) ;
    }

    //------------------------------------------------------------
    /**
     * AgentMovementHistoryLogger の終了
     */
    public void closeAgentMovementHistorLogger() {
        closeLogger(agentMovementHistoryLogger);
    }

    //------------------------------------------------------------
    /**
     * individualPedestriansLogger の初期化
     */
    public void initIndividualPedestriansLogger(String name, String dirPath) {
        individualPedestriansLogDir = dirPath;
        individualPedestriansLogger = initLogger(name, Level.INFO, new java.util.logging.Formatter() {
            public String format(final LogRecord record) {
                return formatMessage(record) + "\n";
            }
        }, dirPath + "/log_individual_pedestrians.csv");
        individualPedestriansLogger.setUseParentHandlers(false); // コンソールには出力しない
        individualPedestriansLoggerFormatter
            .outputHeaderToLoggerInfo(individualPedestriansLogger) ;

    }

    //------------------------------------------------------------
    /**
     * individualPedestriansLogger の終了
     */
    public void closeIndividualPedestriansLogger() {
        if (individualPedestriansLogger == null) {
            return;
        }
        closeLogger(individualPedestriansLogger);

        // log_individual_pedestrians_initial.csv
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(individualPedestriansLogDir + "/log_individual_pedestrians_initial.csv"), "utf-8"));
            writer.write("pedestrianID,pedestrian_moving_model,generated_time,current_traveling_period,distnation_nodeID,assigned_passage_nodes\n");
            for (AgentBase agent : getAllAgentCollection()) {
                StringBuilder buff = new StringBuilder();
                buff.append(agent.ID); buff.append(",");
                buff.append(((WalkAgent)agent).getSpeedCalculationModel().toString().replaceFirst("Model$", "")); buff.append(",");
                buff.append((int)agent.generatedTime.getRelativeTime()); buff.append(",");
                buff.append(simulator.currentTime.getTickUnit()); buff.append(",");
                buff.append(agent.getLastNode().ID); buff.append(",");
                int idx = 0;
                for (Term route : agent.getPlannedRoute()) {
                    if (idx > 0) {
                        buff.append(" ");
                    }
                    idx++;
                    buff.append(route);
                }
                buff.append("\n");
                writer.write(buff.toString());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //------------------------------------------------------------
    //------------------------------------------------------------
    // 画面関係
    //------------------------------------------------------------
    //------------------------------------------------------------
    /**
     * 画面の準備
     */
    public void setupFrame(String generationFile, String scenarioFile,
                           NetworkMap map) {
        control_panel = null;
        clock_label = new JLabel("NOT STARTED");
        time_label = new JLabel("NOT STARTED!");
        evacuatedCount_label = new JLabel("NOT STARTED");
        message = new JTextArea("UNMaps Version 1.9.5\n");
        setup_control_panel(generationFile,
                            scenarioFile,
                            map);
    }

    //------------------------------------------------------------
    /**
     * 制御パネル
     */
    public JPanel getControlPanel() {
        return control_panel;
    }

    //------------------------------------------------------------
    /**
     * スタートボタン
     */
    public JButton getStartButton() {
        return start_button;
    }

    //------------------------------------------------------------
    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJLabel(JPanel panel, int x, int y, int width, int height, int anchor, JLabel label) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = anchor;
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.gridheight = height;
        gbc.insets = new Insets(0, 12, 0, 12);
        ((GridBagLayout)panel.getLayout()).setConstraints(label, gbc);
        panel.add(label);
    }

    //------------------------------------------------------------
    /**
     * GridBagLayout のパネルにラベルを追加する
     */
    private void addJLabel(JPanel panel, int x, int y, int width, int height, JLabel label) {
        addJLabel(panel, x, y, width, height, GridBagConstraints.WEST, label);
    }

    //------------------------------------------------------------
    /**
     * 制御パネルの準備
     */
    private void setup_control_panel(String generationFileName,
                                     String scenarioFileName,
                                     NetworkMap map) {
        control_panel = new JPanel();
        control_panel.setName("Control");
        control_panel.setLayout(new BorderLayout());

        JPanel top_panel = new JPanel();
        top_panel.setLayout(new BorderLayout());

        /* title & clock */
        JPanel titlepanel = new JPanel(new GridBagLayout());
        addJLabel(titlepanel, 0, 0, 1, 1, GridBagConstraints.EAST, new JLabel("Map"));
        if (simulator.getSetupFileInfo().getNetworkMapFile() != null) {
            File map_file =
                new File(simulator.getSetupFileInfo().getNetworkMapFile());
            addJLabel(titlepanel, 1, 0, 1, 1, new JLabel(map_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 0, 1, 1, new JLabel("No map file"));
        }

        addJLabel(titlepanel, 0, 1, 1, 1, GridBagConstraints.EAST, new JLabel("Agent"));
        if (generationFileName != null) {
            File generation_file = new File(generationFileName);
            addJLabel(titlepanel, 1, 1, 1, 1, new JLabel(generation_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 1, 1, 1, new JLabel("No generation file"));
        }

        addJLabel(titlepanel, 0, 2, 1, 1, GridBagConstraints.EAST,
                  new JLabel("Scenario"));
        if (scenarioFileName != null) {
            File scenario_file = new File(scenarioFileName);
            addJLabel(titlepanel, 1, 2, 1, 1, new JLabel(scenario_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 2, 1, 1, new JLabel("No scenario file"));
        }

        addJLabel(titlepanel, 0, 3, 1, 1, GridBagConstraints.EAST,
                  new JLabel("Pollution"));
        if (simulator.getSetupFileInfo().getPollutionFile() != null) {
            File pollution_file =
                new File(simulator.getSetupFileInfo().getPollutionFile());
            addJLabel(titlepanel, 1, 3, 1, 1,
                      new JLabel(pollution_file.getName()));
        } else {
            addJLabel(titlepanel, 1, 3, 1, 1, new JLabel("No pollution file"));
        }

        addJLabel(titlepanel, 0, 4, 1, 1, GridBagConstraints.EAST,
                  new JLabel("ID"));
        /* [2015.02.10 I.Noda] remove scenario_numbers */
        JLabel sl = new JLabel("(NOT DEFINED)");
        sl.setFont(new Font(null, Font.ITALIC, 9));
        addJLabel(titlepanel, 1, 4, 1, 1, sl);

        clock_label.setHorizontalAlignment(JLabel.CENTER);
        clock_label.setFont(new Font("Lucida", Font.BOLD, 18));
        addJLabel(titlepanel, 0, 5, 1, 1, GridBagConstraints.CENTER, clock_label);

        time_label.setHorizontalAlignment(JLabel.LEFT);
        time_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJLabel(titlepanel, 1, 5, 1, 1, time_label);

        evacuatedCount_label.setHorizontalAlignment(JLabel.LEFT);
        evacuatedCount_label.setFont(new Font("Lucida", Font.ITALIC, 12));
        addJLabel(titlepanel, 0, 6, 2, 1, GridBagConstraints.CENTER, evacuatedCount_label);
        top_panel.add(titlepanel, BorderLayout.NORTH);

        /* scenarios */
        JPanel label_toggle_panel = new JPanel(new GridBagLayout());
        label_toggle_panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        GridBagConstraints c = null;

        int max_events = 1;

        int y = 0;
        for (EventBase event : simulator.getScenario().eventList) {
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.fill = GridBagConstraints.WEST;

            ButtonGroup bgroup = new ButtonGroup();
            toggle_scenario_button_groups.add(bgroup);
            class RadioButtonListener implements ActionListener {
                int index;
                EventBase event ;
                NetworkMap map ;
                public RadioButtonListener(EventBase _event,
                                           int _index,
                                           NetworkMap _map) {
                    event = _event;
                    index = _index;
                    map = _map;
                }
                public void actionPerformed(ActionEvent e) {
                    if (index == -2) {
                        event.occur(SimTime.Ending, map) ;
                    } else if (index == -1) {
                        event.unoccur(SimTime.Ending, map) ;
                    } else {
                        Itk.logError("wrong index") ;
                    }
                }
            }

            if (false) {
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = y;
                c.gridwidth = max_events + 3;
                c.fill = GridBagConstraints.EAST;
                label_toggle_panel.add(new JLabel(event.atTime.getAbsoluteTimeString()), c);
            } else {
                JRadioButton radio_button;
                radio_button = new JRadioButton("enabled");
                radio_button.addActionListener(new RadioButtonListener(event, -2, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button = new JRadioButton("disabled | auto:");
                radio_button.addActionListener(new RadioButtonListener(event, -1, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 2;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button = new JRadioButton(event.atTime.getAbsoluteTimeString()) ;
                radio_button.addActionListener(new RadioButtonListener(event, 0, map));
                bgroup.add(radio_button);
                c = new GridBagConstraints();
                c.gridx = 3;
                c.gridy = y;
                label_toggle_panel.add(radio_button, c);
                radio_button.setSelected(true);
            }
            y++;
        }
        JScrollPane scroller = new JScrollPane(label_toggle_panel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroller.setPreferredSize(new Dimension(400, 230));
        top_panel.add(scroller, BorderLayout.CENTER);
        
        JPanel control_button_panel = new JPanel(new FlowLayout());
        ImageIcon start_icon = new ImageIcon(getClass().getResource("/img/start.png"));
        ImageIcon pause_icon = new ImageIcon(getClass().getResource("/img/pause.png"));
        ImageIcon step_icon = new ImageIcon(getClass().getResource("/img/step.png"));

        start_button = new JButton(start_icon);
        start_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { simulator.start(); update_buttons(); }
        });
        control_button_panel.add(start_button);
        pause_button = new JButton(pause_icon);
        pause_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { simulator.pause(); update_buttons(); }
        });
        control_button_panel.add(pause_button);
        step_button = new JButton(step_icon);
        step_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { simulator.step(); update_buttons(); }
        });
        control_button_panel.add(step_button);
        update_buttons();

        control_button_panel.add(new JLabel("wait: "));
        simulationDeferFactorControl =
            new JScrollBar(JScrollBar.HORIZONTAL,
                           simulator.getSimulationDeferFactor(),
                           1, 0, 300);
        simulationDeferFactorControl.addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) { changeSimulationDeferFactor(); }
        });
        simulationDeferFactorControl.setPreferredSize(new Dimension(150, 20));
        control_button_panel.add(simulationDeferFactorControl);
        simulationDeferFactorValue = new JLabel();
        simulationDeferFactorValue.setHorizontalAlignment(JLabel.RIGHT);
        simulationDeferFactorValue.setPreferredSize(new Dimension(30, 10));
        simulationDeferFactorValue.setText("" + simulator.getSimulationDeferFactor());
        control_button_panel.add(simulationDeferFactorValue);

        top_panel.add(control_button_panel, BorderLayout.SOUTH);
        control_panel.add(top_panel, BorderLayout.CENTER);
            
        /* text message */
        message.setEditable(false);
        message.setAutoscrolls(true);
        JScrollPane message_scroller = new JScrollPane(message,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        message_scroller.setPreferredSize(new Dimension(300, 160));

        control_panel.add(message_scroller, BorderLayout.SOUTH);
    }
    
    //------------------------------------------------------------
    /**
     * シミュレーション遅延の制御（画面）
     */
    private void changeSimulationDeferFactor() {
        simulator.setSimulationDeferFactor(simulationDeferFactorControl.getValue());
        simulationDeferFactorValue.setText("" + simulator.getSimulationDeferFactor());
    }

    //------------------------------------------------------------
    /**
     * シミュレーション遅延の制御（画面）
     */
    public void setSimulationDeferFactor(int deferFactor) {
        simulator.setSimulationDeferFactor(deferFactor);
        simulationDeferFactorControl.setValue(simulator.getSimulationDeferFactor());
    }

    //------------------------------------------------------------
    /**
     * ボタン類のアップデート
     */
    public void update_buttons() {
        boolean is_running = simulator.isRunning();
        if (start_button != null) {
            start_button.setEnabled(!is_running);
        }
        if (pause_button != null) {
            pause_button.setEnabled(is_running);
        }
        if (step_button != null) {
            step_button.setEnabled(!is_running);
        }
    }

    //------------------------------------------------------------
    /**
     * evacuation count の計算
     */
    private void updateEvacuatedCount() {
        String evacuatedCount_string;
        if (stuckAgents.isEmpty()) {
            evacuatedCount_string = String.format(
                    "Walking: %d  Generated: %d  Evacuated: %d / %d",
                    numOfWalkingAgents(),
                    numOfAllAgents(),
                    numOfEvacuatedAgents(), getMaxAgentCount());
        } else {
            evacuatedCount_string = String.format(
                    "Walking: %d  Generated: %d  Evacuated(Stuck): %d(%d) / %d",
                    numOfWalkingAgents(),
                    numOfAllAgents(),
                    numOfEvacuatedAgents() - numOfStuckAgents(),
                    numOfStuckAgents(),
                    getMaxAgentCount());
        }
        evacuatedCount_label.setText(evacuatedCount_string);
        SimulationPanel3D.updateEvacuatedCount(evacuatedCount_string);
    }

}
