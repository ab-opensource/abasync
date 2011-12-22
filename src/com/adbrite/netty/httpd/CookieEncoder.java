package com.adbrite.netty.httpd;

/*
 * copy of Cookie encoder with date format fix
 * TODO: remove when netty fix it's format
 * 
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes {@link Cookie}s into an HTTP header value.  This encoder can encode
 * the HTTP cookie version 0, 1, and 2.
 * <p>
 * This encoder is stateful.  It maintains an internal data structure that
 * holds the {@link Cookie}s added by the {@link #addCookie(String, String)}
 * method.  Once {@link #encode()} is called, all added {@link Cookie}s are
 * encoded into an HTTP header value and all {@link Cookie}s in the internal
 * data structure are removed so that the encoder can start over.
 * <pre>
 * // Client-side example
 * {@link HttpRequest} req = ...;
 * {@link CookieEncoder} encoder = new {@link CookieEncoder}(false);
 * encoder.addCookie("JSESSIONID", "1234");
 * res.setHeader("Cookie", encoder.encode());
 *
 * // Server-side example
 * {@link HttpResponse} res = ...;
 * {@link CookieEncoder} encoder = new {@link CookieEncoder}(true);
 * encoder.addCookie("JSESSIONID", "1234");
 * res.setHeader("Set-Cookie", encoder.encode());
 * </pre>
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @version $Rev: 2122 $, $Date: 2010-02-02 11:00:04 +0900 (Tue, 02 Feb 2010) $
 * @see CookieDecoder
 *
 * @apiviz.stereotype utility
 * @apiviz.has        org.jboss.netty.handler.codec.http.Cookie oneway - - encodes
 */
/**
 * AP: THIS ENCODER IS SIMPLY WRONG, AT LEAST FOR THE SERVER SIDE
 * DO NOT USE IT UNTIL VERIFIED
 */
public class CookieEncoder {
	private static final Logger LOG = LoggerFactory
	.getLogger(CookieEncoder.class);
	
	static final class CookieDateFormat extends SimpleDateFormat {

	    private static final long serialVersionUID = 1789486337887402640L;

	    CookieDateFormat() {
	        super("E, dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH);
	        setTimeZone(TimeZone.getTimeZone("GMT"));
	    }
	}

    private final Set<Cookie> cookies = new TreeSet<Cookie>();
    private final boolean server;

    /**
     * Creates a new encoder.
     *
     * @param server {@code true} if and only if this encoder is supposed to
     *               encode server-side cookies.  {@code false} if and only if
     *               this encoder is supposed to encode client-side cookies.
     */
    public CookieEncoder(boolean server) {
        this.server = server;
    }

    /**
     * Adds a new {@link Cookie} created with the specified name and value to
     * this encoder.
     */
    public void addCookie(String name, String value) {
    	if(name==null || name.length()==0) {
    		throw new IllegalArgumentException("bad cookie name");
    	}
        cookies.add(new DefaultCookie(name, value));
    }

    /**
     * Adds the specified {@link Cookie} to this encoder.
     */
    public void addCookie(Cookie cookie) {
    	String name = cookie.getName();
    	if(name==null || name.length()==0) {
    		throw new IllegalArgumentException("bad cookie name");
    	}
        cookies.add(cookie);
        if(LOG.isDebugEnabled()) {
        	LOG.debug("adding cookie "+cookie.getName());
        }
    }

    /**
     * Encodes the {@link Cookie}s which were added by {@link #addCookie(Cookie)}
     * so far into an HTTP header value.  If no {@link Cookie}s were added,
     * an empty string is returned.
     */
    public String encode() {
        String answer;
        if (server) {
            answer = encodeServerSide();
        } else {
            answer = encodeClientSide();
        }
        cookies.clear();
        return answer;
    }

    private String encodeServerSide() {
        StringBuilder sb = new StringBuilder();

        for (Cookie cookie: cookies) {
            if(LOG.isDebugEnabled()) {
            	LOG.debug("encoding cookie "+cookie.getName());
            }

            add(sb, cookie.getName(), cookie.getValue());

            if (cookie.getMaxAge() >= 0) {
                sb.append("; ");
                if (cookie.getVersion() == 0) {
                    addUnquoted(sb, CookieHeaderNames.EXPIRES,
                            new CookieDateFormat().format(
                                    new Date(System.currentTimeMillis() +
                                             cookie.getMaxAge() * 1000L)));
                } else {
                    add(sb, CookieHeaderNames.MAX_AGE, cookie.getMaxAge());
                }
            }

            if (cookie.getDomain() != null) {
                sb.append("; ");
                if (cookie.getVersion() > 0) {
                    add(sb, CookieHeaderNames.DOMAIN, cookie.getDomain());
                } else {
                    addUnquoted(sb, CookieHeaderNames.DOMAIN, cookie.getDomain());
                }
            }
            if (cookie.getPath() != null) {
                sb.append("; ");
                if (cookie.getVersion() > 0) {
                    add(sb, CookieHeaderNames.PATH, cookie.getPath());
                } else {
                    addUnquoted(sb, CookieHeaderNames.PATH, cookie.getPath());
                }
            }

            if (cookie.isSecure()) {
                sb.append("; ");
                sb.append(CookieHeaderNames.SECURE);
            }
            if (cookie.isHttpOnly()) {
                sb.append("; ");
                sb.append(CookieHeaderNames.HTTPONLY);
            }
            if (cookie.getVersion() >= 1) {
                if (cookie.getComment() != null) {
                    sb.append("; ");
                    add(sb, CookieHeaderNames.COMMENT, cookie.getComment());
                }

                sb.append("; ");
                add(sb, CookieHeaderNames.VERSION, 1);

                if (cookie.getCommentUrl() != null) {
                    sb.append("; ");
                    addQuoted(sb, CookieHeaderNames.COMMENTURL, cookie.getCommentUrl());
                }

                if(!cookie.getPorts().isEmpty()) {
                    sb.append("; ");
                    sb.append(CookieHeaderNames.PORT);
                    sb.append('=');
                    sb.append('"');
                    for (int port: cookie.getPorts()) {
                        sb.append(port);
                        sb.append('.');
                    }
                    sb.setCharAt(sb.length() - 1, '"');
                }
                if (cookie.isDiscard()) {
                    sb.append("; ");
                    sb.append(CookieHeaderNames.DISCARD);
                }
            }
        }

        if(LOG.isDebugEnabled()) {
        	LOG.debug("encoding cookie result "+sb);
        }

        return sb.toString();
    }

    private String encodeClientSide() {
        StringBuilder sb = new StringBuilder();

        for (Cookie cookie: cookies) {
            if (cookie.getVersion() >= 1) {
                add(sb, '$' + CookieHeaderNames.VERSION, 1);
            }

            add(sb, cookie.getName(), cookie.getValue());

            if (cookie.getPath() != null) {
                add(sb, '$' + CookieHeaderNames.PATH, cookie.getPath());
            }

            if (cookie.getDomain() != null) {
                add(sb, '$' + CookieHeaderNames.DOMAIN, cookie.getDomain());
            }

            if (cookie.getVersion() >= 1) {
                if(!cookie.getPorts().isEmpty()) {
                    sb.append('$');
                    sb.append(CookieHeaderNames.PORT);
                    sb.append('=');
                    sb.append('"');
                    for (int port: cookie.getPorts()) {
                        sb.append(port);
                        sb.append('.');
                    }
                    sb.setCharAt(sb.length() - 1, '"');
                    sb.append("; ");
                }
            }
        }

        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static void add(StringBuilder sb, String name, String val) {
        if (val == null) {
            addQuoted(sb, name, "");
            return;
        }

        for (int i = 0; i < val.length(); i ++) {
            char c = val.charAt(i);
            switch (c) {
            case '\t': case ' ': case '"': case '(':  case ')': case ',':
            case '/':  case ':': case ';': case '<':  case '=': case '>':
            case '?':  case '@': case '[': case '\\': case ']':
            case '{':  case '}':
                return;
            }
        }

        addUnquoted(sb, name, val);
    }

    private static void addUnquoted(StringBuilder sb, String name, String val) {
        sb.append(name);
        sb.append('=');
        sb.append(val);
    }

    private static void addQuoted(StringBuilder sb, String name, String val) {
        if (val == null) {
            val = "";
        }

        sb.append(name);
        sb.append('=');
        sb.append('"');
        sb.append(val.replace("\\", "\\\\").replace("\"", "\\\""));
        sb.append('"');
    }

    private static void add(StringBuilder sb, String name, int val) {
        sb.append(name);
        sb.append('=');
        sb.append(val);
    }
}
