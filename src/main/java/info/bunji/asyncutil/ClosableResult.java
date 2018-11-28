/*
 * Copyright 2018 Fumiharu Kinoshita
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.bunji.asyncutil.functions.ExecFunc;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.schedulers.Schedulers;

/**
 ************************************************
 * aync execute result.
 *
 * <p>iterate access processed result.<br>
 * return result to immidiate, after call {@code new ClosaleResult()}.<br>
 * usage: (use try-with-resource):
 * <pre>
 * {@code
 * try (ClosaleResult<String> cr = new ClosaleResult(new AsyncSearchProc())) {
 *   for (String r : cr) {
 *     // process result.
 *   }
 * } // call close() on finally block.
 * }
 * </pre>
 * @author f.kinoshita
 * @param <T> result type
 ************************************************
 */
public final class ClosableResult<T> implements Iterable<T>, Closeable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final AsyncProcess<T> process;

	private Iterator<T> iterator;

	private volatile boolean isClosed = false;

	protected static final int DEFAULT_BUF_SIZE = 4096;

	/**
	 **********************************
	 * constructor.
	 * @param proc async process
	 **********************************
	 */
	public ClosableResult(AsyncProcess<T> proc) {
		this(proc, DEFAULT_BUF_SIZE, true);
	}

	/**
	 **********************************
	 * construct with bufsize.
	 * @param proc async process
	 * @param bufSize buffer size
	 **********************************
	 */
	public ClosableResult(AsyncProcess<T> proc, int bufSize) {
		this(proc, bufSize, true);
	}

	/**
	 **********************************
	 * constructor.
	 * @param proc async process
	 * @param isDelayError error notification delay to read error data
	 **********************************
	 */
	public ClosableResult(AsyncProcess<T> proc, boolean isDelayError) {
		this(proc, DEFAULT_BUF_SIZE, isDelayError);
	}

	/**
	 **********************************
	 * constructor.
	 * @param proc async process
	 * @param bufSize buffer size
	 * @param isDelayError indicates if the onError notification may not cut ahead of onNext notification
	 *                   on the other side of the scheduling boundary. If true a sequence ending in onError
	 *                   will be replayed in the same order as was received from upstream
	 **********************************
	 */
	public ClosableResult(AsyncProcess<T> proc, int bufSize, boolean isDelayError) {
		if (bufSize <= 0) {
			throw new IllegalArgumentException("bufSize > 0 required but it was " + bufSize);
		}
		logger.trace("bufsize={} / delayError={}", bufSize, isDelayError);

		this.process = proc;
		Flowable<T> f = Flowable.create(proc, BackpressureStrategy.ERROR)
							.observeOn(Schedulers.newThread(), isDelayError)
							//.doOnRequest(proc.execCallback)
//							.doOnTerminate(new Action() {
//								@Override
//								public void run() throws Exception {
//									logger.debug("call doOnTerminate()");
//									throw new Exception("exception in doOnTerminate()");
//								}
//							})
							.subscribeOn(Schedulers.newThread(), false);

		this.iterator = new FlowableIterable<>(f, bufSize, isDelayError).iterator();
	}

	/**
	 **********************************
	 * constructor.
	 * @param source the source Iterable sequence
	 **********************************
	 */
	public ClosableResult(Iterable<T> source) {
		this(source, DEFAULT_BUF_SIZE, true);
	}

	/**
	 **********************************
	 * コンストラクタ.
	 * @param source the source Iterable sequence
	 * @param bufSize buffer size
	 **********************************
	 */
	public ClosableResult(Iterable<T> source, int bufSize) {
		this(source, bufSize, true);
	}

	/**
	 **********************************
	 * constructor.
	 * @param source the source Iterable sequence
	 * @param bufSize buffeer size
	 * @param isDelayError indicates if the onError notification may not cut ahead of onNext notification
	 *                   on the other side of the scheduling boundary. If true a sequence ending in onError
	 *                   will be replayed in the same order as was received from upstream
	 **********************************
	 */
	public ClosableResult(Iterable<T> source, int bufSize, boolean isDelayError) {
		this.process = null;

		Flowable<T> f = Flowable.fromIterable(source)
				.observeOn(Schedulers.newThread(), isDelayError)
				.subscribeOn(Schedulers.newThread(), false);

		this.iterator = new FlowableIterable<>(f, bufSize, isDelayError).iterator();
	}

	// TODO chain process?
	public ClosableResult(List<? extends AsyncProcess<T>> pprocList) {
		throw new RuntimeException("not implemented yet.");
	}

	// instant method for lambda
	public static <T> ClosableResult<T> create(ExecFunc<T> execFunc) {
		return create(execFunc, DEFAULT_BUF_SIZE);
	}

	// instant method for lambda
	public static <T> ClosableResult<T> create(ExecFunc<T> execFunc, int bufSize) {
		return new ClosableResult<T>(new AsyncProcess<T>().setExecute(execFunc), bufSize);
	}

	// instant method for lambda
	public static <T> ClosableResult<T> create(ExecFunc<T> execFunc, boolean isDelayError) {
		return new ClosableResult<T>(new AsyncProcess<T>().setExecute(execFunc), isDelayError);
	}

	// instant method for lambda
	public static <T> ClosableResult<T> create(ExecFunc<T> execFunc, int bufSize, boolean isDelayError) {
		return new ClosableResult<T>(new AsyncProcess<T>().setExecute(execFunc), bufSize, isDelayError);
	}


	/**
	 ********************************************
	 * blocking iterable class.
	 * @param <T> element type
	 ********************************************
	 */
	private static final class FlowableIterable<T> implements Iterable<T> {
		final IteratorSubscriber<T> iterator;

		FlowableIterable(Flowable<T> source, int bufSize, boolean delayError) {
			iterator = new IteratorSubscriber<>(bufSize, delayError);
			source.subscribe(iterator);
		}

		@Override
		public Iterator<T> iterator() {
			return iterator;
		}

		/**
		 ****************************************
		 * blocking iterator class.
		 * @param <T> element type
		 ****************************************
		 */
		private static final class IteratorSubscriber<T>
										extends AtomicReference<Subscription>
										implements FlowableSubscriber<T>, Iterator<T> {

			private final BlockingQueue<T> queue;
			private final long limit;
			private final Lock lock;
			private final Condition condition;
			private volatile boolean done;
			private final boolean delayError;
			Throwable error;
			long produced;
			long bufSize;

		    IteratorSubscriber(int bufSize, boolean delayError) {
				this.queue = new LinkedBlockingQueue<>(bufSize);
				this.bufSize = bufSize;
				this.limit = bufSize - (bufSize >> 2);
				this.delayError = delayError;
				this.lock = new ReentrantLock();
				this.condition = lock.newCondition();
			}

			@Override
			public boolean hasNext() {
				for (;;) {
					boolean d = done;
					boolean isEmpty = queue.isEmpty();

					if (!d && isEmpty) {
						lock.lock();
						try {
							while (!done && queue.isEmpty()) {
								condition.await();
							}
						} catch (InterruptedException ie) {
							get().cancel();
							throw ExceptionHelper.wrapOrThrow(ie);	// internal method
						} finally {
							lock.unlock();
						}
					} else {
						if (d && isEmpty) {
							Throwable e = error;
							if (e != null) {
								throw ExceptionHelper.wrapOrThrow(e);
							}
							return false;
						}
						return true;
					}
				}
			}

			@Override
			public T next() {
				if (hasNext()) {
					T value = queue.poll();
					long p = produced + 1;
					if (p == limit) {
						produced = 0;
						get().request(p);
					} else {
						produced = p;
					}
					return value;
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
		        throw new UnsupportedOperationException("remove");
			}

			void signalConsumer() {
				lock.lock();
				try {
					condition.signalAll();
				} finally {
					lock.unlock();
				}
			}

			@Override
			public void onNext(T t) {
				if (!queue.offer(t)) {
					SubscriptionHelper.cancel(this);
					// FIXME
					onError(new MissingBackpressureException("Queue full?!"));
				} else {
					signalConsumer();
				}
			}

			@Override
			public void onError(Throwable t) {
				error = t;
				done = true;
				if (!delayError) {
					queue.clear();
				}
				signalConsumer();
			}

			@Override
			public void onComplete() {
				done = true;
				signalConsumer();
			}

			@Override
			public void onSubscribe(Subscription s) {
				//logger.debug("call onSubscribe()");
				set(s);
				s.request(bufSize);
			}
		}
	}

	/**
	 **********************************
	 * get result list.
	 * <br>
	 * blocking method.
	 * @return result list
	 **********************************
	 */
	public List<T> toList() {
		List<T> results = new ArrayList<>();
		for (T val : this) {
			results.add(val);
		}
		return results;
	}

	@Override
	public Iterator<T> iterator() {
		return iterator;
	}

	@Override
	public final void close() throws IOException {
		if (!isClosed) {
			isClosed = true;
			logger.trace("{}.close()", getClass().getSimpleName());
			if (process != null) {
				if (!process.isDisposed()) {
					process.dispose();
				}
			}
		}
	}
}
