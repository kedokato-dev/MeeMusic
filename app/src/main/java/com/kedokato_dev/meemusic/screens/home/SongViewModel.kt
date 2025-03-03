import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kedokato_dev.meemusic.Models.Song
import com.kedokato_dev.meemusic.Repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SongState {
    object Loading : SongState()
    data class Success(val songs: List<Song>) : SongState()
    data class Error(val message: String) : SongState()
}

class SongViewModel(private val repository: SongRepository) : ViewModel() {
    private val _state = MutableStateFlow<SongState>(SongState.Loading)
    val state: StateFlow<SongState> = _state.asStateFlow()

    init {
        fetchSongs() // Gọi API ngay khi ViewModel được tạo
    }

    fun fetchSongs() {
        viewModelScope.launch {
            _state.value = SongState.Loading
            try {
                Log.d("SongViewModel", "📢 Đang gọi API lấy danh sách bài hát...")
                val result = repository.getSongs() // Gọi API từ Repository
                Log.d("SongViewModel", "✅ API trả về: $result")
                if (result != null && result.isNotEmpty()) {
                    _state.value = SongState.Success(result)
                } else {
                    _state.value = SongState.Error("Không có dữ liệu!")
                }
            } catch (e: Exception) {
                Log.e("SongViewModel", "❌ Lỗi khi gọi API: ${e.message}", e)
                _state.value = SongState.Error("Lỗi: ${e.message}")
            }
        }
    }
}
