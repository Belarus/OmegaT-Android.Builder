<?php
  header('Content-type: text/plain; charset=UTF-8');
  
  $f = fopen("stat.txt","a");
  if (!$f) {
    die("Error open stat file for write");
  }
  flock($f, LOCK_EX);
  fwrite($f, "====================================================================\n");
  fwrite($f, "  ".date('Y-m-d H:i:s')."\n");
  foreach ($_REQUEST as $var => $value) {
    fwrite($f, $var.' = '.$value."\n");
  }

  $postdata = file_get_contents("php://input");

  fwrite($f, $postdata);
  fwrite($f, "===============\n");
  
  // list dir
  $h = fopen("list2.txt", "r");
  if (!$h) {
      die("Error read dir");
  }
  $ls = array();
  while (!feof($h)) {
      $fd = rtrim(fgets($h, 4096));
      array_push($ls, new FileDisk(explode("|",$fd)));
  }
  fclose($h);
  
  $h = fopen("translated2.txt", "r");
  if (!$h) {
      die("Error read dir");
  }
  $translatedPackages = array();
  while (!feof($h)) {
      $fp = rtrim(fgets($h, 4096));
      array_push($translatedPackages, $fp);
  }
  fclose($h);
  

  // process params
  foreach (explode("\n",$postdata) as $fs) {
      $file = explode("|",$fs);
	  if ($file[0]!='f') {
	    continue;
	  }
      $fp = new FileParam($file);
	  
	  $status = null;
	  $transferSize = 0;
	  if (!in_array($fp->package, $translatedPackages)) {
			$status = "NONEED";
	        $serverFileName = 'noneed';
			$transferSize = 0;
	  }
	  if (!$status) {
		  foreach ($ls as $d) {
			if ($d->package==$fp->package && $d->origVersion==$fp->origVersion) {
			  $transferSize = filesize($d->filename);
			  if ($d->transVersion != $fp->transVersion) {
				// need update
				$status = "UPDATE";
			  } else {
				$status = "LATEST";
			  }
			  $serverFileName = $d->filename;
			  break;
			}
		  }
	  }
	  if (!$status) {
           $status = "NONTRANSLATED";
           $serverFileName = preg_replace('/[^A-Za-z0-9_\-\.]/i', '', $fp->package.'_'.$fp->origVersion.'.apk');
		   if (file_exists('upload2/'.$serverFileName)) {
		        $transferSize = filesize('upload2/'.$serverFileName);
		   } else {
				$transferSize = 0;
		   }
	  }
	  $res=$fp->package.'|'.$fp->origVersion.'|'.$status.'|'.$transferSize.'|'.$serverFileName."\n";
      print $res;
      fwrite($f, $res);
  }
  
  fwrite($f, "===== end\n");
  flock($f, LOCK_UN);
  fclose($f);


class FileDisk {
  public $package;
  public $origVersion, $transVersion;
  public $filename;
  
  function __construct($s) {
      $this->package = $s[0];
	  $this->origVersion = $s[1];
	  $this->transVersion = $s[2];
	  $this->filename = $s[3];
  }
}

class FileParam {
  public $package;
  public $origVersion, $transVersion;
  
  function __construct($s) {
	  $this->package = $s[1];
	  $this->origVersion = $s[2];
	  $this->transVersion = $s[3];
  }
}
?>
