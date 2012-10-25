<?php
    $base = 'http://www.sytadin.fr/';
    $url = '';
    $uri = '';
    $qsa = '?';
    foreach($_GET as $n => $v) {
        if($n == 'uri') {
            /* Prevent .. from URIs */
            if (strpos($n, '..') !== false) {
                exit;
            }
            $uri = explode('?', $v);
            $uri = $uri[0];
            $url = $base . $v;
        } else if($n != 'callback') {
            $qsa .= '&' . $n . '=' . rawurlencode($v);
        }
    }

    $cachefile = 'cache/' . $uri;
    $cachetime = 60;
    // Serve from the cache if it is younger than $cachetime
    if (file_exists($cachefile) && time() - $cachetime < filemtime($cachefile)) {
        include($cachefile);
        exit;
    }

    /* The code to dynamically generate the page goes here */
    $json = file_get_contents($url . $qsa);
    echo sprintf($json);

    // Cache the output to a file
    $fp = fopen($cachefile, 'w');
    fwrite($fp, $json);
    fclose($fp);
?>
