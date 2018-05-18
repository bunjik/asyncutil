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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Action;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.processors.UnicastProcessor;
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
public final class ClosableResult<T> implements Closeable, Iterable<T> {

	@SuppressWarnings("unused")
	private Logger logger = LoggerFactory.getLogger(getClass());

	private Subscription subscription = null;

	/** result subscriber and Iterator. */
	private BlockingSubscriber it;

	private boolean isDelayError = true;

	private RuntimeException exception = null;

	volatile boolean done = false;

	private static final int OBSERVE_BUF = 1024;

	private static final int DEFAULT_QUEUE_SIZE = 2048;

	private final Action finallyAction = new Action() {
		@Override
		public void run() throws Exception {
			//logger.trace("call doFinally() remain queue={} done={}", it.queueSize(), done);
			done = true;
		}
	};

	/**
	 **********************************
	 * execute process.
	 * @param asyncProc async process
	 **********************************
	 */
	public ClosableResult(AsyncProcess<T> asyncProc) {
		this(asyncProc, DEFAULT_QUEUE_SIZE, true);
	}

	/**
	 **********************************
	 * execute process.
	 * @param asyncProc async process
	 * @param delayError indicates if the onError notification may not cut ahead of onNext notification on the other side of the scheduling boundary. If true a sequence ending in onError will be replayed in the same order as was received from upstream
	 **********************************
	 */
	public ClosableResult(AsyncProcess<T> asyncProc, boolean delayError) {
		this(asyncProc, DEFAULT_QUEUE_SIZE, delayError);
	}

	/**
	 **********************************
	 * execute process.
	 * @param asyncProc async process
	 * @param bufSize size of buffer
	 **********************************
	 */
	public ClosableResult(AsyncProcess<T> asyncProc, int bufSize) {
		this(asyncProc, bufSize, true);
	}

	/**
	 **********************************
	 * execute process.
	 * @param asyncProc async process
	 * @param bufSize size of buffer
	 * @param delayError indicates if the onError notification may not cut ahead of onNext notification on the other side of the scheduling boundary. If true a sequence ending in onError will be replayed in the same order as was received from upstream
	 **********************************
	 */
	public ClosableResult(AsyncProcess<T> asyncProc, int bufSize, boolean delayError) {
		this(UnicastProcessor.create(asyncProc, BackpressureStrategy.ERROR), bufSize, delayError, Schedulers.newThread());
	}

	/**
	 **********************************
	 * result from Iterarable instance.
	 * @param source iterable value
	 **********************************
	 */
	public ClosableResult(Iterable<T> source) {
		this(source, DEFAULT_QUEUE_SIZE);
	}

	/**
	 **********************************
	 * result from Iterarable instance.
	 * @param source iterable values
	 * @param bufSize the size of the buffer size
	 **********************************
	 */
	public ClosableResult(Iterable<T> source, int bufSize) {
		this(UnicastProcessor.fromIterable(source), bufSize, false, Schedulers.newThread());
	}

	/**
	 **********************************
	 * execute frowable.
	 * @param flowable execute flowable instance
	 * @param bufSize the size of the buffer size
	 * @param delayError indicates if the onError notification may not cut ahead of onNext notification on the other side of the scheduling boundary. If true a sequence ending in onError will be replayed in the same order as was received from upstream
	 * @param scheduler execute scheduler
	 **********************************
	 */
	private ClosableResult(Flowable<T> flowable, int bufSize, boolean delayError, Scheduler scheduler) {
		if (bufSize <= 0) {
			closeQuietly();
			throw new IllegalArgumentException("buffer size is greater than 0.");
		}

		isDelayError = delayError;
		it = new BlockingSubscriber(bufSize);
		flowable.observeOn(scheduler, delayError, OBSERVE_BUF)
				.subscribeOn(Schedulers.newThread())
				.doFinally(finallyAction)
				.subscribe(it);
	}

	/**
	 **********************************
	 *
	 * @param procList
	 **********************************
	 */
	public ClosableResult(Collection<? extends AsyncProcess<T>> procList) {
		if (procList == null || procList.isEmpty()) {
			closeQuietly();
			throw new IllegalArgumentException("process list is null or empty.");
		}

		List<Flowable<T>> procs = new ArrayList<>();
		for (AsyncProcess<T> proc : procList) {
			procs.add(UnicastProcessor.create(proc, BackpressureStrategy.ERROR));
		}

		isDelayError = true;
		it = new BlockingSubscriber(128);
		UnicastProcessor
				.merge(procs, 1)
				.subscribeOn(Schedulers.newThread())
				.doFinally(finallyAction)
				.subscribe(it);
	}

	/*
	 **********************************
	 * (non Javadoc)
	 * @see java.lang.Iterable#iterator()
	 **********************************
	 */
	@Override
	public Iterator<T> iterator() {
		return it;
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

	/*
	 **********************************
	 * (non Javadoc)
	 * @see java.io.Closeable#close()
	 **********************************
	 */
	@Override
	public final synchronized void close() throws IOException {
		if (!done) {
			//logger.trace("call close()");
			subscription.cancel();
		}
	}

	/*
	 ********************************************
	 * (non Javadoc)
	 * @see java.lang.Object#finalize()
	 ********************************************
	 */
	@Override
	protected void finalize() throws Throwable {
		if (subscription != null) {
			closeQuietly();
		}
		super.finalize();
	}

	private void closeQuietly() {
		try {
			close();
		} catch (Throwable t) {
			// do nothing.
		}
	}

	/**
	 ********************************************
	 * reslt queue subscriber class.
	 ********************************************
	 */
	private final class BlockingSubscriber implements Subscriber<T>, Iterator<T> {

		/** logger */
		private Logger logger = LoggerFactory.getLogger(getClass());
		/** result queue */
		private BlockingQueue<T> queue;
		/** next value */
		private volatile T nextVal = null;

		private BlockingSubscriber(int bufSize) {
			queue = new LinkedBlockingQueue<>(bufSize);
		}

		/*
		 ******************************
		 * (non Javadoc)
		 * @see org.reactivestreams.Subscriber#onSubscribe(org.reactivestreams.Subscription)
		 ******************************
		 */
		@Override
		public void onSubscribe(Subscription s) {
			//logger.trace("call onSubscribe()");
			subscription = s;
			subscription.request(Long.MAX_VALUE);
		}

		/*
		 ******************************
		 * (non Javadoc)
		 * @see org.reactivestreams.Subscriber#onNext(java.lang.Object)
		 ******************************
		 */
		@Override
		public void onNext(T t) {
			try {
				while (!queue.offer(t, 500, TimeUnit.MILLISECONDS)) {
					logger.trace("wait for queue space.");;
				}
			} catch (InterruptedException e) {
				logger.trace("interrupted onNext({})", t);
				done = true;
			}
		}

		/*
		 ******************************
		 * (non Javadoc)
		 * @see org.reactivestreams.Subscriber#onError(java.lang.Throwable)
		 ******************************
		 */
		@Override
		public void onError(Throwable t) {
			//logger.trace("call onError({}} msg={} remain queue={}", t.getClass().getSimpleName(), t.getMessage(), queue.size());
			exception = ExceptionHelper.wrapOrThrow(t);
			if (!isDelayError) {
				queue.clear();
				done = true;
			}
		}

		/*
		 ******************************
		 * (non Javadoc)
		 * @see org.reactivestreams.Subscriber#onComplete()
		 ******************************
		 */
		@Override
		public void onComplete() {
			// do nothing.
			//logger.trace("call onComplete()");
		}

		/*
		 ******************************
		 * (non Javadoc)
		 * @see java.util.Iterator#hasNext()
		 ******************************
		 */
		@Override
		public boolean hasNext() {
			while (true) {
				try {
					nextVal = queue.poll(100, TimeUnit.MILLISECONDS);
					if (nextVal != null) {
						break;
					}
				} catch (InterruptedException ie) {
					//logger.trace("interrupted in hasNext()");
				}

				if (exception != null) {
					if (!isDelayError || (isDelayError && queue.isEmpty())) {
						//logger.trace("in hasNext() throw Exception={}", exception.getClass().getSimpleName());
						throw exception;
					}
					//logger.trace("continue hasNext() exception={} queue={} nextVal={}", exception != null, queue.size(), nextVal);
				} else if (done) {
					break;
				}
			}
			return nextVal != null;
		}

		/*
		 ******************************
		 * (non Javadoc)
		 * @see java.util.Iterator#next()
		 ******************************
		 */
		@Override
		public T next() {
			try {
				if (nextVal == null) {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
				}
				return nextVal;
			} finally {
				nextVal = null;
			}
		}

		/*
		 ******************************
		 * (non Javadoc)
		 * @see java.util.Iterator#remove()
		 ******************************
		 */
		@Override
		public void remove() {
			nextVal = null;
		}
	}
}
