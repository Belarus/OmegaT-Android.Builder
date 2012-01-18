<?php
  header('Content-type: text/plain; charset=UTF-8');

  $file = $_GET['file'];
  $pos =  $_GET['pos'];

  $sfile = str_replace('/', '', $file);
  $sfile = str_replace('\\', '', $sfile);
  
  $f = fopen("stat.txt","a");
  if (!$f) {
    die("Error open stat file for write");
  }
  flock($f, LOCK_EX);
  fwrite($f,"===== upload2 $pos: $file => $sfile\n");

  $postdata = file_get_contents("php://input");
  if ($pos>40000000) return;


  $fo = fopen("upload2/$sfile","a");
  if (!$fo) {
    die("Error open upload file $sfile for write");
  }
  flock($fo, LOCK_EX);
  fseek($fo,$pos);
  fwrite($fo, $postdata);
  flock($fo, LOCK_EX);
  fclose($fo);

  
  fwrite($f, "===== endupload\n");
  flock($f, LOCK_UN);
  fclose($f);

?>
