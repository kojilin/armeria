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

public class Proxy {

    static final Proxy DIRECT = new Proxy(ProxyType.DIRECT, null);

    public static Proxy ofHttpProxy(SocketAddress socketAddress) {
        return new Proxy(ProxyType.HTTP_PROXY, socketAddress);
    }

    public static Proxy ofHttpProxy(SocketAddress socketAddress, String username, String password) {
        return new Proxy(ProxyType.HTTP_PROXY, socketAddress, username, password);
    }

    public static Proxy ofHttpProxy(SocketAddress socketAddress, boolean forceTunneling) {
        return new Proxy(ProxyType.HTTP_PROXY, socketAddress);
    }

    public static Proxy ofHttpProxy(SocketAddress socketAddress, String username, String password,
                                    boolean forceTunneling) {
        return new Proxy(ProxyType.HTTP_PROXY, socketAddress, username, password);
    }

    public static Proxy ofSocks4(SocketAddress socketAddress) {
        return new Proxy(ProxyType.SOCKS4, socketAddress);
    }

    public static Proxy ofSocks5(SocketAddress socketAddress) {
        return new Proxy(ProxyType.SOCKS5, socketAddress);
    }

    public static Proxy ofSocks5(SocketAddress socketAddress, String username, String password) {
        return new Proxy(ProxyType.SOCKS5, socketAddress, username, password);
    }

    private final ProxyType proxyType;

    @Nullable
    private final SocketAddress socketAddress;
    @Nullable
    private final String username;
    @Nullable
    private final String password;

    private Proxy(ProxyType proxyType, @Nullable SocketAddress socketAddress) {
        this(proxyType, socketAddress, null, null);
    }

    private Proxy(ProxyType proxyType, @Nullable SocketAddress socketAddress, @Nullable String username,
                  @Nullable String password) {
        this.proxyType = proxyType;
        this.socketAddress = socketAddress;
        this.username = username;
        this.password = password;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    @Nullable
    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public boolean isForceTunneling() {
        return false;
    }

    public enum ProxyType {
        DIRECT,
        SOCKS4,
        SOCKS5,
        HTTP_PROXY,
    }
}
