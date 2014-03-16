LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := FaceContact
LOCAL_SRC_FILES := FaceContact.cpp

include $(BUILD_SHARED_LIBRARY)
