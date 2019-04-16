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

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bunji.asyncutil.AsyncProc.ExecuteFunc;
import info.bunji.asyncutil.functions.PostFunc;

/**
 ************************************************
 * Async process base class.
 * <br>
 * for previous releasse compatible.
 * @author f.kinoshita
 ************************************************
 */
public abstract class AsyncProcess<T> {

	/** logger */
    protected Logger logger = LoggerFactory.getLogger(getClass());
    /** converted  AsyncProc instance */
    private final AsyncProc<T> internalProc;
    /** invoke append method */
    private static Method appendMethod;

    static {
        try {
            // get append method
            appendMethod = ExecuteFunc.class.getDeclaredMethod("append", Object.class);
            appendMethod.setAccessible(true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     **********************************
     **********************************
     */
    public AsyncProcess() {
        final AsyncProcess<T> process = this;

        // wrap execute()
        ExecuteFunc<T> execFunc = new ExecuteFunc<T>() {
            @Override
            public void execute() throws Exception {
                process.execute();
            }
        };

        // wrap postProcess()
        PostFunc postFunc = new PostFunc() {
            @Override
            public void execute(ExecResult result) {
                process.postProcess();
            }
        };

        // convert AsyncProc instance
        internalProc = new AsyncProc<T>().setExecFunc(execFunc).setPostFunc(postFunc);
    }

    /**
     **********************************
     * get internal asyncProc instance.
     * @return AsyncProc instance
     **********************************
     */
    final AsyncProc<T> getAsyncProc() {
        return internalProc;
    }

    /**
     **********************************
     * execute process impl.
     * @throws Exception
     **********************************
     */
    protected abstract void execute() throws Exception;

    /**
     **********************************
     * emit value to async process.
     * @param value process result value
     **********************************
     */
    protected final void append(T value) {
        try {
        	appendMethod.invoke(internalProc.getExecFunc(), value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     **********************************
     * call execute finish.
     * (do nothing default)
     **********************************
     */
    protected void postProcess() {
        logger.trace("call postProcess()");
    }

    /**
     **********************************
     * execute process.
     * @return async process result
     **********************************
     */
    public final ClosableResult<T> run() {
        return new ClosableResult<T>(internalProc);
    }

    /**
     **********************************
     * execute process.
     * @param bufSize append buffer size
     * @return async process result
     **********************************
     */
    public final ClosableResult<T> run(int bufSize) {
        return new ClosableResult<T>(internalProc, bufSize);
    }

    /**
     **********************************
     * execute process.
     * @param isDelayError if true, the exception is delayed until all added data is read.
     *                     if false, immediately raise an exception.
     * @return async process result
     **********************************
     */
    public final ClosableResult<T> run(boolean isDelayError) {
        return new ClosableResult<T>(internalProc, isDelayError);
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
    public final ClosableResult<T> run(int bufSize, boolean isDelayError) {
        return new ClosableResult<T>(internalProc, bufSize, isDelayError);
    }
}
