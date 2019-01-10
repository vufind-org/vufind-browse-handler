package org.vufind.solr.handler;

/**
 * Provide access to the on-disk SQLite browse index.
 *
 */
class BrowseSource
{
    public String DBpath;
    public String field;
    public String dropChars;
    public String normalizer;
    public boolean retrieveBibId;
    public int maxBibListSize;

    private HeadingsDB headingsDB = null;
    private long loanCount = 0;


    public BrowseSource(String DBpath,
                        String field,
                        String dropChars,
                        String normalizer,
                        boolean retrieveBibId,
                        int maxBibListSize)
    {
        this.DBpath = DBpath;
        this.field = field;
        this.dropChars = dropChars;
        this.normalizer = normalizer;
        this.retrieveBibId = retrieveBibId;
        this.maxBibListSize = maxBibListSize;
    }

    // Get a HeadingsDB instance.  Caller is expected to call `queryFinished` on
    // this when done with the instance.
    public synchronized HeadingsDB getHeadingsDB()
    {
        if (headingsDB == null) {
            headingsDB = new HeadingsDB(this.DBpath, this.normalizer);
        }

        // If no queries are running, it's a safe point to reopen the browse index.
        if (loanCount <= 0) {
            headingsDB.reopenIfUpdated();
            loanCount = 0;
        }

        loanCount += 1;

        return headingsDB;
    }

    public synchronized void returnHeadingsDB(HeadingsDB headingsDB)
    {
        loanCount -= 1;
    }
}