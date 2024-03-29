package com.estudos.goquiz.views

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.util.Log
//Para Debug: import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.estudos.goquiz.viewModels.GoQuizViewModel
import com.estudos.goquiz.R
import com.estudos.goquiz.databinding.ActivityMainBinding
import com.estudos.goquiz.infrastructure.Constants
import com.estudos.goquiz.infrastructure.SharedPreferencesGoQuiz
import com.google.android.material.snackbar.Snackbar


//Para Debug: private const val TAG = "MainActivity"

/**
 * Implementação de View.OnClickListener para evitar lambdas extensas no código
 *
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {


    private lateinit var binding: ActivityMainBinding

    private val quizViewModel: GoQuizViewModel by viewModels()

    /**
     * Cheat Launcher - Responsável por chamar a Activity CheattActivity e
     * receber uma Intent com o resultado do contrato passado
     *
     * A única possibilidade de resultCode implementada é Result_Ok, qualquer outro resultado não precisa
     * ser tratado
     *
     */
    private val cheatLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            /** *result.data nunca será Null, pois esta linha só é executada com Result_Ok, que em
             * CheatActivity, sempre retorna um boolean (true) */
            quizViewModel.isCheater =
                result.data!!.getBooleanExtra(Constants.STATEINTENT.EXTRA_ANSWER_SHOWN, false)
            quizViewModel.numCheatTokens--
            countTokens()
        }
    }

    /**
     * Difficulty Launcher - Responsável por chamar a Activity DifficultActivity e
     * receber uma Intent com o resultado do contrato passado
     *
     * A única possibilidade de resultCode implementada é Result_Ok, qualquer outro resultado não precisa
     * ser tratado
     *
     */
    private val difficultyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            /** *If para testar se a dificuldade escolhida é diferente da armazenada */
            if (quizViewModel.difficulty != result.data!!.getIntExtra(Constants.STATEINTENT.NUM_OF_DIFFICULTY, 1)){
                /** *result.data nunca será Null, pois esta linha só é executada com Result_Ok, que em
                 * DifficultyActivity, retorna o número da dificuldade */
                quizViewModel.difficulty =
                    result.data!!.getIntExtra(Constants.STATEINTENT.NUM_OF_DIFFICULTY, 1)
                SharedPreferencesGoQuiz(this).storeInt(Constants.KEY.NUM_OF_DIFFICULTY_KEY, quizViewModel.difficulty)
                resetGame()
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)

        //Para Debug: Log.d(TAG, "Create")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /** *Se for a primeira vez que usuário usa o app, ele é obrigado a fornecer uma dificuldade */
        if (SharedPreferencesGoQuiz(this).getInt(Constants.KEY.NUM_OF_DIFFICULTY_KEY) == 0) launcherDifficulty()
        /** *Verifica se há valor salvo na SharedPreferences e seta este valor para quizViewModel.difficulty */
        restoreDifficulty()
        /** *builda a lista de questões para iniciar o programa */
        buildQuestionList()
        /** *inicia a contagem de numeros de cheats disponíveis/usados */
        countTokens()

        /** *Aplica o Blur no Botão Cheat apenas nos Android com suporte a RenderEffect.createBlurEffect (API 31)
         * Caso não disponível aplica 0.5F no alpha do Botão Cheat*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blurCheatButton()
        else setAlphaCheatButton()

        Log.d(Constants.TAG.MAIN_ACTIVITY_TAG,"main")

        /** *Implementação com função OnClick */
        binding.textQuestion.setOnClickListener(this)
        binding.buttonTrue.setOnClickListener(this)
        binding.buttonFalse.setOnClickListener(this)
        binding.buttonNext.setOnClickListener(this)
        binding.buttonPrev.setOnClickListener(this)
        binding.buttonReset.setOnClickListener(this)
        binding.buttonCheat.setOnClickListener(this)
        binding.buttonChangeDifficulty.setOnClickListener(this)

        /** *Implementação com Lambda
         * Este código foi deixado apenas para amostragem, e foi deixado
         * de ser atualizado na versão 1.1*/
        /*
        binding.buttonTrue.setOnClickListener {
            response(it)
        }

        binding.buttonFalse.setOnClickListener {
            response(it)
        }

        binding.buttonNext.setOnClickListener {
            update()
        }

        binding.buttonPrev.setOnClickListener {
            previous(it)
        }

        binding.buttonReset.setOnClickListener {
            resetGame(it)
        }

        binding.textQuestion.setOnClickListener {
            update()
        }

        binding.buttonCheat.setOnClickListener {
            cheat()
        }
         */
    }

    //Função para implementação com lambda

    /*
    private fun response(v:View) {
        var answer = true
        if (v.id == R.id.button_False)
            answer = false

        if (quizViewModel.currentQuestionAnswer == answer) {
            quizViewModel.oneMoreHit()
            if (quizViewModel.isCheater) {
                Snackbar.make(v, R.string.judgment_toast_ok, Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(v, R.string.msgCorrect, Snackbar.LENGTH_SHORT).show()
            }
            update()
        } else {
            if (quizViewModel.isCheater) {
                Snackbar.make(v, R.string.judgment_toast_wrong, Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(v, R.string.msgWrong, Snackbar.LENGTH_LONG).show()
            }
            update()
        }
    }

    private fun cheat() {
        if (quizViewModel.nCheatTokens > 0) {
            val answerIsTrue = quizViewModel.currentQuestionAnswer
            val intent = CheatActivity.newIntent(this@MainActivity, answerIsTrue)
            cheatLauncher.launch(intent)
        } else {
            Snackbar.make(v, R.string.cantCheat, Snackbar.LENGTH_LONG).show()
        }
   }
     */

    /** *Função onClick com o @param View, que representa it na implementação com lambda */
    override fun onClick(v: View) {
        if ((v.id == R.id.button_True) || (v.id == R.id.button_False)) {

            /** *A resposta muda para false caso o botão clicado tenha sido o R.id.button_false */
            var answer = true
            if (v.id == R.id.button_False)
                answer = false

            /** *Se acertou = + um hit, mas caso houver cheat e a dificuldade for maior que 2
             * não há contagem de hit
             * Caso não, há apenas a mensagem de erro, ou de erro com cheat
             * Depois de ambos os casos, há o update*/
            if (quizViewModel.currentQuestionAnswer == answer) {
                if (quizViewModel.isCheater) {
                    Snackbar.make(v, R.string.judgment_toast_ok, Snackbar.LENGTH_SHORT).show()
                    if (quizViewModel.difficulty <= 2) quizViewModel.oneMoreHit()
                } else {
                    Snackbar.make(v, R.string.msgCorrect, Snackbar.LENGTH_SHORT).show()
                    quizViewModel.oneMoreHit()
                }
                update()
            } else {
                if (quizViewModel.isCheater) {
                    Snackbar.make(v, R.string.judgment_toast_wrong, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(v, R.string.msgWrong, Snackbar.LENGTH_LONG).show()
                }
                update()
            }
        }

        if (v.id == R.id.button_Next) {
            update()
        }

        if (v.id == R.id.button_Prev) {
            previous(v)
        }

        if (v.id == R.id.text_question) {
            update()
        }

        if (v.id == R.id.button_Reset) {
            Snackbar.make(v, R.string.msgReset, Snackbar.LENGTH_SHORT).show()
            resetGame()
        }

        /** *Caso o numero de tokens de cheats seja positivo, é chamado a CheatActivity.newIntent
         * para a criação de uma nova intent com o extra answerIsTrue
         * Caso não, é mostrado a mensagem R.string.cantCheat*/
        if (v.id == R.id.button_Cheat) {
            if (quizViewModel.numCheatTokens > 0) {
                val answerIsTrue = quizViewModel.currentQuestionAnswer
                val intent = CheatActivity.newIntent(this@MainActivity, answerIsTrue)
                cheatLauncher.launch(intent)
            } else {
                Snackbar.make(v, R.string.cantCheat, Snackbar.LENGTH_LONG).show()
            }
        }

        if (v.id == R.id.button_Change_difficulty) {
            launcherDifficulty()
        }

    }

    /** *É chamado newIntent da DifficultyActivity, levando quizViewModel.difficulty e quizViewModel.possibilityOfDifficulty()
     * como parâmetros, que serviram como extras da Intent criada*/
    private fun launcherDifficulty (){
        val intent = DifficultyActivity.newIntent(this@MainActivity, quizViewModel.difficulty, quizViewModel.possibilityOfDifficulty())
        difficultyLauncher.launch(intent)
    }

    /** *caso o update na viewModel seja possível, é atualizado o texto de R.id.textQuestion
     * Caso não, é finalizado o game */
    private fun update() {
        if (quizViewModel.update()) {
            binding.textQuestion.setText(quizViewModel.currentQuestionText)
        } else
            finishGame()
    }

    /** *Função para dar blur no botão de cheat, apenas executável a partir da API 31 */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun blurCheatButton() {
        val effect = RenderEffect.createBlurEffect(
            5.0f,
            5.0f,
            Shader.TileMode.CLAMP
        )
        binding.buttonCheat.setRenderEffect(effect)
    }

    /** *Função para setar alpha do buttonCheat executada apenas abaixo da API 31 */
    private fun setAlphaCheatButton() {
        binding.buttonCheat.alpha = 0.5F
    }

    /** *caso o previous na viewModel seja possível, é atualizado o texto de R.id.textQuestion
     * Caso não, é mostrado a mensagem R.string.msgNotPrev  */
    private fun previous(v: View) {
        if (quizViewModel.previous())
            binding.textQuestion.setText(quizViewModel.currentQuestionText)
        else
            Snackbar.make(v, R.string.msgNotPrev, Snackbar.LENGTH_LONG).show()
    }

    /** *Verifica se há valor salvo em SharedPreferencesGoQuiz e atualiza a ViewModel com este valor*/
    private fun restoreDifficulty(){
        val sharedPreferenceDifficulty = SharedPreferencesGoQuiz(this).getInt(Constants.KEY.NUM_OF_DIFFICULTY_KEY)
        if (sharedPreferenceDifficulty != 0){
            quizViewModel.difficulty = sharedPreferenceDifficulty
        }
    }

    /** *Mostra a mensagem de R.string.msgCongratulations, e desativa os botões do jogo */
    private fun finishGame() {
        val congratulation = getString(
            R.string.msgCongratulations,
            quizViewModel.currentHits,
            quizViewModel.difficulty()
        )

        binding.textTrueOrFalse.text = ""
        binding.textQuestion.text = congratulation
        binding.buttonPrev.isEnabled = false
        binding.buttonNext.isEnabled = false
        binding.buttonTrue.isEnabled = false
        binding.buttonFalse.isEnabled = false
        binding.buttonCheat.isEnabled = false
    }

    /** *Rebuilda as questões, atualiza o texto de textQuestion e o texto de textDifficulty */
    private fun buildQuestionList() {
        quizViewModel.buildQuestionList()
        binding.textQuestion.setText(quizViewModel.currentQuestionText)
        binding.textDifficulty.text = String.format(
            getString (R.string.yourDifficulty),
            getString(quizViewModel.textDifficult()))
    }

    /** *ativa os botões do jogo, faz o rebuild, recontagem e chama a resetGame da ViewModel */
    private fun resetGame() {
        quizViewModel.resetGame()
        buildQuestionList()
        countTokens()
        binding.textTrueOrFalse.setText(R.string.trueOrFalse)
        binding.buttonPrev.isEnabled = true
        binding.buttonNext.isEnabled = true
        binding.buttonTrue.isEnabled = true
        binding.buttonFalse.isEnabled = true
        binding.buttonCheat.isEnabled = true
    }

    private fun countTokens() {
        val tokenText = getString(
            R.string.countTokens,
            quizViewModel.numCheatTokens,
            quizViewModel.numTotalTokens
        )
        binding.textTokens.text = tokenText
    }

    /** *Usada para testar algumas funções */
    override fun onDestroy() {
        //Para Debug: Log.d(TAG, "Destroy")
        super.onDestroy()
    }
}