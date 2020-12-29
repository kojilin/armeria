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

package com.linecorp.armeria.common.rxjava2;

import javax.annotation.Nullable;

import org.reactivestreams.Subscriber;

import com.google.errorprone.annotations.concurrent.GuardedBy;

import com.linecorp.armeria.common.RequestContext;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility class to keep {@link RequestContext} during RxJava operations.
 */
public final class RequestContextAssembly {
    @Nullable
    @GuardedBy("RequestContextAssembly.class")
    private static BiFunction<? super Completable, ? super CompletableObserver, ? extends CompletableObserver>
            oldOnCompletableSubscribe;

    @Nullable
    @GuardedBy("RequestContextAssembly.class")
    private static BiFunction<? super Maybe, MaybeObserver, ? extends MaybeObserver> oldOnMaybeSubscribe;

    @Nullable
    @GuardedBy("RequestContextAssembly.class")
    private static BiFunction<? super Single, ? super SingleObserver, ? extends SingleObserver>
            oldOnSingleSubscribe;

    @Nullable
    @GuardedBy("RequestContextAssembly.class")
    private static BiFunction<? super Flowable, ? super Subscriber, ? extends Subscriber>
            oldOnFlowableSubscribe;

    @Nullable
    @GuardedBy("RequestContextAssembly.class")
    private static BiFunction<? super Observable, ? super Observer, ? extends Observer>
            oldOnObservableSubscribe;

    @GuardedBy("RequestContextAssembly.class")
    private static boolean enabled;

    private RequestContextAssembly() {
    }

    /**
     * Enable {@link RequestContext} during operators.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static synchronized void enable() {
        if (enabled) {
            return;
        }

        oldOnCompletableSubscribe = RxJavaPlugins.getOnCompletableSubscribe();
        RxJavaPlugins.setOnCompletableSubscribe(
                compose(oldOnCompletableSubscribe,
                        new ConditionalOnCurrentRequestContextBiFunction<Completable, CompletableObserver>() {
                            @Override
                            CompletableObserver applyActual(
                                    CompletableObserver t, RequestContext ctx) {
                                return new RequestContextCompletableObserver(t, ctx);
                            }
                        }));

        oldOnMaybeSubscribe =
                (BiFunction<? super Maybe, MaybeObserver, ? extends MaybeObserver>)
                        RxJavaPlugins.getOnMaybeSubscribe();
        RxJavaPlugins.setOnMaybeSubscribe(
                (BiFunction<? super Maybe, MaybeObserver, ? extends MaybeObserver>)
                        compose(oldOnMaybeSubscribe,
                                new ConditionalOnCurrentRequestContextBiFunction<Maybe, MaybeObserver>() {
                                    @Override
                                    MaybeObserver applyActual(MaybeObserver t, RequestContext ctx) {
                                        return new RequestContextMaybeObserver(t, ctx);
                                    }
                                }));

        oldOnSingleSubscribe = RxJavaPlugins.getOnSingleSubscribe();
        RxJavaPlugins.setOnSingleSubscribe(
                compose(oldOnSingleSubscribe,
                        new ConditionalOnCurrentRequestContextBiFunction<Single, SingleObserver>() {
                            @Override
                            SingleObserver applyActual(SingleObserver t,
                                                       RequestContext ctx) {
                                return new RequestContextSingleObserver(t, ctx);
                            }
                        }));

        oldOnFlowableSubscribe = RxJavaPlugins.getOnFlowableSubscribe();
        RxJavaPlugins.setOnFlowableSubscribe(
                compose(oldOnFlowableSubscribe,
                        new ConditionalOnCurrentRequestContextBiFunction<Flowable, Subscriber>() {
                            @Override
                            Subscriber applyActual(Subscriber t,
                                                   RequestContext ctx) {
                                return new RequestContextSubscriber(t, ctx);
                            }
                        }));

        oldOnObservableSubscribe = RxJavaPlugins.getOnObservableSubscribe();
        RxJavaPlugins.setOnObservableSubscribe(
                compose(oldOnObservableSubscribe,
                        new ConditionalOnCurrentRequestContextBiFunction<Observable, Observer>() {
                            @Override
                            Observer applyActual(Observer t,
                                                 RequestContext ctx) {
                                return new RequestContextObserver(t, ctx);
                            }
                        }));

        enabled = true;
    }

    /**
     * Disable {@link RequestContext} during operators.
     */
    public static synchronized void disable() {
        if (!enabled) {
            return;
        }

        RxJavaPlugins.setOnCompletableSubscribe(oldOnCompletableSubscribe);
        oldOnCompletableSubscribe = null;
        RxJavaPlugins.setOnMaybeSubscribe(oldOnMaybeSubscribe);
        oldOnMaybeSubscribe = null;
        RxJavaPlugins.setOnSingleSubscribe(oldOnSingleSubscribe);
        oldOnSingleSubscribe = null;
        RxJavaPlugins.setOnFlowableSubscribe(oldOnFlowableSubscribe);
        oldOnFlowableSubscribe = null;
        RxJavaPlugins.setOnObservableSubscribe(oldOnObservableSubscribe);
        oldOnObservableSubscribe = null;

        enabled = false;
    }

    private static <T1, T2> BiFunction<? super T1, ? super T2, ? extends T2> compose(
            @Nullable BiFunction<? super T1, ? super T2, ? extends T2> before,
            BiFunction<? super T1, ? super T2, ? extends T2> after) {
        if (before == null) {
            return after;
        }
        return (T1 t1, T2 t2) -> after.apply(t1, before.apply(t1, t2));
    }

    private abstract static class ConditionalOnCurrentRequestContextBiFunction<T1, T2>
            implements BiFunction<T1, T2, T2> {
        @Override
        public T2 apply(@NonNull T1 t1, @NonNull T2 t2) throws Exception {
            return RequestContext.mapCurrent(requestContext -> applyActual(t2, requestContext), () -> t2);
        }

        abstract T2 applyActual(T2 t, RequestContext ctx);
    }
}
