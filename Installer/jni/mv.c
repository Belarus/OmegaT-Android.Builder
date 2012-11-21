#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

int main(int argc, char *argv[]) {
  if (argc!=2) return 1;
  int fin, fout;
  
  if (fin=open(argv[0], O_RDONLY)<0)  return 2;

  if (fout=open(argv[1], O_WRONLY)<0) return 3;
  
  if (close(fin)<0)  return 4;
  if (close(fout)<0) return 5;
  
  if (truncate(argv[1], size);
  return 0;
}
