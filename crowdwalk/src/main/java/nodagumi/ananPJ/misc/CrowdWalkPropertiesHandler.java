// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.misc;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;

import org.apache.commons.cli.*;
import net.arnx.jsonic.JSON;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.Agents.WalkAgent;
import nodagumi.ananPJ.BasicSimulationLauncher;
import nodagumi.ananPJ.Simulator.EvacuationSimulator;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;
import nodagumi.ananPJ.Simulator.AgentHandler;

import nodagumi.Itk.*;

/**
 * プロパティファイルを読み込んで設定値を参照する.
 *
 * <h3>プロパティファイルの記述形式</h3>
 * <ul>
 *   <li>
 *     <p>JSON 形式</p>
 *     <pre>
 * {
 *   "設定項目" : 設定値,
 *            ・
 *            ・
 *            ・
 *   "設定項目" : 設定値
 * }</pre>
 *   </li>
 *   <li>
 *     <p>XML 形式(非推奨)</p>
 *     <pre>
 * {@literal <?xml version="1.0" encoding="UTF-8"?>}
 * {@literal <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">}
 * {@literal <properties>}
 *     {@literal <entry key="設定項目">設定値</entry>}
 *                      ・
 *                      ・
 *                      ・
 * {@literal </properties></pre>}
 *     </pre>
 *   </li>
 * </ul>
 *
 * <h3>プロパティファイルの設定項目</h3>
 * <ul>
 *   <li>
 *     <h4>map_file (設定必須)</h4>
 *     <pre>  Map file へのファイルパス
 *
 *  設定値： 絶対パス | カレントディレクトリからの相対パス | ファイル名のみ
 *           (プロパティファイルと同じディレクトリに存在する場合はファイル名のみでも可)</pre>
 *   </li>
 *
 *   <li>
 *     <h4>generation_file (設定必須)</h4>
 *     <pre>  Generation file へのファイルパス
 *
 *  設定値： 絶対パス | カレントディレクトリからの相対パス | ファイル名のみ
 *           (プロパティファイルと同じディレクトリに存在する場合はファイル名のみでも可)</pre>
 *   </li>
 *
 *   <li>
 *     <h4>scenario_file (設定必須)</h4>
 *     <pre>  Scenario file へのファイルパス
 *
 *  設定値： 絶対パス | カレントディレクトリからの相対パス | ファイル名のみ
 *           (プロパティファイルと同じディレクトリに存在する場合はファイル名のみでも可)</pre>
 *   </li>
 *
 *   <li>
 *     <h4>fallback_file</h4>
 *     <pre>  Fallback file(デフォルトセッティング値を記述した JSON ファイル) へのファイルパス
 *  このファイルの内容はリソースデータ "fallbackParameters.json" の内容よりも優先される。
 *
 *  設定値： 絶対パス | カレントディレクトリからの相対パス | ファイル名のみ
 *           (プロパティファイルと同じディレクトリに存在する場合はファイル名のみでも可)
 *  デフォルト値： なし</pre>
 *   </li>
 *
 *   <li>
 *     <h4>pollution_file</h4>
 *     <pre>  Obstructer file へのファイルパス
 *
 *  設定値： 絶対パス | カレントディレクトリからの相対パス | ファイル名のみ
 *           (プロパティファイルと同じディレクトリに存在する場合はファイル名のみでも可)
 *  デフォルト値： なし</pre>
 *   </li>
 *
 *   <li>
 *     <h4>interpolation_interval</h4>
 *     <pre>  Obstructer データを線形補間する間隔(秒)
 *  Obstructer file が設定されている時に設定する
 *
 *  設定値： 0       補間なし
 *           1～n    この間隔で補間する
 *  デフォルト値： 0</pre>
 *   </li>
 *
 *   <li>
 *     <h4>pollution_type</h4>
 *     <pre>  Obstructer type の設定
 *  Obstructer file が設定されている時に設定する
 *
 *  設定値： Flood | Pollution
 *  デフォルト値： Flood</pre>
 *   </li>
 *
 *   <li>
 *     <h4>pollution_color_saturation</h4>
 *     <pre>  Obstructer level別の色の彩度の設定(図1,2)
 *  Obstructer file が設定されている時に設定する
 *  濃 0.0←--------→100.0 薄
 *
 *  設定値： 有理数
 *  デフォルト値： 0.0</pre>
 * <p>
 *   <center>
 *     <img src="./doc-files/pollution_color_saturation_1.png" alt="./doc-files/pollution_color_saturation_1.png"><br>図1　10.0のとき<br><br>
 *     <img src="./doc-files/pollution_color_saturation_2.png" alt="./doc-files/pollution_color_saturation_2.png"><br>図2　1.0のとき
 *   </center>
 * </p>
 *   </li>
 *
 *   <li>
 *     <h4>pollution_color</h4>
 *     <pre>  Obstructer地点の色の設定(図3)
 *  Obstructer file が設定されている時に設定する
 *
 *  設定値： none | hsv | red | blue | orange
 *           none：   なし
 *           hsv：    Obstructer levelごとに色を変える
 *           red：    赤色
 *           blue：   青色
 *           orange： オレンジ色
 *  デフォルト値： orange</pre>
 * <p>
 *   <center>
 *     <img src="./doc-files/pollution_color.png" alt="./doc-files/pollution_color.png"><br>図3　hsv のとき
 *   </center>
 * </p>
 *   </li>
 *
 *   <li>
 *     <h4>use_ruby</h4>
 *     <pre>  Ruby Engine を使うかどうか。
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>ruby_load_path</h4>
 *     <pre> Ruby の標準の LOAD_PATH に追加するパス。
 *  複数のパスを追加する場合には、改行で区切る。
 *
 *  設定値： 改行区切りの path 文字列。
 *  デフォルト値： ""</pre>
 *   </li>
 *
 *   <li>
 *     <h4>ruby_init_script</h4>
 *     <pre> Ruby Engine 初期化のスクリプト。
 *  一般に、"require 'file'" 等を指定するが、基本、ruby の式なら何でもOK。
 *
 *  設定値： ruby の script の文字列。
 *  デフォルト値： ""</pre>
 *   </li>
 *
 *   <li>
 *     <h4>ruby_simulation_wrapper_class</h4>
 *     <pre> updateEveryTick() などのシミュレーション実行制御用のRubyクラス。
 *  CrowdWalkSimulator クラスを継承していることが望ましい。
 *  シミュレーションの初期化でインスタンスが作成され、
 *  EvacuationSimulatorのインスタンスが @body にセットされる。
 *  updateEveryTick() の先頭で preCycle(time) が、
 *  終わりに postCycle(time) が呼ばれる。
 *  空文字列もしくは null なら、インスタンスは生成しない。
 *
 *  設定値： ruby クラス名。
 *  デフォルト値： null</pre>
 *   </li>
 *
 *   <li>
 *     <h4>camera_file</h4>
 *     <pre>  3D シミュレーション画面のカメラワーク設定ファイル。(JSON形式)
 *  3D シミュレーションウィンドウのオープン時に読み込んで Replay チェックボックスを ON にする。
 *
 *  設定値： 絶対パス | カレントディレクトリからの相対パス | ファイル名のみ
 *           (プロパティファイルと同じディレクトリに存在する場合はファイル名のみでも可)
 *  デフォルト値： なし</pre>
 *   </li>
 *
 *   <li>
 *     <h4>camera_2d_file</h4>
 *     <pre>  2D シミュレーション画面のカメラワーク設定ファイル。(JSON形式)
 *  2D シミュレーションウィンドウのオープン時に読み込んで Replay チェックボックスを ON にする。
 *
 *  設定値： 絶対パス | カレントディレクトリからの相対パス | ファイル名のみ
 *           (プロパティファイルと同じディレクトリに存在する場合はファイル名のみでも可)
 *  デフォルト値： なし</pre>
 *   </li>
 *
 *   <li>
 *     <h4>link_appearance_file</h4>
 *     <pre>  各リンクの設定を記述した設定ファイル
 *
 *  設定値： link_appearance_fileへのファイルパス(JSONまたはXML形式)
 *  デフォルト値： なし</pre>
 *   </li>
 *
 *   <li>
 *     <h4>node_appearance_file</h4>
 *     <pre>  各ノードの設定を記述した設定ファイル
 *
 *  設定値： node_appearance_fileへのファイルパス(JSONまたはXML形式)
 *  デフォルト値： なし(シード指定なし)</pre>
 *   </li>
 *
 *   <li>
 *     <h4>randseed</h4>
 *     <pre>  乱数発生のシード
 *
 *  設定値： 整数値
 *  デフォルト値： なし</pre>
 *   </li>
 *
 *   <li>
 *     <h4>random_navigation</h4>
 *     <pre>  random_navigation モードの ON/OFF
 *  エージェントは通常、ゴールまでの最短距離を進むようになっているが、そのように進まないエージェントを
 *  発生させるかどうかを設定することができる。
 *  ※randseedの値を変更しないと、エージェントは同じ挙動をする。
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>evacuated_agents_log_file</h4>
 *     <pre>  ゴールノード別の脱出エージェント数をステップごとに記録するログ
 *
 *  設定値： evacuated_agents_log file へのファイルパス
 *  デフォルト値： なし
 *   </li>
 *
 *   <li>
 *     <h4>agent_movement_history_file</h4>
 *     <pre>  ゴールまでたどり着いたエージェントのゴールした時点でのログ(?)
 *
 *  設定値： agent_movement_history file へのファイルパス
 *  デフォルト値： なし
 *  ログの記述内容：{@link nodagumi.ananPJ.Simulator.AgentHandler#agentMovementHistoryLoggerFormatter}</pre> 
 * </pre>
 *   </li>
 *
 *   <li>
 *     <h4>agent_trail_log</h4>
 *     <pre>  ゴールまでたどり着いたエージェントのゴールした時点でのJSON形式のログ
 *
 *  設定値： { "file" : ログファイルへのパス }
 *  デフォルト値： なし
 *   </li>
 *
 *   <li>
 *     <h4>individual_pedestrians_log_dir</h4>
 *     <pre>  個別のエージェントに対するログデータを保管する場所の指定
 *  ディレクトリを指定した時のみログを収集する。
 *
 *  設定値： ディレクトリへの絶対パス | カレントディレクトリからの相対パス
 *  デフォルト値： なし
 *  ログの記述内容：{@link nodagumi.ananPJ.Simulator.AgentHandler#individualPedestriansLoggerFormatter}</pre> 
 *   </li>
 *
 *   <li>
 *     <h4>record_simulation_screen</h4>
 *     <pre>  シミュレーション画面のスクリーンショットを記録する
 *  ※clear_screenshot_dir が true ではなく、かつ出力先ディレクトリに画像ファイルが存在する
 *    場合はエラー終了する。
 *  ※この設定をtrueにすると、シミュレーションが遅くなる。
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>screenshot_dir</h4>
 *     <pre>  シミュレーション画面のスクリーンショットを保存するディレクトリ
 *
 *  設定値： ディレクトリへの絶対パス | カレントディレクトリからの相対パス
 *  デフォルト値： screenshots</pre>
 *   </li>
 *
 *   <li>
 *     <h4>clear_screenshot_dir</h4>
 *     <pre>  スクリーンショットディレクトリに存在する画像ファイル(.bmp|.gif|.jpg|.png)を
 *  すべて削除する。
 *  ※有効にするためには screenshot_dir の設定が必要
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>screenshot_image_type</h4>
 *     <pre>  スクリーンショットの画像ファイル形式
 *
 *  設定値： bmp | gif | jpg | png
 *  デフォルト値： png</pre>
 *   </li>
 *
 *   <li>
 *     <h4>create_log_dirs</h4>
 *     <pre> log および スクリーンショットの directory がないとき、自動生成するかどうか。
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>defer_factor</h4>
 *     <pre>  1ステップごとの待ち時間(ミリ秒単位)
 *  この設定値を小さくするとシミュレーションは早く進み、大きくするとシミュレーションは遅く進む。
 *  早 0←--------→299 遅
 *
 *  設定値： 0～299
 *  デフォルト値： 0</pre>
 *   </li>
 *
 *   <li>
 *     <h4>vertical_scale</h4>
 *     <pre>  3D シミュレーション画面のマップに対する垂直方向のスケールの大きさ
 *
 *  設定値： 0.1～10.0
 *  デフォルト値： 1.0</pre>
 *   </li>
 *
 *   <li>
 *     <h4>agent_size</h4>
 *     <pre>  エージェントの表示サイズ(図6,7)
 *
 *  設定値： 0.1～9.9
 *  デフォルト値： 1.0</pre>
 * <p>
 *   <center>
 *     <img src="./doc-files/agent_size_1.png" alt="./doc-files/agent_size_1.png"><br>図6　1.0のとき<br><br>
 *     <img src="./doc-files/agent_size_2.png" alt="./doc-files/agent_size_2.png"><br>図7　2.0のとき
 *   </center>
 * </p>
 *   </li>
 *
 *   <li>
 *     <h4>zoom</h4>
 *     <pre>  シミュレーション画面全体の表示倍率
 *  camera_fileを設定し、シミュレーション画面のViewでReplayにチェックを入れたときに適用される。
 *
 *  設定値： 0.0～9.9
 *  デフォルト値： 1.0</pre>
 *   </li>
 *
 *   <li>
 *     <h4>background_color</h4>
 *     <pre>  3D シミュレーション画面の背景色
 *
 *  設定値： 標準のHTML色名 | 16進RGB値("#ffffff")
 *  デフォルト値： white</pre>
 *   </li>
 *
 *   <li>
 *     <h4>show_links_at_actual_width</h4>
 *     <pre>  3D シミュレーション画面で道幅を持つリンクを表示する。
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>outline_color</h4>
 *     <pre>  3D シミュレーション画面で Obstructer 領域を確認するための色
 *
 *  設定値： 標準のHTML色名 | 16進RGB値("#ffffff")
 *  デフォルト値： lime</pre>
 *   </li>
 *
 *   <li>
 *     <h4>centering_by_node_average</h4>
 *     <pre>  3D シミュレーション画面のマップの中心点を従来の方法(全ノード位置の平均)で算出する。
 *  デフォルトはマップ全体に外接する矩形の中心点。
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>change_agent_color_depending_on_speed</h4>
 *     <pre>  エージェントの移動速度によってエージェントの色を変化させる(図8,9)
 *
 *  設定値： true | false
 *  デフォルト値： true</pre>
 * <p>
 *   <center>
 *     <img src="./doc-files/change_agent_color_depending_on_speed_1.png" alt="./doc-files/change_agent_color_depending_on_speed_1.png"><br>図8　true のとき<br><br>
 *     <img src="./doc-files/change_agent_color_depending_on_speed_2.png" alt="./doc-files/change_agent_color_depending_on_speed_2.png"><br>図9　false のとき
 *   </center>
 * </p>
 *   </li>
 *
 *   <li>
 *     <h4>drawing_agent_by_triage_and_speed_order</h4>
 *     <pre>  エージェントをトリアージレベルと移動速度でソートして表示する。
 *  エージェントが重なっている時には、より重症な(Greenの場合は速度が遅い)エージェントが表示される様になる。
 *
 *  設定値： true | false
 *  デフォルト値： true</pre>
 *   </li>
 *
 *   <li>
 *     <h4>show_status</h4>
 *     <pre>  シミュレーション画面上にステータスラインを表示する。
 *  通常のシミュレーション画面にはステータスラインは常に表示されているが、この設定を有効にすると
 *  スクリーンショットにもステータスラインが表示される(図10,11)。
 *
 *  設定値： none | top | bottom
 *           none：  表示しない
 *           top：   上側に表示する
 *           bottom：下側に表示する
 *  デフォルト値： none</pre>
 * <p>
 *   <center>
 *     <img src="./doc-files/show_status_1.png" alt="./doc-files/show_status_1.png"><br>図10　show_status を有効にしなかった場合のスクリーンショット<br><br>
 *     <img src="./doc-files/show_status_2.png" alt="./doc-files/show_status_2.png"><br>図11　show_status を top に設定した場合のスクリーンショット
 *   </center>
 * </p>
 *   </li>
 *
 *   <li>
 *     <h4>show_logo</h4>
 *     <pre>  シミュレーション画面に AIST ロゴを表示する(図12)
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 * <p>
 *   <center>
 *     <img src="./doc-files/show_logo.png" alt="./doc-files/show_logo.png"><br>図12
 *   </center>
 * </p>
 *   </li>
 *
 *   <li>
 *     <h4>simulation_window_open</h4>
 *     <pre>  property fileを読み込んだ際に自動的に Simulation ウィンドウを開く
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>auto_simulation_start</h4>
 *     <pre>  自動的に Simulation ウィンドウを開いて自動的にシミュレーションを開始する
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>exit_with_simulation_finished</h4>
 *     <pre>  GUI モード時にシミュレーション終了と同時にプログラムを終了する
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>mental_map_rules</h4>
 *     <pre>   * 探索において、各リンクの主観的距離の変更ルールを記述。
 *
 *  設定値： ルールを表す JSON 形式の式。
 *  デフォルト値： null
 *  ルールの記述方法：{@link nodagumi.ananPJ.navigation.PathChooser}</pre>
 *   </li>
 *
 *   <li>
 *     <h4>show_background_image</h4>
 *     <pre>  2D シミュレータ上で、シミュレーション画面に背景画像を表示する。
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 *
 *   <li>
 *     <h4>zone</h4>
 *     <pre>  マップデータの平面直角座標系の系番号
 *
 *  2D シミュレータ上で、国土地理院の地理院タイルを用いた背景地図表示を有効にする。
 *  マップファイルのルートの &lt;Group&gt; タグに zone 属性があれば省略可能。
 *  マップ範囲の地理院タイルを読み込むために、初回のみ国土地理院の Web サイトへのアクセスが発生する。
 *  (読み込んだ画像は CrowdWalk/crowdwalk/cache ディレクトリにキャッシュされる)
 *
 *  設定値： 1～19
 *  デフォルト値： なし</pre>
 *   </li>
 *
 *   <li>
 *     <h4>gsi_tile_name</h4>
 *     <pre>  地理院タイルのタイル名(データID)
 *
 *  背景地図に使用する地理院タイルを指定する。
 *  標準地図/淡色地図/English/数値地図25000（土地条件）/白地図/色別標高図/写真が選択可能。
 *  <a href="http://maps.gsi.go.jp/development/ichiran.html" target="_blank">≪地理院タイル一覧≫</a>参照。
 *
 *  設定値： std | pale | english | lcm25k_2012 | blank | relief | ort
 *  デフォルト値： pale</pre>
 *   </li>
 *
 *   <li>
 *     <h4>gsi_tile_zoom</h4>
 *     <pre>  地理院タイルのズームレベル
 *
 *  背景地図に使用する地理院タイルのズームレベルを指定する。
 *  <a href="http://maps.gsi.go.jp/development/siyou.html" target="_blank">≪地理院タイル仕様≫</a>参照。
 *
 *  設定値： 5～18(タイル名により有効範囲が異なる)
 *  デフォルト値： 14</pre>
 *   </li>
 *
 *   <li>
 *     <h4>show_background_map</h4>
 *     <pre>  2D シミュレータ上で、シミュレーション画面に背景地図を表示する。
 *
 *  設定値： true | false
 *  デフォルト値： false</pre>
 *   </li>
 * </ul>
 */
public class CrowdWalkPropertiesHandler {

    /**
     * ファイル・ディレクトリ名を設定する属性名。
     * ここにリストしておくと、パス指定で、絶対・相対の指定がない場合、
     * properties file のパスを追加する。
     */
    public static final String[] DEFINITION_FILE_ITEMS
        = {"map_file",
           "generation_file",
           "scenario_file",
           "camera_file",
           "camera_2d_file",
           "pollution_file",
           "link_appearance_file",
           "node_appearance_file",
           "evacuated_agents_log_file",
           "agent_movement_history_file",
           "individual_pedestrians_log_dir",
           "screenshot_dir",
           "fallback_file"
        };

    protected String propertiesFile = null;
    protected Term prop = null;

    /**
     * Get a properties file name.
     * @return Property file name.
     */
    public String getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Set a properties file name.
     * @param _path a properties file name.
     */
    public void setPropertiesFile(String _path) {
        propertiesFile = _path;
    }

    /**
     * properties file の directory を返す。
     */
    public String getPropertiesDir() {
        File file = new File(propertiesFile) ;
        return file.getAbsoluteFile().getParent() ;
    }


    protected String networkMapFile = null; // path to map file (required)
    public String getNetworkMapFile() {
        return networkMapFile;
    }

    protected String pollutionFile = null; // path to Obstructer file
    public String getPollutionFile() {
        return pollutionFile;
    }

    protected String generationFile = null; // path to generation file
    public String getGenerationFile() {
        return generationFile;
    }

    protected String scenarioFile = null; // path to scenario file
    public String getScenarioFile() {
        return scenarioFile;
    }

    //@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    /**
     * fallback file
     */
    protected String fallbackFile = null;

    //------------------------------------------------------------
    /**
     * fallback file を取得
     */
    public String getFallbackFile() {
        return fallbackFile ;
    }

    protected long randseed = 0;
    public long getRandseed() {
        return randseed;
    }

    // End condition of simulation
    protected int exitCount = 0;
    public int getExitCount() {
        return exitCount;
    }

    protected boolean isAllAgentSpeedZeroBreak = false;
    public boolean getIsAllAgentSpeedZeroBreak() {
        return isAllAgentSpeedZeroBreak;
    }

    /**
     * 経路探索における、各リンクの主観的距離の変更ルール
     */
    protected Term mentalMapRules = null ;
    public Term getMentalMapRules() {
        return mentalMapRules ;
    }

    //------------------------------------------------------------
    /**
     * コンストラクタ
     */
    public CrowdWalkPropertiesHandler() {
        prop = new Term();
    }

    /**
     * コンストラクタ
     */
    public CrowdWalkPropertiesHandler(String _propertiesFile) {
        // load properties
        prop = new Term();
        propertiesFile = _propertiesFile;
        try {
            String path = _propertiesFile.toLowerCase();
            if (path.endsWith(".xml")) {
                scanXmlPropertiesIntoTerm(new FileInputStream(_propertiesFile),
                                          prop) ;
                Itk.logInfo("Load Properties File (XML)",
                            _propertiesFile);
            } else if (path.endsWith(".json")) {
                HashMap<String, Object> map =
                    (HashMap<String, Object>)
                    JSON.decode(new FileInputStream(_propertiesFile));
                prop = new Term().setScannedJson(map, true) ;
                
                Itk.logInfo("Load Properties File (JSON)",
                            _propertiesFile);
            } else {
                throw new Exception("Property file error - unknown extention: "
                                    + _propertiesFile);
            }

            // パス指定がファイル名のみならば
            // プロパティファイルのディレクトリパスを付加する
            File propertyFile = new File(_propertiesFile);
            String propertyDirPath = propertyFile.getParent();
            if (propertyDirPath == null) {
                propertyDirPath = ".";
            }
            for (String property_item : DEFINITION_FILE_ITEMS) {
                String filePath = getString(property_item, null);
                if (filePath != null) {
                    File file = new File(filePath);
                    if (file.getParent() == null) {
                        prop.setArg(property_item,
                                    propertyDirPath.replaceAll("\\\\", "/") +
                                    "/" + filePath);
                    }
                }
            }

            // input files
            networkMapFile = getString("map_file", null);
            pollutionFile = getString("pollution_file", null);
            generationFile = getString("generation_file", null);
            scenarioFile = getString("scenario_file", null);
            fallbackFile = getString("fallback_file", null) ;

            // create random with seed
            randseed = getInteger("randseed", 0);

            // exit count
            exitCount = getInteger("exit_count", 0);
            isAllAgentSpeedZeroBreak = getBoolean("all_agent_speed_zero_break",
                                                  true);

            // 早い内に設定ミスをユーザーに知らせるための検査
            String obstructerType = getString("pollution_type", null);
            if (obstructerType != null) {
                AgentBase.setObstructerType(obstructerType);
                ObstructerBase.createInstance(obstructerType) ;
            }
            getDouble("pollution_color_saturation", 0.0);

            /* [2015.01.07 I.Noda] to switch agent queue in the link directions.*/
            String queueOrderStr = getString("queue_order", null) ;
            if(queueOrderStr != null) {
                if(queueOrderStr.equals("front_first")) {
                    AgentHandler.useFrontFirstOrderQueue(true) ;
                    Itk.logInfo("use front_first order to sort agent queue.") ;
                } else if (queueOrderStr.equals("rear_first")) {
                    AgentHandler.useFrontFirstOrderQueue(false) ;
                    Itk.logInfo("use rear_first order to sort agent queue.") ;
                } else {
                    Itk.logError("unknown queue_order:" + queueOrderStr) ;
                    Itk.logError_("use default order (rear_first)") ;
                }
            }

            // [2016.01.30 I.Noda] mentalRule check
            mentalMapRules = getTerm("mental_map_rules",null) ;

        } catch (IOException ioe) {
            Itk.logError("IO exception") ;
            ioe.printStackTrace();
            Itk.quitByError() ;
        } catch(Exception e) {
            e.printStackTrace() ;
            System.err.println(e.getMessage());
            Itk.logError("unknown exception") ;
            Itk.quitByError() ;
        }
    }

    //--------------------------------------------------
    /**
     * scan XML file as Properties into Term
     */
    private Term scanXmlPropertiesIntoTerm(InputStream stream, Term term) {
        try {
            Properties _prop = new Properties() ;
            _prop.loadFromXML(stream) ;

            for(String key : _prop.stringPropertyNames()) {
                term.setArg(Itk.intern(key),
                            Itk.intern(_prop.getProperty(key))) ;
            }
        } catch (IOException ioe) {
            Itk.logError("IO exception") ;
            ioe.printStackTrace();
            Itk.quitByError() ;
        } catch(Exception e) {
            e.printStackTrace() ;
            System.err.println(e.getMessage());
            Itk.logError("unknown exception") ;
            Itk.quitByError() ;
        }
            
        return term ;
    }

    //--------------------------------------------------
    /**
     * check defined
     */
    public boolean isDefined(String key) {
        String value = prop.getArgString(key);
        return ! (value == null || value.trim().isEmpty());
    }

    //--------------------------------------------------
    /**
     * get property (raw)
     */
    public static String getProperty(Term prop, String key) {
        if (prop.hasArg(key)) {
            return prop.getArgString(key);
        } else {
            return null;
        }
    }

    //--------------------------------------------------
    /**
     * get String property
     */
    public String getString(String key, String defaultValue) {
        String value = prop.getArgString(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
    /** */
    public String getStringInPattern(String key, String defaultValue,
                                     String pattern[]) throws Exception
    {
        String value = prop.getArgString(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        value = value.toLowerCase();
        for (String str : pattern) {
            if (str.toLowerCase().equals(value)) {
                return value;
            }
        }
        throw new Exception("Property error - value is not in the pattern:" +
                            key + ":" + value + " not in " + pattern);
    }

    //--------------------------------------------------
    /**
     * get Boolean property
     */
    public boolean getBoolean(String key, boolean defaultValue)
        throws Exception
    {
        Object value = prop.getArg(key) ;
        
        if(value == null) { return defaultValue ; } ;

        if(value instanceof String) {
            String strValue = ((String)value).toLowerCase() ;
            
            if (strValue.trim().isEmpty()) { return defaultValue; }

            switch(strValue) {
            case "true":
            case "on":
                return true ; 
            case "false":
            case "off" :
                return false ; 
            default:
                throw new Exception("Property error - wrong boolean value:" +
                                    key + ":" + value);
            }
        } else if(value instanceof Boolean || value instanceof Term) {
            return prop.getArgBoolean(key) ;
        } else {
            throw new Exception("Property error - wrong boolean value:" +
                                key + ":" + value);
        }
    }
    
    //--------------------------------------------------
    /**
     * get Integer property
     */
    public int getInteger(String key, int defaultValue) throws Exception {
        Object value = prop.getArg(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            if(value instanceof String || Term.isStringTerm(value)) {
                return Integer.parseInt(prop.getArgString(key)) ;
            } else {
                return prop.getArgInt(key) ;
            }
        } catch(NumberFormatException e) {
            throw new Exception("Property error - wrong integer value:" +
                                key + ":" + value);
        }
    }
    
    //--------------------------------------------------
    /**
     * get Double property
     */
    public double getDouble(String key, double defaultValue) throws Exception {
        Object value = prop.getArg(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            if(value instanceof String || Term.isStringTerm(value)) {
                return Double.parseDouble(prop.getArgString(key)) ;
            } else {
                return prop.getArgDouble(key) ;
            }
        } catch(NumberFormatException e) {
            throw new Exception("Property error - wrong double value:" +
                                key + ":" + value);
        }
    }

    //--------------------------------------------------
    /**
     * get Term property
     */
    public Term getTerm(String key, Term defaultValue) {
        if(prop != null) {
            if(prop.hasArg(key)) {
                return prop.getArgTerm(key) ;
            } else {
                return defaultValue ;
            }
        } else {
            return null ;
        }
    }

    //--------------------------------------------------
    /**
     *  check property to create directories for logs automatically
     *  if not exists.
     */
    public boolean doesCreateLogDirAutomatically() {
        try {
            return getBoolean("create_log_dirs", true) ;
        } catch(Exception e) {
            Itk.logWarn("property file error." + e.getMessage()) ;
            return false ;
        }
    }
    
    //--------------------------------------------------
    /**
     *  get directory path specified by key.
     */
    public String getDirectoryPath(String key, String defaultValue) throws Exception {
        String value = prop.getArgString(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        File file = new File(value);
        if (! file.exists()) {
            if(doesCreateLogDirAutomatically()) {
                Itk.logWarn("create directory", file) ;
                file.mkdirs() ;
            } else {
                throw new Exception("Property error - 指定されたディレクトリが存在しません: " + key + ":" + value);
            }
        }
        if (! file.isDirectory()) {
            throw new Exception("Property error - 指定されたパスがディレクトリではありません: " + key + ":" + value);
        }
        return value;
    }

    //--------------------------------------------------
    /**
     *  get file path.
     *  if not exist, raise Exception.
     */
    public String getFilePath(String key, String defaultValue) throws Exception {
        String value = prop.getArgString(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        File file = new File(value);
        if (! file.exists()) {
            throw new Exception("Property error - 指定されたファイルが存在しません: " + key + ":" + value);
        }
        if (! file.isFile()) {
            throw new Exception("Property error - 指定されたパスがファイルではありません: " + key + ":" + value);
        }
        return value;
    }

    //--------------------------------------------------
    /**
     *  get file path.
     *  If existing is true,  cause Exception when the file does not exist.
     *  If existing is false, cause Exception when the file exists.
     *  
     */
    public String getFilePath(String key, String defaultValue, boolean existing) throws Exception {
        if (existing) {
            return getFilePath(key, defaultValue);
        }
        String value = prop.getArgString(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        File file = new File(value);
        if (file.exists() && ! file.isFile()) {
            throw new Exception("Property error - 指定されたパスがファイルではありません: " + key + ":" + value);
        }
        return value;
    }

    //------------------------------------------------------------
    /**
     * パスのリストを取得。
     * パスは区切り記号で区切られているとする。
     * 区切り記号のデフォルトは改行。
     */
    public ArrayList<String> getPathList(String key,
                                         ArrayList<String> defaultValue) {
        return getPathList(key, defaultValue, "\n") ;
    }

    /**
     * パスのリストを取得。
     * パスは区切り記号で区切られているとする。
     */
    public ArrayList<String> getPathList(String key,
                                         ArrayList<String> defaultValue,
                                         String separator) {
        String value = prop.getArgString(key) ;
        if(value == null || value.trim().isEmpty()) {
            return defaultValue ;
        } else {
            ArrayList<String> pathList = new ArrayList<String>();
            String[] arrayedPath = value.split(separator);
            for(String path : arrayedPath) {
                pathList.add(path.trim()) ;
            }
            return pathList ;
        }
    }

    public static void main(String[] args) throws IOException {

        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("properties_file")
                          .hasArg(true).withDescription("Path of properties file")
                          .isRequired(true).create("p"));

        CommandLineParser parser = new BasicParser();
        CommandLine cli = null;

        try {
            cli = parser.parse(options, args);
        } catch (MissingOptionException moe) {
            moe.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("CrowdWalkPropertiesHandler", options, true);
            Itk.logError("Missing Opetion Exception") ;
            System.exit(1);
        } catch (ParseException e) {
            e.printStackTrace();
            Itk.logError("Parse Exception") ;
            System.exit(1);
        }
        String propertiesFile = cli.getOptionValue("p");

        CrowdWalkPropertiesHandler nph =
            new CrowdWalkPropertiesHandler(propertiesFile);
    }
}
