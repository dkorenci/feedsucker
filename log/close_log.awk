# if app has not finished log xml files do not have
# closing </log> tag, add it if necessary
# also remove header lines, since basex complains about them
BEGIN {	
  do_close = 1
  close_tag = "</log>"
}

{ 
	if (!( $0 ~ "<\?xml" || $0 ~ "<!DOCTYPE" )) print $0
	if ($0 ~ close_tag) do_close = 0
}

END {  
  if (do_close == 1) print close_tag
}
