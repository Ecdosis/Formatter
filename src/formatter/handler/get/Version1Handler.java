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

import calliope.core.constants.Database;
import formatter.exception.FormatterException;
import formatter.constants.Params;
import org.json.simple.JSONObject;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONValue;
/**
 * Get the version1 metadata field for a document
 * @author desmond
 */
public class Version1Handler extends MetadataHandler
{
    /**
     * When all else fails, get version1 from the MVD
     * @param jObj the BSON object from the CORTEX
     * @throws CompareException if MVDFile read failed
     */
    protected void getMetadataFromObject( JSONObject jObj ) 
        throws FormatterException
    {
        try
        {
            String body = (String)jObj.get(JSONKeys.BODY);
            if ( body != null )
            {
                MVD mvd = MVDFile.internalise(body);
                String groupPath = mvd.getGroupPath((short)1);
                String shortName = mvd.getVersionShortName((short)1);
                metadataValue = groupPath+"/"+shortName;
            }
            else    // give up
                metadataValue = "";
        }
        catch ( Exception e )
        {
            throw new FormatterException(e);
        }
    }
    /**
     * Get the version1 metadata item from the CORTEX BSON
     * @param conn the database connection
     * @throws CompareException if the database fetch failed
     */
    void getMetadataFromCortex( Connection conn ) throws FormatterException
    {
        try
        {
            String res = conn.getFromDb(Database.CORTEX,docid);
            JSONObject jObj2 = (JSONObject)JSONValue.parse(res);
            if ( jObj2.containsKey(JSONKeys.VERSION1) )
                metadataValue = (String) jObj2.get(JSONKeys.VERSION1);
            else
                getMetadataFromObject( jObj2 );
        }
        catch ( Exception e )
        {
            throw new FormatterException(e);
        }
    }
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws FormatterException
    {
        try 
        {
            Connection conn = Connector.getConnection();
            docid = request.getParameter(Params.DOCID);
            if ( docid != null && docid.length()> 0 )
            {
                String res = conn.getFromDb(Database.METADATA,docid);
                JSONObject  jObj1 = null;
                if ( res != null )
                {
                    //System.out.println("found metadata for "+docid);
                    jObj1 = (JSONObject)JSONValue.parse(res);
                    if ( jObj1.containsKey(JSONKeys.VERSION1) )
                    {
                        //System.out.println("found version1 in metadata");
                        metadataValue = (String) jObj1.get(JSONKeys.VERSION1);
                    }
                }
                if ( metadataValue == null )
                {
                    //System.out.println("Getting version1 from cortex");
                    getMetadataFromCortex( conn );
                }
            }
            else
            {
                metadataValue = "";
                System.out.println("version1 not found for "+docid+"; setting to empty string");
            }
            System.out.println("version1="+metadataValue);
            response.setContentType("text/plain");
            response.getWriter().write(metadataValue);
        }
        catch ( Exception e )
        {
            throw new FormatterException(e);
        }
    }
}
