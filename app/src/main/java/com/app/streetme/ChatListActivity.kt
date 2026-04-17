package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ChatListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val chatList = arrayListOf<ChatPreview>()
    private lateinit var adapter: ChatListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        initViews()
        loadChats()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_chats)
        backButton = findViewById(R.id.back_button)

        backButton.setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ChatListAdapter(chatList) { chat ->
            // Sohbete tıklandığında
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("chat_id", chat.id)
            intent.putExtra("other_user_id", chat.otherUserId)
            intent.putExtra("other_user_name", chat.otherUserName)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        database.child("chats").orderByChild("lastMessageTime")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatList.clear()
                    for (chatSnapshot in snapshot.children) {
                        val chatId = chatSnapshot.key ?: continue
                        val lastMessage = chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                        val lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0
                        val lastMessageSender = chatSnapshot.child("lastMessageSender").getValue(String::class.java) ?: ""

                        // Diğer kullanıcı ID'sini bul
                        val otherUserId = chatId.replace(currentUserId, "").replace("_", "")
                        if (otherUserId.isEmpty()) continue

                        // Diğer kullanıcı bilgilerini al
                        database.child("users").child(otherUserId).get()
                            .addOnSuccessListener { userSnapshot ->
                                val user = userSnapshot.getValue(StreetMeUser::class.java)
                                val otherUserName = user?.adSoyad ?: "Kullanıcı"

                                val isRead = lastMessageSender == currentUserId

                                chatList.add(
                                    ChatPreview(
                                        id = chatId,
                                        otherUserId = otherUserId,
                                        otherUserName = otherUserName,
                                        lastMessage = lastMessage,
                                        lastMessageTime = lastMessageTime,
                                        unreadCount = if (!isRead) 1 else 0
                                    )
                                )
                                adapter.notifyDataSetChanged()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    data class ChatPreview(
        val id: String,
        val otherUserId: String,
        val otherUserName: String,
        val lastMessage: String,
        val lastMessageTime: Long,
        val unreadCount: Int
    )

    class ChatListAdapter(
        private val chats: List<ChatPreview>,
        private val onItemClick: (ChatPreview) -> Unit
    ) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.user_name_text)
            private val messageText: TextView = itemView.findViewById(R.id.last_message_text)
            private val timeText: TextView = itemView.findViewById(R.id.time_text)
            private val unreadBadge: TextView = itemView.findViewById(R.id.unread_badge)

            fun bind(chat: ChatPreview, onClick: (ChatPreview) -> Unit) {
                nameText.text = chat.otherUserName
                messageText.text = chat.lastMessage
                timeText.text = formatTime(chat.lastMessageTime)

                if (chat.unreadCount > 0) {
                    unreadBadge.visibility = View.VISIBLE
                    unreadBadge.text = chat.unreadCount.toString()
                } else {
                    unreadBadge.visibility = View.GONE
                }

                itemView.setOnClickListener { onClick(chat) }
            }

            private fun formatTime(timestamp: Long): String {
                val date = java.util.Date(timestamp)
                val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                return format.format(date)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(chats[position], onItemClick)
        }

        override fun getItemCount() = chats.size
    }
}