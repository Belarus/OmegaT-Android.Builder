#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <utime.h>
#include <string.h>

struct stat origstat;
char *mode;
char *fin;
char *fout;

void set(char *f) {
  if (chown(f, origstat.st_uid, origstat.st_gid)!=0) { fprintf(stderr,"Error in chown(%s)",f); _exit(1); }
  if (chmod(f, origstat.st_mode)!=0) { fprintf(stderr,"Error in chmod(%s)",f); _exit(1); }

  struct utimbuf tm;
  tm.actime = origstat.st_atime;
  tm.modtime = origstat.st_mtime;
  if (utime(f, &tm)!=0) { fprintf(stderr,"Error in utime(%s)",f); _exit(1); }
}

void copy(char *f_from, char *f_to) {
  int fin, fout;
  uint8_t buffer[65536];
  if ((fin=open(f_from, O_RDONLY))<0)  { fprintf(stderr,"Error in open(%s)",f_from); _exit(1); }
  if ((fout=open(f_to, O_WRONLY | O_TRUNC | O_CREAT))<0) { fprintf(stderr,"Error in open(%s)",f_to); _exit(1); }
  while(1) {
    ssize_t rd = read(fin, buffer, sizeof(buffer));
    if (rd<0) { fprintf(stderr,"Error in read(%s)",f_from); _exit(1); }
    if (rd==0) {break;}
    if (write(fout, &buffer, rd)!=rd) { fprintf(stderr,"Error in write(%s)",f_to); _exit(1); }
  }

  if (close(fin)<0)  { fprintf(stderr,"Error in close(%s)",f_from); _exit(1); }
  if (close(fout)<0) { fprintf(stderr,"Error in close(%s)",f_to); _exit(1); }
}

void runWrite() {
  copy(fin, fout);
  set(fout);
}

void runRename() {
  if (strlen(fout)>1000) { fprintf(stderr,"Too long filename(%s)",fout); _exit(1); }

  char tmp[1024];
  strcpy(tmp, fout);
  strcat(tmp, ".new");
  copy(fin, tmp);
  set(tmp);
  if (rename(tmp, fout)!=0) { fprintf(stderr,"Error in rename(%s,%s)",tmp,fout); _exit(1); }
}

// params: <mode: write, rename> <new file> <original file>

void main(int argc, char *argv[]) {
  if (argc!=4) { fprintf(stderr,"Wrong parameters"); _exit(1); }
  mode = argv[1];
  fin = argv[2];
  fout = argv[3];

  if (stat(fout, &origstat)!=0) { fprintf(stderr,"Error in stat(%s)",fout); _exit(1); }

  if (strcmp("write",mode)==0) {
    runWrite();
  } else if (strcmp("rename",mode)==0) {
    runRename();
  } else {
    fprintf(stderr,"Wrong mode"); _exit(1);
  }
}

