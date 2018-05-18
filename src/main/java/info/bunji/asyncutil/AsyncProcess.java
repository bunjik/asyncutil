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

import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.disposables.Disposable;

/**
 ************************************************
 * aync process base class.
 *
 * @param <T> result type
 * @author f.kinoshita
 ************************************************
 */
public abstract class AsyncProcess<T> implements FlowableOnSubscribe<T>, Disposable {

	/** logger */
	protected Logger logger = LoggerFactory.getLogger(getClass());

	private FlowableEmitter<T> emitter;

	protected Set<Listener> listeners = new LinkedHashSet<>();

	private volatile boolean isDisposed = false;

	public AsyncProcess() {
	}

	/**
	 **********************************
	 * implements data processing.
	 * <br>
	 * data process and emit values in this method.<br>
	 * if call {@code append(T)} or {@code append(Iterable<T>)}, apply values to result.
	 *
	 * @throws Exception exception
	 **********************************
	 */
	protected abstract void execute() throws Exception;

	/**
	 **********************************
	 * execute process.
	 * @return iteratable result
	 **********************************
	 */
	public final ClosableResult<T> run() {
		return run(true);
	}

	/**
	 **********************************
	 * execute process.
	 * @param delayError indicates if the onError notification may not cut ahead of onNext notification on the other side of the scheduling boundary. If true a sequence ending in onError will be replayed in the same order as was received from upstream
	 * @return iteratable result
	 **********************************
	 */
	public final ClosableResult<T> run(boolean delayError) {
		return new ClosableResult<>(this, delayError);
	}

	/**
	 **********************************
	 * execute process.
	 * @param bufSize the size of the buffer size
	 * @return iteratable result
	 **********************************
	 */
	public final ClosableResult<T> run(int bufSize) {
		return new ClosableResult<>(this, bufSize);
	}

	/**
	 **********************************
	 * execute process.
	 * @param bufSize the size of the buffer size
	 * @param delayError indicates if the onError notification may not cut ahead of onNext notification on the other side of the scheduling boundary. If true a sequence ending in onError will be replayed in the same order as was received from upstream
	 * @return iteratable result
	 **********************************
	 */
	public final ClosableResult<T> run(int bufSize, boolean delayError) {
		return new ClosableResult<>(this, bufSize, delayError);
	}

	/*
	 **********************************
	 * (non Javadoc)
	 * @see io.reactivex.FlowableOnSubscribe#subscribe(io.reactivex.FlowableEmitter)
	 **********************************
	 */
	@Override
	public final void subscribe(FlowableEmitter<T> emitter) {
		this.emitter = emitter.serialize();
		this.emitter.setDisposable(this);

		try {
			// fire start event
			for (Listener l : listeners) {
				l.onStart();
			}

			// execute process
			execute();

			// finish process
			this.emitter.onComplete();

		} catch (InterruptedException t) {
			logger.trace("interrupted in execute() {}", t.getMessage());
		} catch (ProcessCancelledException t) {
			logger.trace("cancelled in execute() {}", t.getMessage());
		} catch (Throwable t) {
			logger.trace("exception in execute() {}", t.getClass().getSimpleName());
			this.emitter.onError(t);
		}
	}

	/**
	 **********************************
	 * emit values to AsyncResult.
	 * <br>
	 * remove values if emitted.
	 *
	 * @param values process result values
	 **********************************
	 */
	protected final void append(Iterable<T> values) throws ProcessCancelledException {
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
			while (!isDisposed && emitter.requested() == 0L) {
				Thread.sleep(100);
			}
			if (isDisposed) {
				//logger.debug("dispose in append(" + value + ")");
				throw new ProcessCancelledException("disposed append(" + value + ")");
			}
			emitter.onNext(value);
		} catch (InterruptedException ie) {
			//logger.debug("interrupted in append({})", value);
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

	/*
	 **********************************
	 * (non Javadoc)
	 * @see io.reactivex.disposables.Disposable#dispose()
	 **********************************
	 */
	@Override
	public final void dispose() {
		//logger.trace("call dispose() isCancelled={}", emitter.isCancelled());
		doPostProcess();
		isDisposed = true;
	}

	/*
	 **********************************
	 * (non Javadoc)
	 * @see io.reactivex.disposables.Disposable#isDisposed()
	 **********************************
	 */
	@Override
	public final boolean isDisposed() {
		//logger.trace("call isDisposed() isCancelled={}", emitter.isCancelled());
		return isDisposed;
	}

	/**
	 **********************************
	 * call process finished.
	 **********************************
	 */
	private final void doPostProcess() {
		try {
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
	 * event listener interface.
	 **********************************
	 */
	public static interface Listener extends EventListener {

		/**
		 * before process start.
		 */
		void onStart();

		/**
		 * after process finished.
		 */
		void onFinish();
	}
}
