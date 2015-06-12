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


public class PollutionCalculator {
    static double AGENT_HEIGHT = 1.5;

    double nextEvent = 0;
    double timeScale;
    private double maxPollutionLevel = 0.0;

    public static boolean debug = false;

    HashMap<Integer, MapArea> polluted_area_sorted;

    private ArrayList<PollutionInstant> pollutionInstantList =
        new ArrayList<PollutionInstant>();

    private Iterator<PollutionInstant> pollutionInstantIterator = null;

    private PollutionInstant nextInstant = null;

    public PollutionCalculator(String scheduleFileName,
            ArrayList<MapArea> _pollution, double _timeScale, double interpolationInterval) {
        if (scheduleFileName == null || scheduleFileName.isEmpty()) {
	    Itk.logInfo("Load Pollution File", "(none)") ;
            nextEvent = -1.0;
        } else {
            readData(scheduleFileName);
	    Itk.logInfo("Load Pollution File", scheduleFileName);
	    Itk.logInfo("MAX Pollution Level", maxPollutionLevel) ;
            linearInterpolation(interpolationInterval);
            pollutionInstantIterator = pollutionInstantList.iterator();
            if (pollutionInstantIterator.hasNext()) {
                nextInstant = pollutionInstantIterator.next();
                nextEvent = nextInstant.relativeTime ;
            } else {
                nextEvent = -1.0;
            }
        }
        
        setup_polluted_areas(_pollution);
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

    // pollution データを interval 秒区分で線形補間する
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

    public void updateNodesLinksAgents(double time,
                                       NetworkMapBase map,
                                       Collection<AgentBase> agents) {
        //if (debug) System.err.println("PC update: " + time + ", next: " + nextEvent);

        if (nextEvent != -1.0 && nextEvent <= time) {
            // System.out.println("  PC update next event: " + time);
            update_pollution();

            // pollution対象リンクの汚染フラグを更新する(汚染度が0に戻ることも考慮する)
	    for (MapLink link : map.getLinks()) {
                if (link.getIntersectedMapAreas().isEmpty()) {
                    continue;
                }
                link.setPolluted(false);
                for (MapArea area : link.getIntersectedMapAreas()) {
                    if ((Double)area.getUserObject() != 0.0) {
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
                    pollutionLevel = (Double)area.getUserObject();
                    if (debug) System.err.println(agent.ID + " " + pollutionLevel);
                    agent.exposed(pollutionLevel * timeScale);
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
    private void setup_polluted_areas(ArrayList<MapArea> areas) {
        polluted_area_sorted = new HashMap<Integer, MapArea>();

        //System.out.println("in setup_polluted_areas");

        for (MapArea area : areas) {
            //System.out.println("in setup_polluted_areas"+areas);

            Matcher m = area.matchTag("^(\\d+)$");
            if (m != null) {
                int index = Integer.parseInt(m.group(0));

                polluted_area_sorted.put(index, area);
            }
            // current density 値の初期化
            area.setUserObject(new Double(0.0));
        }
    }

    private void update_pollution() {
        Itk.logDebug("PC: updating pollution ",nextEvent);

        for (Integer index : polluted_area_sorted.keySet()) {
            MapArea area = polluted_area_sorted.get(index);

            /* [2015.06.12 I.Noda]
             * 現状で、Area のタグに書かれる数字の Index と、
             * pollutionInstant の中のindex はひとつずれている。
             * いずれは、タグ（文字列として）と pollutionInstant 内の
             * index を、hash table で結ばないといけない。
             */
            Double pollutionLevel = nextInstant.getValue(index-1) ;

            area.setUserObject(pollutionLevel);
        }

        if (pollutionInstantIterator.hasNext()) {
            nextInstant = pollutionInstantIterator.next();
            nextEvent = nextInstant.relativeTime ;
        } else {
            nextEvent = -1.0;
        }
    }
    
    public ArrayList<MapArea> getPollutions() {
        return new ArrayList<MapArea>(polluted_area_sorted.values());
    }

    public double getMaxPollutionLevel() { return maxPollutionLevel; }


    //============================================================
    /**
     * ある特定時点での Pollution 状況。
     */
    public static class PollutionInstant {
        //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
        /**
         * タイムスライスの時刻
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
