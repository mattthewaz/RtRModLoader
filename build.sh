#!/bin/sh
cd "$(dirname "$0")"
mvn install -q && echo "[RtRModLoader] Build OK"
