/*
 * Copyright 2015 JBoss Inc
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

import io.apiman.common.servlet.ApimanCorsFilter;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 *
 *
 * @author eric.wittmann@redhat.com
 */
public class Main {

    @SuppressWarnings("nls")
    public static final void main(String [] args) throws Exception {
        Server server;

        long startTime = System.currentTimeMillis();
        System.out.println("**** Starting Server (" + Main.class.getSimpleName() + ")");

        ContextHandlerCollection handlers = new ContextHandlerCollection();
        ServletContextHandler proxy = new ServletContextHandler(ServletContextHandler.SESSIONS);
        proxy.setContextPath("/");
//        proxy.addFilter(LocaleFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        proxy.addFilter(ApimanCorsFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        ServletHolder servlet = new ServletHolder(new ProxyServlet());
        proxy.addServlet(servlet, "/*");
        handlers.addHandler(proxy);

        // Create the server.
        int serverPort = 12345;
        server = new Server(serverPort);
        server.setHandler(handlers);
        server.start();
        long endTime = System.currentTimeMillis();
        System.out.println("******* Started in " + (endTime - startTime) + "ms");

    }

}
