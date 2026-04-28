package com.streetme.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class IlanAdapter(
    private val onItemClick: (Ilan) -> Unit
) : ListAdapter<Ilan, IlanAdapter.ViewHolder>(IlanDiffCallback()) {

    // ListAdapter kullandığımız için klasik List ve getItemCount'a gerek kalmadı.
    // Bu sayede daha akıcı animasyonlar ve yüksek performans elde edersin.

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ilanImage: ImageView = itemView.findViewById(R.id.ilan_image)
        private val baslikText: TextView = itemView.findViewById(R.id.baslik_text)
        private val fiyatText: TextView = itemView.findViewById(R.id.fiyat_text)
        private val kategoriText: TextView = itemView.findViewById(R.id.kategori_text)

        fun bind(ilan: Ilan, onClick: (Ilan) -> Unit) {
            baslikText.text = ilan.baslik
            fiyatText.text = ilan.getFiyatText()
            kategoriText.text = ilan.kategori

            // Glide Optimizasyonu
            Glide.with(itemView.context)
                .load(ilan.resimUrl.ifEmpty { null }) // Boş string yerine null göndererek placeholder tetiklenir
                .placeholder(R.drawable.ic_ilan_default)
                .error(R.drawable.ic_ilan_default) // Hata durumunda varsayılan resim
                .transition(DrawableTransitionOptions.withCrossFade()) // Yumuşak geçiş animasyonu
                .centerCrop() // Resmi çerçeveye orantılı sığdırır
                .into(ilanImage)

            itemView.setOnClickListener { onClick(ilan) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ilan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    // Değişen ilanları hızlıca tespit eden mekanizma
    class IlanDiffCallback : DiffUtil.ItemCallback<Ilan>() {
        override fun areItemsTheSame(oldItem: Ilan, newItem: Ilan): Boolean {
            return oldItem.id == newItem.id // ID'ler aynı mı?
        }

        override fun areContentsTheSame(oldItem: Ilan, newItem: Ilan): Boolean {
            return oldItem == newItem // İçerik değişti mi?
        }
    }
}