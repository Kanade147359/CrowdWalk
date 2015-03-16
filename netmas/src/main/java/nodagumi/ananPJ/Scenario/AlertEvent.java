// -*- mode: java; indent-tabs-mode: nil -*-
/** Itk EventBase.java
 * @author:: Itsuki Noda
 * @version:: 0.0 2015/01/29 I.Noda
 * <B>History:</B>
 * <UL>
 *   <LI> [2015/01/29]: Create This File. </LI>
 * </UL>
 * <B>Usage:</B>
 * ...
 */

package nodagumi.ananPJ.Scenario;

import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.Scenario.Scenario;

import nodagumi.Itk.* ;

//============================================================
/**
 * Alert Event (EVACUATE)。
 * 指定した場所のエージェントにメッセージを伝える。
 * <pre>
 *  { "type" : "Alert",
 *    "atTime" : __Time__,
 *    "placeTag" : __Tag__,
 *    "message" : __String__}
 *
 *  __Time__ ::= "hh:mm:ss"
 * </pre>
 * <p>
 * [2015.01.21 I.Noda] 現状では避難だけだが、
 * いろいろな指示（エージェントの意思変更）・情報提供(エージェントへの条件付与)
 * できるようにしたほうが良い。
 * [2015.0216 I.Noda] 上記を実装。
 */
public class AlertEvent extends PlacedEvent {
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 情報を示すフラグ
     */
    public Term message = null ;

    //----------------------------------------
    /**
     * JSON Term による setup
     */
    public void setupByJson(Scenario _scenario,
                            Term eventDef) {
        super.setupByJson(_scenario, eventDef) ;

        message = eventDef.getArgTerm("message") ;
    }

    //----------------------------------------
    /**
     * 終了イベント発生処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean occur(double time, NetworkMapBase map) {
	return occur(time, map, false) ;
    }

    //----------------------------------------
    /**
     * 終了イベント発生逆処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @return : true を返す。
     */
    @Override
    public boolean unoccur(double time, NetworkMapBase map) {
	return occur(time, map, true) ;
    } ;

    //----------------------------------------
    /**
     * 終了イベント発生処理
     * @param time : 現在の絶対時刻
     * @param map : 地図データ
     * @param inverse : 逆操作かどうか
     * @return : true を返す。
     */
    public boolean occur(double time, NetworkMapBase map, boolean inverse) {
        for(MapLink link : map.getLinks()) {
            if(link.hasTag(placeTag)) {
                if(message == null) { 
                    /* [2015.02.15 I.Noda] should be obsolete.
                     * for backward compatibility
                     */
                    link.setEmergency(!inverse) ;
                } else {
                    double relativeTime = scenario.calcRelativeTime(time) ;
                    link.addAlertMessage(message, relativeTime, !inverse) ;
                }
            }
        }
        return true ;
    }
} // class AlertEvent

