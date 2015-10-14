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
import calliope.core.handler.AeseVersion;
import calliope.core.constants.Database;
import calliope.core.constants.Formats;
import calliope.core.constants.JSONKeys;
import calliope.core.handler.EcdosisMVD;
import calliope.exception.AeseException;
import calliope.json.JSONResponse;
import calliope.json.corcode.Range;
import calliope.json.corcode.STILDocument;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import edu.luc.nmerge.mvd.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import html.Comment;
import java.util.BitSet;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Get a project document from the database
 * @author desmond
 */
public class FormatterGetHandler extends FormatterHandler
{
    public void handle(HttpServletRequest request,
            HttpServletResponse response, String urn) throws FormatterException 
    {
        try 
        {
            String prefix = Utils.first( urn );
            urn = Utils.pop(urn);
            if ( prefix != null )
            {
                if ( prefix.equals(Service.LIST))
                {
                    new ListHandler().handle(request,response,Path.pop(urn));
                }
                else if ( prefix.equals(Service.TABLE) )
                {
                    new TableHandler().handle(request,response,Path.pop(urn));
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
     * Fetch and load an MVD
     * @param db the database 
     * @param docID
     * @return the loaded MVD
     * @throws an FormatterException if not found
     */
    protected EcdosisMVD loadMVD( String db, String docID ) 
        throws FormatterException
    {
        try
        {
            String data = Connector.getConnection().getFromDb(db,docID);
            if ( data.length() > 0 )
            {
                JSONObject doc = (JSONObject)JSONValue.parse(data);
                if ( doc != null )
                    return new EcdosisMVD( doc );
            }
            throw new FormatterException( "MVD not found "+docID );
        }
        catch ( Exception e )
        {
            throw new FormatterException( e );
        }
    }
    protected String getVersionTableForUrn( String urn ) 
        throws FormatterException
    {
        try
        {
            JSONObject doc = loadJSONDocument( Database.CORTEX, urn );
            String fmt = (String)doc.get(JSONKeys.FORMAT);
            if ( fmt != null && fmt.startsWith(Formats.MVD) )
            {
                EcdosisMVD mvd = loadMVD( Database.CORTEX, urn );
                return mvd.mvd.getVersionTable();
            }
            else if ( fmt !=null && fmt.equals(Formats.TEXT) )
            {
                // concoct a version list of length 1
                StringBuilder sb = new StringBuilder();
                String version1 = (String)doc.get(JSONKeys.VERSION1);
                if ( version1 == null )
                    throw new FormatterException("Lacks version1 default");
                sb.append("Single version\n");
                String[] parts = version1.split("/");
                for ( int i=0;i<parts.length;i++ )
                {
                    sb.append(parts[i]);
                    sb.append("\t");
                }
                sb.append(parts[parts.length-1]+" version");
                sb.append("\n");
                return sb.toString();
            }
            else
                throw new FormatterException("Unknown of null Format");
        }
        catch ( Exception e )
        {
            throw new FormatterException(e);
        }   
    }
    /**
     * Use this method to retrieve the doc just to see its format
     * @param db the database to fetch from
     * @param docID the doc's ID
     * @return a JSON doc as returned by Mongo
     * @throws FormatterException 
     */
    JSONObject loadJSONDocument( String db, String docID ) 
        throws FormatterException
    {
        try
        {
            String data = Connector.getConnection().getFromDb(db,docID);
            if ( data.length() > 0 )
            {
                JSONObject doc = (JSONObject)JSONValue.parse(data);
                if ( doc != null )
                    return doc;
            }
            throw new FormatterException( "Doc not found "+docID );
        }
        catch ( Exception e )
        {
            throw new FormatterException( e );
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
    protected AeseVersion doGetResourceVersion( String db, String docID, 
        String vPath ) throws FormatterException
    {
        AeseVersion version = new AeseVersion();
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
     * Get an enumerated set of parameters prefix&lt;N&gt;
     * @param prefix each parameter in the set starts with this
     * @param map the request's param map
     * @return an array of parameter values indexed by param's number
     * @param addDefault add a "default" value if none found
     * @throws a AeseParamException if a parameter was wrongly specified
     */
    protected String[] getEnumeratedParams( String prefix, Map map, 
        boolean addDefault ) throws FormatterException
    {
        ArrayList<String> array = new ArrayList<String>();
        Set keys = map.keySet();
        Iterator iter = keys.iterator();
        // get relevant param keys
        while ( iter.hasNext() )
        {
            String key = (String) iter.next();
            if ( key.startsWith(prefix) )
                array.add( (String)map.get(key) );
        }
        String[] params;
        if ( array.isEmpty() )
        {
            if ( addDefault )
                array.add( Formats.DEFAULT ); 
            params = new String[array.size()];
            array.toArray( params );
        }
        else
        {
            params = new String[array.size()];
            // get their values in a properly indexed array
            for ( int i=0;i<array.size();i++ )
            {
                String number = array.get(i).substring(prefix.length());
                int index = 0;
                if ( number.length() == 0 )
                {
                    if ( array.size() != 1 )
                        throw new ParamException(
                            "can't mix unindexed and indexed params of type "
                            +prefix);
                    else
                        params[0] = (String)map.get( array.get(i) );
                }
                else
                {
                    index = Integer.parseInt(number) - 1;
                    if ( index < 0 || index >= array.size() )
                        throw new ParamException("parameter index "
                            +index+" out of range" );
                    else
                        params[index] = (String)map.get( array.get(i) );
                }
            }
            // check for missing params
            for ( int i=0;i<params.length;i++ )
                if ( params[i] == null )
                    throw new ParamException("missing param at index "+(i+1));
        }
        return params;
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
     * Compute the IDs of spans of text in a set of versions
     * @param corCodes the existing corCodes array
     * @param mvd the MVD to use
     * @param version1 versionID of the base version
     * @param spec a comma-separated list of versionIDs 
     * @return an updated corCodes array
     */
    String[] addMergeIds( String[] corCodes, MVD mvd, String version1, 
        String spec )
    {
        STILDocument doc = new STILDocument();
        int base = mvd.getVersionByNameAndGroup(
            Utils.getShortName(version1),
            Utils.getGroupName(version1));
        ArrayList<Pair> pairs = mvd.getPairs();
        BitSet merged = mvd.convertVersions( spec );
        int start = -1;
        int len = 0;
        int pos = 0;
        int id = 1;
        for ( int i=0;i<pairs.size();i++ )
        {
            Pair p = pairs.get( i );
            if ( p.versions.nextSetBit(base)==base )
            {
                if ( containsVersions(p.versions,merged) )
                {
                    if ( start == -1 )
                        start = pos;
                    len += p.length();
                }
                else if ( start != -1 )
                {
                    // add range with annotation to doc
                    try
                    {
                        // see diffs/default corform
                        Range r = new Range("merged", start, len );
                        r.addAnnotation( "mergeid", "v"+id );
                        id++;
                        doc.add( r );
                        start = -1;
                        len = 0;
                    }
                    catch ( Exception e )
                    {
                        // ignore it: we just failed to add that range
                        start = -1;
                        len = 0;
                    }
                }
                // the position within base
                pos += p.length();
            }
        }
        // coda: in case we have a part-fulfilled range
        if ( start != -1 )
        {
            try
            {
                Range r = new Range("merged", start, len );
                r.addAnnotation( "mergeid", "v"+id );
                id++;
                doc.add( r );
            }
            catch ( Exception e )
            {
                // ignore it: we just failed to add that range
            }
        }
        // add new CorCode to the set
        String[] newCCs = new String[corCodes.length+1];
        newCCs[newCCs.length-1] = doc.toString();
        for ( int i=0;i<corCodes.length;i++ )
            newCCs[i] = corCodes[i];
        return newCCs;
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
        String version1 = request.getParameter( Params.VERSION1 );
        if ( version1 == null )
        {
            try
            {
                response.getWriter().println(
                    "<p>version1 parameter required</p>");
            }
            catch ( Exception e )
            {
                throw new FormatterException( e );
            }
        }
        else
        {
            String selectedVersions = request.getParameter( 
                Params.SELECTED_VERSIONS );
            //System.out.println("version1="+version1);
            AeseVersion corTex = doGetResourceVersion( Database.CORTEX, urn, version1 );
            // 1. get corcodes and styles
            Map map = request.getParameterMap();
            String[] corCodes = getEnumeratedParams( Params.CORCODE, map, true );
            String[] styles = getEnumeratedParams( Params.STYLE, map, false );
            HashSet<String> styleSet = new HashSet<String>();
            for ( int i=0;i<styles.length;i++ )
                styleSet.add( styles[i] );
            try
            {
                for ( int i=0;i<corCodes.length;i++ )
                {
                    String ccResource = Utils.canonisePath(urn,corCodes[i]);
                    AeseVersion hv = doGetResourceVersion( Database.CORCODE, 
                        ccResource, version1 );
                    Comment comment = new Comment();
                    comment.addText( "version-length: "+hv.getVersionLength() );
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().println( comment.toString() );
                    styleSet.add( hv.getStyle() );
                    corCodes[i] = new String(hv.getVersion());
                }
            }
            catch ( Exception e )
            {
                // this won't ever happen because UTF-8 is always supported
                throw new FormatterException( e );
            }
            // 2. add mergeids if needed
            if ( selectedVersions != null && selectedVersions.length()>0 )
            {
                corCodes = addMergeIds( corCodes, corTex.getMVD(), version1, 
                    selectedVersions );
                styleSet.add( "diffs/default" );
            }
            // 3. recompute styles array (docids)
            styles = new String[styleSet.size()];
            styleSet.toArray( styles );
            // 4. convert style names to actual corforms
            styles = fetchStyles( styles );
            // 5. call the native library to format it
            JSONResponse html = new JSONResponse(JSONResponse.HTML);
            String text = corTex.getVersionString();
    //        // debug
//            try{
//                String textString = new String(text,"UTF-8");
//                System.out.println(textString);
//            }catch(Exception e){}
            // end
//            if ( text.length==30712 )
//            {
//                try
//                {
//                    String textStr = new String( text, "UTF-8");
//                    System.out.println(textStr );
//                }
//                catch ( Exception e )
//                {
//                }
//            }
            int res = new AeseFormatter().format( 
                text, corCodes, styles, html );
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
}
