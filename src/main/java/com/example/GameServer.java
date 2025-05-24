package com.example; // *クラスのグループを整理するためのもの

import static spark.Spark.*; // Sparkフレームワークの静的インポート *クラス名なしでメソッドを呼び出せるようにする
import java.sql.*; // データベース接続用のライブラリ
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson; // JSON処理用のライブラリ
import com.google.gson.reflect.TypeToken; // 型の変換に使用
import java.lang.reflect.Type;

public class GameServer {
    public static void main(String[] args) {

        // publicディレクトリの静的ファイル提供可処理
        staticFiles.location("/public");

        // MySQL接続情報
        String url = "jdbc:mysql://localhost:3306/ninja_game?serverTimezone=UTC";
        String user = "root";
        String password = "MySQLDevServer001@";

        // スコアの登録(request, response)
        post("api/score", (req, res) -> {
            // クライアント(JavaScript)からjsonデータ受け取り
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> data = gson.fromJson(req.body(), type); // JSONをマップに変換

            String name = (String) data.get("name"); // プレイヤー名取得
            Object scoreObj = data.get("score"); // スコア取得

            // データ型のチェック
            Double scoreDouble = null;
            if (scoreObj instanceof Number) {
                // - instanceof は、オブジェクトの型を確認するため
                scoreDouble = ((Number) scoreObj).doubleValue(); // 数値型なら変換
            } else {
                res.status(400);
                return "Invalid score";
            }

            int score = scoreDouble.intValue();

            // ターミナルでのログチェック用
            System.out.println("Received name: " + name);
            System.out.println("Received score (raw): " + data.get("score"));
            System.out.println("Parsed score: " + score);

            // データベース接続
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                // データベースにスコア登録
                String sql = "INSERT INTO scores (name, score, created_at) VALUES (?, ? ,now())";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name); // プレイヤー名設定
                    stmt.setInt(2, score); // スコア設定
                    stmt.executeUpdate(); // 実行
                }
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "Error";
            }
            return "OK";
        });

        // ランキングの取得(request, response)
        get("/api/ranking", (_, res) -> {
            res.type("application/json"); // レスポンスをJSONに設定
            List<Map<String, Object>> result = new ArrayList<>();

            // データベースに接続
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                // sqlクエリ作成
                String sql = "SELECT name, score, created_at FROM scores ORDER BY score DESC LIMIT 10"; // スコアを降順 10名まで
                ResultSet rs = conn.createStatement().executeQuery(sql);
                while (rs.next()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("name", rs.getString("name")); // 名前取得
                    entry.put("score", rs.getInt("score")); // スコア取得
                    result.add(entry);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Gson().toJson(result); // JSONに変換して返却
        });
    }
}
