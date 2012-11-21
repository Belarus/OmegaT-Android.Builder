LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
 
LOCAL_MODULE    := stat
LOCAL_SRC_FILES := stat.c

## logging
## LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)


# ========================================================
include $(CLEAR_VARS)

LOCAL_MODULE    := mv
LOCAL_SRC_FILES := mv.c

include $(BUILD_EXECUTABLE)
