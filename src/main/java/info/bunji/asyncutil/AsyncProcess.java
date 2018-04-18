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

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;

/**
 ************************************************
 * aync process base class.
 *
 * @param <T> result type
 * @author f.kinoshita
 ************************************************
 */
public abstract class AsyncProcess<T> implements FlowableOnSubscribe<T> {

	/** logger */
	protected Logger logger = LoggerFactory.getLogger(getClass());

	private FlowableEmitter<T> emitter;

	protected List<Listener> listeners = new ArrayList<>();

	/**
	 **********************************
	 *
	 **********************************
	 */
	public AsyncProcess() {
	}

	/**
	 **********************************
	 *
	 * @return
	 **********************************
	 */
	protected boolean isCancelled() {
		return emitter.isCancelled();
	}

	/*
	 **********************************
	 * (non Javadoc)
	 * @see io.reactivex.FlowableOnSubscribe#subscribe(io.reactivex.FlowableEmitter)
	 **********************************
	 */
	@Override
	public final void subscribe(FlowableEmitter<T> emitter) {
		//this.emitter = emitter;
		this.emitter = emitter.serialize();

		try {
			// fire start event
			for (Listener l : listeners) {
				l.onStart();
			}

			// execute process
			execute();

			// finish process
			emitter.onComplete();

		} catch (ProcessCancelledException t) {
			logger.trace("interrupted in execute() {}", t.getMessage());
		} catch (Throwable t) {
			emitter.onError(t);
		} finally {
			doPostProcess();
		}
	}

	/**
	 **********************************
	 * implements data processing.
	 * <br>
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
	protected final void append(Iterable<T> values) throws InterruptedException {
		if (values != null) {
			Iterator<T> it = values.iterator();
			while (it.hasNext()) {
				append(it.next());
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
	protected final void append(T value) throws ProcessCancelledException {
		try {
			boolean isCancelled = false;
			while (!(isCancelled = emitter.isCancelled()) && emitter.requested() == 0L) {
				Thread.sleep(50);
			}
			if (isCancelled) {
				throw new ProcessCancelledException("already cancelled(" + value + ")");
			}
			emitter.onNext(value);
		} catch (InterruptedException ie) {
			throw new ProcessCancelledException("interrupted append(" + value + ")");
		}
	}

	/**
	 **********************************
	 * call process finished(completed/close/exception).
	 **********************************
	 */
	protected void postProcess() {
		// do nothing.
	}

	/**
	 **********************************
	 *
	 * @return
	 **********************************
	 */
	public final ClosableResult<T> run() {
		return new ClosableResult<>(this);
	}

	/**
	 **********************************
	 *
	 **********************************
	 */
	private final void doPostProcess() {
		try {
			// TODO 反転は不要？
			//Collections.reverse(listeners);
			for (Listener l : listeners) {
				l.onFinish();
			}
		} finally {
			postProcess();
		}
	}

	/**
	 **********************************
	 * set event listener.
	 * <br>
	 * if already set listener, overwrite setting.
	 * @param listener event listener
	 **********************************
	 */
	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	/**
	 **********************************
	 * event listener interface
	 **********************************
	 */
	public static interface Listener extends EventListener {

		/**
		 * before process start
		 */
		void onStart();

		/**
		 * after process finished
		 */
		void onFinish();
	}
}
