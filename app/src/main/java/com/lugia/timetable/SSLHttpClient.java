/*
 * Copyright (c) 2014 Lugia Programming Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.lugia.timetable;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

// IMPORTATNT CORE FILE
// DO NOT MODIFY THIS IF YOU NOT SURE WHAT YOU'RE DOING

// This file is now work very correctly, modify of this file is not likely to happen, unless
// there is something unexpected...

/**
 * Core class file for authenticate and handshake with HTTPS Server
 */
public final class SSLHttpClient extends DefaultHttpClient
{
    private final HttpContext mHttpContext = new BasicHttpContext();
    private final CookieStore mCookieStore = new BasicCookieStore();
    
    // private default constructor, so user cannot instantiate using default constructor
    private SSLHttpClient() { }
    
    private SSLHttpClient(ClientConnectionManager conman, HttpParams params)
    {
        super(conman, params);
        
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);
    }
    
    public static SSLHttpClient getHttpClient() throws KeyManagementException,
                                                       KeyStoreException,
                                                       NoSuchAlgorithmException,
                                                       UnrecoverableKeyException
                                                       
    {
        HttpClient client = new DefaultHttpClient();
        
        X509TrustManager tm = createX509TrustManager();
        
        SSLContext ctx = SSLContext.getInstance("TLS");
        
        ctx.init(null, new TrustManager[] { tm }, null);
        
        SSLSocketFactory ssf = new MySSLSocketFactory(ctx);
        
        ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        
        ClientConnectionManager ccm = client.getConnectionManager();
        
        SchemeRegistry sr = ccm.getSchemeRegistry();
        sr.register(new Scheme("https", ssf, 443));
        
        return new SSLHttpClient(new ThreadSafeClientConnManager(client.getParams(), sr), client.getParams());
    }
    
    public final HttpResponse executeResponse(HttpUriRequest request) throws ClientProtocolException,
                                                                             IOException
    {
        return super.execute(request, mHttpContext);
    }
    
    private static X509TrustManager createX509TrustManager()
    {
        return new X509TrustManager()
        {
            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException { }
            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException { }
            public X509Certificate[] getAcceptedIssuers() { return null; }
        };
    }
    
    private static TrustManager createTrustManager()
    {
        return new X509TrustManager()
        {
            public void checkClientTrusted(X509Certificate[] chain, String authType) { }
            public void checkServerTrusted(X509Certificate[] chain, String authType) { }
            public X509Certificate[] getAcceptedIssuers() { return null; }
        };
    }
    
    private static final class MySSLSocketFactory extends SSLSocketFactory
    {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        
        public MySSLSocketFactory(KeyStore truststore) throws KeyManagementException,
                                                              KeyStoreException,
                                                              NoSuchAlgorithmException,
                                                              UnrecoverableKeyException
        {
            super(truststore);
            
            TrustManager tm = createTrustManager();
            
            sslContext.init(null, new TrustManager[] { tm }, null);
        }
        
        public MySSLSocketFactory(SSLContext context) throws KeyManagementException,
                                                             KeyStoreException,
                                                             NoSuchAlgorithmException,
                                                             UnrecoverableKeyException
        {
            super(null);
            sslContext = context;
        }
        
        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, 
                                                                                                   UnknownHostException
        {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }
        
        @Override
        public Socket createSocket() throws IOException
        {
            return sslContext.getSocketFactory().createSocket();
        }
    }
}
