from datetime import datetime, timedelta
import json

def increment_time_by_one_second(time_obj):
    new_time_obj = time_obj + timedelta(seconds=1)
    return new_time_obj

def generate_json(number_of_people, exit_capacity, starting_point , goal_points, time_obj, planned_route,thresholds):
    # JSON文字列を保持するリスト
    generated_json = []

    total_people_generated = 0  # 生成した人数の合計
    goal_point_index = 0  # 現在のゴールポイントのインデックス
    threshold_index = 0  # 現在のthresholdのインデックス

    current_goal = goal_points[goal_point_index]  # 最初のゴールポイント
        # 空でないthresholdを探す
    if thresholds and len(thresholds) > 0:
        current_threshold = thresholds[threshold_index]  # 最初のしきい値
    else:
        current_threshold = float('inf')  # しきい値がない場合は無限大に設定（無限に達しない）
        
    number_of_generated_people = 0  # 生成した人数

    while number_of_generated_people < number_of_people:
        # 5秒ごとに exit_capacity を2倍にする
        if (time_obj.second % 5 == 0) and (time_obj.second % 15 != 0):
            current_exit_capacity = exit_capacity * 2
        # 15秒ごとに exit_capacity を3倍にする
        elif time_obj.second % 15 == 0:
            current_exit_capacity = exit_capacity * 3
        else:
            current_exit_capacity = exit_capacity  # それ以外の時は元のcapacity

        # エージェントデータの作成
        agent_data = {
            "rule": "EACH",
            "agentType": {"className": "NaiveAgent"},
            "startTime": time_obj.strftime("%H:%M:%S"),
            "total": current_exit_capacity,
            "duration": 60,
            "startPlace": starting_point,
            "goal": current_goal,
            "plannedRoute": planned_route,
        }
        # 辞書をリストに追加
        generated_json.append(agent_data)

        # 生成した人数の合計を更新
        number_of_generated_people += current_exit_capacity      

        # 合計人数が current_threshold に達したらゴールを変更
        if total_people_generated >= current_threshold:
            total_people_generated = 0  # 合計をリセット
            goal_point_index += 1  # 次のゴールに切り替え
            threshold_index += 1  # 次のしきい値に切り替え

            if threshold_index < len(thresholds):
                current_threshold = thresholds[threshold_index]
            else:
                current_threshold = thresholds[-1]  # リストを超えたら最後のしきい値を使う

            if goal_point_index < len(goal_points):
                current_goal = goal_points[goal_point_index]
            else:
                current_goal = goal_points[-1]  # リストを超えたら最後のゴールを使う
            
        # 時間を1秒進める
        time_obj = increment_time_by_one_second(time_obj)

    return generated_json

def load_data_from_json(filename):
    with open(filename, 'r') as file:
        data = json.load(file)
    return data

def open_mozaic(scenario_json,current_time):
    scenario_json.append({
        "type" : "OpenGate",
        "atTime" : current_time,
        "gateTag" : "gate_mozaic_north_stairs"
    },{
        "type" : "CloseGate",
        "atTime" : current_time,
        "gateTag" : "gate_Anpanman_museam"
    })

def open_Anpanman(scenario_json,current_time):
    scenario_json.append({
        "type" : "OpenGate",
        "atTime" : current_time,
        "gateTag" : "gate_Anpanman_museam"
    },{
        "type" : "CloseGate",
        "atTime" : current_time,
        "gateTag" : "gate_mozaic_north_stairs"
    })

def scenario_json(start_time, end_time, time_delta):
    
    scenario_json = []
    current_time = start_time

    open_mozaic = True
    while current_time <= end_time:
        if open_mozaic:
            open_mozaic(scenario_json)
        else:
            open_Anpanman(scenario_json)
        current_time += time_delta  # 3分進める

    
