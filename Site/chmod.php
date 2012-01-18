<?php
  header('Content-type: text/plain; charset=UTF-8');

  
  // list dir
  $h = opendir('upload/');
  if (!$h) {
      die("Error read dir");
  }

  while ($f = readdir($h)) {
  print "$f\n";
      chmod("upload/$f",0666);
  }
  closedir($h);
?>
