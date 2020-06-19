package me.saket.press.shared.sync

import assertk.assertThat
import assertk.assertions.isEqualTo
import me.saket.press.shared.RobolectricTest
import me.saket.press.shared.db.NoteId
import me.saket.press.shared.testDeviceInfo
import me.saket.press.shared.fakedata.fakeNote
import me.saket.press.shared.sync.git.File
import me.saket.press.shared.sync.git.FileNameRegister
import kotlin.test.Test

class FileNameRegisterTest : RobolectricTest() {

  private val directory = testDeviceInfo().appStorage
  private val register = FileNameRegister(directory)

  @Test fun `generates unique file names to avoid conflicts`() {
    with(register) {
      val note1 = fakeNote(noteId = NoteId.generate(), content = "# abc")
      assertThat(fileNameFor(note1)).isEqualTo("abc.md")
      assertThat(noteIdFor("abc.md")).isEqualTo(note1.uuid)

      // Same note, updated content.
      val note2 = note1.copy(content = "# abc def")
      assertThat(fileNameFor(note2)).isEqualTo("abc_def.md")
      assertThat(noteIdFor("abc_def.md")).isEqualTo(note2.uuid)

      // Different note, same content.
      val note3 = fakeNote(noteId = NoteId.generate(), content = note1.content)
      assertThat(fileNameFor(note3)).isEqualTo("abc_2.md")
      assertThat(noteIdFor("abc_2.md")).isEqualTo(note3.uuid)
    }
  }

  @Test fun `generation of a new file name for resolving conflict`() {
    File(directory, "uncharted.md").write("A Thief's End")
    File(directory, "uncharted_2.md").write("The Lost Legacy")

    val conflictingFile = File(directory, "uncharted.md")
    val newName = register.findNewNameOnConflict(conflictingFile)
    assertThat(newName).isEqualTo("uncharted_3.md")
  }
}
