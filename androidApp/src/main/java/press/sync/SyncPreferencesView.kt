package press.sync

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.browser.customtabs.CustomTabsIntent
import com.jakewharton.rxbinding3.view.detaches
import com.squareup.contour.ContourLayout
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import me.saket.press.R
import me.saket.press.shared.localization.strings
import me.saket.press.shared.sync.SyncPreferencesEvent.DisableSyncClicked
import me.saket.press.shared.sync.SyncPreferencesEvent.SetupHostClicked
import me.saket.press.shared.sync.SyncPreferencesPresenter
import me.saket.press.shared.sync.SyncPreferencesUiEffect
import me.saket.press.shared.sync.SyncPreferencesUiEffect.OpenUrl
import me.saket.press.shared.sync.SyncPreferencesUiModel
import me.saket.press.shared.sync.SyncPreferencesUiModel.SyncDisabled
import me.saket.press.shared.sync.SyncPreferencesUiModel.SyncEnabled
import me.saket.press.shared.sync.git.GitHost
import me.saket.press.shared.sync.git.GitHost.GITHUB
import me.saket.press.shared.theme.TextStyles
import me.saket.press.shared.theme.applyStyle
import me.saket.press.shared.ui.subscribe
import me.saket.press.shared.ui.uiUpdates
import press.extensions.setDisplayedChild
import press.extensions.updateMargins
import press.theme.themeAware
import press.theme.themed
import press.widgets.PressButton
import press.widgets.PressToolbar
import press.widgets.dp

class SyncPreferencesView @AssistedInject constructor(
  @Assisted context: Context,
  @Assisted private val onDismiss: () -> Unit,
  private val presenter: SyncPreferencesPresenter
) : ContourLayout(context) {

  private val toolbar = themed(PressToolbar(context)).apply {
    title = context.strings().sync.title
    setNavigationOnClickListener { onDismiss() }
    applyLayout(
        x = matchParentX(),
        y = topTo { parent.top() }
    )
  }

  private val syncDisabledView = SyncDisabledView(context)
  private val syncEnabledView = SyncEnabledView(context)

  private val contentFlipperView = ViewFlipper(context).apply {
    animateFirstView = false
    setInAnimation(context, R.anim.slide_and_fade_in_from_bottom)
    setOutAnimation(context, R.anim.slide_and_fade_out_to_top)
    addView(syncDisabledView)
    addView(syncEnabledView)
    applyLayout(
        x = matchParentX(marginLeft = 22.dip, marginRight = 22.dip),
        y = topTo { toolbar.bottom() + 8.ydip }.bottomTo { parent.bottom() }
    )
  }

  init {
    themeAware {
      background = ColorDrawable(it.window.backgroundColor)
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    syncEnabledView.disableButton.setOnClickListener {
      presenter.dispatch(DisableSyncClicked)
    }

    presenter.uiUpdates()
        .takeUntil(detaches())
        .observeOn(mainThread())
        .subscribe(models = ::render, effects = ::render)
  }

  private fun render(model: SyncPreferencesUiModel) {
    return when (model) {
      is SyncDisabled -> {
        contentFlipperView.setDisplayedChild(syncDisabledView)
        syncDisabledView.render(model, onClick = { host ->
          presenter.dispatch(SetupHostClicked(host))
        })
      }
      is SyncEnabled -> {
        contentFlipperView.setDisplayedChild(syncEnabledView)
        syncEnabledView.render(model)
      }
    }
  }

  private fun render(effect: SyncPreferencesUiEffect) {
    return when (effect) {
      is OpenUrl -> CustomTabsIntent.Builder()
          .addDefaultShareMenuItem()
          .build()
          .launchUrl(context, Uri.parse(effect.url))
    }
  }

  @AssistedInject.Factory
  interface Factory {
    fun create(context: Context, onDismiss: () -> Unit): SyncPreferencesView
  }
}

private class SyncDisabledView(context: Context) : ContourLayout(context) {
  private val messageView = themed(TextView(context)).apply {
    text = context.strings().sync.sync_disabled_message
    TextStyles.Secondary.applyStyle(this)
    applyLayout(
        x = matchParentX(),
        y = topTo { parent.top() }
    )
  }

  private val gitHostButtons = LinearLayout(context).apply {
    orientation = VERTICAL
    applyLayout(
        x = matchParentX(),
        y = topTo { messageView.bottom() + 20.ydip }
    )
  }

  init {
    contourHeightOf { gitHostButtons.bottom() }
  }

  fun render(model: SyncDisabled, onClick: (GitHost) -> Unit) {
    gitHostButtons.removeAllViews()
    model.availableGitHosts.forEach { host ->
      val button = themed(PressButton(context)).apply {
        text = context.strings().sync.setup_sync_with_host.format(host.displayName())
        setOnClickListener { onClick(host) }
      }
      gitHostButtons.addView(button, WRAP_CONTENT, WRAP_CONTENT)
      button.updateMargins(bottom = dp(8))
    }
  }
}

private class SyncEnabledView(context: Context) : ContourLayout(context) {
  private val setupInfoView = themed(TextView(context)).apply {
    TextStyles.Secondary.applyStyle(this)
    applyLayout(
        x = matchParentX(),
        y = topTo { parent.top() }
    )
  }

  private val statusView = themed(TextView(context)).apply {
    TextStyles.Secondary.applyStyle(this)
    applyLayout(
        x = matchParentX(),
        y = topTo { setupInfoView.bottom() + 16.ydip }
    )
  }

  val disableButton = themed(PressButton(context)).apply {
    text = "Disable sync on this device"
    applyLayout(
        x = leftTo { parent.left() },
        y = topTo { statusView.bottom() + 20.ydip }
    )
  }

  init {
    contourHeightOf { disableButton.bottom() }
  }

  fun render(model: SyncEnabled) {
    setupInfoView.text = model.setupInfo
    statusView.text = model.status
  }
}
