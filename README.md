[![CI Status](https://github.com/vufind-org/vufind-browse-handler/actions/workflows/ci.yaml/badge.svg?branch=dev)](https://github.com/vufind-org/vufind-browse-handler/actions/workflows/ci.yaml)

Care and feeding of the VuFind Solr browse request handler
==========================================================



0.  Background
--------------

This Solr plugin was originally developed to support the browse
functionality of the National Library of Australia's Catalogue
(http://catalogue.nla.gov.au). It later became a standard component
of VuFind. Please read the LICENSE file that accompanies this file
for details regarding the distribution of this software.



1.  Compiling it
----------------

You'll need Ant to get everything compiled:

    ant jars -Dvufind.dir=/usr/local/vufind

should give you the two required jar files:

    browse-handler.jar
    browse-indexing.jar

  

2.  Creating your browse indexes
--------------------------------

### 2.1.  Create lists of headings for browsing.

Now we produce a list of the headings we want to browse over.  We want to browse on:

* Any term that appears in a particular index of our bib data (e.g. subject-browse)
* Any non-preferred term from our authority index whose preferred
form is linked to from our bib data (i.e. appears in the above index).

The PrintBrowseHeadings class does this: grabs headings from these
sources, produces a sort key for each heading and prints out a big
file with lines of the form:

    <Sort key>^A<Heading>

Running it:

    java -cp browse-indexing.jar PrintBrowseHeadings /path/to/your/bib/data/index subject-browse authority.index subjects.tmp
    java -cp browse-indexing.jar PrintBrowseHeadings /path/to/your/bib/data/index author-browse authority.index names.tmp

By default this assumes you're using my default field names in your authority index, which are:

* preferred (1xx)
* insteadOf (4xx)

If you're not, you can provide the field names using Java system properties
on the above command lines.  For example, VuFind uses:

    -Dfield.preferred=heading -Dfield.insteadof=use_for


Next we just need to remove any duplicates.  I do this using the GNU
sort program from the command-line because it's amazingly fast even on
big files:

    sort -T /var/tmp -u --field-separator=$'\1' -k1 subjects.tmp -o sorted-subjects.tmp
    sort -T /var/tmp -u --field-separator=$'\1' -k1 names.tmp -o sorted-names.tmp



### 2.2.  Creating the SQLite DB

The last step is to load all the headings into an SQLite database
(which acts as the browse index, effectively).  CreateBrowseSQLite
does this:

    java -cp browse-indexing.jar CreateBrowseSQLite sorted-names.tmp namesbrowse.db
    java -cp browse-indexing.jar CreateBrowseSQLite sorted-subjects.tmp subjectsbrowse.db


And that's the indexing process.  At the end of this you should have
one SQLite database per browse type, and an index of your authority
data.  Everything else is disposable!




3.  Configuring Solr
--------------------

### 3.1.  Jar files

Now that we've got our indexes built, we just need to configure the
Browse request handler to use them.  Start by copying the
browse-handler to Solr's lib directory.

    cp browse-handler.jar solr/WEB-INF/lib



### 3.2.  Solr configuration

Then configure your browse types in solrconfig.xml:

```
    <requestHandler name="/browse" class="org.vufind.solr.handler.BrowseRequestHandler">
       <str name="sources">names,subjects</str>

       <!-- These definitions should match the field names used in the authority index. -->
       <str name="preferredHeadingField">preferred</str>
       <str name="useInsteadHeadingField">insteadOf</str>
       <str name="seeAlsoHeadingField">seeAlso</str>
       <str name="scopeNoteField">scopeNote</str>

       <lst name="names">
         <str name="DBpath">/path/to/your/namesbrowse.db</str>
         <str name="field">author-browse</str>
       </lst>

       <lst name="subjects">
         <str name="DBpath">/path/to/your/subjectsbrowse.db</str>
         <str name="field">subject-browse</str>
         <str name="dropChars">[]()',</str>
       </lst>
    </requestHandler>
```


### 3.3.  Testing

Finally, start up Solr and test that things are working:

    http://yourhost.example.com:8080/solr/browse?source=subjects&from=boats&rows=20



4.  Running updates
-------------------

The browse request handler has been designed to automatically detect
updates to these indexes and reloads them as required.  The steps are
simple:

    mv mybrowse.db mybrowse.db.old;  mv mybrowse.db.new mybrowse.db
    my authority.index authority.index.old; mv authority.index.new authority.index


5.  Development
---------------

Running the unit tests:

    ant test -Dvufind.dir=/usr/local/vufind

Coding style is One True Brace style. In astyle:

    astyle --mode=java --style=1tbs -U -H -I -R 'browse-handler/*' 'browse-indexing/*' 'common/*' 'tests/org/*'

