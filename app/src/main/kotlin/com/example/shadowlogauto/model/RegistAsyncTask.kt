package com.example.shadowlogauto.model

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import com.example.shadowlogauto.tensorflow_demo.Classifier
import com.example.shadowlogauto.tensorflow_demo.TensorFlowImageClassifier
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.features2d.AKAZE
import org.opencv.features2d.DescriptorMatcher
import org.opencv.imgproc.Imgproc

// 特徴量比較で使用するdistanceの数
private const val DISTANCE_NUM: Int = 5

// リーダーアイコン画像の大きさ
private const val LEADER_COLS: Int = 1939
private const val LEADER_ROWS: Int = 1092
private const val LEADER_ICON_COLS: Int = 39
private const val LEADER_ICON_ROWS: Int = 38

// カードの領域を抽出する際のアスペクト比の最大値/最小値
private const val MIN_WH_RATIO = 1.2
private const val MAX_WH_RATIO = 1.5

// tensorflow Mobilenetv2
private const val INPUT_SIZE = 224
private const val IMAGE_MEAN = 0
private const val IMAGE_STD = 255f
private const val INPUT_NAME = "Placeholder"
private const val OUTPUT_NAME = "final_result"
private val MODEL_FILE = mapOf("E" to "file:///android_asset/elf_graph.pb",
        "R" to "file:///android_asset/royal_graph.pb",
        "W" to "file:///android_asset/witch_graph.pb",
        "Nc" to "file:///android_asset/necromancer_graph.pb")
private val LABEL_FILE = mapOf("E" to "file:///android_asset/elf_labels.txt",
        "R" to "file:///android_asset/royal_labels.txt",
        "W" to "file:///android_asset/witch_labels.txt",
        "Nc" to "file:///android_asset/necromancer_labels.txt")

class RegistAsyncTask(private val inputBitmap: Bitmap,
                      private val resourceTurnBitmapList: MutableList<Bitmap>,
                      private val resourceClassBitmapList: MutableList<Bitmap>,
                      private val myClassStr: String,
                      private val assets: AssetManager) : AsyncTask<Int, Int, Int>(){
    //    RegistrationFragmentでViewの表示の変更を行うためにListenerを作成
    private lateinit var listener : Listener

    private var firstTurn: Boolean = true
    private var opponentClassChildId = 0
    private val cardMulliganList = mutableListOf<Boolean>()
    private val cardNameList = mutableListOf<String>()

    //    非同期処理を開始する前に実行される関数
    override fun onPreExecute() {
        listener.onPreSuccess()
    }

    //    onPreExecute()が完了後に、実行される関数
    override fun doInBackground(vararg params: Int?): Int {
//        ターン(先攻、後攻)を判断
        firstTurn = analysisTurn(inputBitmap)

//        相手のクラスを識別し、チェックするラジオボタンを取得
        opponentClassChildId = analysisOpponentClass(inputBitmap)

//        入力画像からカードを抽出、マリガン結果を抽出
        val cardBitmapList = cardExtraction(inputBitmap)

//        Tensorflowの処理
//        モデルファイルとラベルは、mMyClassRadioButton.textで入力された自分のクラスのものを使用
        val classifier : Classifier = TensorFlowImageClassifier.create(assets, MODEL_FILE[myClassStr], LABEL_FILE[myClassStr], INPUT_SIZE, IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME)

//        3枚のカードを識別し、結果をTextViewにセット
        for (card in cardBitmapList) {
            val cardResized = Bitmap.createScaledBitmap(card, INPUT_SIZE, INPUT_SIZE, true)
            val results = classifier.recognizeImage(cardResized)
            cardNameList.add(results[0].title)

//            TODO DEBUG用
            Log.i("ShadowLogRegist", results.toString())
        }

        return 0
    }

    //    doInBackground()が完了後に、実行される関数
    override fun onPostExecute(result: Int?) {
        listener.onSuccess(inputBitmap, firstTurn, opponentClassChildId, cardNameList, cardMulliganList)
    }

    fun setListener(listener : Listener) {
        this.listener = listener
    }

    //    RegistrationFragmentが実装するListnerを定義
    interface Listener {
        //        非同期処理を実行する前に実行する関数
        fun onPreSuccess()
        //        非同期処理を実行する関数
        fun onSuccess(bitmap: Bitmap, firstTurn: Boolean, opponentClassChildId: Int, cardNameList: MutableList<String>, cardMulliganList: MutableList<Boolean>)
    }

    //    入力画像から3枚のカードを抽出する関数
    private fun cardExtraction(bitmap: Bitmap?): MutableList<Bitmap> {
//        入力画像をMat型に変換
        val targetMat = Mat()
        Utils.bitmapToMat(bitmap, targetMat)

//        カードを抽出する際に使用する定数を定義
        val maxAreaSize = (targetMat.rows() * 0.3) * (targetMat.cols() * 0.15)
        val minAreaSize = (targetMat.rows() * 0.2) * (targetMat.cols() * 0.1)
        val maxWidthSize = targetMat.cols() * 0.2

//        morphology処理をする際のカーネルを定義
        val kernelSize = targetMat.rows() / 200
        val kernel = Mat(kernelSize, kernelSize, CvType.CV_8UC1)

//       マリガン画面の中で、カードが表示される領域を抽出
        val width1 = (targetMat.cols() * 0.18).toInt()
        val width2 = (targetMat.cols() * 0.82).toInt()
        val height1 = (targetMat.rows() * 0.05).toInt()
        val height2 = (targetMat.rows() * 0.95).toInt()
        val roi = Rect(width1, height1, width2 - width1, height2 - height1)
        val targetMatRoi = Mat(targetMat, roi)

//        TODO DEBUG用
        val dstTargetBitmap = Bitmap.createBitmap(targetMatRoi.width(), targetMatRoi.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(targetMatRoi, dstTargetBitmap)

//        閾値処理を行うために、グレースケール画像を生成
        val gray = Mat()
        Imgproc.cvtColor(targetMatRoi, gray, Imgproc.COLOR_RGB2GRAY)

//        閾値処理
        val thrTargetMat = Mat()

//        抽出したカード画像を格納するリストを定義
        val cards = mutableListOf<Bitmap>()

//        抽出したカード画像の位置を格納するリストを定義
        val cardsXywh = mutableListOf<Rect>()

        val rangeStart = Imgproc.threshold(gray, thrTargetMat, 0.0, 255.0, Imgproc.THRESH_TRIANGLE + Imgproc.THRESH_BINARY).toInt()

        for (i in (rangeStart - 20)..(rangeStart + 100) step 3) {
//            1回目のループのみImgproc.THRESH_TRIANGLE、2回目以降のループは、固定値の閾値処理を行う
            if(i != (rangeStart - 1)) {
                Imgproc.threshold(gray, thrTargetMat, i.toDouble(), 255.0, Imgproc.THRESH_BINARY)
            }

//            morphology処理
            val img = Mat()
            Imgproc.morphologyEx(thrTargetMat, img, Imgproc.MORPH_CLOSE, kernel)

//            カードの輪郭を抽出する処理
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

            for (contour in contours) {
//                輪郭内の面積を計算
                val area = Imgproc.contourArea(contour)

//                面積を元に、カードでないと判断できるものを省く
                if (maxAreaSize < area) {
                    continue
                }
                if (area < minAreaSize) {
                    continue
                }

//                縦横比を計算
                val rect = Imgproc.boundingRect(contour)
                val ratio = rect.height.toDouble() / rect.width.toDouble()

//                縦横比を元に、カードでないと判断できるものを省く
                if (ratio < MIN_WH_RATIO) {
                    continue
                }
                if (MAX_WH_RATIO < ratio) {
                    continue
                }

//                前回までのループ処理で、すでに検出しているカードの場合は省く
                var already = false
                for (cardXywh in cardsXywh) {
                    if (Math.abs(rect.x - cardXywh.x) < maxWidthSize) {
                        already = true
                        continue
                    }
                }
                if (already) {
                    continue
                }

                cardsXywh.add(rect)
            }

            if (cardsXywh.size >= 3) {
                break
            }
        }

//        xの小さい順にソート
        cardsXywh.sortWith(kotlin.Comparator { a, b ->
            (a.x - b.x)
        })

//        カード画像をcardsに格納
        for (cardXywh in cardsXywh){
            val card = Mat(targetMatRoi, cardXywh)
            val dst = Bitmap.createBitmap(card.width(), card.height(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(card, dst)
            cards.add(dst)

//            マリガン結果を抽出
            cardMulliganList.add(cardXywh.y >= targetMat.rows() / 2)
        }
        return cards
    }

    //    相手のクラスを識別する関数
    private fun analysisOpponentClass(bitmap: Bitmap): Int {
//        入力画像をMat型に変換
        val targetMat = Mat()
        Utils.bitmapToMat(bitmap, targetMat)

//        入力画像の右上部分を抽出するために、roiを生成
        val targetRoi = Rect((targetMat.cols() * 0.8).toInt(), 0, (targetMat.cols() * 0.1).toInt(), (targetMat.rows() * 0.2).toInt())

//        BitmapをMat型に変換
        val classMatList = mutableListOf<Mat>()
        resourceClassBitmapList.forEach {
            val mat = Mat()
            val dst = Mat()
            Utils.bitmapToMat(it, mat)
//            入力画像の大きさの値を元に、リーダーアイコン画像の大きさを入力画像のリーダーアイコン画像の大きさに近くする
            val width = LEADER_ICON_COLS * (targetMat.cols().toDouble() / LEADER_COLS.toDouble())
            val height = LEADER_ICON_ROWS * (targetMat.rows().toDouble() / LEADER_ROWS.toDouble())
            Imgproc.resize(mat, dst, Size(width, height))
//            Listに格納
            classMatList.add(dst)
        }

//        入力画像の右上部分を抽出
        val targetMatTrimming = Mat(targetMat, targetRoi)

//        テンプレートマッチを実施
        val mmr = mutableListOf<Core.MinMaxLocResult>()
        classMatList.forEach {
            val searchCols = targetMatTrimming.cols() - it.cols() + 1
            val searchRows = targetMatTrimming.rows() - it.rows() + 1
            val mat = Mat(searchRows, searchCols, CvType.CV_32FC1)

            Imgproc.matchTemplate(targetMatTrimming, it, mat, Imgproc.TM_CCOEFF_NORMED)
            mmr.add(Core.minMaxLoc(mat))
        }

//        テンプレートマッチの結果、最もマッチしたクラスのラジオボタンのChildIdを返す
        return mmr.indexOf(mmr.maxBy { a -> a.maxVal })
    }


    //    ターン(先攻、後攻)を識別する関数
    private fun analysisTurn(bitmap : Bitmap): Boolean {
//        BitmapをMat型に変換
        val descriptorFirst = matchDistance(resourceTurnBitmapList[0], "drawble")
        val descriptorSecond = matchDistance(resourceTurnBitmapList[1], "drawble")
        val descriptorTurn = matchDistance(bitmap, "turn")

//        マッチングのアルゴリズムを指定
        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE)

//        マッチングを実施
        val matchFirst = MatOfDMatch().apply { matcher.match(descriptorFirst, descriptorTurn, this) }
        val matchSecond = MatOfDMatch().apply { matcher.match(descriptorSecond, descriptorTurn, this) }

//        distanceを小さい順にDISTANCE_NUM個の総和を出力する関数
        val firstSum = sumDistance(matchFirst)
        val secondSum = sumDistance(matchSecond)

//        先攻の場合true、後攻の場合falseを返す
        return firstSum <= secondSum
    }

    //    特徴量を計算する関数
    private fun matchDistance(bitmap: Bitmap, target: String): Mat {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        when (target){
//            ターン(先攻、後攻)を対象とする場合
            "turn" -> {
//                左上部分を抽出
                val roi = Rect(0, 0, mat.cols() / 10, mat.rows() / 7)
                mat = Mat(mat, roi)
            }
//            drawble内のファイルを対象とする場合
            "drawble" -> {
//                何もしない
            }
        }

//        特徴点抽出を行うアルゴリズムをAKAZEに指定
        val detector = AKAZE.create()

//        特徴点を抽出
        val keypoints = MatOfKeyPoint().apply { detector.detect(mat, this) }

//        特徴量を記述し、return
        return Mat().apply { detector.compute(mat, keypoints, this) }
    }

    //    distanceを小さい順にDISTANCE_NUM個の総和を出力する関数
    private fun sumDistance(match : MatOfDMatch) : Float {
//        distanceの昇順に並び替える
        val matchList = match.toList()
        matchList.sortWith(kotlin.Comparator { a, b ->
            (a.distance - b.distance).toInt()
        })

//        distanceの小さい順にDISTANCE_NUM個抽出
        val matchListSlice = matchList.take(DISTANCE_NUM)

//        distanceのsumを計算
        var matchSum = 0f
        for (m in matchListSlice) {
            matchSum += m.distance
        }

        return matchSum
    }
}