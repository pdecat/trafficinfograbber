<?php
    /*
     * %%Ignore-License
     */
    $base = 'http://www.sytadin.fr/';
    $url = '';
    $qsa = '?';
    foreach($_GET as $n => $v) {
        if($n == 'uri') {
            $url = $base . $v;
        } else if($n != 'callback') {
            $qsa .= '&' . $n . '=' . rawurlencode($v);
        }
    }
    $json = file_get_contents($url . $qsa);
    echo sprintf($json);
?>
