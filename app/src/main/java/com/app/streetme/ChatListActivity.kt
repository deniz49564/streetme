package com.streetme.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

    private var chatListener: ValueEventListener? = null
    private var chatQuery: Query? = null

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
            val intent = Intent(this, ChatActivity::class.java)
            // ChatActivity'deki karşılık gelen anahtarları (key) kullandık
            intent.putExtra("user_id", chat.otherUserId)
            intent.putExtra("user_name", chat.otherUserName)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun loadChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Zaman damgasına göre sıralı sorgu
        chatQuery = database.child("chats").orderByChild("lastMessageTime")

        chatListener = chatQuery?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempMap = mutableMapOf<String, ChatPreview>()
                val totalChats = snapshot.childrenCount
                var processedChats = 0

                if (totalChats == 0L) {
                    chatList.clear()
                    adapter.notifyDataSetChanged()
                    return
                }

                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue
                    val lastMessage = chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                    val lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long::class.java) ?: 0
                    val lastMessageSender = chatSnapshot.child("lastMessageSender").getValue(String::class.java) ?: ""

                    val otherUserId = chatId.replace(currentUserId, "").replace("_", "")

                    database.child("users").child(otherUserId).get().addOnSuccessListener { userSnapshot ->
                        val otherUserName = userSnapshot.child("adSoyad").getValue(String::class.java) ?: "Kullanıcı"
                        val isRead = lastMessageSender == currentUserId

                        val preview = ChatPreview(
                            id = chatId,
                            otherUserId = otherUserId,
                            otherUserName = otherUserName,
                            lastMessage = lastMessage,
                            lastMessageTime = lastMessageTime,
                            unreadCount = if (!isRead) 1 else 0 // Basit bir bayrak, geliştirilebilir
                        )

                        tempMap[chatId] = preview
                        processedChats++

                        // Tüm kullanıcı bilgileri tamamlandığında listeyi güncelle ve sırala
                        if (processedChats.toLong() == totalChats) {
                            updateUI(tempMap)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatListActivity, "Hata: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(tempMap: Map<String, ChatPreview>) {
        chatList.clear()
        // Mesajları yeniden zamana göre (büyükten küçüğe) sıralıyoruz
        chatList.addAll(tempMap.values.sortedByDescending { it.lastMessageTime })
        adapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        chatListener?.let { chatQuery?.removeEventListener(it) }
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
                if (timestamp == 0L) return ""
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