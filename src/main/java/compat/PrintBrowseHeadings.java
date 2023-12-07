public class PrintBrowseHeadings
{
    public static void main(String args[]) throws Exception
    {
        System.err.print("\n\n\n" +
                           "************************************************************\n" +
                           "DEPRECATION WARNING: You are using the 'PrintBrowseHeadings' class.\n" +
                           "This still works, but it has been renamed to 'org.vufind.solr.indexing.PrintBrowseHeadings'.\n" +
                           "You should switch to avoid breakage in future versions.\n" +
                           "************************************************************\n\n\n");

        org.vufind.solr.indexing.PrintBrowseHeadings.main(args);
    }
}
