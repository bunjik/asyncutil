[![Maven Central](https://img.shields.io/maven-central/v/info.bunji/asyncutil.svg)](http://mvnrepository.com/artifact/info.bunji/asyncutil)
[![Build Status](https://img.shields.io/travis/bunjik/asyncutil/master.svg)](https://travis-ci.org/bunjik/asyncutil)
[![License](http://img.shields.io/:license-apache-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Coverage Status](https://img.shields.io/coveralls/bunjik/asyncutil/master.svg)](https://coveralls.io/github/bunjik/asyncutil?branch=master)
# asyncutil
RxJava based asynchronous processing utility.

## 概要
リスト処理を容易に非同期化するためのユーティリティ。

## 利用ケース

1. 非同期にデータを順次処理したい場合
2. 大量のデータを順次処理したい場合

## Example code

### 1. process class
```Java
public class DbExportProcess extends AsyncProcess<String> {

    private Connection conn = null;

    // Async execure process.
    @Override
    public void execute() {
        conn = DriverManager.getConnection("jdbc:xxxxx:xxx");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM TEST;");
        while (rs.next()) {
            // emit value
            append(re.getString("name"));
        }
    }

    @Overide
    public void close() throws IOExcption {
      if (conn != null) {
          conn.close();
      }
    }
}
```

### 2. run processs
```Java
DbExportProcess proc = new DbExportProcess();
try (ClosableResult rersults : proc.run()) {
    for (String name : results) {

    }
} catch (IOException ioe) {
    ioe.printStackTrace();
}
```
