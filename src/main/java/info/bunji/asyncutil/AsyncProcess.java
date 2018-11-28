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

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bunji.asyncutil.functions.ExecFunc;
import info.bunji.asyncutil.functions.ListenerFunc;
import info.bunji.asyncutil.functions.PostProcFunc;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.disposables.Disposable;

/**
 ************************************************
 * aync process base class.(lambda support)
 * @param <T> result type
 * @author f.kinoshita
 ************************************************
 */
public class AsyncProcess<T> implements FlowableOnSubscribe<T>, Disposable {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private FlowableEmitter<T> emitter;

	private AtomicBoolean isDisposed = new AtomicBoolean(false);

	private ExecFunc<T> execCallback = null;

	private PostProcFunc postCallback = PostProcFunc.NOP;

	private ListenerFunc startCallback = ListenerFunc.NOP;

	private ListenerFunc endCallback = ListenerFunc.NOP;

	public static final <T> AsyncProcess<T> create(ExecFunc<T> callback) {
		return new AsyncProcess<T>().setExecute(callback);
	}

	/**
	 **********************************
	 * set execute function.
	 * @param callback executeFunction
	 * @return this instance
	 **********************************
	 */
	public AsyncProcess<T> setExecute(ExecFunc<T> callback) {
		if (callback == null) {
			throw new IllegalArgumentException("callback  function can not null.");
		}
		if (execCallback != null) {
			throw new IllegalStateException("execute function already set.");
		}
		execCallback = callback;
		return this;
	}

	/**
	 **********************************
	 * set callback for post process.
	 * @param callback postproces call back
	 * @return this instance
	 **********************************
	 */
	public AsyncProcess<T> setPostProcess(PostProcFunc callback) {
		postCallback = callback;
		return this;
	}

	public AsyncProcess<T> doStart(ListenerFunc callback) {
		startCallback = callback;
		return this;
	}

	public AsyncProcess<T> doFinish(ListenerFunc callback) {
		endCallback = callback;
		return this;
	}

	@Override
	public final void subscribe(FlowableEmitter<T> emitter) {
		//logger.trace("call AsyncProc.subscribe()");
		this.emitter = emitter;
		this.emitter.setDisposable(this);
		try {
			execCallback.accept(this, this.emitter);

			// exec start listener
			startCallback.run();

			// execute Process
			execCallback.run();

			this.emitter.onComplete();
		} catch (Throwable t) {
			if (this.emitter.tryOnError(t)) {
				this.emitter.onError(t);
			} else {
				logger.trace("AsyncProc cancelled.");
			}
		}
	}

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
	 * @param delayError indicates if the onError notification may not cut ahead of onNext notification
	 *        on the other side of the scheduling boundary. If true a sequence ending in onError
	 *        will be replayed in the same order as was received from upstream
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
	 * @param delayError indicates if the onError notification may not cut ahead of onNext notification
	 *        on the other side of the scheduling boundary. If true a sequence ending in onError
	 *        will be replayed in the same order as was received from upstream
	 * @return iteratable result
	 **********************************
	 */
	public final ClosableResult<T> run(int bufSize, boolean delayError) {
		return new ClosableResult<>(this, bufSize, delayError);
	}

	@Override
	public final void dispose() {
		if (!isDisposed.get()) {
			isDisposed.set(true);
			//logger.trace("AsyncProcess.dispose() isDisposed={}", isDisposed);
			doPostProcess();
		}
	}

	@Override
	public final boolean isDisposed() {
		//logger.trace("AsyncProcess.isDisposed()={}", isDisposed);
		return isDisposed.get();
	}

	/**
	 **********************************
	 * call process finished.
	 **********************************
	 */
	private final void doPostProcess() {
		postCallback.run();
		endCallback.run();
	}
}
