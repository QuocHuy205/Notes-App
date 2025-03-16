import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

// Database Helper class
class NotesDatabaseHelper(context: Application) : SQLiteOpenHelper(context, "notes.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS notes")
        onCreate(db)
    }
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val dbHelper = NotesDatabaseHelper(application)
    private val db = dbHelper.writableDatabase

    var notes by mutableStateOf(listOf<Pair<Int, String>>())
        private set

    init {
        loadNotes()
    }

    fun addNote(content: String) {
        val values = ContentValues().apply { put("content", content) }
        db.insert("notes", null, values)
        loadNotes()
    }

    fun updateNote(id: Int, newContent: String) {
        val values = ContentValues().apply { put("content", newContent) }
        db.update("notes", values, "id = ?", arrayOf(id.toString()))
        loadNotes()
    }

    fun deleteNote(id: Int) {
        db.delete("notes", "id = ?", arrayOf(id.toString()))
        loadNotes()
    }

    private fun loadNotes() {
        notes = mutableListOf<Pair<Int, String>>().apply {
            val cursor = db.query("notes", arrayOf("id", "content"), null, null, null, null, null)
            while (cursor.moveToNext()) {
                add(cursor.getInt(0) to cursor.getString(1))
            }
            cursor.close()
        }
    }
}

@Composable
fun NotesApp(viewModel: NotesViewModel) {
    var text by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var editingNoteId by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Nhập ghi chú") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF03A9F4),
                unfocusedBorderColor = Color(0xFF03A9F4),
                cursorColor = Color(0xFF03A9F4)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)
        )

        Row {
            Button(
                onClick = {
                    if (text.isNotEmpty()) {
                        if (isEditing) {
                            editingNoteId?.let { viewModel.updateNote(it, text) }
                            isEditing = false
                        } else {
                            viewModel.addNote(text)
                        }
                        text = ""
                        editingNoteId = null
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
            ) {
                Text(if (isEditing) "Cập nhật" else "Thêm ghi chú")
            }

            if (isEditing) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        isEditing = false
                        text = ""
                        editingNoteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                ) {
                    Text("Trở về")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(viewModel.notes) { (id, note) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .clickable {
                            text = note
                            editingNoteId = id
                            isEditing = true
                        },
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (note.length > 50) note.take(50) + "..." else note, // Cắt bớt nếu quá dài
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp
                        )
                        Button(
                            onClick = { viewModel.deleteNote(id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                        ) {
                            Text("Xóa")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(context: Context) {
    val viewModel: NotesViewModel = ViewModelProvider(
        LocalContext.current as ViewModelStoreOwner
    ).get(NotesViewModel::class.java)

    NotesApp(viewModel)
}
