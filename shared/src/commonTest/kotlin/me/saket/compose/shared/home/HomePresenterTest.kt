package me.saket.compose.shared.home

import com.badoo.reaktive.subject.publish.publishSubject
import com.badoo.reaktive.test.observable.test
import com.benasher44.uuid.uuid4
import me.saket.compose.shared.fakedata.fakeNote
import me.saket.compose.shared.home.HomeEvent.NewNoteClicked
import me.saket.compose.shared.navigation.FakeNavigator
import me.saket.compose.shared.navigation.ScreenKey
import me.saket.compose.shared.note.FakeNoteRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class HomePresenterTest {

  private val noteRepository = FakeNoteRepository()
  private val navigator = FakeNavigator()

  private val presenter = HomePresenter(noteRepository, navigator)
  private val events = publishSubject<HomeEvent>()

  @Test fun `populate notes on creation`() {
    val noteUuid = uuid4()
    noteRepository.savedNotes += listOf(fakeNote(
        uuid = noteUuid,
        localId = -1L,
        content = "# Nicolas Cage\nOur national treasure"
    ))

    val testObserver = presenter.uiModels(events).test()

    val noteUiModels = listOf(HomeUiModel.Note(
        noteUuid = noteUuid,
        adapterId = -1L,
        title = "Nicolas Cage",
        body = "Our national treasure"
    ))

    val uiModel = testObserver.values[0]
    assertEquals(noteUiModels, uiModel.notes)
  }

  @Test fun `open new note screen when new note is clicked`() {
    presenter.uiModels(events).test()

    events.onNext(NewNoteClicked)

    assertSame(ScreenKey.ComposeNewNote, navigator.backstack.last())
  }
}
