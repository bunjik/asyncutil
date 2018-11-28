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

import org.junit.Test;

import info.bunji.asyncutil.AsyncProcess;
import info.bunji.asyncutil.AsyncTestBase;
import io.reactivex.FlowableEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;

/**
 ************************************************
 *
 * @author f.kinoshita
 ************************************************
 */
public class ExecFuncTest extends AsyncTestBase {

	@Test(expected = IllegalArgumentException.class)
	public void testAccept_nullProcs() throws Exception {
		ExecIntFunc func = new ExecIntFunc();
		func.accept(null, new EmptyFlowableEmitter());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAccept_nullEmitter() throws Exception {
		AsyncProcess<Integer> proc = new AsyncProcess<>();
		ExecIntFunc func = new ExecIntFunc();
		func.accept(proc, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAccept_nullParams() throws Exception {
		ExecIntFunc func = new ExecIntFunc();
		func.accept(null, null);
	}

	@Test(expected = IllegalAccessError.class)
	public void testAccept_illegalCaller() throws Exception {
		AsyncProcess<Integer> proc = new AsyncProcess<>();
		FlowableEmitter<Integer> emitter = new EmptyFlowableEmitter();
		ExecIntFunc func = new ExecIntFunc();
		func.accept(proc, emitter);
	}

	// test ExecFunc class
	public static class ExecIntFunc extends ExecFunc<Integer> {
		@Override
		public void run() throws Exception {
			// do nothing.
		}
	}

	private static class EmptyFlowableEmitter implements FlowableEmitter<Integer> {
		@Override
		public void onNext(Integer value) {
		}

		@Override
		public void onError(Throwable error) {
		}

		@Override
		public void onComplete() {
		}

		@Override
		public void setDisposable(Disposable d) {
		}

		@Override
		public void setCancellable(Cancellable c) {
		}

		@Override
		public long requested() {
			return 0;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public FlowableEmitter<Integer> serialize() {
			return null;
		}

		@Override
		public boolean tryOnError(Throwable t) {
			return false;
		}
	}
}
