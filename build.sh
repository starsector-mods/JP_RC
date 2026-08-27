#!/bin/bash
set -e

# Ensure we're in the mod directory
cd "$(dirname "$0")"

STARSECTOR_CORE="../.."
MODS_DIR=".."

CLASSPATH=""
for jar in "$STARSECTOR_CORE"/*.jar; do
    if [ -f "$jar" ]; then
        if [ -z "$CLASSPATH" ]; then
            CLASSPATH="$jar"
        else
            CLASSPATH="$CLASSPATH:$jar"
        fi
    fi
done

# Helper function to find and add mod libraries dynamically
add_jar_by_name() {
    local jar_name="$1"
    # Find the first matching jar file in the mods directory (up to 3 levels deep)
    local jar_path=$(find "$MODS_DIR" -maxdepth 3 -type f -name "$jar_name" | head -n 1)
    if [ -n "$jar_path" ]; then
        CLASSPATH="$CLASSPATH:$jar_path"
    else
        echo "Warning: Could not find library $jar_name in $MODS_DIR"
    fi
}

add_jar_by_name "LazyLib.jar"
add_jar_by_name "LazyLib-Kotlin.jar"
add_jar_by_name "MagicLib.jar"
add_jar_by_name "MagicLib-Kotlin.jar"
add_jar_by_name "Graphics.jar"
add_jar_by_name "ExerelinCore.jar"

find src -name "*.java" > sources.txt
mkdir -p bin
javac -Xlint:unchecked -Xlint:deprecation -Xlint:-options --release 17 -sourcepath "" -implicit:none -cp "$CLASSPATH" -d bin @sources.txt

cd bin
jar cf ../jars/JunkPirates.jar .
cd ..

echo "JunkPirates.jar built successfully"
