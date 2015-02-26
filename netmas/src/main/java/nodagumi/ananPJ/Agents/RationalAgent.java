// -*- mode: java; indent-tabs-mode: nil -*-
/** Rational Agent
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/02/15 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/02/15]: Create This File. </LI>
 *   <LI> [YYYY/MM/DD]: add more </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Agents;

import java.io.Serializable;
import java.util.Random;
import java.util.HashMap;

import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.BustleAgent ;
import nodagumi.ananPJ.Agents.Think.ThinkEngine;

import nodagumi.Itk.*;

//======================================================================
/**
 * 理性のある、ルールに基づいて行動するエージェント
 */
public class RationalAgent extends BustleAgent
    implements Serializable {
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * クラス名。
     * ClassFinder でも参照できるようにしておく。
     */
    public static String typeString = "RationalAgent" ;
    public static String getTypeName() { return typeString ;}

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * 気まぐれ度合い（cost への random の度合い）の規定値
     */
    static final public double FallBack_CapriciousMargin = 200.0 ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 気まぐれ度合い（cost への random の度合い）
     */
    public double capriciousMargin = FallBack_CapriciousMargin ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 推論エンジン
     */
    public ThinkEngine thinkEngine = new ThinkEngine() ;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * alert された message
     */
    public HashMap<Term, Double> alertedMessageTable =
        new HashMap<Term, Double>() ;

    //------------------------------------------------------------
    // コンストラクタ
    //------------------------------------------------------------
    /**
     * 引数なしconstractor。 ClassFinder.newByName で必要。
     */
    public RationalAgent(){}

    //------------------------------------------------------------
    /**
     * constractor。
     */
    public RationalAgent(int _id, Random _random) {
        init(_id, _random) ;
    }

    //------------------------------------------------------------
    /**
     * 複製操作のメイン
     */
    public AgentBase copyAndInitializeBody(AgentBase _r) {
        RationalAgent r = (RationalAgent)_r ;
        super.copyAndInitializeBody(r) ;
        r.capriciousMargin = capriciousMargin ;
        return r ;
    }

    //------------------------------------------------------------
    /**
     * Conf による初期化。
     */
    @Override
    public void initByConf(Term conf, Term fallback) {
        super.initByConf(conf, fallback) ;

        capriciousMargin = getDoubleFromConfig("margin", capriciousMargin) ;

        thinkEngine.setAgent(this) ;
        thinkEngine.setRule(getTermFromConfig("rule", null)) ;
    } ;

    //------------------------------------------------------------
    /**
     * あるwayを選択した場合の目的地(_target)までのコスト。
     * 正規のコストに、ランダム要素を加味する。
     */
    @Override
    public double calcWayCostTo(MapLink _way, MapNode _node, Term _target) {
        double cost = super.calcWayCostTo(_way, _node, _target) ;
        double noise = capriciousMargin * random.nextDouble() ;
        return cost + noise;
    }

    //------------------------------------------------------------
    // alertMessage
    //------------------------------------------------------------
    /**
     * Alert 関係
     */
    public void alertMessage(Term message, double time) {
        alertedMessageTable.put(message, time) ;
    }

    //------------------------------------------------------------
    // 推論
    //------------------------------------------------------------
    /**
     * シミュレーション各サイクルの前半に呼ばれる。
     */
    @Override
    public void preUpdate(double time) {
        currentTime = time ;
        thinkCycle() ;
        super.preUpdate(time) ;
    }

    //------------------------------------------------------------
    /**
     * 思考ルーチン
     * 状態が変わる毎に呼ばれるべき。
     */
    public Term thinkCycle() {
        return thinkEngine.think() ;
    }

} // class RationalAgent
