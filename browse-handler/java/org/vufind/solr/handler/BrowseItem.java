package org.vufind.solr.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Container class for data in a single browse entry.
 * <p>
 * This container class is implemented as a {@code Map}, so we can easily
 * add it to the Solr respone object, and interact with it again as a member
 * of the {@link BrowseResponse} object.
 * <p>
 * Only a fixed set of keys are valid, as they must be predictable for the
 * consuming client code. To enforce that fixed set of keys, and the types
 * of values, there are get and set methods for each key/value pair.
 *
 * <table>
 * <caption>Valied keys for BrowseItem</caption>
 * <tr>
 * <th>Key</th>
 * <th>Type</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>{@code sort_key}</td>
 * <td>{@code String}</td>
 * <td>sort key for the heading - Required</td>
 * </tr>
 * <tr>
 * <td>{@code heading}</td>
 * <td>{@code String}</td>
 * <td>heading literal - Required</td>
 * </tr>
 * <tr>
 * <td>{@code seeAlso}</td>
 * <td>{@code List<String>}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@code useInstead}</td>
 * <td>{@code List<String>}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@code note}</td>
 * <td>{@code String}</td>
 * <td>note about the heading from heading core</td>
 * </tr>
 * <tr>
 * <td>{@code ids}</td>
 * <td>{@code List<String>}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@code fields}</td>
 * <td>{@code Map<String, List<Collection<String>>>}</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>{@code count}</td>
 * <td>{@code Integer}</td>
 * <td></td>
 * </tr>
 * </table>
 */
@SuppressWarnings("serial")
public class BrowseItem extends HashMap<String, Object>
{
    public BrowseItem(String sort_key, String heading)
    {
        this.setSortKey(sort_key);
        this.setHeading(heading);
    }

    public void setSortKey(String sort_key)
    {
        this.put("sort_key", sort_key);
    }
    public void setHeading(String heading)
    {
        this.put("heading", heading);
    }

    public void setSeeAlso(List<String> seeAlso)
    {
        this.put("seeAlso", seeAlso);
    }

    public void setUseInstead(List<String> useInstead)
    {
        this.put("useInstead", useInstead);
    }

    public void setNote(String note)
    {
        this.put("note", note);
    }

    /**
     * Set the list of IDs of bibs that match this heading.
     * <p>
     * Bib IDs are gathered into {@code List<Collection<String>>}.
     * That is, IDs are passed in as a List of Collections, but stored
     * as on flat List of IDs.
     * <p> see bibinfo in
     * BibDB.matchingIDs() and populateItem().
     *
     * @param idList List of Collection of bib IDs.
     */
    public void setIds(List<Collection<String>> idList)
    {
        List<String>ids = new ArrayList<String> ();
        for (Collection<String> idCol : idList) {
            ids.addAll(idCol);
        }
        this.put("ids", ids);
    }

    public void setFields(Map<String, List<Collection<String>>> fields)
    {
        this.put("fields", fields);
    }

    public void setCount(int count)
    {
        this.setCount(new Integer(count));
    }

    public void setCount(Integer count)
    {
        this.put("count", count);
    }

    public String getHeading()
    {
        return optString((String) this.get("heading"));
    }

    public String getSortKey()
    {
        return optString((String) this.get("sort_key"));
    }

    @SuppressWarnings("unchecked")
    public List<String> getSeeAlso()
    {
        return optListString((List<String>) this.get("seeAlso"));
    }

    @SuppressWarnings("unchecked")
    public List<String> getUseInstead()
    {
        return optListString((List<String>) this.get("useInstead"));
    }

    public String getNote()
    {
        return optString((String) this.get("note"));
    }

    @SuppressWarnings("unchecked")
    public List<String> getIds()
    {
        return optListString((List<String>) this.get("ids"));
    }

    @SuppressWarnings("unchecked")
    public Map<String, List<Collection<String>>> getFields()
    {
        // The things we do when we're not set up for Optional...
        Map<String, List<Collection<String>>> fields = (Map<String, List<Collection<String>>>) this.get("fields");

        return (fields != null ? fields : new HashMap<String, List<Collection<String>>>());
    }

    /**
     * Returns the hit count for this heading.
     *
     * @return number of records that match the heading
     */
    public Integer getCount()
    {
        Integer count = (Integer) this.get("count");
        return (count != null ? count : 0);
    }

    /**
     * Returns the hit count for this heading.
     *
     * @return number of records that match the heading
     */
    public int getCountAsInt()
    {
        return this.getCount().intValue();
    }

    /**
     * Put a new key / value pair, admitting only a restricted set of keys.
     * <p>
     * Will get rid of this if working entirely thought the set/get
     * methods looks good.
     */
    public Object put(String key, Object value)
    {
        if (!(key == "sort_key" || key == "heading" || key == "seeAlso" ||
                key == "useInstead" || key == "note" || key == "count" ||
                key == "ids" || key == "fields")) {
            throw new java.lang.IllegalArgumentException("Illegal key: " + key);
        }
        return super.put(key, value);
    }

    // These are helper methods because we're not modeled for Optional
    // TODO: Move these to a utility class for reuse elsewhere

    /**
     * Return argument or fresh {@code String } if null
     *
     * @param  s A string
     * @return s or a new {@code String}
     */
    static private String optString(String s)
    {
        return (s != null ? s : new String());
    }

    /**
     * Return argument or fresh {@code List<String>} if null
     *
     * @param list A list
     * @return list or an new {@code List<String>}
     */
    static private List<String> optListString(List<String> list)
    {
        return (list != null ? list : new ArrayList<String>());
    }
}
