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
 *  (c) copyright Desmond Schmidt 2017
 */
package formatter.handler.get;

import formatter.constants.Params;
import formatter.exception.FormatterException;
import java.util.ArrayList;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author desmond
 */
public class ShortListHandler extends FormatterGetHandler
{
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
        try
        {
            String table = getVersionTableForUrn( docid, true );
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println( table );
        }
        catch (Exception e )
        {
            throw new FormatterException(e);
        }
    }
}
