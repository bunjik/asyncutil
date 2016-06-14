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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable.OnSubscribe;
import rx.Subscriber;

/**
 * 非同期に実行したい処理を実装するクラス.
 * <br>
 * getPartialResults()を実装し、データを部分的に返却することで、
 * 返却したデータから、順次呼び出し元に返却することが可能。<br>
 * getPartialResults()で明示的に終了とするまでは、切り返しメソッドが呼び出される。<br>
 * なお、この処理はデータベースのアクセスだけでなく、データを順次処理するものであれば
 * データ変換等にも利用可能である。
 *
 * @param <T> 非同期処理の結果型
 * @author f.kinoshita
 */
public abstract class AsyncProcess<T> implements OnSubscribe<List<T>> {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected Subscriber<? super List<T>> subscriber;

	/*
	 **********************************
	 *  (非 Javadoc)
	 * @see rx.functions.Action1#call(java.lang.Object)
	 **********************************
	 */
	@Override
	public final void call(Subscriber<? super List<T>> subscriber) {
		this.subscriber = subscriber;
		try {
			logger.trace("start async process.");

			/// 処理の実行
			execute();

			// 正常終了時は、ここで終了を通知する、
			if (!subscriber.isUnsubscribed()) {
				subscriber.onCompleted();
			}
		} catch (Throwable t) {
			subscriber.onError(t);
		} finally {
			logger.trace("finish async process.");
			postProcess();
		}
	}

	/**
	 **********************************
	 * データの処理メソッド.
	 *
	 * ここで、データ全体の取得や変換等の実処理を行なう。<br>
	 * 処理中に {@code append(T) } または　{@code append(List<T>) } を
	 * 呼び出すことで、処理済の結果を呼び出し元で取得可能にする。<br>
	 * 反映する単位が小さすぎるとオーバーヘッドが高くなるため、注意が必要。<br>
	 *
	 * @throws Exception exception
	 **********************************
	 */
	protected abstract void execute() throws Exception;

	/**
	 **********************************
	 * 処理済の結果リストを呼び出し元に渡す.
	 *
	 * 処理実行後、引数のリストは空にされます。
	 *
	 * @param list 追加する要素のリスト
	 **********************************
	 */
	protected final void append(List<T> list) {
		if (subscriber == null) {
			throw new IllegalStateException("before process execution.");
		}
		if (!subscriber.isUnsubscribed()) {
			subscriber.onNext(list);
			list.clear();
		}
	}

	/**
	 **********************************
	 * 処理済の結果を呼び出し元に渡す.
	 *
	 * @param entity 単一の追加要素
	 **********************************
	 */
	protected final void append(T entity) {
		if (subscriber == null) {
			throw new IllegalStateException("before process execution.");
		}
		if (!subscriber.isUnsubscribed()) {
			subscriber.onNext(Arrays.asList(entity));
		}
	}

	/**
	 **********************************
	 * 読込側で処理が中断されているかを返す.
	 *
	 * @return 中断されている場合はtrue、そうでない場合はfalseを返す。
	 **********************************
	 */
	protected boolean isInterrupted() {
		return subscriber == null ? false: subscriber.isUnsubscribed();
	}

	/**
	 **********************************
	 * 処理終了時(完了/中断/エラー)に呼び出される処理.
	 *
	 * 必要に応じてリソースの開放等を行う。<br>
	 * デフォルトは何も行いません。<br>
	 * RessultSetのクローズなど、必要に応じて実装してください。<br>
	 * なお、このメソッド内の例外は呼び出し元には通知されません。
	 **********************************
	 */
	public void postProcess() {
		logger.trace("call postProcess()");
	}
}
