// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.Simulator.Obstructer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import nodagumi.ananPJ.Agents.AgentBase;

import nodagumi.Itk.*;

/**
 * エージェントの歩行及び生命維持を妨害する要素.
 */
public abstract class ObstructerBase {
    public static enum TriageLevel { GREEN, YELLOW, RED, BLACK }

    /**
     * Obstructer type とクラス名の対応表
     */
    public static String OBSTRUCTER_CLASSES = "/obstructer_classes.json";

    /**
     * エージェントへのリンク
     */
    protected AgentBase agent = null ;

    /**
     * 現状のトリアージレベル
     */
    protected TriageLevel currentTriageLevel = TriageLevel.GREEN;

    /**
     * 前回のトリアージレベル(更新チェック用)
     */
    protected TriageLevel lastTriageLevel = TriageLevel.GREEN;

    /**
     * エージェントが死亡しているかどうかのフラグ
     */
    protected boolean dead = false;

    /**
     * 初期化.
     */
    public void init(AgentBase agent) {
        this.agent = agent;
    }

    public abstract void expose(double exposureAmount);
    public abstract void effect();
    protected abstract TriageLevel calcTriage();

    /* the state of the agent */
    public int getTriage() {
        return currentTriageLevel.ordinal();
    }

    /**
     * 現状の暴露量によるエージェントの死亡判定
     */
    protected boolean calcDead() {
        return getTriage() >= TriageLevel.BLACK.ordinal();
    }

    /**
     * エージェントが死亡しているかどうか
     */
    public boolean isDead() {
        return dead;
    }

    //--------------------------------------------------
    /**
     * dumpResult 用の値。(現在値)
     */
    abstract public double currentValueForLog() ;

    //--------------------------------------------------
    /**
     * dumpResult 用の値。(累積値)
     */
    abstract public double accumulatedValueForLog() ;

    //============================================================
    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    private static ClassFinder classFinder = new ClassFinder() ;

    //============================================================
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    /**
     * デフォルトのPollutionクラス登録
     */
    static {
        classFinder.aliasByJson(resourceToString(OBSTRUCTER_CLASSES));
    }

    /**
     * サブクラスのインスタンスを生成する
     */
    public static ObstructerBase createInstance(String className) {
        try {
            return (ObstructerBase)classFinder.newByName(className) ;
        } catch (Exception ex) {
            ex.printStackTrace() ;
            Itk.logError("pollution_type の設定が間違っています。",
                         className) ;
            System.exit(1);
        }
        return null;
    }

    /**
     * サブクラスのインスタンスを生成して初期化する
     */
    public static ObstructerBase createAndInitialize(String className, AgentBase agent) {
        ObstructerBase obstructer = createInstance(className);
        obstructer.init(agent);
        return obstructer;
    }

    /**
     * リソースファイルを文字列に変換する
     */
    public static String resourceToString(String resourceName) {
        StringBuilder buff = new StringBuilder();
        try {
            InputStream is = ObstructerBase.class.getResourceAsStream(resourceName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                buff.append(line);
            } 
            br.close();
        } catch (Exception ex) {
            Itk.logError("リソースファイルの読み込みに失敗しました", resourceName);
            System.exit(1);
        }
        return buff.toString();
    }
}