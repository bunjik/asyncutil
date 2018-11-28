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
package info.bunji.asyncutil.functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bunji.asyncutil.AsyncProcess;
import info.bunji.asyncutil.exception.ProcessExecuteException;
import io.reactivex.FlowableEmitter;

/**
 ************************************************
 * .
 * @author f.kinoshita
 ************************************************
 */
public abstract class ExecFunc<T> {
	/** logger */
	protected Logger logger = LoggerFactory.getLogger(getClass());
	/** target process */
	private AsyncProcess<T> proc;
	/** target emitter */
	private FlowableEmitter<T> emitter;

	/**
	 **********************************
	 *
	 * @throws Exception
	 **********************************
	 */
	public abstract void run() throws Exception;

	/**
	 **********************************
	 * internal use.
	 * @param proc
	 * @param emitter
	 **********************************
	 */
	public final void accept(AsyncProcess<T> proc, FlowableEmitter<T> emitter) {
		if (proc == null || emitter == null) {
			throw new IllegalArgumentException();
		}
		validateCaller(AsyncProcess.class);
		this.proc = proc;
		this.emitter = emitter;
	}

	private void validateCaller(Class<?> clazz) throws IllegalAccessError {
		// TODO 固定ではなく、自分を呼び出したメソッドの直前のメソッドを探すべき？
		StackTraceElement elem = Thread.currentThread().getStackTrace()[3];
		logger.debug("caller={}.{}", elem.getClassName(), elem.getMethodName());
		if (!elem.getClassName().equals(clazz.getName())) {
			throw new IllegalAccessError("illegal caller.");
		}
	}

	/**
	 **********************************
	 * emit single value.
	 * @param value value
	 **********************************
	 */
	protected final void append(T value) {
		while (!proc.isDisposed()) {
			if (emitter.requested() <= 0) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new ProcessExecuteException("process disposed.", e);
				}
			} else {
				break;
			}
		}
		if (proc.isDisposed()) {
			emitter.onComplete();
			logger.debug("process disposed.");
			throw new ProcessExecuteException("process disposed.");
		}
		emitter.onNext(value);
	}
}
