// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.lang.reflect.Method;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map;

import net.arnx.jsonic.JSON ;

import nodagumi.ananPJ.Simulator.EvacuationSimulator;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.Agents.AwaitAgent;
import nodagumi.ananPJ.Agents.*;

import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.SimClock;

import nodagumi.Itk.*;


//======================================================================
/**
 * エージェント生成機構。
 * <h3>現在利用可能なエージェントリスト</h3>
 * Generation File に書ける config の内容は、以下のリンク先参照。
 * <ul>
 *  <li> {@link nodagumi.ananPJ.Agents.WalkAgent WalkAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.AwaitAgent AwaitAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.NaiveAgent NaiveAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.CapriciousAgent CapriciousAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.BustleAgent BustleAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.RationalAgent RationalAgent} </li>
 *  <li> {@link nodagumi.ananPJ.Agents.RubyAgent RubyAgent} </li>
 * </ul>
 */
public abstract class AgentFactory {
    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * エージェントの短縮名の登録
     */
    static public ClassFinder classFinder = new ClassFinder() ;

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * デフォルトのエージェント登録
     */
    static {
        registerAgentClass(WalkAgent.class) ;
        registerAgentClass(AwaitAgent.class) ;
        registerAgentClass(NaiveAgent.class) ;
        registerAgentClass(CapriciousAgent.class) ;
        registerAgentClass(BustleAgent.class) ;
        registerAgentClass(RationalAgent.class) ;
        registerAgentClass(RubyAgent.class) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * 使用出来るエージェントタイプの登録
     */
    static public void registerAgentClass(Class<?> agentClass) {
        try {
            classFinder.registerClassDummy(agentClass) ;
            String aliasName =
                (String)
                classFinder.callMethodForClass(agentClass, "getTypeName", true) ;
            classFinder.alias(aliasName, agentClass) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("cannot process registerAgentClass()") ;
            Itk.logError_("agentClass",agentClass) ;
        }
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * エージェントクラスのインスタンス生成（ゼロ引数で）
     */
    static public AgentBase newAgentByName(String agentClassName) {
        try {
            AgentBase agent =
                (AgentBase)classFinder.newByName(agentClassName) ;
            return agent ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("can not find the class") ;
            Itk.logError_("agentClassName", agentClassName) ;
            return null ;
        }
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * エージェントクラスのダミーエージェントの取得
     */
    static public Object getDummyAgent(Class<?> agentClass) {
        return classFinder.getClassDummy(agentClass) ;
    }

    //============================================================
    //------------------------------------------------------------
    /**
     * 使用可能なエージェントクラスのリスト
     */
    static public String[] getKnownAgentClassNameList() {
        return classFinder.aliasTable.keySet().toArray(new String[0]) ;
    }

    //============================================================
    //============================================================
    /**
     * エージェント生成用設定情報用クラス
     * あまりに引数が多いので、整理。
     */
    static public class Config {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * エージェントのクラス名
         */
        public String agentClassName = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * エージェント設定情報 (JSON Object)
         */
        public Term agentConf = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 出発場所
         */
        public OBNode startPlace = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成条件
         */
        public String[] conditions = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 目的地
         */
        public Term goal = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 経路
         */
        public List<Term> plannedRoute ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 開始時刻
         */
        public SimTime startTime = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 持続時間
         */
        public double duration = 0.0 ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成数
         */
        public int total = 0 ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * スピードモデル
         */
        public SpeedCalculationModel speedModel ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * fallback 情報
         */
        public Term fallbackParameters = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 設定文字列（generation file 中の設定情報の文字列）
         */
        public String originalInfo = null ;

        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * 生成ルール名
         */
        public String ruleName = null ;
        
        //------------------------------
        /**
         * JSONへの変換用
         */
        public Term toTerm() {
            Term jTerm = new Term() ;
            { // agentType
                Term agentType = new Term() ;
                agentType.setArg("className", agentClassName) ;
                agentType.setArg("config", agentConf) ;
                jTerm.setArg("agentType", agentType) ;
            }
            jTerm.setArg("startPlace",startPlace) ;
            jTerm.setArg("conditions",conditions);
            jTerm.setArg("goal",goal);
            jTerm.setArg("plannedRoute",plannedRoute) ;
            jTerm.setArg("startTime",startTime.getAbsoluteTimeString()) ;
            jTerm.setArg("duration",duration) ;
            jTerm.setArg("total",total) ;
            jTerm.setArg("speedModel", speedModel) ;
            jTerm.setArg("name", ruleName) ;

            return jTerm ;
        }
    } // end class Config

    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /** 目的地 */
    protected Term goal;
    public Term getGoal() { return goal ; }

    /** 経由地 */
    private List<Term> plannedRoute;
    public List<Term> getPlannedRoute() { return plannedRoute ; }
    public List<Term> setPlannedRoute(List<Term> route) {
        return plannedRoute = route ;
    }
    
    /** スピードモデル */
    protected SpeedCalculationModel speedModel = null;
    public SpeedCalculationModel getSpeedModel() { return speedModel ; }
    public SpeedCalculationModel setSpeedModel(SpeedCalculationModel _model) {
        return speedModel = _model ; }

    /** エージェントに付与するタグ */
    private List<String> tags = new ArrayList<String>();
    public List<String> getTags() { return tags ; }

    protected boolean enabled = true;
    public boolean isEnabled() { return enabled ; }

    Random random = null;
    SimTime startTime ;
    double duration;
    int total;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * ルールの名前。
     * CSV 形式では、ルールの順番の数字の文字列。
     * Json 形式で、名前 "name" が与えられている場合は、その名前（文字列）。
     * "name" が与えられていなければ、ルールの順番の数字の文字列。
     * ログ出力で使用。
     */
    public String ruleName = null;
    public String getRuleName() { return ruleName ; } ;
    
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 設定文字列（generation file 中の設定情報の文字列)。
     * ログ出力でのみ使用。
     */
    private String configLine;
    final public String getConfigLine() { return configLine ; }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * エージェントクラスの規定値
     */
    static public String DefaultAgentClassName = "NaiveAgent" ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントクラスの名前を格納。
     */
    public String agentClassName = DefaultAgentClassName ;
    
    private Term agentConf = null ; // config in json Term
    public Term getAgentConf() { return agentConf ; }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback parameters
     */
    public Term fallbackParameters = null ;

    //------------------------------------------------------------
    /**
     *  Config によるコンストラクタ
     */
    public AgentFactory(Config config, Random _random) {
        random = _random;
        init(config, _random) ;
    }

    //------------------------------------------------------------
    /**
     *  初期化。他で Override できるように。
     */
    public void init(Config config, Random _random) {
        if(config.agentClassName != null &&
           config.agentClassName.length() > 0) {
            agentClassName = config.agentClassName ;
            agentConf = config.agentConf ;
        }
        goal = config.goal ;
        plannedRoute = config.plannedRoute ;
        startTime = config.startTime ;
        duration = config.duration ;
        total = config.total ;
        fallbackParameters = config.fallbackParameters ;
        speedModel = config.speedModel ;
        configLine = config.originalInfo ;
        ruleName = config.ruleName ;

        parse_conditions(config.conditions);
    }

    //------------------------------------------------------------
    /**
     *  生成ルールの conditions の処理。
     *  これは単純に、生成したエージェントへのタグとなる。
     */
    protected void parse_conditions(String[] conditions) {
        if (conditions == null) return;
        for (int i = 0; i < conditions.length; i++) {
            final String condition = Itk.intern(conditions[i]) ;
            tags.add(condition);
        }
    }

    //------------------------------------------------------------
    /**
     *  終了判定。
     *  tryUpdateAndGenerate だけから参照。
     */
    protected boolean isFinished(SimTime currentTime) {
        return (currentTime.calcDifferenceFrom(startTime) > duration &&
                generated >= total);
    }

    //------------------------------------------------------------
    /**
     * エージェントを初期化。
     * すぐにエージェントに制御を戻す。
     * （ruby generation rule への対応のため）
     */
    public void initAgent(AgentBase agent, Term fallback) {
        agent.initByFactory(this, fallback) ;
    }
    
    //------------------------------------------------------------
    /**
     * エージェントを初期位置(startPlace)に置く。
     */
    abstract protected void placeAgent(AgentBase agent);

    /* [2014.12.24 I.Noda] should fixed
     * 以下のアルゴリズム、正確に正しく total のエージェントを生成しない
     * 可能性がある。
     * 予め total 個の乱数時刻を発生させ、それをソートしておき、
     * 指定した時間までの分を生成するようにすべき。
     */
    int generated = 0;

    /* TODO must wait finish until generate &
     * must control here with scenario */
    //------------------------------------------------------------
    /**
     * エージェント生成
     */
    public void tryUpdateAndGenerate(EvacuationSimulator simulator,
                                     SimTime currentTime,
                                     List<AgentBase> agents) {
        if (!isEnabled()) return;

        if (isFinished(currentTime)) {
            enabled = false;
            return;
        }

        if (currentTime.isBefore(startTime)) {
            /* not yet time to start generating agents */
            return;
        }

        double duration_left =
            startTime.getAbsoluteTime() + duration - currentTime.getAbsoluteTime() ;
        int agent_to_gen = total - generated;
        if (duration_left > 0) {
            double r
                = (double)(agent_to_gen) / duration_left * currentTime.getTickUnit() ;
            // tkokada: free timeScale update
            if (((int) r) > agent_to_gen)
                r = agent_to_gen;
            agent_to_gen = (int)r;
            if (random.nextDouble() < r - (double)agent_to_gen) {
                agent_to_gen++;
            }
        }
        /* else, no time left, must generate all remains */

        // fallbacks
        Term fallbackForAgent = getFallbackForAgent() ;

        /* [I.Noda] ここで Agent 生成 */
        for (int i = 0; i < agent_to_gen; ++i) {
            generated++;
            launchAgent(agentClassName,
                        simulator,
                        currentTime,
                        agents,
                        fallbackForAgent) ;
        }
    }

    //------------------------------------------------------------
    /**
     * agent 一人を生成。
     * @param agentClassName : エージェントクラス名。
     */
    public AgentBase launchAgent(String agentClassName,
                                 EvacuationSimulator simulator,
                                 SimTime currentTime,
                                 List<AgentBase> agents,
                                 Term fallbackForAgent) {
        AgentBase agent = null ;
        try {
            agent = newAgentByName(agentClassName) ;
            agent.init(random, simulator, this, currentTime,
                       fallbackForAgent) ;
        } catch (Exception ex ) {
            Itk.logError("class name not found") ;
            Itk.logError_("agentClassName", agentClassName) ;
            ex.printStackTrace();
            System.exit(1) ;
        }

        agents.add(agent);
        placeAgent(agent); // この時点では direction が 0.0 のため、add_agent_to_lane で agent は登録されない
        agent.prepareForSimulation() ;
        agent.getCurrentLink().agentEnters(agent);  // ここで add_agent_to_lane させる
        return agent ;
    }

    //------------------------------------------------------------
    /**
     * agent 用の fallback を取得。
     */
    
    public Term getFallbackForAgent() {
        if (fallbackParameters != null) {
            return SetupFileInfo.filterFallbackTerm(fallbackParameters,
                                                    "agent") ;
        } else {
            return null ;
        }
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * 扱い可能な directive かのチェック
     */
    public boolean isKnownDirectiveInAgentClass(Term directive) {
        return isKnownDirectiveInAgentClass(agentClassName, directive) ;
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * 扱い可能な directive かのチェック
     */
    static public boolean isKnownDirectiveInAgentClass(String className,
                                                       Term directive) {
        try {
            return
                (Boolean)
                classFinder
                .callMethodForClass(className, "isKnownDirective", false,
                                    directive) ;
            /*
            Class<?> klass = classFinder.get(className) ;
            Object agent = getDummyAgent(klass) ;
            Method method = klass.getMethod("isKnownDirective",Term.class) ;
            return (boolean)method.invoke(agent,directive) ;
            */
        } catch(Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("can not check the directive") ;
            Itk.logError_("directive", directive) ;
            Itk.logError_("agentClass", className) ;
            return false ;
        }
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * directive の中の経由場所tagの取り出し
     */
    public int pushPlaceTagInDirectiveByAgentClass(Term directive,
                                                   ArrayList<Term> nodeTagList,
                                                   ArrayList<Term> linkTagList) {
        return pushPlaceTagInDirectiveByAgentClass(agentClassName,
                                                   directive,
                                                   nodeTagList,
                                                   linkTagList) ;
    }

    //------------------------------------------------------------
    /**
     * agentClassName で指定されたエージェントクラスで
     * directive の中の経由場所tagの取り出し
     */
    static public int pushPlaceTagInDirectiveByAgentClass(String className,
                                                          Term directive,
                                                          ArrayList<Term> nodeTagList,
                                                          ArrayList<Term> linkTagList)
    {
        try {
            return
                (Integer)
                classFinder
                .callMethodForClass(className, "pushPlaceTagInDirective", false,
                                    directive, nodeTagList, linkTagList) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("can not pushPlaceTag.") ;
            Itk.logError_("directive", directive) ;
            Itk.logError_("agentClass", className) ;
            return 0 ;
        }
    }

    //------------------------------------------------------------
    /**
     * planned route の中で、経由地点の tag を取り出す。
     * directive の中の経由場所tagも Agent Class に応じて取り出す。
     */
    public ArrayList<Term> getNakedPlannedRoute() {
        ArrayList<Term> routeTags = new ArrayList<Term>();

        for(Term candidate : plannedRoute) {
            if(isKnownDirectiveInAgentClass(candidate)) {
                pushPlaceTagInDirectiveByAgentClass(candidate, routeTags, routeTags) ;
            } else {
                routeTags.add(candidate);
            }
        }
        return routeTags;
    }

    //------------------------------------------------------------
    /**
     * planned route のクローンを作る。
     * agent の generation で利用。
     */
    public List<Term> clonePlannedRoute() {
        if(getPlannedRoute() == null) {
            return new ArrayList<Term>() ;
        } else {
            return new ArrayList<Term>(getPlannedRoute()) ;
        }
    }

    //------------------------------------------------------------
    /**
     * エージェント生成ルールの情報を文字列で返す。
     * パネル表示用。
     */
    abstract public String getStartInfo();

    //------------------------------------------------------------
    /**
     * エージェント生成の出発地点のオブジェクトを返す。
     * パネル用。
     */
    abstract public OBNode getStartObject();

    public int getMaxGeneration() {
        return total;
    }

    public void setRandom(Random _random) {
        random = _random;
    }

    public void setDuration(double _duration) {
        duration = _duration;
    }

    public double getDuration() {
        return duration;
    }
}

//;;; Local Variables:
//;;; mode:java
//;;; tab-width:4
//;;; End:
