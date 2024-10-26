# Cloudwalk 追加スクリプト

## github リポジトリ

https://github.com/Kanade147359/CrowdWalk

## 使い方

Crowdwalkディレクトリに各ファイルを追加してください
input_data.json、property.json、generation_json.pyのpathを書き換えてから、simulation.sh を使って起動してください

```
./simulation 
```

オプション --recordを使うとその際のinput_data.json、generation.jsonを保存できます。保存したいディレクトリは適宜simulation.sh内のfolder_path変数を変更してください。

```
./simulation --record
```

## input_data.jsonの構成

現在 generation.jsonの自動生成のみ対応しています。各要素にはリストを用意しており、一つのインデックスに一つの生成ルールが対応します。

### 例 

input_data.json

```
{
    "generation": {
        "number_of_people": [1000,2000,3000],
        "agent_type": [["N",100],["C",50],["B",30],],
        "exit_capacity": [2, 2, 2],
        "starting_point": ["START-A","START-B","START-C"],
        "goal_point": [["GOAL-A1","GOAL-A2"]],["GOAL-B"],["GOAL-C]],
    　   "planned_route": [
            ["POINT-A"],  ["POINT-B"], ["POINT-C"]
         ],
        "thresholds": [
            [500],
            [],
            []
        ],
        "disruptors" : {
            "number_of_people":[100],
            "timing":["19:10:00"],
            "starting_point":["GOAL-A1"],
            "goal_point":["START-A"],
            "planned_route":[[""]]
        }
    },
}

```

### generation

- number_of_people : 合計生成人数
- agent_type : エージェントタイプ
  - {エージェントタイプ、割合}　指定した割合指定したタイプのエージェントが生成される。
  - 割合はNaiveAgentと指定したエージェントタイプの比率を表す。
  - 例 : {"B",50} NaiveAgentが50%生成、BustleAgentが50%生成
  - タイプの指定は各タイプの頭文字で指定
    - "N" : NaiveAgent
    - "C" : CapriciousAgent
    - "B" : BustleAgent
    - "R" : RationalAgent
  - RubyAgentは今は対応していません
- exit_capacity : 何列でエージェントが発生するか
  - 1列は76人/分でエージェントが発生する
- start_point : エージェントの生成地点のID
- goal_point : ゴール地点のID
  - thresholdsでゴール地点を切り替える場合、複数指定すること
- planned_route : 経由地点 リストで複数指定可能
- thresholds : 生成途中でゴール地点を切り替える場合、閾値となる人数
  - 指定する場合ゴール地点をその分用意しないとエラーとなる
- disruptors : 突発的に発生させるエージェント
  - 全てNaiveAgentで生成される
  - リストに複数情報を加えることで複数組のエージェントを発生させることができる
    - number_of_people : 生成人数
    - timing : 生成時間 (時:分:秒)で指定
    - starting_point : エージェントの生成地点のID
    - goal_point : ゴール地点のID
    - planned_route : 中継地点 リストで複数指定可能
