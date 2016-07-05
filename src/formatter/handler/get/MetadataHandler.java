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
 *  (c) copyright Desmond Schmidt 2016
 */
package formatter.handler.get;

import formatter.exception.FormatterException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import formatter.constants.Params;
import org.json.simple.*;
/**
 * Get the version metadata
 * @author desmond
 */
public class MetadataHandler extends FormatterGetHandler
{
    String metadataValue;
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws FormatterException
    {
        // we need the docid only
        // return the metadata for the document
        try
        {
            this.docid = request.getParameter(Params.DOCID);
            Connection conn = Connector.getConnection();
            String bson = conn.getFromDb(Database.METADATA, docid );
            if ( bson != null )
            {
                JSONObject jObj = (JSONObject)JSONValue.parse(bson);
                JSONArray sources;
                if ( jObj.containsKey(JSONKeys.SOURCES) )
                {
                    sources = (JSONArray)jObj.get(JSONKeys.SOURCES);
                }
                else
                    sources = new JSONArray();
                response.setContentType("application/json");
                response.getWriter().write(sources.toJSONString());
            }
        }
        catch ( Exception e )
        {
            throw new FormatterException(e);
        }
    }
}
