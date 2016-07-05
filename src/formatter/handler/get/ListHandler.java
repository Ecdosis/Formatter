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
import calliope.core.exception.NativeException;
import calliope.AeseFormatter;
import formatter.exception.FormatterException;
import formatter.constants.Params;
import calliope.json.JSONResponse;
import calliope.core.handler.GetHandler;
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
     * Scan the table text to see how many top groups are defined
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
                if ( cols[0].length()>0 && !cols[0].equals("top") )
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
        ArrayList<String> styleNames = new ArrayList<String>();
        for ( int i=0;i<styles.length;i++ )
            styleNames.add(styles[i]);
        docid = request.getParameter( Params.DOCID );
        version1 = request.getParameter(Params.VERSION1);
        try
        {
            String table = getVersionTableForUrn( docid );
            table = unescape(table);
            String listName = request.getParameter( Params.NAME );
            if ( listName == null )
                listName = "versions";
            String listId = request.getParameter( Params.LISTID );
            String markup = GetHandler.markupVersionTable( table, listName,
                listId, version1 );
            String[] corcodes = new String[1];
            corcodes[0] = markup;
            String[] css = fetchStyles( styleNames );
            JSONResponse html = new JSONResponse(JSONResponse.HTML );
            //System.out.println("about to format list");
            int res = new AeseFormatter().format( table, corcodes, css, html );
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
