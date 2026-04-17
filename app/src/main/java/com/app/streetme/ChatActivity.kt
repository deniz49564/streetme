package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.database.ktx.database

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var userNameTextView: TextView
    private lateinit var backButton: ImageButton

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var messagesAdapter: MessagesAdapter

    private var chatId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private val messagesList = arrayListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Intent'ten verileri al
        otherUserId = intent.getStringExtra("user_id") ?: ""
        otherUserName = intent.getStringExtra("user_name") ?: "Kullanıcı"

        if (otherUserId.isEmpty()) {
            Toast.makeText(this, "Hata: Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupFirebase()
        loadMessages()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_messages)
        messageEditText = findViewById(R.id.message_edit_text)
        sendButton = findViewById(R.id.send_button)
        userNameTextView = findViewById(R.id.user_name_text_view)
        backButton = findViewById(R.id.back_button)

        userNameTextView.text = otherUserName

        backButton.setOnClickListener { finish() }
        sendButton.setOnClickListener { sendMessage() }

        messagesAdapter = MessagesAdapter(messagesList, auth.currentUser?.uid ?: "")
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = messagesAdapter
    }

    private fun setupFirebase() {
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        val myId = auth.currentUser?.uid ?: return

        chatId = if (myId < otherUserId) {
            "${myId}_$otherUserId"
        } else {
            "${otherUserId}_$myId"
        }
    }

    private fun loadMessages() {
        database.child("chats").child(chatId).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    messagesList.clear()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(ChatMessage::class.java)
                        message?.let {
                            messagesList.add(it)
                        }
                    }
                    messagesAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messagesList.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isEmpty()) return

        val myId = auth.currentUser?.uid ?: return

        val message = ChatMessage(
            id = database.child("chats").child(chatId).child("messages").push().key ?: return,
            senderId = myId,
            receiverId = otherUserId,
            message = messageText,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        database.child("chats").child(chatId).child("messages").child(message.id)
            .setValue(message)
            .addOnSuccessListener {
                messageEditText.text.clear()

                val lastMessage = mapOf(
                    "lastMessage" to messageText,
                    "lastMessageTime" to message.timestamp,
                    "lastMessageSender" to myId
                )
                database.child("chats").child(chatId).updateChildren(lastMessage)
            }
    }
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
) {
    fun getFormattedTime(): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }
}

class MessagesAdapter(
    private val messages: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_MY_MESSAGE = 1
        private const val TYPE_OTHER_MESSAGE = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) {
            TYPE_MY_MESSAGE
        } else {
            TYPE_OTHER_MESSAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_MY_MESSAGE -> {
                val view = layoutInflater.inflate(R.layout.item_my_message, parent, false)
                MyMessageViewHolder(view)
            }
            else -> {
                val view = layoutInflater.inflate(R.layout.item_other_message, parent, false)
                OtherMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is MyMessageViewHolder -> holder.bind(message)
            is OtherMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    class MyMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timeText: TextView = itemView.findViewById(R.id.time_text)

        fun bind(message: ChatMessage) {
            messageText.text = message.message
            timeText.text = message.getFormattedTime()
        }
    }

    class OtherMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val timeText: TextView = itemView.findViewById(R.id.time_text)

        fun bind(message: ChatMessage) {
            messageText.text = message.message
            timeText.text = message.getFormattedTime()
        }
    }
}