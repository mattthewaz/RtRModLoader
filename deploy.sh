#!/bin/sh
cd "$(dirname "$0")"
mkdir -p "$(pwd)/../output"
OUT="$(realpath "$(pwd)/../output")"
cp target/RtRModLoader.jar "$OUT/"
echo "[RtRModLoader] Deployed to $OUT"
