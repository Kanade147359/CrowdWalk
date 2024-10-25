
## 自動エージェント生成スクリプト

### 使い方

Crowdwalkディレクトリにある simulation.sh を使って起動してください

```
./simulation 
```

オプション --recordを使うとその際のinput_data.jsonを保存できます。保存したいディレクトリは適宜simulation.sh内のfolder_path変数を変更してください。

```
./simulation --record
```

### input_data.jsonの書き方

今のところgeneration.jsonの自動生成のみ実装しています。


### input_data.json例

```

{
    "generation": {
        "number_of_people": [2067, 2065, 2000, 1100, 2046, 2089],
        "agent_type": [["N",100],["N",100],["N",100],["N",100],["N",100],["N",100]],
        "exit_capacity": [2, 2, 2, 2, 2, 2],
        "starting_point": ["START-AM","START-M","START-M-2F","START-KB","START-KB","START-OSS"],
        "goal_point": [["ID7-1"],["ID7-2"],["ID7-2F"], ["ID1"], ["ID-EDU"],["ID5"]],
        "planned_route": [
            ["HR-W-1", "HR-W-2", "HR-W-3", "HR-W-4", "HR-W-5","HR-W-6"],
            ["HR-E-4", "HR-E-5"],
            ["UMIE-2F-1"],
            ["KG-S-3, KG-S-4"],
            ["KG-S-3"],
            ["KG-N-2", "KG-N-3", "HL-W-1"]
        ],
        "thresholds": [
            [],
            [],
            [],
            [],
            [],
            []
        ],
        "disruptors" : {
            "number_of_people":[],
            "timing":[],
            "starting_point":[],
            "goal_point":[],
            "planned_route":[]
        }
    },
    "scenario": {
        "number_of_people": [5335, 2067, 4065],
        "exit_capacity": [3, 3, 3],
        "starting_point": ["START_KB", "START_AM", "START_M"],
        "goal_point": ["ID5", "ID6-1", "ID6-2"],
        "planned_route": [
            ["HL1", "HL"],
            ["KB-1", "HR-W-1", "HR-W-2", "HR-W-3", "HR-W-4", "HR-W-5"],
            ["HR-E-3", "HR-E-4", "HR-E-5"]
        ]
    }
}
```
