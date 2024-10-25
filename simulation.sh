#!/bin/bash

# 現在の日時を取得 (形式: YYYYMMDD_HHMMSS)
timestamp=$(date +"%Y%m%d_%H%M%S")
# input_data.json の保存先のフォルダパス 適宜変更
folder_path="Kobe-Harborland/video/${timestamp}"

python3 python-script/generate_json.py
mv generation.json Kobe-Harborland/
cd crowdwalk
sh ./quickstart.sh ../Kobe-Harborland/properties.json -2g
cd ..

# --record が引数にあるかどうかを確認
if [[ "$*" == *"--record"* ]]; then
  # 日時を表すフォルダを作成
  mkdir -p "$folder_path"

  # input_data.json を作成したフォルダ内にコピー
  cp input_data.json "${folder_path}/input_data.json"
  echo "input_data.json が ${folder_path}/input_data.json にコピーされました。"
fi