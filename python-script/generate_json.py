import json
from datetime import datetime
import module

def main():
    time_obj = datetime.strptime("19:00:00", '%H:%M:%S')

    # データを読み込む
    data = module.load_data_from_json('./input_data.json')

    # それぞれのリストを取得
    number_of_people_list = data["generation"]["number_of_people"]
    exit_capacity_list = data["generation"]["exit_capacity"]
    starting_point_list = data["generation"]["starting_point"]
    goal_point_list = data["generation"]["goal_point"]
    plannedRoute_list = data["generation"]["planned_route"]

    all_generated_json = []
    path_generated_json = "generation.json"
    # path_scenario_json = "scenario.json"
    # generate.jsonの生成
    for number_of_people, exit_capacity, start, goal, planned_route in zip(number_of_people_list, exit_capacity_list, starting_point_list, goal_point_list, plannedRoute_list):
        generated_json = module.generate_json(number_of_people, exit_capacity, start, goal, time_obj, planned_route)
        all_generated_json.extend(generated_json)  # 結果を全体のリストに結合
    # scenario.jsonの生成
    # for number_of_people, exit_capacity, start, goal, plannedRoute in zip(number_of_people_list, exit_capacity_list, starting_point_list, goal_point_list, plannedRoute_list):
    #     generated_json = module.generate_json(number_of_people, exit_capacity, start, goal, time_obj)
    #     all_generated_json.extend(generated_json)  # 結果を全体のリストに結合
    # startTimeでソート
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