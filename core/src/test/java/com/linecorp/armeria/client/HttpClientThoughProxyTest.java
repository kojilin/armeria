/*
 * Copyright 2018 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.armeria.client;

import java.net.InetSocketAddress;

import org.junit.Rule;
import org.junit.Test;

import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.testing.junit4.server.ServerRule;

/**
 *
 */
public class HttpClientThoughProxyTest {

    private static final String PATH = "/test";

    @Rule
    public final ServerRule server = new ServerRule() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service(PATH, (ctx, req) -> {
                System.out.println(">>>>>Access!!");
                return HttpResponse.of("test");
            });
        }
    };

//    @Rule
//    public MockServerRule mockProxyRule = new MockServerRule(this, 9999);
//
//    private MockServerClient mockServerClient;

    private final ClientFactory clientFactory =
            ClientFactory.builder()
                         .proxy(Proxy.ofHttpProxy(new InetSocketAddress("127.0.0.1", 3129),
                                                  "foo", "bar"))
                         .build();

    @Test
    public void throughHttpProxy() throws Exception {
        final WebClient client = WebClient.builder(server.uri(SessionProtocol.HTTP))
                                          .factory(clientFactory)
                                          .build();

        try {
            System.out.println(server.httpPort());
            System.out.println(client.get("/test").aggregate().get().contentUtf8());
        } catch (Exception e) {
            e.printStackTrace();
        }
//        LogEventRequestAndResponse[] logEventRequestAndResponses =
//                mockServerClient.retrieveRecordedRequestsAndResponses(request());
//        System.out.println(Arrays.toString(logEventRequestAndResponses));
    }
}
