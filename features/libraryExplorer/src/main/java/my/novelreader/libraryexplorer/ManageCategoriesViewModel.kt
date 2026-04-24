package my.novelreader.libraryexplorer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import my.novelreader.data.CategoriesRepository
import my.novelreader.feature.local_database.tables.Category
import javax.inject.Inject

@HiltViewModel
class ManageCategoriesViewModel @Inject constructor(
    private val categoriesRepository: CategoriesRepository,
) : ViewModel() {

    var categories by mutableStateOf<List<Category>>(emptyList())
        private set

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categories = categoriesRepository.getAllCategories()
        }
    }

    suspend fun addCategory(name: String) {
        val newId = (categories.maxOfOrNull { it.id } ?: 0) + 1
        val newCategory = Category(
            id = newId,
            name = name,
            sortOrder = categories.size
        )
        categoriesRepository.createCategory(newCategory)
        loadCategories()
    }

    suspend fun updateCategory(category: Category) {
        categoriesRepository.updateCategory(category)
        loadCategories()
    }

    suspend fun deleteCategory(categoryId: Int) {
        categoriesRepository.deleteCategory(categoryId)
        loadCategories()
    }

    suspend fun reorderCategories(orderedCategoryIds: List<Int>) {
        orderedCategoryIds.forEachIndexed { index, categoryId ->
            val category = categories.find { it.id == categoryId } ?: return@forEachIndexed
            categoriesRepository.updateCategory(category.copy(sortOrder = index))
        }
        loadCategories()
    }
}
