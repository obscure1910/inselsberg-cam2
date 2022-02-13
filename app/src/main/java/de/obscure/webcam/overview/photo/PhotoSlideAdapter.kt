package de.obscure.webcam.overview.photo

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class PhotoSlideAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> WebcamFragment()
        1 -> PanomaxFragment()
        2 -> TabarzFragment()
        else -> WebcamFragment()
    }

}