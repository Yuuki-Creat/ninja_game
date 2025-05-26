const player = document.getElementById('player') //指定したIDのHTML要素を取得
const obstacle = document.getElementById('obstacle')
const scoreDisplay = document.getElementById('score')
const playerNameInput = document.getElementById('playerName')

let isJumping = false;  //ジャンプ中かどうかの判定
let gravity = 1.0       //重力係数
let playerBottom = 0;   //プレイヤーのY座標
let jumpHeight = 100    //ジャンプの高さ
let score = 0;
let currentFrame = 1;
let runAnimationInterval;

// ゲームオーバー時に送信し、ランキング取得
function gameOver() {
    alert("ゲームオーバー！\nスコア: " + score);
    scoreDisplay.textContent = score;
    // 非同期処理 送信完了後fetchRankingを実行
    sendScore().then(() => {
        fetchRanking();
    });

    score = 0;
    scoreDisplay.textContent = score; // スコアを表示するUIを更新
    obstacle.style.left = '800px'; // 障害物リセット

    // 名前入力欄を有効化（再スタート待ち）
    playerNameInput.disabled = false;
    playerNameInput.focus(); // 入力欄にフォーカス(すぐ入力できるようにする)

    const gameArea = document.getElementById("game");
    gameArea.classList.remove("scroll-Background");
    const player = document.getElementById("player");
    player.classList.remove("running");
    // stopRunningAnimation();
}

//障害物の移動と当たり判定
function startObstacle() {
    let obstacleLeft = 800; //障害物の初期位置を設定

    // 一定間隔で処理を実行(setInterval)
    let timer = setInterval(() => {
        // 5点ごとにスピードアップ（ランダム性あり）
        let baseSpeed = 5 + Math.floor(score / 5); // スコア5ごとに加速
        let obstacleSpeed = Math.floor(Math.random() * 6) + baseSpeed;

        if (obstacleLeft < 0) { //障害物をリセット
            obstacleLeft = 800; //リスタート
            score++; // スコア加算
            scoreDisplay.textContent = score; // スコア表示更新
        } else { //障害物を左へ移動
            obstacleLeft -= obstacleSpeed;
            obstacle.style.left = obstacleLeft + 'px'; // スタイル更新(cssのleftプロパティ更新)
        }

        //当たり判定（シンプルな短形動作）
        if (
            obstacleLeft < 100 && obstacleLeft > 50 && //障害物がプレイヤーの位置にある
            playerBottom < 50 //プレイヤーの高さが障害物より低い
        ) {
            clearInterval(timer); // 障害物の動作を停止
            gameOver(); // ゲームオーバーを処理
        }
    }, 20); // 20ミリ秒ごとに5px左へ移動
}

function fetchRanking() {
    fetch('http://localhost:4567/api/ranking') // サーバーからランキングデータ取得(エンドポイントにGETリクエストを送信)
    .then(response => response.json()) // 取得したデータをJSONに変換(オブジェクトに変換)
    .then(data => {
        const rankingDiv = document.getElementById('ranking'); // ランキング表示用の要素取得
        rankingDiv.innerHTML = ''; // 既存のランキングをクリア

        // 取得したランキングデータをループ処理
        data.forEach((entry, index) => {
            const item = document.createElement('div'); // 新しいdiv要素を作成
            item.textContent = `${index + 1}位: ${entry.name} - ${entry.score}点`; // ランキング情報をセット
            item.appendChild(rankText);
            rankingDiv.appendChild(item); // 画面に追加
        });
    });
}

// スコア送信処理
function sendScore() {
    const name = document.getElementById('playerName').value.trim(); // 入力された名前を取得,前後の空白を削除
    if (!name) {
        alert("名前を入力してください");
        return Promise.resolve(); // エラー時に空のPromiseを返す(処理を継続しない)
    }

    // サーバーへPOSTリクエストを送信
    return fetch('http://localhost:4567/api/score', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name: name, score: score}) // JavaScriptオブジェクトをJSONへ変換し、送信
    });
}

// function startRunningAnimation() {
//     runAnimationInterval = setInterval(() => {
//         currentFrame = currentFrame === 1 ? 2 : 1;
//         player.style.backgroundImage = `url('images/ninja${currentFrame}.png')`;
//     }, 150); // 0.15秒ごとに画像を切り替える
// }

// function stopRunningAnimation() {
//     clearInterval(runAnimationInterval);
// }

//ジャンプ処理
function jump() {
    if (isJumping) return; //ジャンプ中なら処理しない
    isJumping = true;

    const jumpSpeed = 5 * gravity
    const intervalTime = 20;

    let upInterval = setInterval(() => {
        if (playerBottom >= jumpHeight) {
            // 上昇が終わったら下降に切り替える
            clearInterval(upInterval); //各動作が完了すると停止

            let downInterval = setInterval(() => {
                if (playerBottom <= 0) {
                    clearInterval(downInterval); // 地面に到達したら下降を終了
                    isJumping = false;
                    playerBottom = 0;
                } else {
                    playerBottom -= jumpSpeed;
                    player.style.bottom = playerBottom + 'px';
                }
            }, intervalTime);
        } else {
            // 上昇処理
            playerBottom += jumpSpeed;
            player.style.bottom = playerBottom + 'px';
        }
    }, intervalTime); //プレイヤーの高さを20msごとに更新してジャンプを表現 20ミリ秒ごとに処理を繰り返す
}

//キー入力イベント（スペースキーでジャンプ）
document.addEventListener('keydown', (e) => { //キーボードの入力監視
    if (!isJumping) {
        if (e.code === 'Space') { //キーボードのスペースが押されたらjump()を実行
            jump();
        }
    } 
});

// タップ（クリック）でもジャンプできるように
document.getElementById('jumpbutton').addEventListener("click", function () {
    jump();
});

// ゲーム開始処理
playerNameInput.addEventListener('keydown', function (e) {
    // stopRunningAnimation(); //画像走る処理停止
    if (e.code === 'Enter') {
        const gameArea = document.getElementById("game");
        gameArea.classList.add("scroll-Background");

        const player = document.getElementById("player");
        player.classList.add("running");

        const name = e.target.value.trim();
        if (!name) {
            alert('名前を入力してください')
            return;
        }
        // ゲーム開始
        startObstacle();
        // startRunningAnimation();　// 画像走る処理開始
        // 入力欄を無効化
        playerNameInput.disabled = true;

        // フォーカス外してソフトキーボードを閉じる
        playerNameInput.blur();
        // 画面をゲームエリアにスクロール（スマホ対策
        setTimeout(() => {
            gameArea.scrollIntoView({behavior: "smooth", block: "center"});
        }, 300);
    }
});

function updateJumpButtonVisibility() {
    const isLandscape = window.matchMedia("(orientation: landscape)").matches;
    const widthOK = window.innerWidth <= 1200;

    const isMobile = /Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent);

    const jumpButton = document.getElementById("jumpbutton")
    if (isLandscape && widthOK && isMobile) {
        jumpButton.style.display = "block"
    } else {
        jumpButton.style.display = "none"
    }
}

window.addEventListener("resize", updateJumpButtonVisibility);
window.addEventListener("orientationchange", updateJumpButtonVisibility);
window.addEventListener("load", updateJumpButtonVisibility);

// 縦向き判定
function checkOrientation() {
    const isPortrait = window.matchMedia("(orientation: portrait)").matches;
    const rotateWarning = document.getElementById("rotate-warning");
    // rotateWarning.style.display = isPortrait ? "block" : "none";

    // rotate-warning内部以外を非表示にする
    const hideTargets = document.querySelectorAll(
        "body > h1, body > div:not(#rotate-warning), #playerName, #score, #ranking, h2"
    );

    if (isPortrait) {
        rotateWarning.style.display = "flex";
        hideTargets.forEach(el => el.style.display = "none");
    } else {
        rotateWarning.style.display = "none";
        hideTargets.forEach(el => el.style.display = "");
    }
}

window.addEventListener("DOMContentLoaded", checkOrientation);
window.addEventListener("resize", checkOrientation);
window.addEventListener("orientationchange", checkOrientation);
window.addEventListener("load", checkOrientation);
