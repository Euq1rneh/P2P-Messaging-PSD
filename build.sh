#!/bin/bash

# Variables
SRC_DIR="src"
BIN_DIR="bin"
DIST_DIR="dist"
CLIENT_JAR="dist/messagingAppPeer.jar"
SERVER_JAR="dist/messagingAppServer.jar"

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

# Create the Client JAR (includes common package)
echo "Building Client JAR..."
jar --create --file "$CLIENT_JAR" --manifest <(echo "Main-Class: client.peer.Main") -C "$BIN_DIR" client common

# Create the Server JAR (includes common package)
echo "Building Server JAR..."
jar --create --file "$SERVER_JAR" --manifest <(echo "Main-Class: server.server.Main") -C "$BIN_DIR" server common

echo "Build completed successfully. JAR files are in the $DIST_DIR directory."
