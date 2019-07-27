/*
 * Copyright 2016 Fumiharu Kinoshita
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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

/**
 ************************************************
 * interval execute process.
 *
 * @author Fumiharu Kinoshita
 ************************************************
 */
public abstract class AsyncIntervalProcess<T> extends AsyncProcess<T> {

	/** processing interval(ms). */
	private long interval;

	/**
	 **********************************
	 * consrctor.
	 * @param interval processing interval(millis)
	 **********************************
	 */
	public AsyncIntervalProcess(final long interval) {
		super();
		if (interval <= 0) {
			throw new IllegalArgumentException("interval is greater than 0.");
		}
		this.interval = interval;
	}

	/**
	 **********************************
	 * implements interval processing.
	 *
	 * @return true if continue process, otherwise false
	 * @throws Exception
	 **********************************
	 */
	protected abstract boolean executeInterval() throws Exception;

    /*
     **********************************
     * @see info.bunji.asyncutil.AsyncProcess#execute()
     **********************************
     */
    @Override
    protected final void execute() throws Exception {

        final Timer timer = new Timer(false);
        final CountDownLatch latch = new CountDownLatch(1);

        ExecTimerTask task = new ExecTimerTask(latch, timer);
        timer.scheduleAtFixedRate(task, 0, interval);

        // wait execute finish
        latch.await();
        timer.cancel();

        if (task.exception != null) {
        	throw task.exception;
        }
	}

	protected final void dispose() {
		getAsyncProc().dispose();
	}

	/*
	 * internal execute timer task
	 */
    private final class ExecTimerTask extends TimerTask {
        private volatile Exception exception = null;
        private final CountDownLatch latch;
        private final Timer timer;

        private ExecTimerTask(CountDownLatch latch, Timer timer) {
        	this.latch = latch;
        	this.timer = timer;
        }

        @Override
        public void run() {
            try {
                if (getAsyncProc().isDisposed() || !executeInterval()) {
                    timer.cancel();
                    latch.countDown();
                }
            } catch (Exception e) {
                // set exception.
                exception = e;
                timer.cancel();
                latch.countDown();
            }
        }
    }
}
