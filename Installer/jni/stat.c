#include "stat.h"

#include <fcntl.h>
#include <sys/stat.h>
#include <android/log.h> // logging


jobject getObjectField(JNIEnv *env, jobject obj, const char *fieldName, const char *fieldSignature) {

  jclass classObj = (*env)->GetObjectClass(env, obj);

  jfieldID fieldFi = (*env)->GetFieldID(env, classObj, fieldName, fieldSignature);

  return (*env)->GetObjectField(env, obj, fieldFi);
} 

jobject getObjectMethod(JNIEnv *env, jobject obj, const char *methodName, const char *methodSignature) {

  jclass classObj = (*env)->GetObjectClass(env, obj);

  jmethodID methodMe = (*env)->GetMethodID(env, classObj, methodName, methodSignature);

  return (*env)->CallObjectMethod(env, obj, methodMe);
} 

void setLongField(JNIEnv *env, jobject obj, const char *fieldName, long value) {

  jclass classObj = (*env)->GetObjectClass(env, obj);

  jfieldID fieldFi = (*env)->GetFieldID(env, classObj, fieldName, "J");

  return (*env)->SetLongField(env, obj, fieldFi, value);
}


JNIEXPORT void JNICALL Java_org_alex73_android_common_JniWrapper_getPermissions
  (JNIEnv *env, jclass cl, jobject fi) {

  jobject file = getObjectField(env, fi, "file", "Ljava/lang/String;");
  
  struct stat buf;
  
  const char *nativePath = (*env)->GetStringUTFChars(env, file, 0);
  int r=stat(nativePath, &buf);
  (*env)->ReleaseStringUTFChars(env, file, nativePath);
  if (r==0) {
    setLongField(env, fi, "perm", buf.st_mode);
    setLongField(env, fi, "owner", buf.st_uid);
    setLongField(env, fi, "group", buf.st_gid);
  } else {
    setLongField(env, fi, "perm", -1);
    setLongField(env, fi, "owner", -1);
    setLongField(env, fi, "group", -1);
  }
}
