package org.example.app

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.*

/**
 * NotesApp - A minimal modern notes Android app.
 * Features: Create/edit/delete notes, search, organize by tags, list/grid & detail views.
 * Colors: primary (#1976D2), accent (#FFD600), secondary (#FFFFFF), light theme, minimal UI.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var adapter: NotesAdapter
    private lateinit var detailLayout: ViewGroup
    private lateinit var mainListContainer: ViewGroup
    private lateinit var tagBar: ChipGroup

    private var notes: MutableList<Note> = mutableListOf()
    private var filteredNotes: MutableList<Note> = mutableListOf()
    private var selectedTag: String? = null
    private var showGrid: Boolean = false
    private var selectedNote: Note? = null
    private val primaryColor by lazy { ContextCompat.getColor(this, R.color.primary) }
    private val accentColor by lazy { ContextCompat.getColor(this, R.color.accent) }
    private val secondaryColor by lazy { ContextCompat.getColor(this, R.color.secondary) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme_Light)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.notesRecyclerView)
        fab = findViewById(R.id.fab)
        searchView = findViewById(R.id.search_view)
        detailLayout = findViewById(R.id.note_detail_panel)
        mainListContainer = findViewById(R.id.list_container)
        tagBar = findViewById(R.id.tag_bar)

        // Demo notes for quick start
        notes.addAll(
            listOf(
                Note(
                    id = UUID.randomUUID().toString(),
                    title = "Welcome",
                    content = "This is your first note! Edit or delete me. Create new notes using the '+' button.",
                    tags = listOf("info")
                ),
                Note(
                    id = UUID.randomUUID().toString(),
                    title = "Organize your notes with tags",
                    content = "Try adding a tag when you create or edit a note. Tap tags to filter notes.",
                    tags = listOf("tips", "organization")
                )
            )
        )

        adapter = NotesAdapter(filteredNotes, this,
            onClick = { showNoteDetail(it) },
            onLongClick = { showNoteOptions(it) }
        )

        recyclerView.adapter = adapter
        updateGridLayout()
        refreshNoteList()

        fab.setOnClickListener { showNoteDialog(null) }

        setupSearch()
        refreshTagBar()
    }

    private fun setupSearch() {
        searchView.queryHint = "Search notes..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                filterNotes(q, selectedTag)
                return true
            }
        })
    }

    private fun refreshTagBar() {
        tagBar.removeAllViews()
        val allTags = notes.flatMap { it.tags }.distinct().sorted()
        if (allTags.isNotEmpty()) {
            val allChip = Chip(this).apply {
                text = "All"
                isCheckable = true
                isChecked = selectedTag == null
                setOnClickListener {
                    selectedTag = null
                    filterNotes(searchView.query.toString(), null)
                    refreshTagBar()
                }
            }
            tagBar.addView(allChip)
            for (tag in allTags) {
                val chip = Chip(this).apply {
                    text = tag
                    isCheckable = true
                    isChecked = selectedTag == tag
                    setOnClickListener {
                        selectedTag = tag
                        filterNotes(searchView.query.toString(), selectedTag)
                        refreshTagBar()
                    }
                }
                tagBar.addView(chip)
            }
        }
    }

    private fun updateGridLayout() {
        recyclerView.layoutManager = if (showGrid) {
            GridLayoutManager(this, 2)
        } else {
            GridLayoutManager(this, 1)
        }
    }

    private fun filterNotes(searchText: String?, tag: String?) {
        filteredNotes.clear()
        val query = searchText?.lowercase()?.trim() ?: ""
        val base = if (tag == null) notes else notes.filter { it.tags.contains(tag) }
        filteredNotes.addAll(
            base.filter {
                it.title.lowercase().contains(query) ||
                        it.content.lowercase().contains(query)
                        || it.tags.any { t -> t.lowercase().contains(query) }
            }
        )
        adapter.notifyDataSetChanged()
    }

    private fun refreshNoteList() {
        filterNotes(searchView.query.toString(), selectedTag)
        refreshTagBar()
        showList()
    }

    private fun showNoteDetail(note: Note) {
        selectedNote = note
        mainListContainer.visibility = View.GONE
        detailLayout.visibility = View.VISIBLE

        detailLayout.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val v = inflater.inflate(R.layout.layout_note_detail, detailLayout, false)
        val titleView = v.findViewById<TextView>(R.id.detail_title)
        val contentView = v.findViewById<TextView>(R.id.detail_content)
        val tagGroup = v.findViewById<ChipGroup>(R.id.detail_tag_group)
        val btnEdit = v.findViewById<Button>(R.id.detail_btn_edit)
        val btnDelete = v.findViewById<Button>(R.id.detail_btn_delete)
        val btnBack = v.findViewById<ImageButton>(R.id.detail_btn_back)

        titleView.text = note.title
        contentView.text = note.content
        tagGroup.removeAllViews()
        for (tag in note.tags) {
            val chip = Chip(this)
            chip.text = tag
            chip.isClickable = false
            chip.isCheckable = false
            tagGroup.addView(chip)
        }
        btnEdit.setOnClickListener {
            showNoteDialog(note)
        }
        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete") {_,_ ->
                    notes.removeAll { it.id == note.id }
                    refreshNoteList()
                    showList()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        btnBack.setOnClickListener {
            showList()
        }
        detailLayout.addView(v)
    }

    private fun showList() {
        selectedNote = null
        detailLayout.removeAllViews()
        detailLayout.visibility = View.GONE
        mainListContainer.visibility = View.VISIBLE
    }

    private fun showNoteOptions(note: Note): Boolean {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle(note.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showNoteDialog(note)
                    1 -> {
                        notes.removeAll { it.id == note.id }
                        refreshNoteList()
                    }
                }
            }
            .show()
        return true
    }

    private fun showNoteDialog(note: Note?) {
        val isEdit = note != null
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_note, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.dialogTitleInput)
        val contentInput = dialogView.findViewById<EditText>(R.id.dialogContentInput)
        val tagInput = dialogView.findViewById<EditText>(R.id.dialogTagInput)
        titleInput.setText(note?.title ?: "")
        contentInput.setText(note?.content ?: "")
        tagInput.setText(note?.tags?.joinToString(separator = ", ") ?: "")
        AlertDialog.Builder(this)
            .setTitle(if (isEdit) "Edit Note" else "New Note")
            .setView(dialogView)
            .setPositiveButton(if (isEdit) "Save" else "Create") { _, _ ->
                val t = titleInput.text.toString().trim()
                val c = contentInput.text.toString().trim()
                val tags = tagInput.text.toString().split(',').mapNotNull { it.trim().takeIf { str-> str.isNotEmpty() } }
                if (t.isEmpty()) {
                    Toast.makeText(this, "Title can't be empty.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (isEdit) {
                    val idx = notes.indexOfFirst { it.id == note!!.id }
                    if (idx != -1) notes[idx] = note.copy(title=t, content=c, tags=tags)
                } else {
                    notes.add(0, Note(UUID.randomUUID().toString(), t, c, tags))
                }
                refreshNoteList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val gridToggle = menu?.findItem(R.id.action_toggle_grid)
        gridToggle?.setOnMenuItemClickListener {
            showGrid = !showGrid
            updateGridLayout()
            adapter.notifyDataSetChanged()
            it.isChecked = showGrid
            true
        }
        return true
    }
}

// PUBLIC_INTERFACE
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val tags: List<String>
)

// Adapter for notes list/grid
private class NotesAdapter(
    private val items: List<Note>,
    private val context: Context,
    val onClick: (Note) -> Unit,
    val onLongClick: (Note) -> Boolean
) : RecyclerView.Adapter<NotesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.note_card, parent, false)
        return ViewHolder(v)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = items[position]
        holder.title.text = note.title
        holder.content.text = if (note.content.length > 60)
            "${note.content.take(57)}..." else note.content
        holder.tagGroup.removeAllViews()
        for (tag in note.tags) {
            val chip = Chip(context)
            chip.text = tag
            chip.isClickable = false
            chip.isCheckable = false
            holder.tagGroup.addView(chip)
        }
        holder.itemView.setOnClickListener { onClick(note) }
        holder.itemView.setOnLongClickListener { onLongClick(note) }
    }
    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.card_title)
        val content: TextView = view.findViewById(R.id.card_content)
        val tagGroup: ChipGroup = view.findViewById(R.id.card_tag_group)
    }
}
