/*
 * This file is part of Formatter.
 *
 *  Formatter is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Formatter is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Formatter.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */
package formatter.handler.get;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.exception.AeseException;
import calliope.core.exception.NativeException;
import calliope.AeseFormatter;
import formatter.exception.FormatterException;
import formatter.constants.Params;
import html.HTMLNames;
import calliope.core.constants.JSONKeys;
import calliope.json.JSONResponse;
import calliope.json.corcode.STILDocument;
import calliope.json.corcode.Range;
import org.json.simple.JSONObject;
import java.util.ArrayList;
import java.util.Map;
/**
 * Read the versions of the specified CorTex. Format them into:
 * a) plain text copy of the short and long names
 * b) a layer of STIL markup describing the text to say which are the long 
 * names, and which are the short ones. If the user supplies a CorForm, 
 * then format the resulting combination into HTML and return it. The user 
 * can then animate the HTML in any way using php+javascript or whatever.
 * @author desmond
 */
public class ListHandler extends FormatterGetHandler
{
    /**
     * Make a slash-delimited version id
     * @param shortName the short name of the version
     * @param groupPath an array of group names in order
     * @return a slash-delimited string containing all starting with "/"
     */
    String makeVersionId( String shortName, ArrayList<String> groupPath )
    {
        StringBuilder sb = new StringBuilder();
        sb.append("/" );
        for ( int i=0;i<groupPath.size();i++ )
        {
            sb.append( groupPath.get(i) );
            sb.append("/");
        }
        sb.append( shortName );
        return sb.toString();
    }
    /**
     * can the table text to see how many top groups are defined
     * @param lines an array of version table lines
     * @return the number of top groups defined
     */
    int countTopGroups( String[] lines )
    {
        int nTopGroups = 0;
        for ( int i=1;i<lines.length;i++ )
        {
            String[] cols = lines[i].split("\t");
            if ( cols.length>2 )
            {
                if ( cols[0].length()>0 )
                    nTopGroups++;
            }
        }
        return nTopGroups;
    }
    private String unescape( String str)
    {
        if ( str.contains("%2f") )
            str = str.replace("%2f","/");
        else if (str.contains("%2F") )
            str = str.replace("%2F","/");
        return str;
    }
    /**
     * Annotate a raw text table with standoff properties suitable for a list
     * @param table the raw text table returned by nmerge
     * @param listName the NAME of the list in HTML
     * @param listId the id of the select list
     * @return the markup of the list
     * @throws AeseException 
     */
    String markupVersionTable( String table, String listName, String listId ) 
        throws AeseException
    {
        STILDocument doc = new STILDocument();
        String[] lines = table.split("\n");
        if ( lines.length > 0 )
        {
            ArrayList<Range> groups = new ArrayList<Range>();
            ArrayList<String> groupPath = new ArrayList<String>();
            int offset = lines[0].length()+1;
            Range r = new Range( JSONKeys.DESCRIPTION, 0, lines[0].length() );
            if ( listId != null && listId.length()>0 )
                r.addAnnotation( JSONKeys.ID, listId );
            doc.add( r );
            JSONObject group = null;
            int groupEnd = 0;
            int listStart = offset;
            Range list = new Range( JSONKeys.LIST, offset, 0 );
            list.addAnnotation( JSONKeys.NAME, listName );
            list.addAnnotation( JSONKeys.ID, listName );
            JSONObject listDoc = doc.add( list );
            int numTopGroups = countTopGroups( lines );
            for ( int i=1;i<lines.length;i++ )
            {
                String[] cols = lines[i].split("\t");
                if ( cols.length > 2 )
                {
                    for ( int j=0;j<cols.length-2;j++ )
                    {
                        // find groups
                        if ( cols[j].length()>0 )
                        {
                            // group names will be attributes not content
                            Range removed = new Range( JSONKeys.EMPTY, offset, 
                                cols[j].length()+1 );
                            removed.removed = true;
                            doc.add( removed );
                            // initial length of 0
                            Range g = new Range( (j==0&&numTopGroups==1)?
                                JSONKeys.TOPGROUP:JSONKeys.GROUP, 
                                offset+cols[j].length()+1, 0 );
                            g.addAnnotation( JSONKeys.NAME, cols[j] );
                            // if a group is already defined in 
                            // the current last position, remove it
                            if ( groups.size()-1==j )
                            {
                                Range h = groups.get(j);
                                // set the length of the old group 
                                group.put(JSONKeys.LEN,
                                    new Integer(groupEnd-h.offset));
                                groups.remove( j );
                            }
                            groups.add( g );
                            // set current group
                            group = doc.add( g );
                            // update group path
                            if ( j<groupPath.size() )
                                groupPath.set( j, cols[j] );
                            else
                                groupPath.add( cols[j] );
                        }
                        offset += cols[j].length()+1;
                    }
                    String versionId = makeVersionId(cols[cols.length-2],
                        groupPath);
                    Range shortName = new Range( JSONKeys.VERSION_SHORT, 
                        offset, cols[cols.length-2].length() );
                    if ( version1 != null && versionId.equals(version1) )
                        shortName.addAnnotation(HTMLNames.SELECTED,HTMLNames.SELECTED);
                    // add long name as description
                    shortName.addAnnotation( JSONKeys.DESCRIPTION, 
                        cols[cols.length-1] ); 
                    //System.out.println(cols[cols.length-1]);
                    // add group-path+version as ID
                    shortName.addAnnotation( JSONKeys.VERSION1, versionId );
                    doc.add( shortName );
                    offset += cols[cols.length-2].length()+1; // short name+\t
                    // erase long name
                    Range longName = new Range( JSONKeys.EMPTY, 
                        offset, cols[cols.length-1].length() );
                    longName.removed = true;
                    doc.add( longName );
                    offset += cols[cols.length-1].length()+1; // long name+\n
                    groupEnd = offset-1;
                }
                else
                    throw new AeseException("ill-formed group/version record");
            }
            if ( group != null && groups.size() > 0 )
            {
                Range h = groups.get(groups.size()-1);
                group.put(JSONKeys.LEN,new Integer(groupEnd-h.offset));
            }
           // else
             //   throw new AeseException("no groups defined");
            // update list length
            listDoc.put( JSONKeys.LEN, new Integer(offset-listStart) );
        }
        else
            throw new AeseException( "invalid version table: no CRs");
        return doc.toString();
    }
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws FormatterException
    {
        Map map = request.getParameterMap();
        String[] styles = (String[])map.get( Params.STYLE );
        if ( styles == null )
        {
            styles = new String[1];
            styles[0] = "list/default";
        }
        docid = request.getParameter( Params.DOCID );
        try
        {
            String table = getVersionTableForUrn( docid );
            table = unescape(table);
            String listName = request.getParameter( Params.NAME );
            if ( listName == null )
                listName = "versions";
            String listId = request.getParameter( Params.LIST_ID );
            String markup = markupVersionTable( table, listName, listId );
            String[] corcodes = new String[1];
            corcodes[0] = markup;
            String[] css = fetchStyles( styles );
            JSONResponse html = new JSONResponse(JSONResponse.HTML );
            //System.out.println("about to format list");
            int res = new AeseFormatter().format( table, 
                corcodes, css, html );
            if ( res == 0 )
                throw new NativeException("formatting failed");
            else
            {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().println( html.getBody() );
            }
        }
        catch ( Exception e )
        {
            throw new FormatterException( e );
        }
    }
}
