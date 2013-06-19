#!/bin/bash
#
# Run the test suite

set -e

solrwar="$1"

if [ "$solrwar" = "" ]; then
    echo "Usage: $0 <path-to-solr.war>"
    exit
fi

if [ ! -e "$solrwar" ]; then
    echo "File '$solrwar' does not exist"
    exit
fi


solrwar=$(cd $(dirname $solrwar); pwd)/$(basename $solrwar)
tmpdir=$(cd $(dirname $0); pwd)/tmp


# build it
(
    cd "$(dirname $0)/../"
    ant jars -Dsolr.war="$solrwar"
)


# Extract Lucene from the solr.war
(
    mkdir -p "$tmpdir"
    cd "$tmpdir"
    jar xf "$solrwar" WEB-INF/lib
)


# Run the test suite
(
    cd "$(dirname $0)"

    java -Dfile.encoding=UTF-8 -cp "../build/common:../build/browse-indexing:../build/browse-handler:../libs/*:$tmpdir/WEB-INF/lib/*" clojure.main tests.clj
)


function cleanup {
    rm -rf "$tmpdir/WEB-INF" && rmdir "$tmpdir"
}

trap cleanup EXIT
