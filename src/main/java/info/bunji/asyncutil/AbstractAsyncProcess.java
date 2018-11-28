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

import java.lang.reflect.Method;
import java.util.EventListener;

import info.bunji.asyncutil.functions.ExecFunc;
import info.bunji.asyncutil.functions.ListenerFunc;
import info.bunji.asyncutil.functions.PostProcFunc;

/**
 ************************************************
 * AsyncProcess base class(for non lambda).
 *
 * @author f.kinoshita
 ************************************************
 */
public abstract class AbstractAsyncProcess<T> extends AsyncProcess<T> {

	private final ExecFunc<T> execFunc;

	private static Method appendMethod;

	static {
		try {
			appendMethod = ExecFunc.class.getDeclaredMethod("append", Object.class);
			appendMethod.setAccessible(true);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 **********************************
	 * constractor.
	 **********************************
	 */
	public AbstractAsyncProcess() {
		super();

		execFunc = new ExecFunc<T>() {
			@Override
			public void run() throws Exception {
				execute();
			}
		};

		setExecute(execFunc);
		setPostProcess(new PostProcFunc() {
			@Override
			public void run() {
				postProcess();
			}
		});
	}

	/**
	 **********************************
	 * execute process.
	 * @throws Exception
	 **********************************
	 */
	protected abstract void execute() throws Exception;

	/**
	 **********************************
	 * emit value to AsyncResult.
	 * @param value process result value
	 **********************************
	 */
	protected final void append(T value) {
		try {
			appendMethod.invoke(execFunc, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 **********************************
	 * emit values to AsyncResult.<br>
	 * @param values process result values
	 **********************************
	 */
	protected final void append(Iterable<T> values) {
		if (values != null) {
			for (T value : values) {
				append(value);
			}
		}
	}

	/**
	 **********************************
	 * call exeecute finish.
	 * (do noshing default)
	 **********************************
	 */
	protected void postProcess() {
		// do nothing default.
	}

	/**
	 **********************************
	 * add event listener for execute start/finish.
	 * @param listener
	 **********************************
	 */
	public void addListener(final Listener listener) {
		doStart(new ListenerFunc() {
			@Override
			public void run() {
				listener.onStart();
			}
		})
		.doFinish(new ListenerFunc() {
			@Override
			public void run() {
				listener.onFinish();
			}
		});
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
