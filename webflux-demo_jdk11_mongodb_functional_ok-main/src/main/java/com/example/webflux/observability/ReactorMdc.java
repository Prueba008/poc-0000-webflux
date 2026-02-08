package com.example.webflux.observability;

import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

public final class ReactorMdc {

    private static boolean enabled = false;

    private ReactorMdc(){}

    public static void enable() {
        if (enabled) return;
        enabled = true;

        Hooks.onEachOperator("mdcContext", Operators.lift((scannable, subscriber) ->
                new MdcSubscriber(subscriber)));
    }

    public static void disable() {
        if (!enabled) return;
        Hooks.resetOnEachOperator("mdcContext");
        enabled = false;
    }

    static class MdcSubscriber<T> implements CoreSubscriber<T> {

        private final CoreSubscriber<T> delegate;

        MdcSubscriber(CoreSubscriber<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Context currentContext() {
            return delegate.currentContext();
        }

        @Override
        public void onSubscribe(Subscription s) {
            delegate.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            withMdc(delegate.currentContext(), () -> delegate.onNext(t));
        }

        @Override
        public void onError(Throwable t) {
            withMdc(delegate.currentContext(), () -> delegate.onError(t));
        }

        @Override
        public void onComplete() {
            withMdc(delegate.currentContext(), delegate::onComplete);
        }

        private void withMdc(Context ctx, Runnable r) {
            if (ctx.hasKey(CorrelationIdWebFilter.CTX_KEY)) {
                MDC.put(CorrelationIdWebFilter.CTX_KEY, ctx.get(CorrelationIdWebFilter.CTX_KEY));
            }
            try {
                r.run();
            } finally {
                MDC.remove(CorrelationIdWebFilter.CTX_KEY);
            }
        }
    }
}
