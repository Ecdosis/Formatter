/* This file is part of calliope.
 *
 *  calliope is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  calliope is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with calliope.  If not, see <http://www.gnu.org/licenses/>.
 */
package formatter.path;
import formatter.Test;
import java.util.StringTokenizer;
import calliope.core.exception.CalliopeException;
import calliope.core.constants.Database;

/**
 * A basic path parser
 * @author desmond
 */
public class Path implements Test
{
    /** the resource name, if not null points to a valid db resource */
    protected String resource;
    /** the original path before conversion into a resource */
    protected String path;
    /** the database or service name */
    protected String name;
    /** String to convert "/" to */
    private static String ESC_SLASH = "%2F";
    /** string to convert space to %20 */
    private static String ESC_SPACE = "%20";
    /**
     * Construct the path simply
     * @param path a URN
     */
    public Path( String urn ) throws CalliopeException
    {
        StringTokenizer st = new StringTokenizer( urn, "/" );
        this.name = st.nextToken();
        this.path = getPath( st );
        this.resource = makeDocId( this.path );
    }
    /**
     * Turn a formal URN into a docid (escape "/" to "%2F")
     * @param urn the urn to convert
     * @return a valid docId
     */
    protected final String makeDocId( String urn )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<urn.length();i++ )
        {
            char token = urn.charAt( i );
            if ( token == '/' )
                sb.append( ESC_SLASH );
            else if ( token == ' ' )
                sb.append( ESC_SPACE );
            else
                sb.append( token );
        }
        return sb.toString();
    }
    /**
     * Convert a string tokenizer's contents into a URN path
     * @param st the string tokenizer
     * @return a String being the slash-delimited path
     */
    protected final String getPath( StringTokenizer st )
    {
        StringBuilder sb = new StringBuilder();
        boolean initial = true;
        while ( st.hasMoreTokens() )
        {
            if ( !initial )
                sb.append("/");
            sb.append( st.nextToken() );
            initial = false;
        }
        return sb.toString();
    }
    /**
     * Just get the resource minus the database/service name
     * @param withName prepend the dbname
     * @return the resource as a path
     */
    public String getResourcePath( boolean withName )
    {
        if ( withName )
            return name+"/"+path;
        else
            return path;
    }
    /**
     * Get the already once fetched resource or null
     * @param withPrefix if true prepend the db or service name
     * @return a String
     */
    public String getResource()
    {
        return "/"+name+"/"+resource;
    }
    /**
     * Does this path as it stands, support versions?
     * @return true if it does
     */
    public boolean hasVersions()
    {
        return this.name.equals(Database.CORTEX)
            || this.name.equals(Database.CORCODE);
    }
    /**
     * Get the "DB" or service name
     * @return a string
     */
    public String getName()
    {
        return name;
    }
    /**
     * Set the "DB" or service name 
     * @param a string
     */
    public void setName( String name )
    {
        this.name = name;
    }
    /**
     * Pop off the frontmost part of the path
     * @param path the path to pop
     * @return the popped path
     */
    public static String pop( String path )
    {
        while ( path.length()>0 && path.startsWith("/") )
            path = path.substring(1);
        int pos = path.indexOf("/");
        if ( pos != -1 )
            path = path.substring( pos+1 );
        return path;
    }
    /**
     * Remove the rightmost segment of the path and resource
     * @return the remains of the path
     */
    public static String chomp( String path )
    {
        String popped = "";
        int index = path.lastIndexOf( "/" );
        if ( index != -1 )
            popped = path.substring( 0, index );
        return popped;
    }
    /**
	 * Chop off the first component of a urn
	 * @param urn the urn to chop
	 * @return the first urn component
	 */
	public static String first( String urn )
	{ 
		int slashPos1 = -1;
		if ( urn.startsWith("/") )
		    slashPos1 = urn.indexOf( "/" );
		int slashPos2 = urn.indexOf( "/", slashPos1+1 );
		if ( slashPos1 != -1 && slashPos2 != -1 )
		    return urn.substring(slashPos1+1, slashPos2 );
		else if ( slashPos1 != -1 && slashPos2 == -1 )
		    return urn.substring( slashPos1+1 );
		else if ( slashPos1 == -1 && slashPos2 != -1 )
		    return urn.substring( 0,slashPos2 );
		else
		    return urn;
	}
    /**
	 * Extract the second component of a urn
	 * @param urn the urn to extract from
	 * @return the second urn component
	 */
	public static String second( String urn )
	{ 
        int start=-1,end=-1;
		for ( int state=0,i=0;i<urn.length();i++ )
        {
            char token = urn.charAt(i);
            switch ( state )
            {
                case 0:// always pass first char
                    state = 1;
                    break;
                case 1: 
                    if ( token == '/' )
                        state = 2;
                    break;
                case 2:
                    start=i;
                    if ( token == '/' )
                    {
                        state = -1;
                        end = i;
                    }
                    else
                        state = 3;
                    break;
                case 3:
                    if ( token == '/' )
					{
                        end = i;
						state = -1;
					}
                    break;
            }
            if ( state == -1 )
                break;
        }
        if ( end == -1 )
            end = urn.length();
		if ( start == -1 )
			start = urn.length();
        return urn.substring( start, end );
	}
    String testFirst()
    {
        StringBuilder sb = new StringBuilder();
        boolean failed = false;
        // test first
		String res = first("banana/apple/pear");
		if ( res== null || !res.equals("banana") )
        {
			sb.append("Path:test 1 failed\n");
            failed = true;
        }
		res = first("/banana/apple/pear");
		if ( res== null || !res.equals("banana") )
        {
			sb.append("Path:test 2 failed\n");
            failed = true;
        }
		res = first("/banana");
		if ( res== null || !res.equals("banana") )
        {
			sb.append("Path:test 3 failed\n");
            failed = true;
        }
		res = first("banana");
		if ( res== null || !res.equals("banana") )
        {
			sb.append("Path:test 4 failed\n");
            failed = true;
        }
		res = first("/banana/");
		if ( res== null || !res.equals("banana") )
        {
			sb.append("Path:test 5 failed\n");
            failed = true;
        }
        res = first("banana/");
		if ( res== null || !res.equals("banana") )
        {
			sb.append("Path:test 6 failed\n");
            failed = true;
        }
        return sb.toString();
	}
    private String testChomp()
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            String p = "path/to/glory/f1";
            String res = Path.chomp(p);
            if ( res == null || !res.equals("path/to/glory") )
            {
                sb.append("failed chomp:test1\n");
            }
            p = "path/to/glory/f1/";
            res = Path.chomp(p);
            if ( res == null || !res.equals("path/to/glory") )
            {
                sb.append("failed chomp:test2\n");
            }
            p = "f1";
            res = Path.chomp(p);
            if ( res == null || !res.equals("") )
            {
                sb.append("failed chomp:test3\n");
            }
            p = "path/f1";
            res = Path.chomp(p);
            if ( res == null || !res.equals("path") )
            {
                sb.append("failed chomp:test4\n");
            }
            p = "f1/";
            res = Path.chomp(p);
            if ( res == null || !res.equals("") )
            {
                sb.append("failed chomp:test5\n");
            }
        }
        catch ( Exception e )
        {
            sb.append( e.getMessage() );
            sb.append("\n");
        }
        return sb.toString();
    }
    /**
     * Test the path
     * @return a response if it passed
     */
	public String test()
	{
        StringBuilder sb = new StringBuilder();
        sb.append( testFirst() );
        sb.append( testChomp() );
        return sb.toString();
    }
}