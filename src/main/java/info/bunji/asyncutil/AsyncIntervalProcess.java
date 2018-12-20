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

	/*
	 **********************************
	 * @see info.bunji.asyncutil.AsyncProcess#execute()
	 **********************************
	 */
	@Override
	protected final void execute() throws Exception {
		while (!getAsyncProc().isDisposed()) {
			if (!executeInterval()) {
				break;
			}
			Thread.sleep(interval);
		}
	}

	protected final void dispose() {
		getAsyncProc().dispose();
	}

	/**
	 **********************************
	 * implements interval processing.
	 *
	 * @return true if continue process, otherwise false
	 **********************************
	 */
	protected abstract boolean executeInterval() throws Exception;
}
