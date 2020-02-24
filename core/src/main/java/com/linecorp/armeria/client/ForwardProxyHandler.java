/*
 * Copyright 2020 LINE Corporation
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
 *
 */

package com.linecorp.armeria.client;

import java.net.SocketAddress;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

public class ForwardProxyHandler extends ChannelOutboundHandlerAdapter {

    private final SocketAddress proxyAddress;
    @Nullable
    private final AsciiString authorization;

    protected ForwardProxyHandler(SocketAddress proxyAddress) {
        this(proxyAddress, null, null);
    }

    public ForwardProxyHandler(SocketAddress proxyAddress, @Nullable String username,
                               @Nullable String password) {
        this.proxyAddress = proxyAddress;
        if (username != null && password != null) {
            final ByteBuf authz = Unpooled.copiedBuffer(username + ':' + password, CharsetUtil.UTF_8);
            final ByteBuf authzBase64 = Base64.encode(authz, false);
            authorization = new AsciiString("Basic " + authzBase64.toString(CharsetUtil.US_ASCII));
            authz.release();
            authzBase64.release();
        } else {
            authorization = null;
        }
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                        ChannelPromise promise) throws Exception {
        ctx.connect(proxyAddress, localAddress, promise);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println(">>>>>proxy write msg:" + msg);
        ctx.write(msg, promise);
    }
}
