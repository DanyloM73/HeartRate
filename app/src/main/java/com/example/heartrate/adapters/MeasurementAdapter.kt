package com.example.heartrate.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.heartrate.R
import com.example.heartrate.data.MeasurementEntity
import com.example.heartrate.databinding.MeasurementItemBinding

class MeasurementAdapter(
    private val activity: AppCompatActivity,
    private val measurements: MutableList<MeasurementEntity>
) : RecyclerView.Adapter<MeasurementAdapter.MeasurementViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MeasurementViewHolder {
        val binding = MeasurementItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return MeasurementViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MeasurementViewHolder,
        position: Int
    ) {
        holder.bind(measurements[position])
    }

    override fun getItemCount(): Int = measurements.size

    fun clearMeasurements() {
        val size = measurements.size
        if (size > 0) {
            measurements.clear()
            notifyItemRangeRemoved(0, size)
        }
    }

    inner class MeasurementViewHolder(
        private val binding: MeasurementItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(measurement: MeasurementEntity) {
            binding.bpmValue.text = activity.getString(
                R.string.bpm_value, measurement.bpm
            )
            binding.timeTv.text = measurement.time
            binding.dateTv.text = measurement.date
        }
    }
}
