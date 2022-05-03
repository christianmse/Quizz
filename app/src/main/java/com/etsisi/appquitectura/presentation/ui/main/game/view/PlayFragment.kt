package com.etsisi.appquitectura.presentation.ui.main.game.view

import android.content.Context
import android.os.CountDownTimer
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.etsisi.appquitectura.R
import com.etsisi.appquitectura.databinding.FragmentPlayBinding
import com.etsisi.appquitectura.databinding.ItemTabHeaderBinding
import com.etsisi.appquitectura.domain.enums.GameNavType
import com.etsisi.appquitectura.domain.model.AnswerBO
import com.etsisi.appquitectura.domain.model.QuestionBO
import com.etsisi.appquitectura.presentation.common.BaseFragment
import com.etsisi.appquitectura.presentation.common.GameListener
import com.etsisi.appquitectura.presentation.common.PlayFragmentListener
import com.etsisi.appquitectura.presentation.components.ZoomOutPageTransformer
import com.etsisi.appquitectura.presentation.dialog.enums.DialogType
import com.etsisi.appquitectura.presentation.dialog.model.DialogConfig
import com.etsisi.appquitectura.presentation.ui.main.adapter.QuestionsViewPagerAdapter
import com.etsisi.appquitectura.presentation.ui.main.game.model.ItemGameMode
import com.etsisi.appquitectura.presentation.ui.main.game.viewmodel.PlayViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class PlayFragment : BaseFragment<FragmentPlayBinding, PlayViewModel>(
    R.layout.fragment_play,
    PlayViewModel::class
), PlayFragmentListener, TabLayout.OnTabSelectedListener, GameListener {

    val args: PlayFragmentArgs by navArgs()
    private val questionsAdapter by lazy { QuestionsViewPagerAdapter(this) }
    private val viewPager: ViewPager2
        get() = mBinding.viewPager
    private val readySetGoCounter by lazy {
        object : CountDownTimer(READY_SET_GO_COUNT_DOWN, READY_SET_GO_COUNTER_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                mViewModel.setNavType(GameNavType.START_GAME)
            }
        }
    }

    private companion object {
        const val READY_SET_GO_COUNTER_INTERVAL = 1000L
        const val READY_SET_GO_COUNT_DOWN = 4000L
        const val NEXT_QUESTION_DELAY = 300L
        const val SELECTED_ALPHA = 1.0F
        const val UNSELECTED_ALPHA = 0.2F
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        with(requireActivity()) {
            onBackPressedDispatcher.apply {
                addCallback(this@PlayFragment, object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (args.navType == GameNavType.GAME_MODE) {
                            onBackPressed()
                        } else {
                            this.remove()
                            isEnabled = false
                            navigator.openNavigationDialog(
                                type = DialogType.WARNING_LEAVING_GAME,
                                config = DialogConfig(title = R.string.dialog_leaving_game_title, body = R.string.dialog_leaving_game_body)
                            )
                        }
                    }
                })
            }
        }
    }

    override fun setUpDataBinding(mBinding: FragmentPlayBinding, mViewModel: PlayViewModel) {
        with(mBinding) {
            lifecycleOwner = viewLifecycleOwner
            lifecycle.addObserver(mViewModel)
            viewModel = mViewModel
            mViewModel.setNavType(args.navType)
            if (args.navType == GameNavType.PRE_START_GAME) {
                readySetGoCounter.start().also {
                    readyToStartGame.playAnimation()
                }
            }
            listener = this@PlayFragment
            this@PlayFragment.viewPager.apply {
                adapter = questionsAdapter
                isUserInputEnabled = false
                setPageTransformer(ZoomOutPageTransformer())
            }
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.customView = ItemTabHeaderBinding.inflate(layoutInflater, tab.view, false)
                    .apply {
                        lifecycleOwner = viewLifecycleOwner
                        questionIndex = position + 1
                        viewModel = mViewModel
                    }.root
                tab.view.setOnTouchListener { v, event -> true }
                setTabAlpha(tab, false)
            }.attach()
            tabLayout.apply {
                setSelectedTabIndicator(null)
                addOnTabSelectedListener(this@PlayFragment)
            }
            executePendingBindings()
        }
    }

    override fun observeViewModel(mViewModel: PlayViewModel) {
        with(mViewModel) {
            navType.observe(viewLifecycleOwner) {
                if (it == GameNavType.PRE_START_GAME) {
                    fetchInitialQuestions(args.gameMode)
                }
            }
            questions.observe(viewLifecycleOwner) {
                questionsAdapter.addData(it, mBinding.tabLayout.selectedTabPosition)
            }
        }
    }

    override fun onGameMode(item: ItemGameMode) {
        navigator.startGame(item.action)
    }

    override fun onTabSelected(tab: TabLayout.Tab?) = setTabAlpha(tab, true)

    override fun onTabUnselected(tab: TabLayout.Tab?) = setTabAlpha(tab, false)

    override fun onTabReselected(tab: TabLayout.Tab?) = setTabAlpha(tab, true)

    private fun setTabAlpha(tab: TabLayout.Tab?, selected: Boolean) {
        if (selected) {
            mViewModel.setCurrentTabIndex(tab?.position?.plus(1) ?: 1)
        }
        tab?.customView?.alpha = if (selected) SELECTED_ALPHA else UNSELECTED_ALPHA
    }

    private fun setNextQuestion() {
        with(viewPager) {
            postDelayed({
                if (currentItem < adapter?.itemCount?.minus(1) ?: 0) {
                    setCurrentItem(currentItem + 1, true)
                } else {
                    navigator.openResultFragment(mViewModel._userGameResult)
                }
            }, NEXT_QUESTION_DELAY)
        }
    }

    override fun onAnswerClicked(
        question: QuestionBO,
        answer: AnswerBO,
        points: Long,
        userMarkInMillis: Long
    ) {
        mViewModel.setGameResultAccumulated(question, answer, points, userMarkInMillis)
        setNextQuestion()
    }

}