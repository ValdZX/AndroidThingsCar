package ua.com.valdzx.car.terminal.utils

import android.widget.SeekBar

class EmptyOnSeekBarChangeListener(val onProgressChanged: (Int) -> Unit) : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        onProgressChanged.invoke(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        //NOP
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        //NOP
    }
}