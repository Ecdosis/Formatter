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
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws FormatterException
    {
        docid = request.getParameter( Params.DOCID );
        version1 = request.getParameter(Params.VERSION1);
        try
        {
            String table = getVersionTableForUrn( docid, false );
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println( table );
        }
        catch ( Exception e )
        {
            throw new FormatterException( e );
        }
    }
}
