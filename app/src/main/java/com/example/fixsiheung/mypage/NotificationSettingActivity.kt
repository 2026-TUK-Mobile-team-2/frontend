package com.example.fixsiheung.mypage

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.fixsiheung.R
import com.example.fixsiheung.databinding.ActivityNotificationSettingBinding
import com.google.android.material.snackbar.Snackbar

class NotificationSettingActivity : AppCompatActivity() {

    private val binding by lazy { ActivityNotificationSettingBinding.inflate(layoutInflater) }
    private val prefs by lazy { getSharedPreferences("notification_prefs", MODE_PRIVATE) }
    private var initialSettings = mapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        loadSettings()
        saveInitialSettings()
        updateSaveButton()

        listOf(
            binding.switchStatus,
            binding.switchEmpathy,
            binding.switchNewComplaint,
            binding.switchCategory
        ).forEach { switch ->
            switch.setOnCheckedChangeListener { _, _ ->
                updateSaveButton()
            }
        }

        listOf(
            binding.chipTrash,
            binding.chipFacilities,
            binding.chipRoad,
            binding.chipAny
        ).forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                updateSaveButton()
            }
        }

        // 전체 알림 토글
        binding.switchAll.setOnCheckedChangeListener { _, isChecked ->
            setSubSwitchesEnabled(isChecked)
            updateSaveButton()
        }

        // 카테고리 알림
        binding.switchCategory.setOnCheckedChangeListener { _, isChecked ->
            binding.chipGroupCategory.visibility = if (isChecked) View.VISIBLE else View.GONE
            updateSaveButton()
        }

        // 저장 버튼
        binding.btnSave.setOnClickListener {
            binding.btnSave.isEnabled = false
            binding.btnSave.text = "저장 중..."

            binding.root.postDelayed({
                saveSettings()
                saveInitialSettings()
                updateSaveButton()

                Snackbar.make(binding.root, "설정이 성공적으로 저장되었습니다.", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(R.color.primary))
                    .setTextColor(Color.WHITE)
                    .show()

                binding.btnSave.text = "저장"
            }, 800)
        }
    }

    private fun setSubSwitchesEnabled(isEnabled: Boolean) {
        val alpha = if (isEnabled) 1.0f else 0.4f

        binding.switchStatus.isEnabled = isEnabled
        binding.switchEmpathy.isEnabled = isEnabled
        binding.switchNewComplaint.isEnabled = isEnabled
        binding.switchCategory.isEnabled = isEnabled

        binding.cardActivity.alpha = alpha
        binding.cardNewComplaint.alpha = alpha

        if (!isEnabled) {
            binding.chipGroupCategory.visibility = View.GONE
        }
    }

    private fun loadSettings() {
        binding.switchAll.isChecked = prefs.getBoolean("all", true)
        binding.switchStatus.isChecked = prefs.getBoolean("status", true)
        binding.switchEmpathy.isChecked = prefs.getBoolean("empathy", true)
        binding.switchNewComplaint.isChecked = prefs.getBoolean("new_complaint", true)
        binding.switchCategory.isChecked = prefs.getBoolean("category", true)

        binding.chipTrash.isChecked = prefs.getBoolean("cat_trash", true)
        binding.chipFacilities.isChecked = prefs.getBoolean("cat_facilities", true)
        binding.chipRoad.isChecked = prefs.getBoolean("cat_road", true)
        binding.chipAny.isChecked = prefs.getBoolean("cat_any", true)

        if (binding.switchCategory.isChecked) {
            binding.chipGroupCategory.visibility = View.VISIBLE
        }
        setSubSwitchesEnabled(binding.switchAll.isChecked)
    }

    private fun saveSettings() {
        prefs.edit()
            .putBoolean("all", binding.switchAll.isChecked)
            .putBoolean("status", binding.switchStatus.isChecked)
            .putBoolean("empathy", binding.switchEmpathy.isChecked)
            .putBoolean("new_complaint", binding.switchNewComplaint.isChecked)
            .putBoolean("category", binding.switchCategory.isChecked)
            .putBoolean("cat_trash", binding.chipTrash.isChecked)
            .putBoolean("cat_facilities", binding.chipFacilities.isChecked)
            .putBoolean("cat_road", binding.chipRoad.isChecked)
            .putBoolean("cat_any", binding.chipAny.isChecked)
            .apply()
    }

    private fun saveInitialSettings() {
        initialSettings = mapOf(
            "all" to binding.switchAll.isChecked,
            "status" to binding.switchStatus.isChecked,
            "empathy" to binding.switchEmpathy.isChecked,
            "new_complaint" to binding.switchNewComplaint.isChecked,
            "category" to binding.switchCategory.isChecked,
            "cat_trash" to binding.chipTrash.isChecked,
            "cat_facilities" to binding.chipFacilities.isChecked,
            "cat_road" to binding.chipRoad.isChecked,
            "cat_any" to binding.chipAny.isChecked,
        )
    }

    private fun hasChanges(): Boolean {
        return initialSettings["all"] != binding.switchAll.isChecked ||
                initialSettings["status"] != binding.switchStatus.isChecked ||
                initialSettings["empathy"] != binding.switchEmpathy.isChecked ||
                initialSettings["new_complaint"] != binding.switchNewComplaint.isChecked ||
                initialSettings["category"] != binding.switchCategory.isChecked ||
                initialSettings["cat_trash"] != binding.chipTrash.isChecked ||
                initialSettings["cat_facilities"] != binding.chipFacilities.isChecked ||
                initialSettings["cat_road"] != binding.chipRoad.isChecked ||
                initialSettings["cat_any"] != binding.chipAny.isChecked
    }

    private fun updateSaveButton() {
        val changed = hasChanges()

        // 카테고리 알림 ON인데 아무것도 선택 안 했으면 저장 불가
        val categoryInvalid = binding.switchCategory.isChecked &&
                !binding.chipTrash.isChecked &&
                !binding.chipFacilities.isChecked &&
                !binding.chipRoad.isChecked &&
                !binding.chipAny.isChecked

        binding.btnSave.isEnabled = changed && !categoryInvalid
        binding.btnSave.alpha = if (changed && !categoryInvalid) 1.0f else 0.5f
    }
}