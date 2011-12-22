package com.adbrite.netty.httpd;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adbrite.util.QuotedStringTokenizer;

/**
 * Parser for client->server cookies. Please note that server->client cookie has different format.
 * @author apesternikov
 *
 */
public class CookieParser {

	private static final Logger LOG = LoggerFactory
	.getLogger(CookieParser.class);

	static Cookie[] parse(List<String> headers) {
		int version = 0;
		ArrayList<Cookie> cookies = new ArrayList<Cookie>(headers.size());
		for(String hdr: headers)
		{
            try
            {
                // Save a copy of the unparsed header as cache.
                
                // Parse the header
                String name = null;
                String value = null;

                Cookie cookie = null;

                boolean invalue=false;
                boolean quoted=false;
                boolean escaped=false;
                int tokenstart=-1;
                int tokenend=-1;
                for (int i = 0, length = hdr.length(), last=length-1; i < length; i++)
                {
                    char c = hdr.charAt(i);
                    
                    // Handle quoted values for name or value
                    if (quoted)
                    {
                        if (escaped)
                        {
                            escaped=false;
                            continue;
                        }
                        
                        switch (c)
                        {
                            case '"':
                                tokenend=i;
                                quoted=false;

                                // handle quote as last character specially
                                if (i==last)
                                {
                                    if (invalue)
                                        value = hdr.substring(tokenstart, tokenend+1);
                                    else
                                    {
                                        name = hdr.substring(tokenstart, tokenend+1);
                                        value = "";
                                    }
                                }
                                break;
                                
                            case '\\':
                                escaped=true;
                                continue;
                            default:
                                continue;
                        }
                    }
                    else
                    {
                        // Handle name and value state machines
                        if (invalue)
                        {
                            // parse the value
                            switch (c)
                            {
                                case ' ':
                                case '\t':
                                    continue;
                                    
                                case '"':
                                    if (tokenstart<0)
                                    {
                                        quoted=true;
                                        tokenstart=i;
                                    }
                                    tokenend=i;
                                    if (i==last)
                                    {
                                        value = hdr.substring(tokenstart, tokenend+1);
                                        break;
                                    }
                                    continue;

                                case ';':
                                case ',':
                                    if (tokenstart>=0)
                                        value = hdr.substring(tokenstart, tokenend+1);
                                    else
                                        value="";
                                    tokenstart = -1;
                                    invalue=false;
                                    break;
                                    
                                default:
                                    if (tokenstart<0)
                                        tokenstart=i;
                                    tokenend=i;
                                    if (i==last)
                                    {
                                        value = hdr.substring(tokenstart, tokenend+1);
                                        break;
                                    }
                                    continue;
                            }
                        }
                        else
                        {
                            // parse the name
                            switch (c)
                            {
                                case ' ':
                                case '\t':
                                    continue;
                                    
                                case '"':
                                    if (tokenstart<0)
                                    {
                                        quoted=true;
                                        tokenstart=i;
                                    }
                                    tokenend=i;
                                    if (i==last)
                                    {
                                        name = hdr.substring(tokenstart, tokenend+1);
                                        value = "";
                                        break;
                                    }
                                    continue;

                                case ';':
                                case ',':
                                    if (tokenstart>=0)
                                    {
                                        name = hdr.substring(tokenstart, tokenend+1);
                                        value = "";
                                    }
                                    tokenstart = -1;
                                    break;

                                case '=':
                                    if (tokenstart>=0)
                                        name = hdr.substring(tokenstart, tokenend+1);
                                    tokenstart = -1;
                                    invalue=true;
                                    continue;
                                    
                                default:
                                    if (tokenstart<0)
                                        tokenstart=i;
                                    tokenend=i;
                                    if (i==last)
                                    {
                                        name = hdr.substring(tokenstart, tokenend+1);
                                        value = "";
                                        break;
                                    }
                                    continue;
                            }
                        }
                    }

                    // If after processing the current character we have a value and a name, then it is a cookie
                    if (value!=null && name!=null)
                    {
                        // TODO handle unquoting during parsing!  But quoting is uncommon
                        name=QuotedStringTokenizer.unquote(name);
                        value=QuotedStringTokenizer.unquote(value);
                        
                        try
                        {
                            if (name.startsWith("$"))
                            {
                                String lowercaseName = name.toLowerCase();
                                if ("$path".equals(lowercaseName))
                                {
                                    if (cookie!=null)
                                        cookie.setPath(value);
                                }
                                else if ("$domain".equals(lowercaseName))
                                {
                                    if (cookie!=null)
                                        cookie.setDomain(value);
                                }
                                else if ("$port".equals(lowercaseName))
                                {
                                    if (cookie!=null)
                                        cookie.setComment("port="+value);
                                }
                                else if ("$version".equals(lowercaseName))
                                {
                                    version = Integer.parseInt(value);
                                }
                            }
                            else
                            {
                                cookie = new Cookie(name, value);
                                if (version > 0)
                                    cookie.setVersion(version);
                                cookies.add(cookie);
                            }
                        }
                        catch (Exception e)
                        {
                        	if(LOG.isDebugEnabled()) {
                                LOG.debug("Exception in cookie parser", e);
                        	}
                        	else
                        		LOG.warn(e.toString());
                        }

                        name = null;
                        value = null;
                    }
                }

            }
            catch (Exception e)
            {
                LOG.warn("Cookie parser", e);
            }
		}
		return cookies.toArray(new Cookie[cookies.size()]);
	}
}
