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

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable.OnSubscribe;
import rx.Subscriber;

/**
 ************************************************
 * aync process base class.
 *
 * @param <T> result type
 * @author f.kinoshita
 ************************************************
 */
public abstract class AsyncProcess<T> implements OnSubscribe<T> {

	/** logger */
	protected Logger logger = LoggerFactory.getLogger(getClass());

	private Subscriber<? super T> subscriber;

	private Boolean isFinished = false;
//	protected Boolean isFinished = false;

	/**
	 **********************************
	 * {@inheritDoc}
	 **********************************
	 */
	@Override
	public final void call(Subscriber<? super T> subscriber) {
		this.subscriber = subscriber;
		try {
			// execute process
			execute();

			// finish process
			this.subscriber.onCompleted();
//		} catch (ProcessCanceledException t) {
//			// do nothing.
		} catch (Throwable t) {
			for (Throwable e : t.getSuppressed()) {
				if (e instanceof ProcessCanceledException) {
					return;
				}
			}
			this.subscriber.onError(t);
		}
	}

	/**
	 **********************************
	 * implements data processing.
	 * <p>
	 * data process and emit values in this method.<br>
	 * if call {@code append(T)} or {@code append(List<T>)}, apply values to result.
	 *
	 * @throws Exception exception
	 **********************************
	 */
	protected abstract void execute() throws Exception;

	/**
	 **********************************
	 * emit values to AsyncResult.
	 *
	 * remove values if emitted.
	 *
	 * @param values process result values
	 **********************************
	 */
	protected final void append(Iterable<T> values) {
		if (values != null) {
			Iterator<T> it = values.iterator();
			while (it.hasNext()) {
				append(it.next());
				try {
					it.remove();
				} catch (UnsupportedOperationException e) {}
			}
		}
	}

	/**
	 **********************************
	 * emit value to AsyncResult.
	 *
	 * @param value process result value
	 **********************************
	 */
	protected final void append(T value) {
		if (subscriber == null || isInterrupted()) {
			throw new ProcessCanceledException("process canceled.");
		}
		subscriber.onNext(value);
	}

	/**
	 **********************************
	 * returns true, if AsyncResult is interrupted.
	 *
	 * @return true if AsyncResult is interrupted, otherwise false
	 **********************************
	 */
	protected final boolean isInterrupted() {
		if (isFinished) return true;
		return subscriber == null ? false: subscriber.isUnsubscribed();
	}

	/**
	 **********************************
	 * call process finished(completed/close/exception).
	 **********************************
	 */
	protected void postProcess() {
		logger.trace("call postProcess()");
	}

	/**
	 **********************************
	 *
	 **********************************
	 */
	final void doPostProcess() {
		synchronized(isFinished) {
			if (!isFinished) {
				isFinished = Boolean.TRUE;
				postProcess();
			}
		}
	}
}
