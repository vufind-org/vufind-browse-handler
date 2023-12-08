package org.vufind.solr.indexing;

//
// Author: Mark Triggs <mark@teaspoon-consulting.com>
//


public interface Predicate
{
    boolean isSatisfiedBy(Object obj);
}
