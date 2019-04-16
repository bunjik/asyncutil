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

/**
 **************************************
 * process execute result info.
 * @author f.kinoshita
 **************************************
 */
public final class ExecResult {

    private final long processedCount;

	private final long execTime;

    private final Throwable throwable;

    /**
     **********************************
     * @param count processed count
     * @param execTime execute time(ms)
     **********************************
     */
    ExecResult(long count, long execTime) {
        this(count, execTime, null);
    }

    /**
     **********************************
     * @param count processed count
     * @param execTime execute time(ms)
     * @param t occurred exception
     **********************************
     */
    ExecResult(long count, long execTime, Throwable t) {
        this.processedCount = Math.max(count, 0);
        this.execTime = execTime;
        this.throwable = t;
    }

    /**
     **********************************
     * get processed count.
     * @return processed count
     **********************************
     */
    public long getProcessed() {
        return processedCount;
    }

    public boolean isSuccess() {
        return throwable == null;
    }

    /*
    public boolean isCancelled() {
        // implemented yet.
        return false;
    }
    */

    /**
     **********************************
     * get execute time.
     * @return execute time(ms)
     **********************************
     */
    public long getExecTime() {
        return execTime;
    }

    /**
     **********************************
     * get occurred exception.
     * @return occurred exception. not occurred null
     **********************************
     */
    public Throwable getException() {
        return throwable;
    }

    @Override
    public String toString() {
        return String.format("ExecResult: isSuccess=%s/execTime=%dms/processed=%d/exception=%s",
                                        isSuccess(),
                                        getExecTime(),
                                        getProcessed(),
                                        getException());
        //return super.toString();
    }
}
