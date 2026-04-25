#!/bin/bash
# ============================================================
#  Behavioral Biometrics Analysis Tool — Build & Run Script
# ============================================================
echo "Compiling all sources..."
javac -encoding UTF-8 *.java

if [ $? -eq 0 ]; then
    echo "✅ Compilation successful. Launching dashboard..."
    java SecurityDashboard
else
    echo "❌ Compilation failed. Check errors above."
fi
