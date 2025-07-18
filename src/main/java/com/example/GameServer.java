package com.example; // *クラスのグループを整理するためのもの

import static spark.Spark.*; // Sparkフレームワークの静的インポート *クラス名なしでメソッドを呼び出せるようにする
import java.sql.*; // データベース接続用のライブラリ
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.config.Appconfig;
import com.google.gson.Gson; // JSON処理用のライブラリ
import com.google.gson.reflect.TypeToken; // 型の変換に使用

import java.lang.reflect.Type;

public class GameServer {
    public static void main(String[] args) {
        Appconfig.loadEnv(); // .envを読み込む

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "4567"));
        port(port); // Spark JavaのポートをRender環境のPORTに設定

        // publicディレクトリの静的ファイル提供可処理
        staticFiles.location("/public");

        get("/", (req, res) -> {
            res.redirect("/index.html");
            return null;
        });

        // postgre接続情報
        // String host = System.getenv("DB_HOST");
        // String dbport = System.getenv("DB_PORT");
        // String dbName = System.getenv("DB_NAME");
        // String user = System.getenv("DB_USER");
        // String password = System.getenv("DB_PASSWORD");
        String host = "dpg-d0ttpbu3jp1c73f1o950-a";
        String dbport = "5432";
        String dbName = "ninja_game";
        String user = "demo_user";
        String password = "UUsIKJN422mRTId9QqjLkfEpr6P78pGR";

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + dbport + "/" + dbName + "?sslmode=require";

        // String url =
        // "postgresql://demo_user:UUsIKJN422mRTId9QqjLkfEpr6P78pGR@dpg-d0ttpbu3jp1c73f1o950-a/ninja_game";

        // スコアの登録(request, response)
        post("api/score", (req, res) -> {
            // クライアント(JavaScript)からjsonデータ受け取り
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> data = gson.fromJson(req.body(), type); // JSONをマップに変換

            String name = (String) data.get("name"); // プレイヤー名取得
            Object scoreObj = data.get("score"); // スコア取得

            // 名前のバリデーション処理
            if (name == null || name.trim().isEmpty() || name.length() > 50
                    || !name.matches("^[a-zA-Z0-9ぁ-んァ-ン一-龥ー_\\- ]+$")) {
                res.status(400);
                return "Invalid name";
            }

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
            // System.out.println("Received score (raw): " + data.get("score"));
            System.out.println("Parsed score: " + score);

            // データベース接続
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
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
        get("/api/ranking", (req, res) -> {
            res.type("application/json"); // レスポンスをJSONに設定
            List<Map<String, Object>> result = new ArrayList<>();

            // データベースに接続
            try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
                // sqlクエリ作成
                String sql = "SELECT name, score, created_at FROM scores ORDER BY score DESC LIMIT 10"; // スコアを降順 10名まで
                ResultSet rs = conn.createStatement().executeQuery(sql);
                while (rs.next()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("name", rs.getString("name")); // 名前取得
                    entry.put("score", rs.getInt("score")); // スコア取得
                    entry.put("created_at", rs.getTimestamp("created_at"));
                    result.add(entry);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new Gson().toJson(result); // JSONに変換して返却
        });
    }
}
