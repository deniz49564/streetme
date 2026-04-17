package com.streetme.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class IlanAdapter(
    private val ilanlar: List<Ilan>,
    private val onItemClick: (Ilan) -> Unit
) : RecyclerView.Adapter<IlanAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ilanImage: ImageView = itemView.findViewById(R.id.ilan_image)
        private val baslikText: TextView = itemView.findViewById(R.id.baslik_text)
        private val fiyatText: TextView = itemView.findViewById(R.id.fiyat_text)
        private val kategoriText: TextView = itemView.findViewById(R.id.kategori_text)

        fun bind(ilan: Ilan, onClick: (Ilan) -> Unit) {
            baslikText.text = ilan.baslik
            fiyatText.text = ilan.getFiyatText()
            kategoriText.text = ilan.kategori

            if (ilan.resimUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(ilan.resimUrl)
                    .placeholder(R.drawable.ic_ilan_default)
                    .into(ilanImage)
            }

            itemView.setOnClickListener { onClick(ilan) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ilan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(ilanlar[position], onItemClick)
    }

    override fun getItemCount() = ilanlar.size
}