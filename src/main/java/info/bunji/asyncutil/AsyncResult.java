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
import java.util.Collection;
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
import rx.Scheduler;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;

/**
 ************************************************
 * 非同期処理の結果保持クラス.
 * <br>
 * Iterableを実装しているため、拡張for文での繰り返し処理が可能。<br>
 * ただし、非同期処理の結果であることから、メソッドの戻り値として
 * このクラスが返却された時点では、すべての結果が取得できている
 * 保証はありません。<br>
 * また注意点として、Iteratorから返却されたデータはこのクラス内から
 * 削除されるため、結果セットを繰り返し利用することはできません。<br>
 * <br>
 * データの取得処理を中断したい場合(例外による中断も含む)は、
 * 明示的にclose()の呼び出す必要があります。
 * （中断しない場合、データの取得は別スレッドで継続されます）
 * <pre>
 * 実装例(try-with-resource利用時):
 * <code>
 * try (AsyncResults as = xxxxx.search(xxxx)) {
 *   for (T r : as) {
 *     // resultに対する処理を実装
 *   }
 * } // try節を抜ける際、正常/例外発生時ともに自動でclose()が呼び出される。
 * </code>
 * ※非同期処理は単体で利用すると性能が低下します。
 * したがって利用する際は注意が必要で、以下のようなケースに向いています。
 * ・大量のデータを扱う必要があり、メモリ上に全データを展開すると
 * 　問題がある場合（または件数が未知の場合）。
 * ・取得したデータを先頭から読み込むが、途中で中断することが想定される場合。
 * 　（ロジックにより必要なデータが見つかった場合に、以降のデータを破棄するケース）
 *
 *	 逆に、以下の様なケースには不向きです。
 * ・取得する件数が明らかに少ない場合（数百件程度なら同期処理が高速）
 * ・取得したデータを繰り返し利用したい場合
 * ・
 * </pre>
 * @author f.kinoshita
 * @param <T> 戻り値の型
 ************************************************
 */
public class AsyncResult<T> implements Iterable<T>, Closeable {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private AsyncIterator<T> iterator = new AsyncIterator<>();

	private CountDownLatch latch = new CountDownLatch(1);

	private Subscriber<Collection<T>> subscriber;

	private Throwable throwable = null;

	/**
	 ********************************************
	 * コンストラクタ.
	 *
	 * @param o 結果を生成するObservebleの実装クラス
	 * @param queueLimit 結果セットに蓄積できる最大件数
	 * @param scheduler 処理を実行するスレッドのスケジューラ
	 ********************************************
	 */
	AsyncResult(Observable<Collection<T>> o, final int queueLimit, Scheduler scheduler) {

		subscriber = new Subscriber<Collection<T>>() {

			/** processed item count */
			private long processed = 0;

			/*
			 ************************************
			 * @see rx.Observer#onCompleted()
			 ************************************
			 */
			@Override
			public void onCompleted() {
				logger.trace("completed: " + processed + " items.");
			}

			/*
			 ************************************
			 * @see rx.Observer#onError(java.lang.Throwable)
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
			 * 取得結果の受取時に呼び出されるメソッド.
			 * @see rx.Observer#onNext(java.lang.Object)
			 ************************************
			 */
			@Override
			public void onNext(Collection<T> t) {
				processed += t.size();

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
				iterator.queue.addAll(t);
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
				logger.trace("== finish async process ==");
			}
		})
		.subscribeOn(scheduler)
		.subscribe(subscriber);
	}

	/*
	 **********************************
	 * @see java.lang.Iterable#iterator()
	 **********************************
	 */
	@Override
	public Iterator<T> iterator() {
		return iterator;
	}

	/**
	 **********************************
	 * 処理を同期化し、終了後に結果を返す
	 * @return 処理結果のリスト
	 **********************************
	 */
	public List<T> block() {
		List<T> results = new ArrayList<T>();
		for (T elem : this) results.add(elem);
		return results;
	}

	/**
	 **********************************
	 * 結果セットのクローズ.<br>
	 * <br>
	 * 結果の取得が完了してない場合は、別スレッドで動作している
	 * 非同期処理を停止します。<br>
	 * なお、処理停止後も処理済のデータについては取得可能です。<br>
	 *
	 * @see java.io.Closeable#close()
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
	 * 非同期の処理結果を保持するイテレータ.
	 * <br>
	 * 処理結果は呼び出し側に返却した時点で、内部からは削除されます。<br>
	 * @param <E> result type
	 ********************************************
	 */
	class AsyncIterator<E> implements Iterator<E> {

		/** resultset queue */
		private Queue<E> queue = new ConcurrentLinkedQueue<>();

		private static final int NEXT_AWAIT_MS = 10;

		/*
		 ****************************************
		 * @see java.util.Iterator#hasNext()
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
		 * @see java.util.Iterator#next()
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
		 * @see java.util.Iterator#remove()
		 ****************************************
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
