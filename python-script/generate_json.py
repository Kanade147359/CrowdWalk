import json
from datetime import datetime
import module

def main():
    # 開始時刻の設定
    time_obj = datetime.strptime("19:00:00", '%H:%M:%S')
    # 生成したjsonの保存先
    path_generated_json = "Kobe-Harborland/generation.json"

    # データを読み込む
    data = module.load_data_from_json('./input_data.json')

    # それぞれのリストを取得
    number_of_people_list = data["generation"]["number_of_people"]
    exit_capacity_list = data["generation"]["exit_capacity"]
    agent_type_list = data["generation"]["agent_type"]
    starting_point_list = data["generation"]["starting_point"]
    goal_point_list = data["generation"]["goal_point"]
    plannedRoute_list = data["generation"]["planned_route"]
    thresholds = data["generation"]["thresholds"]

    # disruptorsのリストを取得
    number_of_disruptors_list = data["generation"]["disruptors"]["number_of_people"]
    disruptors_timing_list = data["generation"]["disruptors"]["timing"]
    disruptors_starting_point_list = data["generation"]["disruptors"]["starting_point"]
    disruptors_goal_point_list = data["generation"]["disruptors"]["goal_point"]
    disruptors_plannedRoute_list = data["generation"]["disruptors"]["planned_route"]

    all_generated_json = []
    
    # path_scenario_json = "scenario.json"

    # generate.jsonの生成
    for number_of_people,agent_type, exit_capacity, start, goal, planned_route,thresholds in zip(number_of_people_list, agent_type_list,  exit_capacity_list, starting_point_list, goal_point_list, plannedRoute_list, thresholds):
        generated_json = module.generate_json(number_of_people,agent_type, exit_capacity, start, goal, time_obj, planned_route, thresholds)
        all_generated_json.extend(generated_json)  # 結果を全体のリストに結合

    # scenario.jsonの生成
    # for number_of_people, exit_capacity, start, goal, plannedRoute in zip(number_of_people_list, exit_capacity_list, starting_point_list, goal_point_list, plannedRoute_list):
    #     generated_json = module.generate_json(number_of_people, exit_capacity, start, goal, time_obj)
    #     all_generated_json.extend(generated_json)  # 結果を全体のリストに結合
    # startTimeでソート

    for number_of_disruptors, timing, starting_points, goal_points, planned_route in zip(number_of_disruptors_list, disruptors_timing_list, disruptors_starting_point_list, disruptors_goal_point_list, disruptors_plannedRoute_list):
        disruptors_json = module.generate_disruptors_json(number_of_disruptors, timing, starting_points , goal_points, planned_route)

        all_generated_json.extend(disruptors_json)


    sorted_generate_json = sorted(all_generated_json, key=lambda x: datetime.strptime(x["startTime"], '%H:%M:%S'))

    with open (path_generated_json, 'w') as json_file:
        json_file.write('#{ "version" : 2}\n')
        json.dump(sorted_generate_json, json_file, indent=2)
    print(f"データが {path_generated_json} に保存されました。")

    # with open (path_scenario_json, 'w') as json_file:
    #     json_file.write('#{ "version" : 2}\n')
    #     json.dump(sorted_generate_json, json_file, indent=2)
    # print(f"データが {path_scenario_json} に保存されました。")

if __name__ == '__main__':
    main()