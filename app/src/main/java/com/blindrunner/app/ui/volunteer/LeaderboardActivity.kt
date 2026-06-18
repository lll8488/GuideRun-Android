package com.blindrunner.app.ui.volunteer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.R
import com.blindrunner.app.data.local.entity.UserEntity
import kotlinx.coroutines.launch

class LeaderboardActivity : AppCompatActivity() {
    private val app get() = application as BlindRunnerApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        findViewById<Button>(R.id.btn_back).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rv_leaderboard)
        rv.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            try {
                val list = app.database.userDao().getVolunteerLeaderboard()
                val adapter = LeaderboardAdapter()
                adapter.submitList(list)
                rv.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@LeaderboardActivity, "加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class LeaderboardAdapter :
    ListAdapter<UserEntity, LeaderboardAdapter.VH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<UserEntity>() {
        override fun areItemsTheSame(old: UserEntity, new: UserEntity) = old.phone == new.phone
        override fun areContentsTheSame(old: UserEntity, new: UserEntity) = old == new
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tv_rank)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvCount: TextView = view.findViewById(R.id.tv_count)
        val tvRating: TextView = view.findViewById(R.id.tv_rating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.tvRank.text = when (position) {
            0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${position + 1}"
        }
        holder.tvName.text = item.phone
        holder.tvCount.text = "${item.totalRuns}次"
        holder.tvRating.text = if (item.ratingCount > 0)
            "⭐%.1f".format(item.rating) else "--"
    }
}
