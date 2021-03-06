/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.ericwittmann.corsproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

/**
 * A proxy servlet used to get around cross-origin problems when pulling
 * down content from remote sites.
 *
 * @author eric.wittmann@redhat.com
 */
@SuppressWarnings("nls")
public class ProxyServlet extends HttpServlet {

    private static final long serialVersionUID = -4704803997251798191L;

    private static Set<String> EXCLUDE_HEADERS = new HashSet<>();
    static {
        EXCLUDE_HEADERS.add("ETag");
        EXCLUDE_HEADERS.add("Last-Modified");
        EXCLUDE_HEADERS.add("Date");
        EXCLUDE_HEADERS.add("Cache-control");
    }

    /**
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
            IOException {
        String url = "https://issues.jboss.org" + req.getPathInfo();
        if (req.getQueryString() != null) {
            url += "?" + req.getQueryString();
        }

        System.out.println("Proxying to: " + url);
        boolean isWrite = req.getMethod().equalsIgnoreCase("post") || req.getMethod().equalsIgnoreCase("put");

        URL remoteUrl = new URL(url);
        HttpURLConnection remoteConn = (HttpURLConnection) remoteUrl.openConnection();
        if (isWrite) {
            remoteConn.setDoOutput(true);
        }

        String auth = req.getHeader("Authorization");
        if (auth != null) {
            remoteConn.setRequestProperty("Authorization", auth);
        }

        if (isWrite) {
            InputStream requestIS = null;
            OutputStream remoteOS = null;
            try {
                requestIS = req.getInputStream();
                remoteOS = remoteConn.getOutputStream();
                IOUtils.copy(requestIS, remoteOS);
                remoteOS.flush();
            } catch (Exception e) {
                e.printStackTrace();
                resp.sendError(500, e.getMessage());
                return;
            } finally {
                IOUtils.closeQuietly(requestIS);
                IOUtils.closeQuietly(remoteOS);
            }
        }

        InputStream remoteIS = null;
        OutputStream responseOS = null;
        try {
            Map<String, List<String>> headerFields = remoteConn.getHeaderFields();
            for (String headerName : headerFields.keySet()) {
                if (headerName == null) {
                    continue;
                }
                if (EXCLUDE_HEADERS.contains(headerName)) {
                    continue;
                }
                String headerValue = remoteConn.getHeaderField(headerName);
                resp.setHeader(headerName, headerValue);
            }
            resp.setHeader("Cache-control", "no-cache, no-store, must-revalidate"); //$NON-NLS-2$
            remoteIS = remoteConn.getInputStream();
            responseOS = resp.getOutputStream();
            IOUtils.copy(remoteIS, responseOS);
            resp.flushBuffer();
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendError(500, e.getMessage());
        } finally {
            IOUtils.closeQuietly(responseOS);
            IOUtils.closeQuietly(remoteIS);
        }
    }

}
