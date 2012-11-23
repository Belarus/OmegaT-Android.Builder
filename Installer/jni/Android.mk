LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
 
LOCAL_MODULE    := stat
LOCAL_SRC_FILES := stat.c

## logging
## LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)


# ========================================================
include $(CLEAR_VARS)

LOCAL_MODULE    := replacer
LOCAL_SRC_FILES := replacer.c

include $(BUILD_EXECUTABLE)
