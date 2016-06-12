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

import java.util.List;

import rx.Observable;

/**
 ************************************************
 * 非同期処理実行クラス
 *
 * @author f.kinoshita
 ************************************************
 */
public class AsyncExecutor {


	private AsyncExecutor() {
		// do nothing.
	}

	/**
	 **********************************
	 * 指定された処理を非同期で実行し、結果を非同期で返す.
	 *
	 * 非同期処理の実行要求を発行した時点で結果のオブジェクトを返却します。<br>
	 * 呼び出し側では、非同期の処理が完了するのを待たずに処理された結果を
	 * 順次取得できます。
	 *
	 * @param <T> 処理結果の型
	 * @param asyncProc 非同期で実行される処理クラスのインスタンス
	 * @return 処理結果の反復子
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(AsyncProcess<T> asyncProc) {
		return execute(asyncProc, -1);
	}

	/**
	 **********************************
	 * 指定された処理を非同期で実行し、結果を非同期で返す(蓄積件数制限あり).
	 *
	 * 指定された件数以上のデータが結果データ用のキューに存在する場合、取得処理を
	 * 一時停止させ、過剰なメモリを使用しないよう制御する。<br>
	 * 結果データの処理側に対して、データ取得処理のほうが高速な場合にメモリの使用を
	 * 抑制するために利用する。<br>
	 * なお、キューの制限値が少なすぎると、キューの空きを待つ時間が多くなり、
	 * パフォーマンスの低下につながるため、最低でも1000件程度を指定すべきです。
	 *
	 * @param <T> 処理結果の型
	 * @param asyncProc 非同期で実行される処理クラスのインスタンス
	 * @param queueLimit キューに蓄積する最大サイズ
	 * @return 処理結果の反復子
	 **********************************
	 */
	public static <T> AsyncResult<T> execute(AsyncProcess<T> asyncProc, int queueLimit) {
		// 非同期処理の監視用オブジェクトの生成
		Observable<List<T>> o = Observable.create(asyncProc);

		// 結果が格納されるオブジェクトを返す
		return new AsyncResult<>(o, queueLimit);
	}
}
