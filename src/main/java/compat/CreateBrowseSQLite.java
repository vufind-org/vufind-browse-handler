import org.vufind.util.Utils;

public class CreateBrowseSQLite
{
    public static void main(String args[]) throws Exception
    {
        Utils.printDeprecationWarning("You are using the 'CreateBrowseSQLite' class.",
                                      "This still works, but it has been renamed to 'org.vufind.solr.indexing.CreateBrowseSQLite'",
                                      "You should switch to avoid breakage in future versions.");

        org.vufind.solr.indexing.CreateBrowseSQLite.main(args);
    }
}
