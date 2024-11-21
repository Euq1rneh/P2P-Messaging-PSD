#!/bin/bash

# Variables
SRC_DIR="src"
COMMON_DIR="common"
BIN_DIR="bin"
DIST_DIR="dist"
CLIENT_JAR="$DIST_DIR/messagingAppPeer.jar"
SERVER_JAR="$DIST_DIR/messagingAppServer.jar"
MANIFEST_FILE="MANIFEST.MF"

# Ensure bin and dist directories exist
if [ ! -d "$BIN_DIR" ]; then
  mkdir -p "$BIN_DIR"
fi
if [ ! -d "$DIST_DIR" ]; then
  mkdir -p "$DIST_DIR"
fi

# Compile all Java files
echo "Compiling Java source files..."
javac -d "$BIN_DIR" -sourcepath "$SRC_DIR" $(find "$SRC_DIR" -name "*.java")

# Check if compilation was successful
if [ $? -eq 0 ]; then
  echo "Compilation completed successfully."
else
  echo "Compilation failed. Please check the error messages."
  exit 1
fi

# Create a Manifest File for the Client JAR with Class-Path entry
echo "Main-Class: client.peer.Main" > "$MANIFEST_FILE"
echo "Class-Path: common/" >> "$MANIFEST_FILE"

# Create the Client JAR (includes common package)
echo "Building Client JAR..."
jar cmf "$MANIFEST_FILE" "$CLIENT_JAR" -C "$BIN_DIR" client -C "$BIN_DIR" common

# Create a Manifest File for the Server JAR with Class-Path entry
echo "Main-Class: server.server.Main" > "$MANIFEST_FILE"
echo "Class-Path: common/" >> "$MANIFEST_FILE"

# Create the Server JAR (includes common package)
echo "Building Server JAR..."
jar cmf "$MANIFEST_FILE" "$SERVER_JAR" -C "$BIN_DIR" server -C "$BIN_DIR" common

# Remove the MANIFEST file after creating the JARs
rm -f "$MANIFEST_FILE"

echo "Build completed successfully. JAR files are in the $DIST_DIR directory."
