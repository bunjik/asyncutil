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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 ************************************************
 * aync execute result.
 * <p>
 * iterate access processed result.<br>
 * return result to immidiate, after call {@code AsyncEXecutor.execute()}.<br>
 * <pre>
 * usage;(use try-with-resource):
 *
 * try (AsyncResults<String> as =AsyncExecutor.execute(new AsyncSearchProc())) {
 *   for (String r : as) {
 *     // process result.
 *   }
 * } // call close() on finally block.
 * </pre>
 * @author f.kinoshita
 * @param <T> result type
 ************************************************
 */
public class AsyncResult<T> implements Iterable<T>, Closeable {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private AsyncIterator<T> iterator = new AsyncIterator<>();

	private CountDownLatch latch = new CountDownLatch(1);

	private Subscriber<T> subscriber;

	private Throwable throwable = null;

	/**
	 ********************************************
	 * @param o 結果を生成するObservebleの実装クラス
	 * @param queueLimit	limit result queue size
	 ********************************************
	 */
	AsyncResult(Observable<T> o, final int queueLimit) {

		final long startTime = System.currentTimeMillis();

		subscriber = new Subscriber<T>() {

			/** processed item count */
			private long processed = 0;

			/*
			 ************************************
			 * {@inheritDoc}
			 ************************************
			 */
			@Override
			public void onCompleted() {
				logger.trace("completed: " + processed + " items.");
			}

			/*
			 ************************************
			 * {@inheritDoc}
			 ************************************
			 */
			@Override
			public void onError(Throwable e) {
				// 発生した例外を退避して処理を中断する
				logger.trace("error occurred. " + e.getMessage(), e);
				throwable = e;
			}

			/*
			 ************************************
			 * {@inheritDoc}
			 ************************************
			 */
			@Override
			public void onNext(T t) {
				processed++;

				// キューが指定サイズを超えている場合は処理を一旦停止する
				if (queueLimit > 0) {
					while (true) {
						if (queueLimit > iterator.queue.size() || isUnsubscribed()) {
							break;
						}
						logger.trace("waiting process. queue size(now:" + iterator.queue.size() + "/limit:" + queueLimit + ")");
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// do nothing;
						}
					}
				}
				// queueに要素を追加
				iterator.queue.add(t);
			}
		};

		// 別スレッドで取得処理を開始する
		o.doOnSubscribe(new Action0() {
			@Override
			public void call() {
				logger.trace("== start async process ==");
			}
		})
		.doOnUnsubscribe(new Action0() {
			@Override
			public void call() {
				latch.countDown();
				logger.trace("== finish async process(" + (System.currentTimeMillis() - startTime) + "ms) ==");
			}
		})
		.subscribeOn(Schedulers.newThread())
		.subscribe(subscriber);
	}

	/*
	 **********************************
	 * {@inheritDoc}
	 **********************************
	 */
	@Override
	public Iterator<T> iterator() {
		return iterator;
	}

	/**
	 **********************************
	 * synchronize process.
	 * <p>
	 * block async process and return when all process finished.
	 * @return result list
	 **********************************
	 */
	public List<T> block() {
		List<T> results = new ArrayList<T>();
		for (T elem : this) results.add(elem);
		return results;
	}

	/**
	 **********************************
	 * close result.
	 * <p>
	 * if process running, interrupt async process(s).<br>
	 * if process not running, this method has no effect.<br>
	 ***********************************
	 */
	@Override
	public void close() throws IOException {
		// 処理が実行中の場合は中断する。
		if (!subscriber.isUnsubscribed()) {
			subscriber.unsubscribe();
			//iterator.queue.clear();
			logger.trace("process canceled.");
		}
	}

	/**
	 ********************************************
	 * Async result iterator.
	 * <p>
	 * remove value from result, if called {@code next()}

	 * @param <E> result type
	 ********************************************
	 */
	class AsyncIterator<E> implements Iterator<E> {

		/** resultset queue */
		private Queue<E> queue = new ConcurrentLinkedQueue<>();

		private static final int NEXT_AWAIT_MS = 10;

		/*
		 ****************************************
		 * {@inheritDoc}
		 ****************************************
		 */
		@Override
		public boolean hasNext() {
			boolean ret = true;
			boolean isLoaded = false;
			while (true) {
				if (throwable != null) {
					// 例外を検知したら、即座に中断
					Exceptions.propagate(throwable);
				}
				ret = queue.isEmpty();
				if (!ret || (ret && isLoaded)) break;

				try {
					// 結果の取得が未完了かつ、返却結果がない場合は一定時間待つ
					isLoaded = latch.await(NEXT_AWAIT_MS, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					//Exceptions.propagate(e);
				}
			};
			return !ret;
		}

		/*
		 ****************************************
		 * {@inheritDoc}
		 ****************************************
		 */
		@Override
		public E next() {
			if (hasNext()) {
				// 値を取得して削除
				return queue.poll();
			}
			throw new NoSuchElementException();
		}

		/*
		 ****************************************
		 * {@inheritDoc}
		 ****************************************
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
