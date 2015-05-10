LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := opencv
LOCAL_SRC_FILES := opencv.cpp

include $(BUILD_SHARED_LIBRARY)