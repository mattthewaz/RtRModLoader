#!/bin/sh
cd "$(dirname "$0")"
mkdir -p "$(pwd)/../output"
OUT="$(realpath "$(pwd)/../output")"
cp target/RtRModLoader-1.0-SNAPSHOT.jar "$OUT/"
echo "[RtRModLoader] Deployed to $OUT"
