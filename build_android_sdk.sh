#!/bin/sh

# A shell script to build custom android SDK conveniently. Automatically builds for armeabi-v7a and arm64-v8a
# architectures.
#
# Usage:
#  ./build_android_sdk.sh  <PATH-OF-OPENCV-SOURCES> <PATH-OF-OUTPUT-BUILD-AND-INSTALL-DIRECTORY>
#
# Prerequisites:
#  - JDK 8 (JDK 11 won't work currently because NDK gradle doesn't support it)
#  - Android SDK with NDK bundle

# ==================== SET YOUR ENVIRONMENT VARIABLES HERE =================================

export ANDROID_SDK_ROOT=

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/

# List of OpenCV modules to build.
# Note that core, imgcodecs and java will always be built and need not be included.
export MODULES_LIST='imgproc,features2d,xfeatures2d'

# Build type:
#   'Release' for highest speed
#   'MinSizeRel' for smallest size
#   'Debug' for debug build
export BUILD_TYPE=MinSizeRel

# Enable or disable OpenCV capabilities, features and libraries.
export ANDROID_CMAKE_FLAGS="-DCV_TRACE=OFF -DBUILD_EXAMPLES=OFF -DBUILD_ANDROID_EXAMPLES=OFF \
-DBUILD_JPEG=ON -DBUILD_PNG=ON -DBUILD_OPENEXR=OFF -DBUILD_TIFF=OFF -DBUILD_WEBP=OFF \
-DBUILD_JASPER=OFF -DBUILD_ITT=OFF -DBUILD_PACKAGE=OFF -DBUILD_PERF_TESTS=OFF \
-DBUILD_PROTOBUF=OFF -DBUILD_TBB=OFF -DBUILD_TESTS=OFF -DWITH_ADE=OFF -DWITH_CAROTENE=OFF \
-DWITH_IMGCODEC_HDR=OFF -DWITH_IMGCODEC_PFM=OFF -DWITH_IMGCODEC_PXM=OFF \
-DWITH_IMGCODEC_SUNRASTER=OFF -DWITH_ITT=OFF  -DWITH_JASPER=OFF -DWITH_JPEG=ON \
-DWITH_OPENEXR=OFF -DWITH_PNG=ON -DWITH_PROTOBUF=OFF  -DWITH_PTHREADS_PF=ON -DWITH_QUIRC=OFF \
-DWITH_TIFF=OFF -DWITH_WEBP=OFF"



# ==================== END ENVIRONMENT VARIABLES =================================




# Verify inputs and variables.

OPENCV_SRC="$1"
BUILD_DIR="$2"

# Verify source dir.
if [ -z "$OPENCV_SRC" ]; then
  printf "\nERROR: Specify a source directory\n"
  exit 1
fi

if [ -z "$BUILD_DIR" ]; then
  printf "\nERROR: Specify a build directory\n"
  exit 1
fi

OPENCV_SRC=$(readlink -f "$OPENCV_SRC")
if [ ! -d "$OPENCV_SRC/opencv" ]; then
  printf "\nERROR: opencv source not found under $0. Clone https://github.com/opencv/opencv or a fork under $0.\n"
  exit 1
fi
if [ ! -f "$OPENCV_SRC/opencv/CMakeLists.txt" ]; then
  printf "\nERROR: opencv source not found under $0. Clone https://github.com/opencv/opencv or a fork under $0.\n"
  exit 1
fi
if [ ! -d "$OPENCV_SRC/opencv_contrib" ]; then
  printf "\nERROR: opencv_contrib source not found under $0. Clone \
  https://github.com/opencv/opencv_contrib or a fork under $0.\n"
  exit 1
fi
if [ ! -d "$OPENCV_SRC/opencv_contrib/modules" ]; then
  printf "\nERROR: opencv_contrib source not found under $0. Clone \
  https://github.com/opencv/opencv_contrib or a fork under $0.\n"
  exit 1
fi
printf "\nSource directory: $OPENCV_SRC\n"
export OPENCV_SRC

# Detect OpenCV version.
OPENCV_VERSION=$(grep '<version>' "$OPENCV_SRC/opencv/platforms/maven/pom.xml" | sed 's|<version>||' | sed 's|</version>||' | xargs)
printf "\nDetected OpenCV version: $OPENCV_VERSION\n"

if [ ! -d "$BUILD_DIR" ]; then
  printf "\nCreating build directory $BUILD_DIR\n"
  mkdir -p "$BUILD_DIR"
else
  printf "\nCleaning build directory $BUILD_DIR\n"
  rm -rf "$BUILD_DIR"/*
fi
printf "\nBuild directory: $BUILD_DIR\n"
BUILD_DIR=$(readlink -f "$BUILD_DIR")
printf "\nCreating architecture subdirectories under $BUILD_DIR\n"
BUILD_DIR_V7A="$BUILD_DIR/arm-v7a"
BUILD_DIR_V8A="$BUILD_DIR/arm64-v8a"
mkdir -p "$BUILD_DIR_V7A"
mkdir -p "$BUILD_DIR_V8A"
export BUILD_DIR
export BUILD_DIR_V7A
export BUILD_DIR_V8A

if [ -z "$ANDROID_SDK_ROOT" ]; then
  printf "\nERROR: Edit this script and set ANDROID_SDK_ROOT at top to point to your Android SDK root directory\n"
  exit 1
fi

if [ ! -d "$ANDROID_SDK_ROOT" ]; then
  printf "\nERROR: Edit this script and set ANDROID_SDK_ROOT at top to point to your Android SDK root directory\n"
  exit 1
fi

if [ ! -d "$ANDROID_SDK_ROOT/cmake" ]; then
  printf "\nERROR: Android SDK not found under $ANDROID_SDK_ROOT. Edit this script and \
  set ANDROID_SDK_ROOT at top to point to your Android SDK root directory.\n"
  exit 1
fi

export ANDROID_SDK="$ANDROID_SDK_ROOT"
export ANDROID_HOME="$ANDROID_SDK_ROOT"

# Look for NDK under SDK. Use the highest version if multiple versions are installed.
ANDROID_NDK=""
if [ -d "$ANDROID_SDK_ROOT/ndk-bundle" ]; then
  ANDROID_NDK="$ANDROID_SDK_ROOT/ndk-bundle"

elif [ -d "$ANDROID_SDK_ROOT/ndk" ]; then
  ndkver=$(ls "$ANDROID_SDK_ROOT/ndk" | sort -rV | head -1)
  if [ -f "$ANDROID_SDK_ROOT/ndk/$ndkver/ndk-build" ]; then
    ANDROID_NDK="$ANDROID_SDK_ROOT/ndk/$ndkver"
  fi
fi
if [ -z "$ANDROID_NDK" ]; then
  printf "\nERROR: Android NDK not found under $ANDROID_SDK_ROOT. Install an NDK version or NDK bundle.\n"
  exit 1
fi
printf "\nINFO: Using $ANDROID_NDK as NDK root\n"
export ANDROID_NDK


# Look for CMake under SDK. Use the highest version if multiple versions are installed.
CMAKE_DIR=""
cmakever=$(ls "$ANDROID_SDK_ROOT/cmake" | sort -rV | head -1)
if [ -f "$ANDROID_SDK_ROOT/cmake/$cmakever/bin/cmake" ]; then
  CMAKE_DIR="$ANDROID_SDK_ROOT/cmake/$cmakever/bin/"
fi
if [ -z "$CMAKE_DIR" ]; then
  printf "\nERROR: CMake not found under $ANDROID_SDK_ROOT. Install SDK CMake package.\n"
  exit 1
fi
printf "\nINFO: Using CMake and Ninja from $CMAKE_DIR\n"
export CMAKE_DIR


# Verify Java.
java -version
if [ $? -ne 0 ]; then
  printf "\nERROR: Cannot run java. Ensure that java is in PATH.\n"
  exit 1
fi
if [ -n "$SKIP_JAVA_CHECK" ]; then
  printf "\nSkipping Java version check\n"
else
  java -version 2>&1 | grep -q '1.8'
  if [ $? -ne 0 ]; then
    printf "\nERROR: Java does not seem to be Java 8. Higher versions may not work due to NDK Gradle conflicts.\n"
    printf "If you want to to proceed with this java version, define env var SKIP_JAVA_CHECK and rerun this script.\n"
    exit 1
  fi
fi

export BUILD_LIST="'core,imgcodecs,java,$MODULES_LIST'"
printf "\nModules that'll be built: $BUILD_LIST\n"


# Set build flags common to all architectures.

export ANDROID_CMAKE_FLAGS="-GNinja -DCMAKE_MAKE_PROGRAM=$CMAKE_DIR/ninja -DBUILD_WITH_DEBUG_INFO=OFF \
-DINSTALL_CREATE_DISTRIB=ON -DCMAKE_BUILD_TYPE=$BUILD_TYPE -DBUILD_FAT_JAVA_LIB=OFF \
-DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake -DANDROID_SDK_TOOLS_VERSION=26.1.1 \
-DANDROID_STL=c++_shared -DANDROID_TOOLCHAIN=clang \
-DOPENCV_EXTRA_MODULES_PATH=$OPENCV_SRC/opencv_contrib/modules -DBUILD_LIST=$BUILD_LIST \
-DBUILD_ANDROID_PROJECTS=ON $ANDROID_CMAKE_FLAGS"

printf "\n\n$ANDROID_CMAKE_FLAGS\n\n"



# Build for arm-v7a
printf "\n\n\n\nBUILDING FOR ARMEABI-V7A...\n\n\n\n"

cd "$BUILD_DIR_V7A"

"$CMAKE_DIR"/cmake $ANDROID_CMAKE_FLAGS -DANDROID_ABI='armeabi-v7a with NEON' \
     -DANDROID_PLATFORM_ID=2 $OPENCV_SRC/opencv
if [ $? -ne 0 ]; then
  printf "\nError: CMake failed\n"
  exit
fi

"$CMAKE_DIR"/ninja opencv_modules
if [ $? -ne 0 ]; then
  printf "\nError: OpenCV modules build failed\n"
  exit
fi

"$CMAKE_DIR"/ninja install/strip
if [ $? -ne 0 ]; then
  printf "\nError: OpenCV Android module build or installation failed\n"
  exit
fi



# Build for arm64-v8a
printf "\n\n\n\nBUILDING FOR ARM64-V8A...\n\n\n\n"

cd "$BUILD_DIR_V8A"

"$CMAKE_DIR"/cmake  $ANDROID_CMAKE_FLAGS -DANDROID_ABI='arm64-v8a' \
      -DANDROID_PLATFORM_ID=3  $OPENCV_SRC/opencv
if [ $? -ne 0 ]; then
  printf "\nError: CMake failed\n"
  exit
fi

"$CMAKE_DIR"/ninja opencv_modules
if [ $? -ne 0 ]; then
  printf "\nError: OpenCV modules build failed\n"
  exit
fi

"$CMAKE_DIR"/ninja install/strip
if [ $? -ne 0 ]; then
  printf "\nError: OpenCV Android module build or installation failed\n"
  exit
fi


# Create SDK directory.
printf "Installing OpenCV for Android SDK"
INSTALL_DIR="$BUILD_DIR/OpenCV-android-sdk/"
if [ -d "$INSTALL_DIR" ]; then
  rm -rf "$INSTALL_DIR"/*
fi
mkdir -p "$INSTALL_DIR/sdk"
cp -r "$BUILD_DIR_V7A"/install/sdk/* "$INSTALL_DIR/sdk/"
cp -r "$BUILD_DIR_V8A"/install/sdk/* "$INSTALL_DIR/sdk/"
cp -r "$BUILD_DIR_V7A"/install/LICENSE "$INSTALL_DIR/"

cd "$BUILD_DIR"
ZIP_FILE="OpenCV-$OPENCV_VERSION-android-custom-sdk.zip"
zip -r "$ZIP_FILE" OpenCV-android-sdk/

printf "\nDone! SDK archive location: $BUILD_DIR/$ZIP_FILE\n"
