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

package formatter.handler;
import calliope.core.URLEncoder;
import calliope.core.constants.Database;
import calliope.core.constants.Formats;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connector;
import formatter.exception.FormatterException;
import formatter.path.Path;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Abstract super-class for all handlers: PUT, POST, DELETE, GET
 * @author ddos
 */
abstract public class FormatterHandler 
{
    protected String encoding;
    protected String version1;
    protected String docid;
    public FormatterHandler()
    {
        this.encoding = Charset.defaultCharset().name();
    }
    public abstract void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws FormatterException;
    /**
     * Fetch a single style text
     * @param style the path to the style in the corform database
     * @return the text of the style
     */
     /**
     * Get the document body of the given urn or null
     * @param db the database where it is
     * @param docID the docID of the resource
     * @return the document body or null if not present
     */
    private static String getDocumentBody( String db, String docID ) 
        throws FormatterException
    {
        try
        {
            String jStr = Connector.getConnection().getFromDb(db,docID);
            if ( jStr != null )
            {
                JSONObject jDoc = (JSONObject)JSONValue.parse( jStr );
                if ( jDoc != null )
                {
                    Object body = jDoc.get( JSONKeys.BODY );
                    if ( body != null )
                        return body.toString();
                }
            }
            throw new Exception("document "+db+"/"+docID+" not found");
        }
        catch ( Exception e )
        {
            throw new FormatterException( e );
        }
    }
    /**
     * Get a CSS style resource
     * @param style
     * @return
     * @throws FormatterException 
     */
    protected static String fetchStyle( String style ) throws FormatterException
    {
        // 1. try to get each literal style name
        String actual = getDocumentBody(Database.CORFORM,style);
        while ( actual == null )
        {
            // 2. add "default" to the end
            actual = getDocumentBody( Database.CORFORM,
                URLEncoder.append(style,Formats.DEFAULT) );
            if ( actual == null )
            {
                // 3. pop off last path component and try again
                if ( style.length()>0 )
                    style = Path.chomp(style);
                else
                    throw new FormatterException("no suitable format");
            }
        }
        return actual;
    }
    /**
     * Get the actual styles from the database. Make sure we fetch something
     * @param styles an array of style ids
     * @return an array of database contents for those ids
     * @throws a AeseException only if the database is not set up
     */
    protected String[] fetchStyles( ArrayList names ) throws FormatterException
    {
        String[] actual = new String[names.size()];
        for ( int i=0;i<names.size();i++ )
        {
            actual[i] = fetchStyle( (String)names.get(i) );
        }
        return actual;
    }
    protected String getAuthor()
    {
        if ( docid != null )
        {
            String[] parts = docid.split("/");
            if ( parts.length > 1 )
                return parts[1];
            else
                return "";
        }
        else
            return "";
    }
    protected String getWork()
    {
        if ( docid != null )
        {
            String[] parts = docid.split("/");
            if ( parts.length > 2 )
                return parts[2];
            else
                return "";
        }
        else
            return "";
    }
    protected String getSection()
    {
        if ( docid != null )
        {
            String[] parts = docid.split("/");
            if ( parts.length > 3 )
                return parts[3];
            else
                return "";
        }
        else
            return "";
    }
    protected String getSubsection()
    {
        if ( docid != null )
        {
            String[] parts = docid.split("/");
            if ( parts.length > 4 )
                return parts[4];
            else
                return "";
        }
        else
            return "";
    }
    public String guessEncoding(byte[] bytes) 
    {
        org.mozilla.universalchardet.UniversalDetector detector =
            new org.mozilla.universalchardet.UniversalDetector(null);
        detector.handleData(bytes, 0, bytes.length);
        detector.dataEnd();
        String charset = detector.getDetectedCharset();
        if ( charset == null )
            charset = checkForMac(bytes);
        if ( charset == null )
            charset = "UTF-8";
        detector.reset();
        if ( !charset.equals(encoding) ) 
            encoding = charset;
        return encoding;
    }
    private String checkForMac( byte[] data )
    {
        int macchars = 0;
        for ( int i=0;i<data.length;i++ )
        {
            if ( data[i]>=0xD0 && data[i]<=0xD5 )
            {
                macchars++;
                if ( macchars > 5 )
                    break;
            }
        }
        if ( macchars > 5 )
            return "macintosh";
        else
            return null;
    }
}
