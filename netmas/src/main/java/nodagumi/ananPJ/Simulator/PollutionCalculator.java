// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;
import java.util.regex.Matcher;

import javax.media.j3d.Appearance;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMapBase;
import nodagumi.ananPJ.NetworkParts.Area.MapArea;
import nodagumi.ananPJ.NetworkParts.Link.*;
import nodagumi.ananPJ.NetworkParts.Node.*;
import nodagumi.ananPJ.misc.NetmasPropertiesHandler;

import nodagumi.Itk.*;


//======================================================================
/**
 * Pollution による影響を計算する部分。
 */
public class PollutionCalculator {

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * エージェントの高さ。
     * エージェントが Area に入っているとき、確実に床より上の位置に
     * するための下駄。
     * Area の inside-outside は、空間的に行なっているため、必要。
     */
    static double AGENT_HEIGHT = 1.5;

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * 次に pollution を更新する時刻。
     */
    double nextInstantTime = 0;

    /**
     * タイムスケール。被曝量を計算するときに用いる。
     */
    double timeScale;

    /**
     * 最大の pollution level。
     */
    private double maxPollutionLevel = 0.0;

    /**
     * TAG と index のマップ。
     */
    HashMap<String, Integer> tagIndexTable = new HashMap<String, Integer>() ;

    /**
     * TAG のリスト。tagIndexTable の逆テーブルになっている。
     */
    ArrayList<String> tagList = new ArrayList<String>() ;

    /**
     * area と index のマップ。
     */
    HashMap<MapArea, Integer> areaIndexTable = new HashMap<MapArea, Integer>() ;

    /**
     * Pollution の時間変化の配列。
     */
    private ArrayList<PollutionInstant> pollutionInstantList =
        new ArrayList<PollutionInstant>();

    /**
     * 上記時間変化を順番に取り出すためのもの。
     */
    private Iterator<PollutionInstant> pollutionInstantIterator = null;

    /**
     * 現在の pollution。
     */
    private PollutionInstant currentInstant = null;

    //------------------------------------------------------------
    /**
     * コンストラクタ。
     */
    public PollutionCalculator(String scheduleFileName,
            ArrayList<MapArea> _areaList, double _timeScale, double interpolationInterval) {
        if (scheduleFileName == null || scheduleFileName.isEmpty()) {
	    Itk.logInfo("Load Pollution File", "(none)") ;
            nextInstantTime = -1.0;
        } else {
            readData(scheduleFileName);
	    Itk.logInfo("Load Pollution File", scheduleFileName);
	    Itk.logInfo("MAX Pollution Level", maxPollutionLevel) ;
            linearInterpolation(interpolationInterval);
            pollutionInstantIterator = pollutionInstantList.iterator();
            if (pollutionInstantIterator.hasNext()) {
                currentInstant = pollutionInstantIterator.next();
                nextInstantTime = currentInstant.relativeTime ;
            } else {
                nextInstantTime = -1.0;
            }
        }
        
        setupPollutedAreas(_areaList);
        timeScale = _timeScale;
    }

    //------------------------------------------------------------
    /**
     * pollution data を読み込む。
     * <ul>
     *   <li> ファイルの形式は、CSV。</li>
     *   <li> "#" で始まる行はコメント行 </li>
     *   <li> 各行の先頭は、開始からの時刻。続いて、各エリアの density が並ぶ。</li>
     *   <li> 各エリアとの対応は、各 Area (MapAreaRectangle) の tag と、density の序数。</li>
     *   <li> 各エリアは、序数と同じ整数値のタグを持つ。</li>
     * </ul>
     * 読み込まれたデータは、pollutionInstantList に入れられる。
     * pollutionInstantList の各要素は、[時刻、density1, density2, ...] という
     * PollutionInsatnt の配列。
     */
    private void readData(String fileName) {
        pollutionInstantList.clear();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line = reader.readLine();
            while (line != null) {
                if (! line.trim().startsWith("#")) {
                    String[] strItems = line.split(",");
                    PollutionInstant instant = new PollutionInstant() ;
                    instant.relativeTime = Double.parseDouble(strItems[0]) ;
                    for (int index = 1; index < strItems.length; index++) {
                        double value = Double.parseDouble(strItems[index]) ;
                        // 先頭は time なので、１つずらす。
                        instant.setValue(index-1, value) ;
                        // 新しいタグ生成が必要ならば、それをチェック。
                        if((index-1) >= tagList.size()) {
                            String tag = (new Integer(index)).toString() ;
                            addTag(tag) ;
                        }
                        // pollution の最大値を求める
                        if (value > maxPollutionLevel) {
                            maxPollutionLevel = value;
                        }
                    }
                    pollutionInstantList.add(instant);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    //------------------------------------------------------------
    /**
     * pollution データを interval 秒区分で線形補間する
     */
    private void linearInterpolation(double interval) {
        if (interval <= 0.0 || pollutionInstantList.isEmpty()) {
            return;
        }
        ArrayList<PollutionInstant> interpolatedPollutionInstantList =
            new ArrayList<PollutionInstant>();
        PollutionInstant lastInstant = null;
        for (PollutionInstant instant : pollutionInstantList) {
            if (lastInstant != null) {
                double lastEventTime = lastInstant.relativeTime ;
                double eventTime = instant.relativeTime ;
                if ((eventTime - lastEventTime) > interval) {
                    // 線形補間
                    for (double time = lastEventTime + interval; time < eventTime; time += interval) {
                        PollutionInstant interpolatedInstant
                            = new PollutionInstant() ;
                        interpolatedInstant.relativeTime = time;
                        for (int index = 0; index < instant.valueSize(); index++) {
                            double a = (time - lastEventTime) / (eventTime - lastEventTime);    // 補間係数
                            double v = (lastInstant.getValue(index) +
                                        a * (instant.getValue(index) -
                                             lastInstant.getValue(index)));
                            interpolatedInstant.setValue(index, v) ;
                        }
                        interpolatedPollutionInstantList.add(interpolatedInstant);
                    }
                }
            }
            interpolatedPollutionInstantList.add(instant);
            lastInstant = instant;
        }
        pollutionInstantList.clear();
        pollutionInstantList.addAll(interpolatedPollutionInstantList);
    }

    //------------------------------------------------------------
    /**
     * 毎回呼ばれるメインのサイクル。
     */
    public void updateNodesLinksAgents(double time,
                                       NetworkMapBase map,
                                       Collection<AgentBase> agents) {

        if (nextInstantTime != -1.0 && nextInstantTime <= time) {
            // System.out.println("  PC update next event: " + time);
            updatePollution();

            // pollution対象リンクの汚染フラグを更新する(汚染度が0に戻ることも考慮する)
	    for (MapLink link : map.getLinks()) {
                if (link.getIntersectedMapAreas().isEmpty()) {
                    continue;
                }
                link.setPolluted(false);
                for (MapArea area : link.getIntersectedMapAreas()) {
                    if (area.getPollutionLevel() != 0.0) {
                        link.setPolluted(true);
                        break;
                    }
                }
            }
        }

        for (AgentBase agent : agents) {
            if (agent.isEvacuated())
                continue;
            if (! agent.getCurrentLink().isPolluted()) {
                agent.exposed(0.0);
                continue;
            }

            Double pollutionLevel = null;
            Vector3f point = new Vector3f((float)agent.getPos().getX(),
                    (float)agent.getPos().getY(),
                    (float)(agent.getHeight() + AGENT_HEIGHT));
            for (MapArea area : agent.getCurrentLink().getIntersectedMapAreas()) {
                if (area.contains(point)) {
                    pollutionLevel = area.getPollutionLevel() ;
                    agent.exposed(pollutionLevel * timeScale);
                    Itk.logNone("polluted", agent.ID + " " + pollutionLevel) ;
                    break;
                }
            }
            if (pollutionLevel == null) {
                agent.exposed(0.0);
            }
        }
    }

    //------------------------------------------------------------
    /**
     * Pollution Area のタグがついた MapArea を探す。
     * [2015.06.12 I.Noda]
     * 現状の設定では、数字のみのタグを、Pollution 用のタグと解釈する。
     * これは、あまりにひどい設計なので、修正が必要。
     * また、ある Index を示すタグを持つ Area はただひとつであることを
     * 仮定している。
     * この設定も、まずいか、あるいは、ただひとつであることをチェックする
     * 機能が必要。
     */
    private void setupPollutedAreas(ArrayList<MapArea> areas) {
        for (MapArea area : areas) {
            String matchedTag = null ;
            for(String tag : tagList) {
                if(area.hasTag(tag)) {
                    if(areaIndexTable.containsKey(area)) {
                        Itk.logWarn("The area matchs with multiple pollution tag",
                                    area, tag) ;
                    } else {
                        Integer index = tagIndexTable.get(tag) ;
                        areaIndexTable.put(area, index) ;
                        area.setPollutionLevel(0.0) ;
                    }
                }
            }
        }
    }

    //------------------------------------------------------------
    /**
     * pollution を更新する際に呼ばれる。
     */
    private void updatePollution() {
        Itk.logDebug("PC: updating pollution ",nextInstantTime);

        for (HashMap.Entry<MapArea, Integer> entry : areaIndexTable.entrySet()) {
            MapArea area = entry.getKey() ;
            Integer index = entry.getValue() ;

            double pollutionLevel = currentInstant.getValue(index) ;
            area.setPollutionLevel(pollutionLevel);
        }

        if (pollutionInstantIterator.hasNext()) {
            currentInstant = pollutionInstantIterator.next();
            nextInstantTime = currentInstant.relativeTime ;
        } else {
            nextInstantTime = -1.0;
        }
    }
    
    //------------------------------------------------------------
    /**
     * pollution されたエリアを返す。
     */
    public ArrayList<MapArea> getPollutions() {
        return new ArrayList<MapArea>(areaIndexTable.keySet());
    }

    //------------------------------------------------------------
    /**
     * pollution の最大値。
     */
    public double getMaxPollutionLevel() { return maxPollutionLevel; }

    //------------------------------------------------------------
    /**
     * Tag の登録。
     */
    private boolean addTag(String tag) {
        if(tagIndexTable.containsKey(tag)) {
            Itk.logWarn("duplicated tag",tag) ;
            return false ;
        } else {
            int index = tagList.size() ;
            tagList.add(tag) ;
            tagIndexTable.put(tag, index) ;
            return true ;
        }
    }


    //============================================================
    /**
     * ある特定時点での Pollution 状況。
     */
    public static class PollutionInstant {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * タイムスライスの時刻。
         * 実際には、このタイムスライスが終わる時刻。
         */
        public double relativeTime = 0.0 ;

        /**
         * Pollution の値
         */
        private ArrayList<Double> value = new ArrayList<Double>() ;

        //--------------------------------------------------
        /**
         * Pollution の値を取得。
         */
        public Double getValue(int index) {
            return value.get(index) ;
        }

        //--------------------------------------------------
        /**
         * Pollution の値をセット。
         */
        public void setValue(int index, double val) {
            while(value.size() <= index) { value.add(0.0) ;}
            value.set(index, val) ;
        }

        //--------------------------------------------------
        /**
         * Pollution の値の数。
         */
        public int valueSize() {
            return value.size() ;
        }

    } // end class PollutionInstant

}
