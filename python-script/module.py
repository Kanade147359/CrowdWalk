from datetime import datetime, timedelta
import json

def increment_time_by_one_second(time_obj):
    new_time_obj = time_obj + timedelta(seconds=1)
    return new_time_obj

def generate_json(number_of_people, exit_capacity, starting_point , goal_point, time_obj, planned_route):
    # JSON文字列を保持するリスト
    generated_json = []

    current_exit_capacity = exit_capacity  # 初期の exit_capacity
    for i in range(0, number_of_people, current_exit_capacity):
        # 5秒ごとに exit_capacity を2倍にする
        if (time_obj.second % 5 == 0) and (time_obj.second % 15 != 0):
            current_exit_capacity = exit_capacity * 2
            number_of_people += exit_capacity
        # 15秒ごとに exit_capacity を3倍にする
        elif time_obj.second % 15 == 0:
            current_exit_capacity = exit_capacity * 3
            number_of_people += exit_capacity * 2
        else:
            current_exit_capacity = exit_capacity  # それ以外の時は元のcapacity

        # エージェントデータを生成
        agent_data = {
            "rule": "EACH",
            "agentType": {"className": "NaiveAgent"},
            "startTime": time_obj.strftime("%H:%M:%S"),
            "total": current_exit_capacity,
            "duration": 60,
            "startPlace": starting_point,
            "goal": goal_point,
            "plannedRoute": planned_route,
        }
        # 辞書をリストに追加
        generated_json.append(agent_data)

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

    
