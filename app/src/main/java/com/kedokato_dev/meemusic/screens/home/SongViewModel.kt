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
    data class Success(
        val randomSongs: List<Song>,
        val heartbreakSongs: List<Song>,
        val cheerfulSongs: List<Song>,
        val relaxingSongs: List<Song>,
        val reflectiveSongs: List<Song>
    ) : SongState()
    data class Error(val message: String) : SongState()
}

class SongViewModel(private val repository: SongRepository) : ViewModel() {
    private val _state = MutableStateFlow<SongState>(SongState.Loading)
    val state: StateFlow<SongState> = _state.asStateFlow()

    init {
        fetchSongs()
    }

    fun fetchSongs() {
        viewModelScope.launch {
            _state.value = SongState.Loading
            try {
                Log.d("SongViewModel", "📢 Đang gọi API lấy danh sách bài hát...")
                val allSongs = repository.getSongs()
                Log.d("SongViewModel", "✅ API trả về: $allSongs")

                if (allSongs != null && allSongs.isNotEmpty()) {
                    // Get 10 random songs
                    val randomSongs = allSongs.shuffled().take(10)

                    // Categorize songs based on their properties
                    // For demonstration, we'll categorize randomly based on ID
                    // In a real app, you'd categorize based on actual genres or moods
                    val heartbreakSongs = allSongs.filter { it.id.hashCode() % 4 == 0 }
                    val cheerfulSongs = allSongs.filter { it.id.hashCode() % 4 == 1 }
                    val relaxingSongs = allSongs.filter { it.id.hashCode() % 4 == 2 }
                    val reflectiveSongs = allSongs.filter { it.id.hashCode() % 4 == 3 }

                    _state.value = SongState.Success(
                        randomSongs = randomSongs,
                        heartbreakSongs = heartbreakSongs,
                        cheerfulSongs = cheerfulSongs,
                        relaxingSongs = relaxingSongs,
                        reflectiveSongs = reflectiveSongs
                    )
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