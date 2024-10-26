// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Agents;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import math.geom3d.Vector3D;

import org.w3c.dom.Element;

import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.OBMapPart;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.NetworkMap.Link.*;
import nodagumi.ananPJ.NetworkMap.Link.MapLink.Direction;
import nodagumi.ananPJ.NetworkMap.Node.*;
import nodagumi.ananPJ.Agents.Factory.AgentFactory;
import nodagumi.ananPJ.misc.RoutePlan ;
import nodagumi.ananPJ.misc.Place ;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.misc.SimTime;
import nodagumi.ananPJ.misc.Trail;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.AgentHandler;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase.TriageLevel;

import nodagumi.ananPJ.Agents.WalkAgent.SpeedCalculationModel;

import nodagumi.Itk.* ;

//======================================================================
/**
 * エージェントのベース。抽象クラス。
 */
public abstract class AgentBase extends OBMapPart
implements Comparable<AgentBase> {

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * Agent の詳細設定情報を格納しているもの
     */
    public Term config ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * generatedTime: 生成された時刻
     * finishedTime: ゴールに到達した時刻
     * evacuated: ゴールに到達したかどうかのフラグ
     * stuck: スタックしたかどうかのフラグ(スタックしたら evacuated も true となる)
     */
    public SimTime generatedTime = null;
    public SimTime finishedTime = null;
    private boolean evacuated = false;
    private boolean stuck = false;
    public SimTime currentTime = null ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * handler: 自分を管理している Handler
     */
    protected AgentHandler handler ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 自分を生成した factory
     */
    protected AgentFactory factory ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * goal: 目的地
     * routePlan: 目的地までの経路
     */
    protected Term goal;
    protected RoutePlan routePlan = new RoutePlan();

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 現在地および前回の位置。
     */
    protected Place currentPlace = new Place() ;
    protected Place lastPlace = new Place() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * speed: エージェントが単位時間に進む距離
     * accel: 加速度
     */
    protected double speed;
    protected double accel = 0.0;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * obstructerType: 使用する Obstructer のクラス名
     * obstructer: Obstructer による影響の処理を行う部分。
     */
    protected static String obstructerType = "Flood";
    public ObstructerBase obstructer ;

	//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 経由点の通過情報.
     * [2015.07.11 I.Noda]
     * 現状、使われていない。
     * [2017.06.20 I.Noda]
     * 復活。
     */
    public Trail trail = new Trail() ;

    /**
     * 経由点の通過情報の取得。
     */
    public Trail getTrail() { return trail ; }

    /**
     * 経由点の通過情報を記録するか？
     */
    public boolean doesRecordTrail() { return handler.doesRecordAgentTrail() ; }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 乱数生成器
     */
    protected Random random = null;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    /**
     * リンク上の表示上の横ずれ幅の量
     */
    protected double swing_width;

    /**
     * リンク上の表示上の横ずれ幅の座標差分
     */
    protected Vector3D swing = new Vector3D(0.0, 0.0, 0.0);

    /**
     * 直前の位置。
     * AgentHandler の表示制御あたりで使用。
     */
    protected Point2D lastPosition = null ;

    /**
     * 直前のswing。
     * AgentHandler の表示制御あたりで使用。
     */
    private Vector3D lastSwing = null ;

    //############################################################
    /**
     * 初期化関係
     */
    //------------------------------------------------------------
    /**
     * constractors
     * 引数なしconstractorはClassFinder.newByName で必要。
     */
    public AgentBase() {}

    //------------------------------------------------------------
    /**
     * エージェントのクラス短縮名を取得。
     * AgentBase は abstract なので、null
     */
    public static String getTypeName() { return null; };

    //------------------------------------------------------------
    /**
     * 初期化。constractorから分離。
     * 注意：AgentBase の継承クラスでは、init() よりも initByConf() の方が
     * 先に呼ばれてしまうことに注意。
     */
    public void init(Random _random, EvacuationSimulator simulator, 
                     AgentFactory _factory, SimTime currentTime,
                     Term fallback) {
        super.init(null);
        random = _random;
        swing_width = getRandomDouble() * 2.0 - 1.0;
        calcSwing();
        // ObstructerBase のサブクラスのインスタンスを取得
        obstructer = ObstructerBase.createAndInitialize(obstructerType, this) ;
        //AgentFactory から移したもの
        generatedTime = currentTime ;
        setHandler(simulator.getAgentHandler()) ;

        // 一旦、factory に制御を戻す。
        // （ruby generation rule への対応のため）
        _factory.initAgent(this, fallback) ;
    }

    //------------------------------------------------------------
    /**
     * Factory による初期化。
     * Factory の方から一旦制御を戻す。
     * @param _factory : Agent を生成する Agent Factory。
     * @param fallback : fallback の値を収めたもの。
     */
    public void initByFactory(AgentFactory _factory, Term fallback) {
        setFactory(_factory) ;
        // set route
        //setGoal(new Term(factory.goal, false));
        setGoal(_factory.getGoal()) ; // 多分問題ないはず。[2017.04.22 I.Noda]
        setPlannedRoute(_factory.clonePlannedRoute()) ;
        // tag
        for (final String tag : _factory.getTags()) {
            addTag(tag);
        }
        // further setup by agentConf
        initByConf(_factory.getAgentConf(), fallback) ;
    }
    
    //------------------------------------------------------------
    /**
     * Conf による初期化。
     * 継承しているクラスの設定のため。
     * @param conf json の連想配列形式を scan した Map
     */
    public void initByConf(Term conf, Term fallback){
        if(conf != null) {
            config = conf ;
        } else {
            config = new Term() ;
        }
        if(fallback != null) {
            SetupFileInfo.attachFallback(config, fallback) ;
        }
    } ;

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(double)
     */
    public double getDoubleFromConfig(String slot, double fallback) {
        return SetupFileInfo.fetchFallbackDouble(config, slot, fallback) ;
    }

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(double)
     */
    public double getIntFromConfig(String slot, int fallback) {
        return SetupFileInfo.fetchFallbackInt(config, slot, fallback) ;
    }

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(Term)
     */
    public Term getTermFromConfig(String slot, Term fallback) {
        return SetupFileInfo.fetchFallbackTerm(config, slot, fallback) ;
    }

    //------------------------------------------------------------
    /**
     * Conf からの値の取得(String)
     */
    public String getStringFromConfig(String slot, String fallback) {
        return SetupFileInfo.fetchFallbackString(config, slot, fallback) ;
    }

    //############################################################
    /**
     * インスタンス変数へのアクセス
     */

    //------------------------------------------------------------
    /**
     * ハンドラをセット
     */
    public void setHandler(AgentHandler _handler) {
        handler = _handler ;
        setMap(handler.getMap()) ;
    }

    //------------------------------------------------------------
    /**
     * ハンドラを取得
     */
    public AgentHandler getHandler() {
        return handler ;
    }

    //------------------------------------------------------------
    /**
     * シミュレータへのアクセス
     */
    public EvacuationSimulator getSimulator() {
        return getHandler().getSimulator() ;
    }
    
    //------------------------------------------------------------
    /**
     * factory をセット
     */
    public void setFactory(AgentFactory _factory) {
        factory = _factory ;
    }

    //------------------------------------------------------------
    /**
     * factory をセット
     */
    final public AgentFactory getFactory() {
        return factory ;
    }

    //------------------------------------------------------------
    /**
     * SpeedCalculationModelへのアクセス。
     */
    final public SpeedCalculationModel getSpeedModel() {
        return factory.getSpeedModel() ;
    }

    //------------------------------------------------------------
    /**
     * 避難完了をセット
     */
    public void setEvacuated(boolean evacuated, SimTime evacuateTime,
                             boolean stuck) {
        this.evacuated = evacuated;
        this.finishedTime = evacuateTime ;
        this.stuck = stuck;
    }

    //------------------------------------------------------------
    /**
     * 避難完了かどうかチェック
     */
    final public boolean isEvacuated() {
        return evacuated;
    }

    //------------------------------------------------------------
    /**
     * スタックしたかどうか
     */
    final public boolean isStuck() {
        return stuck;
    }

    //------------------------------------------------------------
    /**
     * 死亡したかどうか
     */
    final public boolean isDead() {
        return obstructer.isDead() ;
    }

    //------------------------------------------------------------
    /**
     * ゴールをセット
     */
    public void setGoal(Term _goal) {
        goal = _goal;
    }

    //------------------------------------------------------------
    /**
     * ゴールを変更
     * 基本的に setGoal と同じだが、
     * シミュレーション途中でゴールを変更する場合に、
     * クラスごとに処理を変更できるようにラッパーとして用意する。
     */
    public void changeGoal(Term _goal) {
        setGoal(_goal) ;
    }

    //------------------------------------------------------------
    /**
     * ゴールを取得
     */
    public Term getGoal() {
        return goal;
    }

    //------------------------------------------------------------
    /**
     * 経路を取得
     */
    public RoutePlan getRoutePlan() {
        return routePlan ;
    } ;

    //------------------------------------------------------------
    /**
     * 全経路をリセット
     */
    public void clearPlannedRoute() {
        setPlannedRoute(new ArrayList<Term>(), true) ;
    }

    //------------------------------------------------------------
    /**
     * 全経路をセット
     */
    public void setPlannedRoute(List<Term> _plannedRoute) {
        setPlannedRoute(_plannedRoute, false) ;
    }

    //------------------------------------------------------------
    /**
     * 全経路をセット
     * @param _plannedRoute : セットするルート(tag の配列)
     * @param resetIndexP : index もリセットするかどうか。
     */
    public void setPlannedRoute(List<Term> _plannedRoute,
                                boolean resetIndexP) {
        routePlan.setRoute(_plannedRoute) ;
        if(resetIndexP) routePlan.resetIndex() ;
    }

    //------------------------------------------------------------
    /**
     * 新しい中継点を挿入。
     */
    public void insertRouteTagSafely(Term tag) {
        routePlan.insertSafely(tag) ;
    }

    //------------------------------------------------------------
    /**
     * 現在の RoutePlan の先頭に新しいsubgoalを追加。(ruby からの呼び出し用)
     */
    public void insertRouteTagSafelyForRuby(Object tag) {
        insertRouteTagSafely(Term.ensureTerm(tag)) ;
    }

    //------------------------------------------------------------
    /**
     * 現在の RoutePlan の先頭をStringで取得。(ruby からの呼び出し用)
     */
    public String getRoutePlanTopInString() {
        return routePlan.top().toString() ;
    }

    //------------------------------------------------------------
    /**
     * 全経路を取得
     */
    public List<Term> getPlannedRoute() {
        return routePlan.getRoute() ;
    }

    //------------------------------------------------------------
    /**
     * 現在の場所を取得
     */
    public Place getCurrentPlace() {
        return currentPlace ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクを取得
     */
    public MapLink getCurrentLink() {
        return currentPlace.getLink() ;
    }

    //------------------------------------------------------------
    /**
     * 直前ノードを取得
     */
    public MapNode getPrevNode() {
        return currentPlace.getEnteringNode() ;
    }

    //------------------------------------------------------------
    /**
     * 次のノードを取得
     */
    public MapNode getNextNode() {
        return currentPlace.getHeadingNode() ;
    }

    //------------------------------------------------------------
    /**
     * ゴール後の最後のノードを取得
     */
    public MapNode getLastNode() {
        return currentPlace.getLastNode() ;
    }

    //------------------------------------------------------------
    /**
     * リンク上の進んだ距離
     */
    public double getAdvancingDistance() {
        return currentPlace.getAdvancingDistance() ;
    }

    //------------------------------------------------------------
    /**
     * リンク上の残りの距離
     */
    public double getRemainingDistance() {
        return currentPlace.getRemainingDistance() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンク上の絶対位置（リンク始点から見た位置）を得る
     */
    public double getPositionOnLink() {
        return currentPlace.getPositionOnLink() ;
    }

    //------------------------------------------------------------
    /**
     * 最後のリンク上の絶対位置（リンク始点から見た位置）を得る
     */
    public double getLastPositionOnLink() {
        return lastPlace.getPositionOnLink() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対する向きを取得
     */
    public Direction getDirection() {
        return currentPlace.getDirection() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対する向きを取得
     */
    public Direction getLastDirection() {
        return lastPlace.getDirection() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対して前向き（リンクの fromNode から toNode）
     * に向かっているか？
     */
    public boolean isForwardDirection() {
        return currentPlace.isForwardDirection() ;
    }

    //------------------------------------------------------------
    /**
     * 現在のリンクに対して逆向き（リンクの toNode から fromNode）
     * に向かっているか？
     */
    public boolean isBackwardDirection() {
        return currentPlace.isBackwardDirection() ;
    }

    //------------------------------------------------------------
    /**
     * エージェントをリンクに配置
     */
    public void place(MapLink link, MapNode enteringNode, 
                      double advancingDistance) {
        currentPlace.set(link, enteringNode, true, advancingDistance) ;
    }

    //------------------------------------------------------------
    /**
     * エージェントをリンクにランダムに配置
     */
    public void placeAtRandomPosition(MapLink link) {
        currentPlace.setAtRandomPosition(link, random) ;
    }

    //------------------------------------------------------------
    /**
     * 向き変更
     */
    protected void turnAround() {
        currentPlace.turnAround() ;
    }
    
    //------------------------------------------------------------
    /**
     * 速度を取得
     */
    public double getSpeed() {
        return speed;
    }

    //------------------------------------------------------------
    /**
     * 速度をセット
     */
    public void setSpeed(double _speed) {
        speed = _speed;
    }

    //------------------------------------------------------------
    /**
     * 加速度を取得
     */
    public double getAcceleration() {
        return accel;
    }

    //------------------------------------------------------------
    /**
     * ???
     */
    public static void setObstructerType(String s) {
        obstructerType = s;
    }
    //------------------------------------------------------------
    /**
     * トリアージレベル
     */
    public TriageLevel getTriage() {
        return obstructer.getTriage() ;
    }

    //------------------------------------------------------------
    /**
     * トリアージレベル（数値）
     */
    public int getTriageInt() {
        return obstructer.getTriageInt() ;
    }

    //------------------------------------------------------------
    /**
     * トリアージレベル（文字列）
     */
    public String getTriageName() {
        return obstructer.getTriageName() ;
    }

    //------------------------------------------------------------
    /**
     * 動作が終了(避難完了または死亡)しているか?
     */
    public boolean finished() {
        return isEvacuated() || isDead();
    }

    //------------------------------------------------------------
    /**
     * 汚染環境(洪水、ガス等)に暴露する
     */
    public void exposed(double c) {
        if (isDead()) {
            return;
        }
        obstructer.expose(c);
        if (isDead()) {
            // do something to record changes by expose()
        }
    }

    //------------------------------------------------------------
    /**
     * 設定文字列（generation file 中の設定情報の文字列）を取得
     */
    final public String getConfigLine() {
        return factory.getConfigLine() ;
    }

    //------------------------------------------------------------
    /**
     * 乱数生成器
     */
    public void setRandom(Random _random) {
        random = _random;
    }

    //------------------------------------------------------------
    /**
     * 乱数生成(実数)
     */
    public double getRandomDouble() {
        return random.nextDouble() ;
    }

    //------------------------------------------------------------
    /**
     * 乱数生成(整数)
     */
    public int getRandomInt() {
        return random.nextInt() ;
    }

    //------------------------------------------------------------
    /**
     * 乱数生成(整数) 上限有り
     */
    public int getRandomInt(int n) {
        return random.nextInt(n) ;
    }

    //############################################################
    /**
     * 経路計画関連
     */

    //------------------------------------------------------------
    /**
     *
     */
    public boolean isPlannedRouteCompleted() {
        return routePlan.isEmpty() ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public void consumePlannedRoute() {
        routePlan.makeCompleted() ;
    }

    //------------------------------------------------------------
    /**
     *
     */
    public String getNextCandidateString() {
        if (isPlannedRouteCompleted()) {
            return "";
        } else {
            Term term = routePlan.top();
            if (term.isString()) {
                return term.getHeadString();
            } else if (term.isNull()) {
                return "";
            } else {
                Term target = (Term)term.getArg("target");
                if (target == null) {
                    return "";
                } else {
                    return target.getHeadString();
                }
            }
        }
    }

    //------------------------------------------------------------
    /**
     * evacuation の完了
     * @param currentTime : 時刻
     * @param onNode : true なら currentPlace.getHeadingNode() 上
     *                 false なら currentPlace.getLink() 上
     * @param stuck : スタックかどうか
     */
    protected void finalizeEvacuation(SimTime currentTime,
                                      boolean onNode, boolean stuck) {
        consumePlannedRoute() ;
        setEvacuated(true, currentTime, stuck) ;
        if(onNode) {
            currentPlace.getHeadingNode().acceptEvacuatedAgent(this,
                                                               currentTime) ;
        }
        if(currentPlace.isWalking()) {
            lastPlace.set(currentPlace) ;
            currentPlace.getLink().agentExits(this) ;
            currentPlace.quitLastLink() ;
        }
        if (getMap() != null) {
            getMap().getNotifier().agentEvacuated(this);
        }
    }

    //############################################################
    /**
     * シミュレーションサイクル
     */
    //------------------------------------------------------------
    /**
     * シミュレーション開始前に呼ばれる。
     */
    abstract public void prepareForSimulation() ;

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    public void preUpdate(SimTime currentTime) {
        this.currentTime = currentTime ;
    }

    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの後半に呼ばれる。
     */
    abstract public boolean update(SimTime currentTime) ;

    //------------------------------------------------------------
    /**
     * 表示用
     */
    abstract public void updateViews();

    //############################################################
    /**
     * エージェントのソート関係
     */
    //------------------------------------------------------------
    /**
     * sort, binarySearch 用比較関数
     * エージェントのリンク上の進み具合で比較。
     * 逆向きなどはちゃんと方向を直して扱う。
     * [2020.02.14 I.Noda]
     * もし、ID が null の場合、null がある方が前に進んでいるとみなす。
     * 両方 null なら、同じ位置と見なす。
     */
    public int compareTo(AgentBase rhs) {
        if(this == rhs) return 0 ;

        double h1 = this.currentPlace.getAdvancingDistance() ;
        double h2 = rhs.currentPlace.getAdvancingDistance() ;

        if(h1 > h2) {
            return 1 ;
        } else if(h1 < h2) {
            return -1 ;
        } else if(ID == null && rhs.ID == null) {
            return 0 ;
        } else if(ID == null) {
            return 1 ;
        } else if(rhs.ID == null) {
            return -1 ;
        } else {
            return ID.compareTo(rhs.ID) ;
        }
    }

    //############################################################
    /**
     * directive 関係
     */
    //------------------------------------------------------------
    /**
     * ある Term が Directive かどうかのチェック
     * 単純に Atom でないかどうかをチェック。
     * 実際の知りするかどうかは、isKnownDirective で定義。
     */
    public boolean isDirectiveTerm(Term term) {
        return !term.isAtom() ;
    }

    //------------------------------------------------------------
    /* [2014.12.29 I.Noda]
     * directive を増やす場合は、継承するクラスで以下２つを再定義していく。
     */
    /**
     * 知っている directive かどうかのチェック
     */
    public boolean isKnownDirective(Term term) {
        return false ;
    }

    //------------------------------------------------------------
    /**
     * Directive のなかの代表的目的地の取得
     * @param directive : 調べる directive。通常の place tag の場合もある。
     *    もし directive が isKnownDirective() なら、なにか返すべき。
     * @return もし directive なら代表的目的地。そうでないなら null
     */
    public Term getPrimalTargetPlaceInDirective(Term directive) {
        return null ;
    }

    //------------------------------------------------------------
    /**
     * ルート のなかの代表的目的地の取得
     * @param workingRoutePlan : 調べるルート。通常の place tag の場合もある。
     *    もし directive が isKnownDirective() なら、なにか返すべき。(???)
     * @return もし directive なら代表的目的地。そうでないなら null
     */
    public Term nakedTargetFromRoutePlan(RoutePlan workingRoutePlan) {
        Term subgoal = workingRoutePlan.top() ;
        if(isKnownDirective(subgoal)) {
            Term nakedSubgoal = getPrimalTargetPlaceInDirective(subgoal) ;
            if(nakedSubgoal != null) {
                return nakedSubgoal ;
            } else {
                return subgoal ;
            }
        } else {
            return subgoal ;
        }
    }

    //------------------------------------------------------------
    /**
     * directive に含まれる目的地タグの抽出
     * @return pushした数
     */
    public int pushPlaceTagInDirective(Term directive,
                                       ArrayList<Term> nodeList,
                                       ArrayList<Term> linkList) {
        int count = 0 ;
        if(isKnownDirective(directive)) {
            Term subgoal = getPrimalTargetPlaceInDirective(directive) ;
            if(subgoal != null) {
                linkList.add(subgoal) ;
                count++ ;
            } else {
                Itk.logWarn("A directive includes no subgoal.") ;
                Itk.logWarn_("directive", directive) ;
            }
        }
        return count ;
    }

    //------------------------------------------------------------
    /* [2014.12.29 I.Noda]
     * 今後の拡張性のため、Route 上にある Atom 以外の Term はすべて
     * Directive とみなす。（つまり、Atom (String) のみを経由地点の tag
     * と扱うことにする。
     */
    // plannedRoute の残り経路がすべて WAIT_FOR/WAIT_UNTIL ならば true を返す
    public boolean isRestAllRouteDirective() {
        if (isPlannedRouteCompleted()) {
            return false;
        }

        int delta = 0 ;
        while (delta < routePlan.length()) {
            Term candidate = routePlan.top(delta);
            if(!isDirectiveTerm(candidate)) {
                return false ;
            }
            delta += 1 ;
        }
        return true;
    }

    //############################################################
    /**
     * Alert 関係
     * RationalAgent 以下でないと意味がない
     */
    public void alertMessage(Term message, SimTime currentTime) {
        // do nothing ;
    }
    //############################################################
    /**
     * 入出力関係
     */

    //------------------------------------------------------------
    /**
     * Agent については、fromDom はサポートしない
     */
    public static OBNode fromDom(Element element) {
        Itk.logError("fromDom() is not supported.") ;
        return null ;
    }

    //------------------------------------------------------------
    /**
     * おそらく、OBNode 汎用のルーチン。
     */
    public final static String getNodeTypeString() {
        return "Agent";
    }
    
    //------------------------------------------------------------
    /**
     * 文字列化
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer() ;
        buffer.append(this.getClass().getSimpleName()) ;
        buffer.append("[") ;
        buffer.append("id:").append(ID) ;
        buffer.append("]") ;
        return buffer.toString() ;
    }

    /**
     * このオブジェクトの状態をテキストで取得する
     */
    public String getStatusText() {
        StringBuilder buff = new StringBuilder();
        buff.append("Agent ID: ").append(this.ID).append("\n");
        buff.append("config: ").append(this.getConfigLine()).append("\n");
        buff.append("type: ").append(this.getClass().getSimpleName()).append("\n");
        buff.append("tags: ").append(this.getTagString()).append("\n");
        buff.append("goal: ").append(this.getGoal()).append("\n");
        buff.append("generated time: ").append(this.generatedTime.getAbsoluteTimeString()).append("\n");
        if (this.isEvacuated()) {
            buff.append("evacuated: true\n");
        } else {
            buff.append("position X: ").append(this.getPosition().getX()).append("\n");
            buff.append("position Y: ").append(this.getPosition().getY()).append("\n");
            buff.append("position Z: ").append(this.getHeight()).append("\n");
            buff.append("drawing position X: ").append(this.getPosition().getX() + this.getSwing().getX()).append("\n");
            buff.append("drawing position Y: ").append(this.getPosition().getY() + this.getSwing().getY()).append("\n");
            buff.append("drawing position Z: ").append(
                this.getHeight() / ((MapPartGroup)this.getCurrentLink().getParent()).getScale()
            ).append("\n");
            buff.append("velocity: ").append(this.getSpeed()).append("\n");
            buff.append("acceleration: ").append(this.getAcceleration()).append("\n");
            buff.append("previous node: ").append(this.getPrevNode().ID).append("\n");
            buff.append("next node: ").append(this.getNextNode().ID).append("\n");
            buff.append("current link: ").append(this.getCurrentLink().ID).append("\n");
            buff.append("advancing distance: ").append(this.getAdvancingDistance()).append("\n");
            buff.append("direction: ").append(this.isForwardDirection() ? "Forward" : "Backward").append("\n");
            buff.append("waiting: ").append(this.isWaiting()).append("\n");
            buff.append("current exposure: ").append(this.obstructer.currentValueForLog()).append("\n");
            buff.append("amount exposure: ").append(this.obstructer.accumulatedValueForLog()).append("\n");
            buff.append("triage: ").append(this.getTriageName()).append("\n");
        }
        return buff.toString();
    }

    //------------------------------------------------------------
    /**
     * リンク上の表示上の横ずれ幅計算
     */
    protected void calcSwing() {
        MapLink currentLink = currentPlace.getLink() ;
        if (null == currentLink) {
            swing = new Vector3D(0.0, 0.0, 0.0);
            return;
        }

        double scale = ((MapPartGroup)(currentLink.getParent())).getScale();
        double fwidth = currentLink.getWidth() / 2 / scale;
        double x1 = currentLink.getFrom().getX();
        double x2 = currentLink.getTo().getX();
        double y1 = currentLink.getFrom().getY();
        double y2 = currentLink.getTo().getY();

        Vector3D v1 = new Vector3D(x2 - x1, y2 - y1, 0.0).normalize();
        Vector3D v2 = new Vector3D(0.0, 0.0, fwidth * swing_width);
        swing = Vector3D.crossProduct(v1, v2);
    }

    /**
     * swing 値の取得
     */
    public Vector3D getSwing() {
        return swing;
    }

    //------------------------------------------------------------
    /**
     * 地図上の絶対位置計算。
     * リンクが直線と仮定。
     */
    public Point2D getPosition() {
        return currentPlace.getPosition() ;
    }

    //------------------------------------------------------------
    /**
     * 地図上の絶対的高さ。
     * リンクが直線と仮定。
     */
    public double getHeight() {
        return currentPlace.getHeight() ;
    }

    /**
     * WAIT_FOR/WAIT_UNTIL 処理中か?
     */
    public boolean isWaiting() {
        return false;
    }

    /**
     * lastPosition を更新する
     *
     * @return 値が変化したら true、変わらなければ false
     */
    public boolean updateLastPosition() {
        Point2D currentPosition = getPosition();
        if (lastPosition == null || ! currentPosition.equals(lastPosition)) {
            lastPosition = currentPosition;
            return true;
        }
        return false;
    }

    /**
     * lastSwing を更新する
     *
     * @return 値が変化したら true、変わらなければ false
     */
    public boolean updateLastSwing() {
        if (lastSwing == null || ! swing.equals(lastSwing)) {
            lastSwing = swing;
            return true;
        }
        return false;
    }

    //------------------------------------------------------------
    /**
     * AgentFactory の individualConfig によりエージェントを設定。
     * <br>
     * Format:
     * <pre>
     *   { 
     *     ...
     *     "place" : { "link" : __LinkId__,
     *                 "enteringNode" : __NodeId__,
     *                 "advancingDistance" : __Distance__ },
     *     "conditions" : [ __Tag__, __Tag__, ... ],
     *     ...
     *   }
     * </pre>
     */
    public void setupByIndividualConfig(Term config) {
        //Itk.dbgVal("indivConfig", config) ;

        setupByIndividualConfig_place(config) ;
        setupByIndividualConfig_conditions(config) ;
        setupByIndividualConfig_goals(config) ;
    }

    //------------------------------
    /**
     * individualConfig 用にエージェント状態をTermにdump。
     * Format は、{@link #setupByIndividualConfig(Term)} に準拠。 
     * @param startTime : エージェント発生時刻。 null なら設定しない。
     */
    public Term dumpTermForIndividualConfig(SimTime startTime) {
        Term config = Term.newObjectTerm() ;
        dumpTermForIndividualConfig_place(config) ;
        dumpTermForIndividualConfig_conditions(config) ;
        dumpTermForIndividualConfig_goals(config) ;

        if(startTime != null) {
            config.setArg("startTime", startTime.getAbsoluteTimeString()) ;
        }
        return config ;
    }
    
    //----------
    /**
     * individualConfig による設定：place.
     */
    private void setupByIndividualConfig_place(Term config) {
        Term _place = SetupFileInfo.fetchFallbackTerm(config,
                                                      "place",
                                                      null) ;
        if(_place != null) {
            boolean isValid = false ;
            if(_place.isObject()) {
                Term linkId = _place.getArgTerm("link") ;
                Term enteringNodeId = _place.getArgTerm("enteringNode") ;
                Term advancingDistTerm =
                    _place.getArgTerm("advancingDistance") ;
                if(linkId != null && enteringNodeId != null) {
                    MapLink link =
                        getMap().findLinkById(linkId.getString()) ;
                    MapNode enteringNode =
                        getMap().findNodeById(enteringNodeId.getString()) ;
                    double advancingDist = 0.0 ;
                    if(advancingDistTerm != null &&
                       advancingDistTerm.isDouble()) {
                        advancingDist = advancingDistTerm.getDouble() ;
                    }
                    place(link, enteringNode, advancingDist) ;
                    isValid = true ;
                }
            }
            if(! isValid) {
                Itk.logError("Illegal place value:",
                             "place=", _place.toJson()) ;
            }
        }
    }
        
    //----------
    /**
     * individualConfig へのdump：place.
     */
    private Term dumpTermForIndividualConfig_place(Term config) {
        Term _place = Term.newObjectTerm() ;
        config.setArg("place", _place) ;

        _place.setArg("link", getCurrentLink().ID) ;
        _place.setArg("enteringNode", getPrevNode().ID) ;
        _place.setArg("advancingDistance", getAdvancingDistance()) ;

        return config ;
    }

    //----------
    /**
     * individualConfig による設定：conditions
     */
    private void setupByIndividualConfig_conditions(Term config) {
        // conditions
        Term _tagList = SetupFileInfo.fetchFallbackTerm(config,
                                                        "conditions",
                                                        null) ;
        if(_tagList != null) {
            if(_tagList.isArray()) {
                for(Object tagObj : _tagList.getArray()) {
                    Term tag = (Term)tagObj ;
                    // Itk.dbgVal("add tag", tag) ;
                    addTag(tag.getString()) ;
                }
            } else {
                Itk.logError("Illegal conditions value:",
                             "condition=", _tagList.toJson()) ;
            }
        }
    }
    
    //----------
    /**
     * individualConfig へのdump：conditions
     */
    private Term dumpTermForIndividualConfig_conditions(Term config) {
        Term _tagList = Term.newArrayTerm() ;
        config.setArg("conditions", _tagList) ;

        for(String tag : getTags()) {
            _tagList.addNth(tag) ;
        }

        return config ;
    }
    
    //----------
    /**
     * individualConfig による設定：goals
     */
    private void setupByIndividualConfig_goals(Term config) {
        if(config.hasArg("goal")) {
            goal = config.getArgTerm("goal") ;
        }
        if(config.hasArg("routePlan")) {
            routePlan.setByTerm(config.getArgTerm("routePlan")) ;
        }
    }
    
    //----------
    /**
     * individualConfig へのdump：goals
     */
    private Term dumpTermForIndividualConfig_goals(Term config) {
        config.setArg("goal", goal) ;
        config.setArg("routePlan",routePlan.toTerm()) ;
            
        return config ;
    }
}
// ;;; Local Variables:
// ;;; mode:java
// ;;; tab-width:4
// ;;; End:
