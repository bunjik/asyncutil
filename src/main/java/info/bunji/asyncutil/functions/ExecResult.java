/*
 * Copyright 2016-2018 Fumiharu Kinoshita
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

/**
 **************************************
 * process execute result info.
 * @author f.kinoshita
 **************************************
 */
public final class ExecResult {

	private final long processedCount;

	private final Throwable throwable;

//	private final long executeTime;

	public ExecResult(long count) {
		this(count, null);
	}

	public ExecResult(long count, Throwable t) {
		this.processedCount = Math.max(count, 0);
		this.throwable = t;
	}

	public long processedCount() {
		return processedCount;
	}

	public boolean isSuccess() {
		return throwable == null;
	}

	public boolean isCancelled() {
		// implemented yet.
		return false;
	}

	public Throwable getException() {
		return throwable;
	}

	@Override
	public String toString() {
		return String.format("ExecResult: isSuccess=%s/processed=%d/exception=%s"
										,isSuccess()
										,processedCount()
										,throwable);
		//return super.toString();
	}
}
