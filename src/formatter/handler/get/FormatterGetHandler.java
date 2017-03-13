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

import calliope.AeseFormatter;
import formatter.exception.*;
import formatter.handler.FormatterHandler;
import formatter.path.Path;
import formatter.constants.Params;
import formatter.constants.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.Utils;
import calliope.core.database.Connector;
import calliope.core.handler.EcdosisVersion;
import calliope.core.constants.Database;
import calliope.core.constants.Formats;
import calliope.core.constants.JSONKeys;
import calliope.core.handler.EcdosisMVD;
import calliope.core.exception.JSONException;
import calliope.json.JSONResponse;
import calliope.core.json.corcode.Range;
import calliope.core.json.corcode.STILDocument;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Set;
import java.util.Iterator;
import html.Comment;
import java.util.BitSet;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import java.io.File;
import java.io.FileWriter;

/**
 * Get a project document from the database
 * @author desmond
 */
public class FormatterGetHandler extends FormatterHandler
{
    /** optional section ranges */
    String selections;
    public void handle(HttpServletRequest request,
            HttpServletResponse response, String urn) throws FormatterException 
    {
        try 
        {
            String prefix = Utils.first( urn );
            urn = Utils.pop(urn);
            if ( prefix != null )
            {
                if ( prefix.equals(Service.SHORTLIST) )
                {
                    new ShortListHandler().handle(request,response, Path.pop(urn));
                }
                else if ( prefix.equals(Service.LIST))
                {
                    new ListHandler().handle(request,response,Path.pop(urn));
                }
                else if ( prefix.equals(Service.TABLE) )
                {
                    new TableHandler().handle(request,response,Path.pop(urn));
                }
                else if ( prefix.equals(Service.VERSION1) )
                    new Version1Handler().handle(request,response,Utils.pop(urn));
                else if ( prefix.equals(Service.METADATA) )
                {
                    new MetadataHandler().handle(request,response,Path.pop(urn));
                }
                else
                    handleGetVersion( request, response, urn );
            }
            else
                throw new FormatterException("Invalid urn (prefix was null) "+urn );
        } 
        catch (Exception e) 
        {
            try
            {
                response.setCharacterEncoding("UTF-8");
                response.getWriter().println(e.getMessage());
            }
            catch ( Exception ex )
            {
                throw new FormatterException(ex);
            }
        }
    }
    /**
     * Try to retrieve the CorTex/CorCode version specified by the path
     * @param db the database to fetch from
     * @param docID the document ID
     * @param vPath the groups/version path to get
     * @return the CorTex/CorCode version contents or null if not found
     * @throws AeseException if the resource couldn't be found for some reason
     */
    protected EcdosisVersion doGetResourceVersion( String db, String docID, 
        String vPath ) throws FormatterException
    {
        EcdosisVersion version = new EcdosisVersion();
        JSONObject doc = null;
        char[] data = null;
        String res = null;
        //System.out.println("fetching version "+vPath );
        try
        {
            res = Connector.getConnection().getFromDb(db,docID);
        }
        catch ( Exception e )
        {
            throw new FormatterException( e );
        }
        if ( res != null )
            doc = (JSONObject)JSONValue.parse( res );
        if ( doc != null )
        {
            String format = (String)doc.get(JSONKeys.FORMAT);
            if ( format == null )
                throw new FormatterException("doc missing format");
            version.setFormat( format );
            if ( version.getFormat().equals(Formats.MVD) )
            {
                MVD mvd = MVDFile.internalise( (String)doc.get(
                    JSONKeys.BODY) );
                if ( vPath == null )
                    vPath = (String)doc.get( JSONKeys.VERSION1 );
                version.setStyle((String)doc.get(JSONKeys.STYLE));
                String sName = Utils.getShortName(vPath);
                String gName = Utils.getGroupName(vPath);
                int vId = mvd.getVersionByNameAndGroup(sName, gName );
                //System.out.println("vId="+vId+" sName="+sName);
                version.setMVD(mvd);
                if ( vId != 0 )
                {
                    data = mvd.getVersion( vId );
                    String desc = mvd.getDescription();
                    //System.out.println("description="+desc);
                    //int nversions = mvd.numVersions();
                    //System.out.println("nversions="+nversions);
                    //System.out.println("length of version "+vId+"="+data.length);
                    if ( data != null )
                        version.setVersion( data );
                    else
                        throw new FormatterException("Version "+vPath+" not found");
                }
                else
                    throw new FormatterException("Version "+vPath+" not found");
            }
            else
            {
                String body = (String)doc.get( JSONKeys.BODY );
                version.setStyle((String)doc.get(JSONKeys.STYLE));
                if ( body == null )
                    throw new FormatterException("empty body");
                try
                {
                    data = body.toCharArray();
                }
                catch ( Exception e )
                {
                    throw new FormatterException( e );
                }
                version.setVersion( data );
            }
        }
        return version;
    }
    /**
     * Does one set of versions entirely contain another
     * @param container the putative container
     * @param contained the containee
     * @return true if all the bits of contained are in container
     */
    boolean containsVersions( BitSet container, BitSet contained )
    {
        for (int i=contained.nextSetBit(0);i>=0;i=contained.nextSetBit(i+1)) 
        {
            if ( container.nextSetBit(i)!= i )
                return false;
        }
        return true;
    }
    /**
     * Return the length of the word starting at offset
     * @param offset the index of the first letter
     * @param text the whole text of this version
     * @return the length of the word there
     */
    int wordLen( int offset, String text )
    {
        int state = 0;
        int lastLetterPos=offset;
        for ( int i=offset;i<text.length();i++ )
        {
            char token = text.charAt(i);
            switch ( state )
            {
                case 0: // seen one letter
                    if ( Character.isWhitespace(token) )
                        return i-offset;
                    else if ( token== '\'' || token == '’')
                        state = 1;
                    else if ( token=='-' )
                    {
                        lastLetterPos = i;
                        state = 4;
                    }
                    else if ( token=='s' )
                        state = 2;
                    else if ( !Character.isLetter(token) )
                        return i-offset;
                    break;
                case 1: // seen apostrophe or hyphen
                    if ( Character.isWhitespace(token) )
                        return (i-1)-offset;
                    if ( Character.isLetter(token) )
                        state = 0;
                    break;
                case 2: // seen 's'
                    if ( token=='\''||token == '’' )
                        state = 3;
                    else if ( Character.isLetter(token)||token=='-' )
                        state = 0;
                    else 
                        return i-offset;
                    break;
                case 3:
                    if ( Character.isWhitespace(token) )
                        return i-offset;
                    else if ( Character.isLetter(token) )
                        state = 0;
                    else
                        return (i-1)-offset;
                    break;
                case 4: // seen hyphen
                    if ( Character.isWhitespace(token) )
                        state = 5;
                    else if ( Character.isLetter(token) )
                        state = 0;
                    else
                        return lastLetterPos-offset;
                    break;
                case 5: // seen hyphen and white space
                    if ( Character.isLetter(token) )
                        state = 0;
                    else if ( !Character.isWhitespace(token) )
                        return lastLetterPos-i;
                    /// else stay in this state
                    break;
            }
        }
        return text.length()-offset;
    }
    /**
     * Convert the selections parameter to a real corcode
     * @param text the whole text of this version
     * @return a corcode as a string containing the selection ranges
     */
    String selectionsToCorcode( String text ) throws JSONException
    {
        String[] parts = selections.split(",");
        STILDocument cc = new STILDocument();
        int[] values = new int[parts.length];
        for ( int i=0;i<parts.length;i++ )
            values[i] = Integer.parseInt(parts[i]);
        Arrays.sort( values );
        for ( int i=0;i<values.length;i++ )
        {
            int len = wordLen(values[i],text);
            System.out.println("len="+len);
            Range r = new Range( "selected", values[i], len );
            cc.add( r );
        }
        cc.put(JSONKeys.STYLE,"TEI/default");
        return cc.toJSONString();
    }
    /**
     * Turn a list of corcode names into an actual list of corcodes
     * @param names the docids of the corcodes
     * @param styleNames an array of style names to augment
     * @param text the text of the version
     * @return an array of actual corcodes
     * @throws FormatterException 
     */
    String[] fetchCorcodes( ArrayList names, ArrayList styleNames, String text ) 
        throws FormatterException
    {
        try
        {
            ArrayList<String> list = new ArrayList<String>();
            for ( int i=0;i<names.size();i++ )
            {
                EcdosisVersion hv = doGetResourceVersion( Database.CORCODE, 
                    (String)names.get(i), version1 );
                if ( !styleNames.contains(hv.getStyle()) )
                    styleNames.add( hv.getStyle() );
                list.add(new String(hv.getVersion()));
            }
            // add selections
            if ( selections != null && selections.length()>0 )
                list.add( selectionsToCorcode(text) );
            String[] corcodes = new String[list.size()];
            list.toArray(corcodes);
            return corcodes;
        }
        catch ( JSONException je )
        {
            throw new FormatterException(je);
        }
    }
    private void dumpText( String fileName, String text )
    {
        try
        {
            File textFile = new File(fileName);
            if ( textFile.exists() )
                textFile.delete();
            textFile.createNewFile();
            FileWriter fw = new FileWriter(textFile);
            fw.write(text);
            fw.close();
        }
        catch ( Exception e )
        {
            // ignore
        }
    }
    /**
     * Format the requested URN version as HTML
     * @param request the original http request
     * @param urn the original request urn
     * @return the converted HTML
     * @throws AeseException 
     */
    protected void handleGetVersion( HttpServletRequest request, 
        HttpServletResponse response, String urn )
        throws FormatterException
    {
        version1 = request.getParameter( Params.VERSION1 );
        if ( version1 == null )
            throw new FormatterException( "version1 parameter required" );
        docid = request.getParameter(Params.DOCID);
        selections = request.getParameter(Params.SELECTIONS);
        EcdosisVersion corTex = doGetResourceVersion( 
            Database.CORTEX, docid, version1 );
        ArrayList<String> ccNames = new ArrayList<String>();
        Map map = request.getParameterMap();
        if ( map.containsKey(Params.CORCODE) )
        {
            String[] values = (String[])map.get(Params.CORCODE);
            for ( int i=0;i<values.length;i++ )
                ccNames.add( values[i]);
        }
        else
        {
            ccNames.add( docid+"/default");
            ccNames.add( docid+"/pages");
        }
        ArrayList<String> styleNames = new ArrayList<String>();
        if ( map.containsKey(Params.STYLE) )
        {
            String[] values = (String[])map.get(Params.STYLE);
            for ( int i=0;i<values.length;i++ )
                styleNames.add( values[i]);
        }
        else
            styleNames.add( corTex.getStyle() );
        String text = corTex.getVersionString();
        String[] corcodes = fetchCorcodes( ccNames, styleNames, text );
        //System.out.println("Fetching corcodes");
        String[] styles = fetchStyles( styleNames );
        // call the native library to format it
        JSONResponse html = new JSONResponse(JSONResponse.HTML);
        // String text, String[] markup, String[] css, JSONResponse output 
        boolean ok = true;
        if ( corcodes == null || corcodes.length==0 )
        {
            System.out.println("corcodes is null or empty");
            ok = false;
        }
        if ( text == null||text.length()==0 )
        {
            System.out.println("text is null or empty");
            ok = false;
        }
        if ( styles==null || styles.length==0 )
        {
            System.out.println("styles is null or empty");
            ok = false;
        }
        int res = 0;
        if ( ok )
        {
            res = new AeseFormatter().format( text, corcodes, styles, html );
        }
        if ( res == 0 )
            throw new NativeException("formatting failed");
        else
        {
            response.setContentType("text/html;charset=UTF-8");
            try
            {
                Comment comment = new Comment();
                comment.addText( "styles: ");
                for ( int i=0;i<styles.length;i++ )
                    comment.addText( styles[i] );
                response.getWriter().println( comment.toString() );
                response.getWriter().println(html.getBody());   
            }
            catch ( Exception e )
            {
                throw new FormatterException( e );
            }
        }
    }
}
