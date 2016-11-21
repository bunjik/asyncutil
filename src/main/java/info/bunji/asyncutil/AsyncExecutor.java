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
import java.util.Arrays;
import java.util.List;

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

	//private static Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

	private static final int DEFAULT_MAX_CONCURRENT = Runtime.getRuntime().availableProcessors() * 2;

	private AsyncExecutor() {
		// do nothing.
	}

	/**
	 **********************************
	 *
	 * @return builder
	 **********************************
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
	 * @param asyncProc async process
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(AsyncProcess<T> asyncProc) {
		return AsyncExecutor.builder()
				.execute(asyncProc);
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
		return AsyncExecutor.builder()
				.queueLimit(queueLimit)
				.execute(asyncProc);
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
		return AsyncExecutor.builder()
				.scheduler(scheduler)
				.execute(asyncProc);
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
				.doOnTerminate(new doPostProcess(asyncProc))
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
	 * <li>max concurrent process : availableProcessors * 2</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProcList		async process list
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(Iterable<? extends AsyncProcess<T>> asyncProcList) {
		return AsyncExecutor.builder()
						.execute(asyncProcList);
	}

	/**
	 **********************************
	 * parallel async executor.
	 * <p>
	 * multiple process async exxecute with default settings.<br>
	 * <ul>
	 * <li>result queue limit : no limit</li>
	 * <li>max concurrent process : availableProcessors * 2</li>
	 * </ul>
	 * @param <T> result type
	 * @param asyncProcList		async process list
	 * @param scheduler 		process execute scheduler
	 * @return iterable process result
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(Iterable<? extends AsyncProcess<T>> asyncProcList, Scheduler scheduler) {
		return AsyncExecutor.builder()
					.scheduler(scheduler)
					.execute(asyncProcList);
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
	public static <T> AsyncResult<T> execute(Iterable<? extends AsyncProcess<T>> asyncProcList, int maxConcurrent) {
		return AsyncExecutor.builder()
					.maxConcurrent(maxConcurrent)
					.execute(asyncProcList);
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
	public static <T> AsyncResult<T> execute(Iterable<? extends AsyncProcess<T>> asyncProcList, int maxConcurrent, int queueLimit) {
		return AsyncExecutor.builder()
				.maxConcurrent(maxConcurrent)
				.queueLimit(queueLimit)
				.execute(asyncProcList);
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
	public static <T> AsyncResult<T> execute(Iterable<? extends AsyncProcess<T>> asyncProcList,
												int maxConcurrent, int queueLimit, Scheduler scheduler) {
		if (maxConcurrent <= 0) {
			// interrupt process
			for (AsyncProcess<T> asyncProc : asyncProcList) {
				try {
					asyncProc.doPostProcess();
				} catch (Exception e) {}
			}
			throw new IllegalArgumentException("maxConcurrent require greater than 0.(current:" + maxConcurrent +")");
		}

		// create observable list
		List<Observable<T>> list = new ArrayList<>();
		for (AsyncProcess<T> asyncProc : asyncProcList) {
			list.add(Observable.create(asyncProc)
					.doOnUnsubscribe(new doPostProcess(asyncProc))
					.subscribeOn(scheduler)
			);
		}

		Observable<T> o = Observable.merge(list, maxConcurrent)
				.doOnUnsubscribe(new doPostProcess(asyncProcList))
				.subscribeOn(scheduler);

		// 結果が格納されるオブジェクトを返す
		return new AsyncResult<>(o, queueLimit);
	}

	/**
	 ********************************************
	 *
	 * @param <T> result type
	 ********************************************
	 */
	private static class doPostProcess implements Action0 {

		private List<AsyncProcess<?>> processList = new ArrayList<>();

		public doPostProcess(AsyncProcess<?> proc) {
			processList.add(proc);
		}

		public doPostProcess(Iterable<? extends AsyncProcess<?>> procList) {
			for (AsyncProcess<?> proc : procList) {
				processList.add(proc);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void call() {
			for (AsyncProcess<?> process : processList) {
				process.doPostProcess();
			}
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
			// do nothing.
		}

		/**
		 ******************************
		 * set process result queue limit.
		 * <p>
		 * if limit size is less than 1, result queue is no limit.
		 *
		 * @param queueLimit	limit result queue size
		 * @return this instance
		 ******************************
		 */
		public Builder queueLimit(final int queueLimit) {
			this.queueLimit = queueLimit;
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
			this.maxConcurrent = maxConcurrent;
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
			return execute(Arrays.asList(proc));
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
		public <T> AsyncResult<T> execute(Iterable<? extends AsyncProcess<T>> proc) {
			return AsyncExecutor.execute(proc, maxConcurrent, queueLimit, scheduler);
		}
	}
}
