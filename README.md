# Formatter

Formatter is a Java web application designed for Tomcat. It can also be run 
in Netbeans for debugging over port 8089. The API is divided into 3 services:

## 1. Formatting a version list

Formatter uses the docid to obtain a list of versions and groups from the 
cortex collection, and uses that to create a &lt;select&gt; list with the 
given list id.

/formatter/list?docid=language/author/work&LIST_ID=select_list_id[&STYLE=style]

The response is a HTML formatted list using the list/default style or the 
specified replacement style (stored in corform collection).

## 2. Format an apparatus table

This formats the MVD table function, which produces a table of differences 
within a range of a given base version, similar to an appratus criticus.
There are many parameters:

* OFFSET: the starting offset in the base version (required)
* LENGTH: the length of the base version to find variants of (default: 100)
* WHOLE_WORDS: "true" variants should be extended to whole words (default: false)
* COMPACT: compact the table by collapsing identical rows (default false)
* HIDE_MERGED: omit text shared by all versions except in base (default false)
* SOME_VERSIONS: a comma-selarated list of versions to select (default "all")
* FIRST_ID: the version id of the base version

## 3. Get HTML of a given version

Given a docid and a version the cortex and corcode are retrieved and 
formatted as HTML.

The parameters are:

* version1: the version id of the version to fetch
* docid: the document identifier of the cortex to format
* CORCODE: the docid of a suitable corcode, or repeat the parameter for 
several (default docid+"/default")
* STYLE: the docid of the corform to use or repeat if several (default: the 
corform specified in the corcode)

Output is HTML formatting using the specified corcode markup and corform 
style.
        
