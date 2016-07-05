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
package formatter.handler.post;

import formatter.constants.Params;
import formatter.handler.FormatterHandler;
import formatter.exception.FormatterException;
import calliope.core.Base64;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Handle setting of post communications
 * @author desmond
 */
public class FormatterPostHandler extends FormatterHandler
{
    String docid;
    boolean isEditor;
    JSONObject userdata;
    
    public FormatterPostHandler()
    {
        this.userdata = new JSONObject();
        // use empty guest account
        this.userdata.put(JSONKeys.NAME, "guest");
        this.userdata.put(JSONKeys.ROLES,new JSONArray());
    }
    /**
     * Process a field we recognise
     * @param fieldName the field's name
     * @param contents its contents
     */
    void processField( String fieldName, String contents )
    {
        //System.out.println("Received field "+fieldName);
        if ( fieldName.equals(Params.DOCID) )
            docid = contents;
        if ( fieldName.equals(Params.VERSION1) )
            version1 = contents;
        else if ( fieldName.equals(Params.USERDATA) )
        {
            String key = "I tell a settlers tale of the old times";
            int klen = key.length();
            char[] data = Base64.decode( contents );
            StringBuilder sb = new StringBuilder();
            for ( int i=0;i<data.length;i++ )
                sb.append((char)(data[i]^key.charAt(i%klen)));
            String json = sb.toString();
//            System.out.println( "USERDATA: decoded json data="+json);
            userdata = (JSONObject)JSONValue.parse(json);
//            System.out.println("json="+json);
//            System.out.println( "user was "+userdata.get(JSONKeys.NAME));
            JSONArray roles = (JSONArray)userdata.get(JSONKeys.ROLES);
//            if ( roles.size()>0 )
//                System.out.println("role was "+roles.get(0));
            if ( roles != null )
                for ( int i=0;i<roles.size();i++ )
                    if ( ((String)roles.get(i)).equals("editor") )
                        isEditor = true;
        }
    }
    /**
     * Parse the import params from the request
     * @param request the http request
     */
    void parseImportParams( HttpServletRequest request ) 
    {
        Map tbl = request.getParameterMap();
        Set<String> keys = tbl.keySet();
        Iterator<String> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            String key = iter.next();
            String[] values = (String[])tbl.get(key);
            for ( int i=0;i<values.length;i++ )
                processField( key, values[i]);
        }
    }
    /**
     * Handle a POST request
     * @param request the raw request
     * @param response the response we will write to
     * @param urn the rest of the URL after stripping off the context
     * @throws ProjectException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws FormatterException
    {
        try
        {
            //System.out.println("About to parse params");
            parseImportParams( request );
            Connection conn = Connector.getConnection();
            String jStr = conn.getFromDb(Database.METADATA,docid);
            JSONObject jDoc = (JSONObject)JSONValue.parse(jStr);
            if ( version1 != null && isEditor )
            {
                //System.out.println("adding "+version1+"to metadata");
                jDoc.put(JSONKeys.VERSION1, version1 );
                jStr = jDoc.toJSONString();
                jStr = jStr.replaceAll("\\\\/","/");
                String resp = conn.putToDb( Database.METADATA, docid, jStr );
                //System.out.println("Server response:"+resp);
            }
            else
                System.out.println("version1="+version1
                    +" isEditor:"+Boolean.toString(isEditor));
        }
        catch ( Exception e )
        {
            try {
                response.getWriter().println( "Status: 500; Exception "+e.getMessage());
            } 
            catch (Exception ex )
            {}
            System.out.println(e.getMessage() );
            throw new FormatterException(e);
        }
    }
}

