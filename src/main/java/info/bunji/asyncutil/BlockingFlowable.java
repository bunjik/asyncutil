/*
 * Copyright 2016-2018 Fumiharu Kinoshita
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
package info.bunji.asyncutil;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.ExceptionHelper;

/**
 ************************************************
 * blocking flowable class.
 * @author f.kinoshita
 * @param <T> element type
 ************************************************
 */
final class BlockingFlowable<T> implements Iterable<T> {

    private final IteratorSubscriber<T> iterator;

    /**
     **********************************
     * @param source flowable instance
     * @param bufSize buffeer size
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     **********************************
     */
    BlockingFlowable(Flowable<T> source, int bufSize, boolean isDelayError) {
        iterator = new IteratorSubscriber<>(bufSize, isDelayError);
        source.subscribe(iterator);
    }

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    /**
     ****************************************
     * blocking iterator class.
     * @param <T> element type
     ****************************************
     */
    private static final class IteratorSubscriber<T>
                                    extends AtomicReference<Subscription>
                                    implements FlowableSubscriber<T>, Iterator<T> {

        private final BlockingQueue<T> queue;
        private final long limit;
        private final Lock lock;
        private final Condition condition;
        private volatile boolean done;
        private final boolean delayError;
        Throwable error;
        long produced;
        long bufSize;

        /**
         **********************************
         * @param bufSize buffeer size
         * @param isDelayError if true, the exception is delayed until all added data is read.
         *                     if false, immediately raise an exception.
         **********************************
         */
        IteratorSubscriber(int bufSize, boolean delayError) {
            this.queue = new LinkedBlockingQueue<>(bufSize);
            this.bufSize = bufSize;
            this.limit = bufSize - (bufSize >> 2);
            this.delayError = delayError;
            this.lock = new ReentrantLock();
            this.condition = lock.newCondition();
        }

        @Override
        public boolean hasNext() {
            for (;;) {
                boolean d = done;
                boolean isEmpty = queue.isEmpty();

                if (!d && isEmpty) {
                    lock.lock();
                    try {
                        while (!done && queue.isEmpty()) {
                            condition.await();
                        }
                    } catch (InterruptedException ie) {
                        get().cancel();
                        throw ExceptionHelper.wrapOrThrow(ie);	// internal method
                    } finally {
                        lock.unlock();
                    }
                } else {
                    if (d && isEmpty) {
                        Throwable e = error;
                        if (e != null) {
                            throw ExceptionHelper.wrapOrThrow(e);
                        }
                        return false;
                    }
                    return true;
                }
            }
        }

        @Override
        public T next() {
            if (hasNext()) {
                T value = queue.poll();
                long p = produced + 1;
                if (p == limit) {
                    produced = 0;
                    get().request(p);
                } else {
                    produced = p;
                }
                return value;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        void signalConsumer() {
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void onNext(T t) {
            if (!queue.offer(t)) {
                SubscriptionHelper.cancel(this);
                // FIXME
                onError(new MissingBackpressureException("queue is full?!"));
            } else {
                signalConsumer();
            }
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            if (!delayError) {
            	// clear unread values
                queue.clear();
            }
            signalConsumer();
        }

        @Override
        public void onComplete() {
            done = true;
            signalConsumer();
        }

        @Override
        public void onSubscribe(Subscription s) {
            //logger.trace("call onSubscribe()");
            set(s);
            s.request(bufSize);
        }
    }
}
