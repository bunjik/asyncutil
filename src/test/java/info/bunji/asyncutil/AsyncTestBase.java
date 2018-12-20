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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bunji.asyncutil.AsyncProc.ExecuteFunc;
import io.reactivex.functions.Action;

/**
 ************************************************
 *
 * @author f.kinoshita
 ************************************************
 */
public abstract class AsyncTestBase {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    @Rule
    public TestName name = new TestName();

    private ThreadLocal<Long> testTime = new ThreadLocal<>();

    @Before
    public void setup() {
        logger.info("*** start {}() ***", name.getMethodName());
        testTime.set(System.currentTimeMillis());
    }

    @After
    public void tearDown() {
        long elapsed = System.currentTimeMillis() - testTime.get();
        logger.info("*** finish {}() ({}ms) ***", name.getMethodName(), elapsed);
    }

    static class IntAsyncProcess extends AsyncProcess<Integer> {
        private final int size;
        private int throwCnt = Integer.MAX_VALUE;

        public IntAsyncProcess(int size) {
            this.size = size;
        }

        public IntAsyncProcess setThrow(int throwCnt) {
            this.throwCnt = throwCnt;
            return this;
        }

        @Override
        protected void execute() throws Exception {
            for (int n = 1; n <= size; n++) {
                append(n);
                if (n >= throwCnt) {
                    throw new IllegalStateException("exception in execute() val=" + n);
                }
            }
        }
    }


    public static class IntExecAction extends ExecuteFunc<Integer> {
        private int size;
        private int throwCnt = Integer.MAX_VALUE;

        public IntExecAction(int size) {
            this.size = size;
        }

        public IntExecAction setThrow(int throwCnt) {
            this.throwCnt = throwCnt;
            return this;
        }

        @Override
        public void execute() throws Exception {
            for (int i = 1; i <= size; i++) {
                append(i);
                if (i >= throwCnt) {
                	//logger.debug("exception occurred now.");
                    throw new IllegalStateException("error in execute() i=" + i);
                }
            }
        }
    }

    public static class TestPostAction implements Action {
        private Logger logger = LoggerFactory.getLogger(getClass());
        @Override
        public void run() {
            logger.debug("call postAction.");
        }
    }

    /**
     ********************************************
     * Test AsyncIntervalProcess class
     ********************************************
     */
    static class TestIntervalProc extends AsyncIntervalProcess<String> {

        private int cycleCount = 10;
        private int count = 0;
        private Exception t = null;
        private int errCnt = Integer.MIN_VALUE;

        public TestIntervalProc(long interval) {
            super(interval);
        }

        public TestIntervalProc setException(Exception t, int errCnt) {
            this.t = t;
            this.errCnt = errCnt;
            return this;
        }

        public TestIntervalProc setCycleCount(int cycleCount) {
            this.cycleCount = cycleCount;
            return this;
        }

        @Override
        protected boolean executeInterval() throws Exception {
            count++;
            //logger.debug("call executeInterval() : {}", count);
            if (errCnt == count) {
                throw t;
            }
            append("" + count);
            return count < cycleCount;
        }

        @Override
        protected void postProcess() {
            logger.debug("call postProcess()");
            super.postProcess();
        }
    }
}
