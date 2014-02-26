<?php /*
 * #%L
 * TrafficInfoGrabber
 * %%
 * Copyright (C) 2010 - 2014 Patrick Decat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */ ?>
<?php
    ini_set('display_errors',0);
    //ini_set('log_errors',1);
    //ini_set('error_log','jsonProxy.log');

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

    $cachedir = 'cache/';
    if (!file_exists($cachedir)) {
        mkdir($cachedir);
    }
    $cachefile = $cachedir . basename($uri);
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
