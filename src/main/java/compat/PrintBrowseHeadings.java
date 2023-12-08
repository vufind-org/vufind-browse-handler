import org.vufind.util.Utils;

public class PrintBrowseHeadings
{
    public static void main(String args[]) throws Exception
    {
        Utils.printDeprecationWarning("You are using the 'PrintBrowseHeadings' class.",
                                      "This still works, but it has been renamed to 'org.vufind.solr.indexing.PrintBrowseHeadings'",
                                      "You should switch to avoid breakage in future versions.");

        org.vufind.solr.indexing.PrintBrowseHeadings.main(args);
    }
}
