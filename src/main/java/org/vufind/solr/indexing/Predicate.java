package org.vufind.solr.indexing;

//
// Author: Mark Triggs <mark@dishevelled.net>
//


public interface Predicate
{
    boolean isSatisfiedBy(Object obj);
}
