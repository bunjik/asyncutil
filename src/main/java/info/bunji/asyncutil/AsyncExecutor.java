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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 ************************************************
 * async process executor
 *
 * @author f.kinoshita
 ************************************************
 */
public final class AsyncExecutor {

	private static Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

	private static final int DEFAULT_MAX_CONCURRENT = 10;

	private AsyncExecutor() {
		// do nothing.
	}

	/**
	 *
	 * @return builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 **********************************
	 * single async executor.
	 * <p>
	 * process async exxecute with default settings.<br>
	 * <ul>
	 * <li>result queue limit : no limit</li>
	 * <li>execute thread scheduler : newThread</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProc		async process
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(AsyncProcess<T> asyncProc) {
		return execute(asyncProc, -1, Schedulers.newThread());
	}

	/**
	 **********************************
	 * single async executor.
	 * <p>
	 * process async exxecute with default settings.<br>
	 * <ul>
	 * <li>execute thread scheduler : newThread</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProc		async process
	 * @param queueLimit	limit result queue size
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(AsyncProcess<T> asyncProc, int queueLimit) {
		return execute(asyncProc, queueLimit, Schedulers.newThread());
	}

	/**
	 **********************************
	 * single async executor.
	 * <p>
	 * process async exxecute with default settings.<br>
	 * <ul>
	 * <li>result queue limit : no limit</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProc		async process
	 * @param scheduler 	process execute scheduler
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(AsyncProcess<T> asyncProc, Scheduler scheduler) {
		return execute(asyncProc, -1, scheduler);
	}

	/**
	 **********************************
	 * single async executor.
	 * <p>
	 * @param <T> result type
	 * @param asyncProc		async process
	 * @param queueLimit	limit result queue size
	 * @param scheduler		process execute scheduler
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(final AsyncProcess<T> asyncProc, int queueLimit, Scheduler scheduler) {
		// 非同期処理の監視用オブジェクトの生成
		Observable<T> o = Observable.create(asyncProc)
				.doOnTerminate(new OnTerminate(asyncProc))
				.subscribeOn(scheduler);

		// 結果が格納されるオブジェクトを返す
		return new AsyncResult<>(o, queueLimit);
	}

	/**
	 **********************************
	 * parallel async executor.
	 * <p>
	 * multiple process async exxecute with default settings.<br>
	 * <ul>
	 * <li>result queue limit : no limit</li>
	 * <li>execute thread scheduler : newThread</li>
	 * <li>max concurrent process : {@value #DEFAULT_MAX_CONCURRENT}</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProcList		async process list
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(Iterable<AsyncProcess<T>> asyncProcList) {
		return execute(asyncProcList, DEFAULT_MAX_CONCURRENT);
	}

	/**
	 **********************************
	 * parallel async executor.
	 * <p>
	 * multiple process async exxecute with default settings.<br>
	 * <ul>
	 * <li>result queue limit : no limit</li>
	 * <li>max concurrent process : {@value #DEFAULT_MAX_CONCURRENT}</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProcList		async process list
	 * @param scheduler 		process execute scheduler
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(Iterable<AsyncProcess<T>> asyncProcList, Scheduler scheduler) {
		return execute(asyncProcList, DEFAULT_MAX_CONCURRENT, -1, scheduler);
	}

	/**
	 **********************************
	 * parallel async executor.
	 * <p>
	 * multiple process async exxecute with default settings.<br>
	 * <ul>
	 * <li>result queue limit : no limit</li>
	 * <li>execute thread scheduler : newThread</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProcList		async process list
	 * @param maxConcurrent		max concurrent execute process
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(Iterable<AsyncProcess<T>> asyncProcList, int maxConcurrent) {
		return execute(asyncProcList, maxConcurrent, -1);
	}

	/**
	 **********************************
	 * parallel async executor.
	 * <p>
	 * multiple process async exxecute with default settings.<br>
	 * <ul>
	 * <li>execute thread scheduler : newThread</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProcList		async process list
	 * @param maxConcurrent		max concurrent execute process
	 * @param queueLimit		limit result queue size
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(Iterable<AsyncProcess<T>> asyncProcList, int maxConcurrent, int queueLimit) {
		return execute(asyncProcList, maxConcurrent, queueLimit, Schedulers.io());
	}

	/**
	 **********************************
	 * parallel async executor.
	 * <p>
	 * @param <T> result type
	 * @param asyncProcList		async process list
	 * @param maxConcurrent		max concurrent execute process
	 * @param queueLimit		limit result queue size
	 * @param scheduler 		process execute scheduler
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(Iterable<AsyncProcess<T>> asyncProcList, int maxConcurrent, int queueLimit, Scheduler scheduler) {

		// create observable list
		List<Observable<T>> list = new ArrayList<>();
		for (AsyncProcess<T> asyncProc : asyncProcList) {
			list.add(Observable.create(asyncProc)
					.doOnTerminate(new OnTerminate(asyncProc))
					.subscribeOn(scheduler)
				);
		}
		Observable<T> o = Observable.merge(list, maxConcurrent);
		//Observable<T> o = Observable.mergeDelayError(list, maxConcurrent);

		// 結果が格納されるオブジェクトを返す
		return new AsyncResult<>(o, queueLimit);
	}

	/**
	 ********************************************
	 *
	 * @param <T> result type
	 ********************************************
	 */
	private static class OnTerminate implements Action0 {

		private AsyncProcess<?> process;

		public OnTerminate(AsyncProcess<?> proc) {
			process = proc;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void call() {
			process.postProcess();
			logger.trace("call doOnTerminate()");
		}
	}

	/**
	 ********************************************
	 * async executor builder
	 ********************************************
	 */
	public static class Builder {
		private int maxConcurrent = DEFAULT_MAX_CONCURRENT;
		private int queueLimit = -1; // no limit
		private Scheduler scheduler = Schedulers.newThread();

		private Builder() {
		}

		/**
		 ******************************
		 * set process result queue limit.
		 *
		 * @param queueLimit	limit result queue size
		 * @return this instance
		 ******************************
		 */
		public Builder queueLimit(final int queueLimit) {
			this.queueLimit = queueLimit > 0 ? queueLimit : 10;
			return this;
		}

		/**
		 ******************************
		 * set process execute thread type(rx.Scheduler)
		 *
		 * @param scheduler process execute scheduler
		 * @return this instance
		 ******************************
		 */
		public Builder scheduler(final Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		/**
		 ******************************
		 * set execute process max parallel size.
		 *
		 * @param maxConcurrent		max concurrent execute process
		 * @return this instance
		 ******************************
		 */
		public Builder maxConcurrent(final int maxConcurrent) {
			this.maxConcurrent = maxConcurrent > 0 ? maxConcurrent : DEFAULT_MAX_CONCURRENT;
			return this;
		}

		/**
		 ******************************
		 * execute single process.
		 *
		 * @param <T> result type
		 * @param proc async process
		 * @return iterable process result
		 ******************************
		 */
		public <T> AsyncResult<T> execute(AsyncProcess<T> proc) {
			return AsyncExecutor.execute(proc, queueLimit, scheduler);
		}

		/**
		 ******************************
		 * execute multiple processes.
		 *
		 * @param <T> result type
		 * @param proc async process list
		 * @return iterable process result
		 ******************************
		 */
		public <T> AsyncResult<T> execute(Iterable<AsyncProcess<T>> proc) {
			return AsyncExecutor.execute(proc, maxConcurrent, queueLimit, scheduler);
		}
	}
}
