package com.adbrite.netty.httpd;
/*
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

/**
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @version $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
 */
final class CookieHeaderNames {
    static final String PATH = "path";

    static final String EXPIRES = "expires";

    static final String MAX_AGE = "max-age";

    static final String DOMAIN = "domain";

    static final String SECURE = "secure";

    static final String HTTPONLY = "HTTPOnly";

    static final String COMMENT = "comment";

    static final String COMMENTURL = "commenturl";

    static final String DISCARD = "Discard";

    static final String PORT = "port";

    static final String VERSION = "version";

    private CookieHeaderNames() {
        // Unused.
    }
}

