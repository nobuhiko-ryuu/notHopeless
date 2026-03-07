package com.nothopeless.app.ui.post

import com.nothopeless.app.data.model.EffectType
import com.nothopeless.app.data.model.KindnessType
import com.nothopeless.app.data.model.SceneType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.nothopeless.app.data.repository.PostRepository

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: PostRepository
    private lateinit var vm: PostViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        vm = PostViewModel(repo)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `必須フィールド未入力で isSubmitEnabled が false`() {
        assertFalse(vm.isSubmitEnabled)
    }

    @Test
    fun `全フィールド入力で isSubmitEnabled が true`() {
        vm.onSceneSelected(SceneType.SHOP)
        vm.onKindnessTypeSelected(KindnessType.CARE)
        vm.onEffectSelected(EffectType.RELIEVED)
        vm.onBodyChanged("バスで席を譲ってもらいました")
        assertTrue(vm.isSubmitEnabled)
    }

    @Test
    fun `body 141字で isSubmitEnabled が false`() {
        vm.onSceneSelected(SceneType.SHOP)
        vm.onKindnessTypeSelected(KindnessType.CARE)
        vm.onEffectSelected(EffectType.RELIEVED)
        vm.onBodyChanged("あ".repeat(141))
        assertFalse(vm.isSubmitEnabled)
    }

    @Test
    fun `送信成功で isSuccess が true`() = runTest {
        coEvery { repo.createPost(any(), any(), any(), any(), any()) } returns "post_id_123"
        vm.onSceneSelected(SceneType.SHOP)
        vm.onKindnessTypeSelected(KindnessType.CARE)
        vm.onEffectSelected(EffectType.RELIEVED)
        vm.onBodyChanged("バスで席を譲ってもらいました")
        vm.submit()
        assertTrue(vm.uiState.value.isSuccess)
    }
}
