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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bunji.asyncutil.AsyncProc.ExecuteFunc;
import info.bunji.asyncutil.functions.PostFunc;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 ************************************************
 * aync execute result.
 *
 * <p>iterate access processed result.<br>
 * return result to immidiate, after call {@code new ClosaleResult()}.<br>
 * usage: (use try-with-resource):
 * <pre>
 * {@code
 * try (ClosaleResult<String> cr = new ClosaleResult(new AsyncSearchProc())) {
 *   for (String r : cr) {
 *     // process result.
 *   }
 * } // call close() on finally block.
 * }
 * </pre>
 * @author f.kinoshita
 * @param <T> result element type
 ************************************************
 */
public final class ClosableResult<T> implements Iterable<T>, Closeable {

    /** logger */
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    /** execute process */
    private final AsyncProc<T> asyncProc;

    private final Iterator<T> iterator;

    private volatile boolean isClosed = false;

    protected static final int DEFAULT_BUF_SIZE = 4096;

    /**
     **********************************
     * @param proc execute AsyncProcess instance
     * @deprecated compatibility for previous versions. use {@link AsyncProcess#run()}
     **********************************
     */
    public ClosableResult(AsyncProcess<T> proc) {
        this(proc, DEFAULT_BUF_SIZE, false);
    }

    /**
     **********************************
     * @param proc execute AsyncProcess instance
     * @param bufSize append buffer size
     * @deprecated compatibility for previous versions. use {@link AsyncProcess#run(int)}
     **********************************
     */
    public ClosableResult(AsyncProcess<T> proc, int bufSize) {
        this(proc, bufSize, false);
    }

    /**
     **********************************
     * @param proc execute AsyncProcess instance
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     * @deprecated compatibility for previous versions. use {@link AsyncProcess#run(boolean)}
     **********************************
     */
    public ClosableResult(AsyncProcess<T> proc, boolean isDelayError) {
        this(proc, DEFAULT_BUF_SIZE, isDelayError);
    }

    /**
     **********************************
     * @param proc execute AsyncProcess instance
     * @param bufSize append buffer size
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     * @deprecated compatibility for previous versions. use {@link AsyncProcess#run(int, boolean)}
     **********************************
     */
    public ClosableResult(AsyncProcess<T> proc, int bufSize, boolean isDelayError) {
        this(proc.getAsyncProc(), bufSize, isDelayError);
    }

    /**
     **********************************
     * @param asyncProc execute AsyncProc instance
     **********************************
     */
    public ClosableResult(AsyncProc<T> asyncProc) {
        this(asyncProc, DEFAULT_BUF_SIZE);
    }

    /**
     **********************************
     * @param asyncProc execute AsyncProc instance
     * @param bufSize append buffer size
     **********************************
     */
    public ClosableResult(AsyncProc<T> asyncProc, int bufSize) {
        this(asyncProc, bufSize, false);
    }

    /**
     **********************************
     * @param asyncProc execute AsyncProc instance
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     **********************************
     */
    public ClosableResult(AsyncProc<T> asyncProc, boolean isDelayError) {
        this(asyncProc, DEFAULT_BUF_SIZE, isDelayError);
    }

    /**
     **********************************
     * @param asyncProc execute AsyncProc instance
     * @param bufSize append buffer size
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     **********************************
     */
	public ClosableResult(AsyncProc<T> asyncProc, int bufSize, boolean isDelayError) {

		logger.trace("exec proc : bufSize={} / delayError={}", bufSize, isDelayError);

        this.asyncProc = asyncProc;
        Flowable<T> f = Flowable.create(asyncProc, BackpressureStrategy.BUFFER)
	                            .doOnRequest(asyncProc.getExecFunc())
                                .doOnError(new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        logger.error("error occurred now. [{}]", t.toString());
                                    }
                                 })
                                .observeOn(Schedulers.newThread(), isDelayError, bufSize)
                                .subscribeOn(Schedulers.newThread(), false);

 	    this.iterator = new BlockingFlowable<>(f, bufSize, isDelayError).iterator();
	}

    /**
     **********************************
     * @param execFunc execute callback
     **********************************
     */
    public ClosableResult(ExecuteFunc<T> execFunc) {
        this(new AsyncProc<T>().setExecFunc(execFunc));
    }

    /**
     **********************************
     * @param execFunc execute callback
     * @param bufSize append buffer size
     **********************************
     */
    public ClosableResult(ExecuteFunc<T> execFunc, int bufSize) {
        this(new AsyncProc<T>().setExecFunc(execFunc), bufSize);
    }

    /**
     **********************************
     * @param execFunc execute callback
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     **********************************
     */
    public ClosableResult(ExecuteFunc<T> execFunc, boolean isDelayError) {
        this(new AsyncProc<T>().setExecFunc(execFunc), isDelayError);
    }

    /**
     **********************************
     * @param execFunc execute callback
     * @param bufSize append buffer size
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     **********************************
     */
    public ClosableResult(ExecuteFunc<T> execFunc, int bufSize, boolean isDelayError) {
        this(new AsyncProc<T>().setExecFunc(execFunc), isDelayError);
    }

    /**
     **********************************
     * @param execFunc execute callback
     * @param postFunc postProcess callback
     **********************************
     */
    public ClosableResult(ExecuteFunc<T> execFunc, PostFunc postFunc) {
        this(new AsyncProc<T>().setExecFunc(execFunc).setPostFunc(postFunc));
    }

    /**
     **********************************
     * @param execFunc execute callback
     * @param postFunc postProcess callback
     * @param bufSize append buffer size
     **********************************
     */
    public ClosableResult(ExecuteFunc<T> execFunc, PostFunc postFunc, int bufSize) {
        this(new AsyncProc<T>().setExecFunc(execFunc).setPostFunc(postFunc), bufSize);
    }

    /**
     **********************************
     * @param execFunc execute callback
     * @param postFunc postProcess callback
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     **********************************
     */
    public ClosableResult(ExecuteFunc<T> execFunc, PostFunc postFunc, boolean isDelayError) {
      this(new AsyncProc<T>().setExecFunc(execFunc).setPostFunc(postFunc), isDelayError);
    }

    /**
     **********************************
     * @param execFunc execute callback
     * @param postFunc postProcess callback
     * @param bufSize append buffer size
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     **********************************
     */
    public ClosableResult(ExecuteFunc<T> execFunc, PostFunc postFunc, int bufSize, boolean isDelayError) {
        this(new AsyncProc<T>().setExecFunc(execFunc).setPostFunc(postFunc), bufSize, isDelayError);
    }

    /**
      **********************************
     * @param source the source Iterable sequence
     **********************************
     */
    public ClosableResult(Iterable<T> source) {
        this(source, DEFAULT_BUF_SIZE, true);
    }

    /**
     **********************************
     * @param source the source Iterable sequence
     * @param bufSize buffer size
     **********************************
     */
    public ClosableResult(Iterable<T> source, int bufSize) {
        this(source, bufSize, true);
    }

    /**
     **********************************
     * @param source the source Iterable sequence
     * @param bufSize buffeer size
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     **********************************
     */
    public ClosableResult(Iterable<T> source, int bufSize, boolean isDelayError) {
        this.asyncProc = null;

        Flowable<T> f = Flowable.fromIterable(source)
                .observeOn(Schedulers.newThread(), isDelayError)
                .subscribeOn(Schedulers.newThread(), false);

        // generate iterator
        this.iterator = new BlockingFlowable<>(f, bufSize, isDelayError).iterator();
    }

    /**
     **********************************
     * get result list(blocking api).
     * <br>
     * blocking method.
     * @return result list
     **********************************
     */
    public List<T> toList() {
        List<T> results = new ArrayList<>();
        for (T val : this) {
            results.add(val);
        }
        return results;
    }

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

    @Override
    public final void close() throws IOException {
        if (!isClosed) {
            isClosed = true;
            logger.trace("{}.close()", getClass().getSimpleName());
            if (asyncProc != null) {
                if (!asyncProc.isDisposed()) {
                    asyncProc.dispose();
                }
            }
        }
    }
}
