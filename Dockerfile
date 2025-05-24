# Maven を含む Java の公式イメージを使用
FROM maven:3.9.6-eclipse-temurin-17

# 作業ディレクトリを作成
WORKDIR /app

# プロジェクトファイルをコンテナにコピー
COPY . .

# Mavenでビルド
RUN mvn clean package

# アプリ起動（JARファイル名はビルド後の成果物に合わせて変更）
CMD ["java", "-cp", "target/ninja-game-1.0-SNAPSHOT.jar", "com.example.GameServer"]
