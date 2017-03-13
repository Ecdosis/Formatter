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

import calliope.core.exception.CalliopeException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import formatter.exception.FormatterException;
import java.util.ArrayList;
/**
 * Handle production of a JSON formatted list of versions
 * @author desmond
 */
public class JSONListHandler extends TextListHandler
{
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws FormatterException
    {
        try
        {
            if ( urn.length()>0 )
            {
                String table = getVersionTableForUrn( urn, false );
                response.setContentType("appliation/json;charset=UTF-8");
                response.getWriter().println( table );
            }
            else
                throw new CalliopeException("Invalid path "+urn);
        }
        catch ( Exception e )
        {
            throw new FormatterException(e);
        }
    }
}
