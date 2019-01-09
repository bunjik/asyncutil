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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bunji.asyncutil.functions.ExecResult;
import info.bunji.asyncutil.functions.PostFunc;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.LongConsumer;

/**
 ************************************************
 * aync process class.(lambda support)
 * @param <T> result type
 * @author f.kinoshita
 ************************************************
 */
public final class AsyncProc<T> implements FlowableOnSubscribe<T>, Disposable {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private volatile FlowableEmitter<T> emitter;

    private ExecuteFunc<T> execFunc = null;

    private PostFunc postFunc = EMPTY_POST_FUNC;

    private AtomicBoolean isDisposed = new AtomicBoolean(false);

    private static final PostFunc EMPTY_POST_FUNC = new PostFunc() {
        @Override
		public void execute(ExecResult result) {
            // do nothing.
        }
    };

    public AsyncProc() {
    }

    public AsyncProc(ExecuteFunc<T> callback) {
        this();
        setExecFunc(callback);
    }

    /**
     *******************************s***
     * xxx.
     * @param callback execute callback
     * @return this instance
     **********************************
     */
    public AsyncProc<T> setExecFunc(ExecuteFunc<T> callback) {
        if (callback == null) {
            //throw new NullPointerException("ExecuteAction can not null.");
            throw new IllegalArgumentException("ExecuteAction can not null.");
        }
        if (execFunc != null) {
            throw new IllegalStateException("ExecuteAction already set.");
        }
        execFunc = callback;
        return this;
    }

    /**
     **********************************
     * xxx.
     * @param callback process finished callback
     * @return this instance
     **********************************
     */
    public AsyncProc<T> setPostFunc(PostFunc callback) {
        if (callback == null) {
            //throw new IllegalArgumentException("callback function can not null.");
            postFunc = EMPTY_POST_FUNC;
        } else {
            postFunc = callback;
        };
        return this;
    }

    ExecuteFunc<T> getExecFunc() {
        return execFunc;
    }

    @Override
    public void dispose() {
        if (!isDisposed.get()) {
            isDisposed.set(true);
            logger.trace("AsyncProc.dispose()");
            try {
ExecResult result = new ExecResult(execFunc.processedCnt.get());
logger.trace(result.toString());
            	postFunc.execute(result);
            } catch (Exception e) {
                logger.error("exception in postFunc. msg=[{}]", e.getMessage());
            }
        }
    }

    @Override
    public boolean isDisposed() {
        return isDisposed.get();
    }

    @Override
    public final void subscribe(FlowableEmitter<T> emitter) throws Exception {
        this.emitter = emitter.serialize();
        this.emitter.setDisposable(this);
        try {
            execFunc.accept(this);

            // execute Process
            execFunc.execute();

            this.emitter.onComplete();
        } catch (Throwable t) {
            if (!this.emitter.tryOnError(t)) {
                logger.debug("AsyncProc cancelled.");
            }
        }
    }

    /**
     **********************************
     * execute process.
     * @return async process result
     **********************************
     */
    public ClosableResult<T> run() {
        return new ClosableResult<T>(this);
    }

    /**
     **********************************
     * execute process.
     * @param bufSize append buffer size
     * @return async process result
     **********************************
     */
    public ClosableResult<T> run(int bufSize) {
        return new ClosableResult<T>(this, bufSize);
    }

    /**
     **********************************
     * execute process.
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     * @return async process result
     **********************************
     */
    public ClosableResult<T> run(boolean isDelayError) {
        return new ClosableResult<T>(this, isDelayError);
    }

    /**
     **********************************
     * execute process.
     * @param bufSize append buffer size
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     * @return async process result
     **********************************
     */
    public ClosableResult<T> run(int bufSize, boolean isDelayError) {
        return new ClosableResult<T>(this, bufSize, isDelayError);
    }

    /**
     ********************************************
     *
     * @param <T> element type
     ********************************************
     */
    public abstract static class ExecuteFunc<T> implements LongConsumer {

        /** logger */
        protected Logger logger = LoggerFactory.getLogger(getClass());

        /** target process */
        private volatile AsyncProc<T> parentProc;

        /** target emitter */
        private FlowableEmitter<T> emitter;

        /** processed item count */
        private final AtomicLong processedCnt = new AtomicLong(0);

        private final AtomicLong requested = new AtomicLong(0);
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition isRequested = lock.newCondition();

        /**
         **********************************
         * execute action impl.
         * @throws Exception
         **********************************
         */
        public abstract void execute() throws Exception;

        /**
         **********************************
         * internal use only.
         * @param proc
         **********************************
         */
        final void accept(AsyncProc<T> proc) {
            this.parentProc = proc;
            this.emitter = proc.emitter;
        }

        /**
         ******************************
         * call onRequest from publisher(internal use only).
         ******************************
         */
        @Override
        public final void accept(long request) {
            //logger.trace("call onRequest({})", request);
            //long cnt = requested.addAndGet(request);
            //logger.debug("hasRequest = {}", cnt);
            requested.addAndGet(request);
            lock.lock();
            try {
                isRequested.signalAll();
            } finally {
                lock.unlock();
            }
        }

        /**
         **********************************
         * emit single value.
         * @param value value
         **********************************
         */
        protected final void append(T value) {
            if (requested.get() <= 0) {
                lock.lock();
                try {
//                  logger.trace("blocking append() no buffer space.");
                  isRequested.awaitUninterruptibly();
//                	isRequested.await();
//                  logger.trace("unlock block.");
//                } catch (InterruptedException e) {
//                    throw new ProcessExecuteException("interrupted process.", e);
                } finally {
                    lock.unlock();
                }
            }
            if (parentProc.isDisposed()) {
            	emitter.onComplete();
                logger.trace("process disposed. isDisposeed={}", parentProc.isDisposed());
                throw new IllegalStateException("process disposed.");
            }
            emitter.onNext(value);
            requested.decrementAndGet();
processedCnt.incrementAndGet();
        }
    }
}
